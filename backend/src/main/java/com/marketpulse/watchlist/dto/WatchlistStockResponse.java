package com.marketpulse.watchlist.dto;

import com.marketpulse.market.dto.MarketQuoteDto;
import java.time.Instant;

public record WatchlistStockResponse(
        String symbol,
        Instant addedAt,
        MarketQuoteDto quote
) {
}
