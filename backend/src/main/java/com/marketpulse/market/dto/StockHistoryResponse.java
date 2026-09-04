package com.marketpulse.market.dto;

import java.util.List;

public record StockHistoryResponse(
        String symbol,
        String range,
        List<StockHistoryPointDto> points
) {
}
