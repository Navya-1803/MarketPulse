package com.marketpulse.market.config;

import com.marketpulse.common.config.MarketProperties;
import com.marketpulse.market.provider.FinnhubMarketDataProvider;
import com.marketpulse.market.provider.MarketDataProvider;
import com.marketpulse.market.provider.MockMarketDataProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MarketProviderConfig {

    @Bean
    @Primary
    public MarketDataProvider marketDataProvider(
            MarketProperties properties,
            MockMarketDataProvider mockProvider,
            FinnhubMarketDataProvider finnhubProvider
    ) {
        if ("finnhub".equalsIgnoreCase(properties.provider())
                && properties.finnhub().apiKey() != null
                && !properties.finnhub().apiKey().isBlank()) {
            return finnhubProvider;
        }
        return mockProvider;
    }
}
