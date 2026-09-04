package com.marketpulse.market.dto;

import com.marketpulse.market.provider.MarketStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record MarketQuoteDto(
        String symbol,
        String companyName,
        BigDecimal price,
        BigDecimal changeAmount,
        BigDecimal changePercent,
        BigDecimal dayHigh,
        BigDecimal dayLow,
        Long volume,
        Instant capturedAt,
        MarketStatus status,
        String message
) {
    public static MarketQuoteDto unavailable(String symbol, String message) {
        return new MarketQuoteDto(symbol, null, null, null, null, null, null, null, Instant.now(), MarketStatus.UNAVAILABLE, message);
    }
}
