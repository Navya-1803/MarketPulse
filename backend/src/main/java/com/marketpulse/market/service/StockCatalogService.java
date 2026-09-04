package com.marketpulse.market.service;

import com.marketpulse.common.response.PageResponse;
import com.marketpulse.market.dto.MarketQuoteDto;
import com.marketpulse.market.provider.MarketDataProvider;
import com.marketpulse.market.provider.MockMarketDataProvider;
import com.marketpulse.market.provider.StockMetadata;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
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
        return getPaginatedQuotes(page, size, query, "ALL", "symbol", "asc");
    }

    public PageResponse<MarketQuoteDto> getPaginatedQuotes(
            int page,
            int size,
            String query,
            String filter,
            String sortBy,
            String sortDirection
    ) {
        String cleanQuery = query != null ? query.trim().toLowerCase(Locale.ROOT) : "";
        String cleanFilter = filter != null ? filter.trim().toUpperCase(Locale.ROOT) : "ALL";
        String cleanSort = sortBy != null ? sortBy.trim().toLowerCase(Locale.ROOT) : "symbol";
        boolean isDesc = "desc".equalsIgnoreCase(sortDirection);

        List<StockMetadata> universe = getUniverse();

        List<StockMetadata> filteredUniverse = universe.stream()
                .filter(stock -> cleanQuery.isEmpty()
                        || stock.symbol().toLowerCase(Locale.ROOT).contains(cleanQuery)
                        || (stock.description() != null && stock.description().toLowerCase(Locale.ROOT).contains(cleanQuery))
                        || (stock.displaySymbol() != null && stock.displaySymbol().toLowerCase(Locale.ROOT).contains(cleanQuery)))
                .toList();

        boolean needsQuoteEvaluation = !cleanFilter.equals("ALL")
                || cleanSort.equals("price")
                || cleanSort.equals("changepercent")
                || cleanSort.equals("volume");

        int sanitizedPage = Math.max(0, page);
        int sanitizedSize = (size <= 0) ? 25 : Math.min(size, 100);

        if (!needsQuoteEvaluation) {
            // Sort by symbol or name before slicing
            List<StockMetadata> sorted = new ArrayList<>(filteredUniverse);
            Comparator<StockMetadata> comp = cleanSort.equals("name")
                    ? Comparator.comparing(m -> m.description() != null ? m.description() : m.symbol(), String.CASE_INSENSITIVE_ORDER)
                    : Comparator.comparing(StockMetadata::symbol, String.CASE_INSENSITIVE_ORDER);
            if (isDesc) {
                comp = comp.reversed();
            }
            sorted.sort(comp);

            int totalElements = sorted.size();
            int fromIndex = Math.min(sanitizedPage * sanitizedSize, totalElements);
            int toIndex = Math.min(fromIndex + sanitizedSize, totalElements);
            List<StockMetadata> slice = sorted.subList(fromIndex, toIndex);

            List<MarketQuoteDto> quotes = slice.parallelStream()
                    .map(this::hydrateQuote)
                    .toList();

            return PageResponse.of(quotes, sanitizedPage, sanitizedSize, totalElements);
        }

        // When filtering by gainers, losers, volume, or sorting by quote attributes:
        // Limit candidate universe to top 150 to keep quote latency and rate-limits protected
        List<StockMetadata> candidates = filteredUniverse.size() > 150
                ? filteredUniverse.subList(0, 150)
                : filteredUniverse;

        List<MarketQuoteDto> allQuotes = candidates.parallelStream()
                .map(this::hydrateQuote)
                .filter(q -> applyFilter(q, cleanFilter))
                .sorted(buildComparator(cleanSort, isDesc))
                .toList();

        int totalElements = allQuotes.size();
        int fromIndex = Math.min(sanitizedPage * sanitizedSize, totalElements);
        int toIndex = Math.min(fromIndex + sanitizedSize, totalElements);
        List<MarketQuoteDto> slice = allQuotes.subList(fromIndex, toIndex);

        return PageResponse.of(slice, sanitizedPage, sanitizedSize, totalElements);
    }

    private MarketQuoteDto hydrateQuote(StockMetadata meta) {
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
    }

    private boolean applyFilter(MarketQuoteDto quote, String filter) {
        return switch (filter) {
            case "GAINERS" -> quote.changePercent() != null && quote.changePercent().compareTo(BigDecimal.ZERO) > 0;
            case "LOSERS" -> quote.changePercent() != null && quote.changePercent().compareTo(BigDecimal.ZERO) < 0;
            case "HIGH_VOLUME" -> quote.volume() != null && quote.volume() >= 10_000_000L;
            case "NEAR_HIGH" -> isNearExtreme(quote.price(), quote.dayHigh());
            case "NEAR_LOW" -> isNearExtreme(quote.price(), quote.dayLow());
            default -> true;
        };
    }

    private boolean isNearExtreme(BigDecimal price, BigDecimal extreme) {
        if (price == null || extreme == null || extreme.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        BigDecimal diff = price.subtract(extreme).abs();
        BigDecimal percentDiff = diff.divide(extreme, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        return percentDiff.doubleValue() <= 2.0;
    }

    private Comparator<MarketQuoteDto> buildComparator(String sortBy, boolean isDesc) {
        Comparator<MarketQuoteDto> comp = switch (sortBy) {
            case "price" -> Comparator.comparing(q -> q.price() != null ? q.price() : BigDecimal.ZERO);
            case "changepercent" -> Comparator.comparing(q -> q.changePercent() != null ? q.changePercent() : BigDecimal.ZERO);
            case "volume" -> Comparator.comparing(q -> q.volume() != null ? q.volume() : 0L);
            case "name" -> Comparator.comparing(q -> q.companyName() != null ? q.companyName() : q.symbol(), String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(MarketQuoteDto::symbol, String.CASE_INSENSITIVE_ORDER);
        };
        return isDesc ? comp.reversed() : comp;
    }

    public List<MarketQuoteDto> getCatalogQuotes(String query) {
        return getPaginatedQuotes(0, 100, query).content();
    }
}
