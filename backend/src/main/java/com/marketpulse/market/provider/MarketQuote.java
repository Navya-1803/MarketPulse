package com.marketpulse.market.provider;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketQuote(
        String symbol,
        String companyName,
        BigDecimal price,
        BigDecimal changeAmount,
        BigDecimal changePercent,
        BigDecimal dayHigh,
        BigDecimal dayLow,
        BigDecimal open,
        BigDecimal previousClose,
        Long volume,
        String currency,
        String exchange,
        Instant capturedAt,
        MarketStatus status
) {
    public MarketQuote(
            String symbol,
            String companyName,
            BigDecimal price,
            BigDecimal changeAmount,
            BigDecimal changePercent,
            BigDecimal dayHigh,
            BigDecimal dayLow,
            Long volume,
            Instant capturedAt,
            MarketStatus status
    ) {
        this(symbol, companyName, price, changeAmount, changePercent, dayHigh, dayLow, null, null, volume, "USD", "US", capturedAt, status);
    }
}
