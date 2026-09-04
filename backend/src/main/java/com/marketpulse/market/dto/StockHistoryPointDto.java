package com.marketpulse.market.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record StockHistoryPointDto(
        Instant timestamp,
        BigDecimal price,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        Long volume
) {
    public StockHistoryPointDto(Instant timestamp, BigDecimal price) {
        this(timestamp, price, price, price, price, null);
    }
}
