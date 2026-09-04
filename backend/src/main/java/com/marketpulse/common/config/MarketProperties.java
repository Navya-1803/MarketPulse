package com.marketpulse.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "market")
public record MarketProperties(
        String provider,
        long cacheTtlSeconds,
        long staleAfterMinutes,
        Finnhub finnhub,
        Change change
) {
    public record Finnhub(String apiKey, String baseUrl) {
    }

    public record Change(
            double notablePercent,
            double significantPercent,
            double criticalPercent,
            double volumeAnomalyMultiplier,
            double nearExtremePercent
    ) {
    }
}
