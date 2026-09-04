package com.marketpulse.market.config;

import com.marketpulse.market.cache.InMemoryQuoteCache;
import com.marketpulse.market.cache.QuoteCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class QuoteCacheConfig {

    @Bean
    @Primary
    public QuoteCache quoteCache(InMemoryQuoteCache inMemoryQuoteCache) {
        return inMemoryQuoteCache;
    }
}
