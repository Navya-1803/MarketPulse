package com.marketpulse.market.cache;

import com.marketpulse.market.provider.MarketQuote;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryQuoteCache implements QuoteCache {

    private record CachedEntry(MarketQuote quote, Instant expiresAt) {
        boolean isExpired(Instant now) {
            return now.isAfter(expiresAt);
        }
    }

    private final Map<String, CachedEntry> cache = new ConcurrentHashMap<>();

    @Override
    public Optional<MarketQuote> get(String symbol) {
        if (symbol == null) {
            return Optional.empty();
        }
        CachedEntry entry = cache.get(symbol.toUpperCase());
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.isExpired(Instant.now())) {
            cache.remove(symbol.toUpperCase());
            return Optional.empty();
        }
        return Optional.ofNullable(entry.quote());
    }

    @Override
    public void put(String symbol, MarketQuote quote, Duration ttl) {
        if (symbol == null || quote == null) {
            return;
        }
        Instant expiresAt = Instant.now().plus(ttl != null ? ttl : Duration.ofSeconds(30));
        cache.put(symbol.toUpperCase(), new CachedEntry(quote, expiresAt));
    }

    @Override
    public void evict(String symbol) {
        if (symbol != null) {
            cache.remove(symbol.toUpperCase());
        }
    }

    @Override
    public void clear() {
        cache.clear();
    }
}
