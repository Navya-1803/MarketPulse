package com.marketpulse.market.service;

import com.marketpulse.common.config.MarketProperties;
import com.marketpulse.market.cache.InMemoryQuoteCache;
import com.marketpulse.market.cache.QuoteCache;
import com.marketpulse.market.dto.MarketQuoteDto;
import com.marketpulse.market.entity.MarketSnapshot;
import com.marketpulse.market.provider.MarketDataProvider;
import com.marketpulse.market.provider.MarketQuote;
import com.marketpulse.market.provider.MarketStatus;
import com.marketpulse.market.repository.MarketSnapshotRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MarketDataService {

    private final MarketDataProvider marketDataProvider;
    private final MarketProperties properties;
    private final MarketSnapshotRepository snapshotRepository;
    private final QuoteCache quoteCache;

    @Autowired
    public MarketDataService(
            MarketDataProvider marketDataProvider,
            MarketProperties properties,
            MarketSnapshotRepository snapshotRepository,
            QuoteCache quoteCache
    ) {
        this.marketDataProvider = marketDataProvider;
        this.properties = properties;
        this.snapshotRepository = snapshotRepository;
        this.quoteCache = quoteCache;
    }

    public MarketDataService(
            MarketDataProvider marketDataProvider,
            MarketProperties properties,
            MarketSnapshotRepository snapshotRepository
    ) {
        this(marketDataProvider, properties, snapshotRepository, new InMemoryQuoteCache());
    }

    public MarketQuoteDto getQuote(String symbol) {
        return toDto(symbol, fetchQuote(symbol));
    }

    public Map<String, MarketQuoteDto> getQuotes(List<String> symbols) {
        Map<String, MarketQuoteDto> result = new LinkedHashMap<>();
        for (String symbol : symbols) {
            result.put(symbol, getQuote(symbol));
        }
        return result;
    }

    public Optional<com.marketpulse.market.dto.StockHistoryResponse> getStockHistory(String symbol, String range) {
        return marketDataProvider.getStockHistory(symbol, range);
    }

    public Optional<MarketQuote> fetchQuote(String symbol) {
        Optional<MarketQuote> cached = quoteCache.get(symbol);
        if (cached.isPresent()) {
            return cached;
        }
        Optional<MarketQuote> quote;
        try {
            quote = marketDataProvider.getLatestQuote(symbol);
        } catch (Exception ex) {
            quote = Optional.empty();
        }
        quote.ifPresent(q -> quoteCache.put(symbol, q, Duration.ofSeconds(properties.cacheTtlSeconds())));
        return quote;
    }

    public MarketQuoteDto toDto(String symbol, Optional<MarketQuote> quote) {
        if (quote.isEmpty()) {
            return MarketQuoteDto.unavailable(symbol, "Temporarily unavailable from the market data provider");
        }
        MarketQuote q = quote.get();
        MarketStatus status = q.status();
        if (Duration.between(q.capturedAt(), Instant.now()).toMinutes() >= properties.staleAfterMinutes()) {
            status = MarketStatus.STALE;
        }
        return new MarketQuoteDto(
                q.symbol(),
                q.companyName(),
                q.price(),
                q.changeAmount(),
                q.changePercent(),
                q.dayHigh(),
                q.dayLow(),
                q.open(),
                q.previousClose(),
                q.volume(),
                q.currency() != null ? q.currency() : "USD",
                q.exchange() != null ? q.exchange() : "US",
                q.capturedAt(),
                status,
                status == MarketStatus.STALE ? "Data is older than the freshness threshold" : null
        );
    }

    public void persistSnapshots(Long userId, List<String> symbols) {
        Instant now = Instant.now();
        for (String symbol : symbols) {
            Optional<MarketQuote> quote = fetchQuote(symbol);
            if (quote.isEmpty()) {
                continue;
            }
            MarketQuote q = quote.get();
            MarketSnapshot snapshot = new MarketSnapshot();
            snapshot.setUserId(userId);
            snapshot.setSymbol(symbol);
            snapshot.setPrice(q.price());
            snapshot.setChangePercent(q.changePercent());
            snapshot.setVolume(q.volume());
            snapshot.setDayHigh(q.dayHigh());
            snapshot.setDayLow(q.dayLow());
            snapshot.setMarketStatus(q.status());
            snapshot.setCapturedAt(now);
            snapshotRepository.save(snapshot);
        }
    }

    public Optional<MarketSnapshot> lastSnapshot(Long userId, String symbol) {
        return snapshotRepository.findTopByUserIdAndSymbolOrderByCapturedAtDesc(userId, symbol);
    }
}
