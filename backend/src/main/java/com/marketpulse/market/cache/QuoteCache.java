package com.marketpulse.market.cache;

import com.marketpulse.market.provider.MarketQuote;
import java.time.Duration;
import java.util.Optional;

/**
 * Cache abstraction for market quotes.
 *
 * Current default implementation is single-instance only; horizontal scaling requires
 * swapping in a distributed implementation (e.g. Redis) — DB writes (watchlists, checkpoints)
 * are already safe for multiple instances via Fix 1's transactional guarantees, so only
 * cache hit-rate is affected, not correctness.
 */
public interface QuoteCache {

    Optional<MarketQuote> get(String symbol);

    void put(String symbol, MarketQuote quote, Duration ttl);

    void evict(String symbol);

    void clear();
}
