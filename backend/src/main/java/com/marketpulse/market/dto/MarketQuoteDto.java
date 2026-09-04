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
        BigDecimal open,
        BigDecimal previousClose,
        Long volume,
        String currency,
        String exchange,
        Instant capturedAt,
        MarketStatus status,
        String message
) {
    public MarketQuoteDto(
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
        this(symbol, companyName, price, changeAmount, changePercent, dayHigh, dayLow, null, null, volume, "USD", "US", capturedAt, status, message);
    }

    public static MarketQuoteDto unavailable(String symbol, String message) {
        return new MarketQuoteDto(symbol, null, null, null, null, null, null, null, null, null, "USD", "US", Instant.now(), MarketStatus.UNAVAILABLE, message);
    }
}
