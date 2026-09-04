package com.marketpulse.market.service;

import com.marketpulse.common.response.PageResponse;
import com.marketpulse.market.dto.MarketQuoteDto;
import com.marketpulse.market.provider.MarketDataProvider;
import com.marketpulse.market.provider.MockMarketDataProvider;
import com.marketpulse.market.provider.StockMetadata;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class StockCatalogService {

    private static final Logger log = LoggerFactory.getLogger(StockCatalogService.class);

    public record CatalogStock(String symbol, String name, String sector) {
    }

    private static final Duration UNIVERSE_REFRESH_INTERVAL = Duration.ofHours(24);

    private final MarketDataProvider marketDataProvider;
    private final MockMarketDataProvider mockMarketDataProvider;
    private final MarketDataService marketDataService;

    private final AtomicReference<List<StockMetadata>> cachedUniverse = new AtomicReference<>(List.of());
    private final AtomicReference<Instant> lastUniverseRefresh = new AtomicReference<>(Instant.EPOCH);

    public StockCatalogService(
            MarketDataProvider marketDataProvider,
            MockMarketDataProvider mockMarketDataProvider,
            MarketDataService marketDataService
    ) {
        this.marketDataProvider = marketDataProvider;
        this.mockMarketDataProvider = mockMarketDataProvider;
        this.marketDataService = marketDataService;
    }

    public synchronized List<StockMetadata> getUniverse() {
        Instant now = Instant.now();
        List<StockMetadata> current = cachedUniverse.get();
        if (current.isEmpty() || Duration.between(lastUniverseRefresh.get(), now).compareTo(UNIVERSE_REFRESH_INTERVAL) > 0) {
            List<StockMetadata> fetched = List.of();
            try {
                fetched = marketDataProvider.getStockUniverse();
            } catch (Exception ex) {
                log.warn("Failed to fetch universe from primary provider: {}", ex.getMessage());
            }

            if (fetched == null || fetched.isEmpty()) {
                // Fallback to mock provider universe (60+ stocks)
                log.info("Using fallback mock universe for stock catalog");
                fetched = mockMarketDataProvider.getStockUniverse();
            }

            cachedUniverse.set(fetched);
            lastUniverseRefresh.set(now);
            log.info("Stock catalog universe initialized/refreshed with {} stocks", fetched.size());
            return fetched;
        }
        return current;
    }

    public List<CatalogStock> getCatalog() {
        return getUniverse().stream()
                .map(m -> new CatalogStock(m.symbol(), m.description(), m.exchange()))
                .toList();
    }

    public Optional<CatalogStock> findBySymbol(String symbol) {
        if (symbol == null) {
            return Optional.empty();
        }
        String clean = symbol.trim().toUpperCase(Locale.ROOT);
        return getUniverse().stream()
                .filter(s -> s.symbol().equalsIgnoreCase(clean))
                .findFirst()
                .map(m -> new CatalogStock(m.symbol(), m.description(), m.exchange()));
    }

    public PageResponse<MarketQuoteDto> getPaginatedQuotes(int page, int size, String query) {
        String cleanQuery = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";
        List<StockMetadata> universe = getUniverse();

        List<StockMetadata> filtered = universe.stream()
                .filter(stock -> cleanQuery.isEmpty()
                        || stock.symbol().toLowerCase(Locale.ROOT).contains(cleanQuery)
                        || (stock.description() != null && stock.description().toLowerCase(Locale.ROOT).contains(cleanQuery))
                        || (stock.displaySymbol() != null && stock.displaySymbol().toLowerCase(Locale.ROOT).contains(cleanQuery)))
                .toList();

        int totalElements = filtered.size();
        int sanitizedPage = Math.max(0, page);
        int sanitizedSize = (size <= 0) ? 25 : Math.min(size, 100);
        int fromIndex = Math.min(sanitizedPage * sanitizedSize, totalElements);
        int toIndex = Math.min(fromIndex + sanitizedSize, totalElements);

        List<StockMetadata> slice = filtered.subList(fromIndex, toIndex);

        List<MarketQuoteDto> quotes = slice.parallelStream()
                .map(meta -> {
                    try {
                        MarketQuoteDto quote = marketDataService.getQuote(meta.symbol());
                        if (quote.companyName() == null || quote.companyName().isBlank() || quote.companyName().equalsIgnoreCase(meta.symbol())) {
                            return new MarketQuoteDto(
                                    quote.symbol(),
                                    meta.description() != null && !meta.description().isBlank() ? meta.description() : meta.symbol(),
                                    quote.price(),
                                    quote.changeAmount(),
                                    quote.changePercent(),
                                    quote.dayHigh(),
                                    quote.dayLow(),
                                    quote.open(),
                                    quote.previousClose(),
                                    quote.volume(),
                                    quote.currency() != null ? quote.currency() : (meta.currency() != null ? meta.currency() : "USD"),
                                    quote.exchange() != null ? quote.exchange() : (meta.exchange() != null ? meta.exchange() : "US"),
                                    quote.capturedAt(),
                                    quote.status(),
                                    quote.message()
                            );
                        }
                        return quote;
                    } catch (Exception ex) {
                        log.warn("Failed fetching quote for {}: {}", meta.symbol(), ex.getMessage());
                        return MarketQuoteDto.unavailable(meta.symbol(), "Temporarily unavailable");
                    }
                })
                .toList();

        return PageResponse.of(quotes, sanitizedPage, sanitizedSize, totalElements);
    }

    public List<MarketQuoteDto> getCatalogQuotes(String query) {
        return getPaginatedQuotes(0, 100, query).content();
    }
}
