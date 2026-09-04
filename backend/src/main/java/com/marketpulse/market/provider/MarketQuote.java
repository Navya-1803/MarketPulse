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
        Long volume,
        Instant capturedAt,
        MarketStatus status
) {
}
