package com.marketpulse.watchlist.dto;

import java.time.Instant;
import java.util.List;

public record WatchlistResponse(
        Long id,
        String name,
        Instant createdAt,
        Instant updatedAt,
        int stockCount,
        List<WatchlistStockResponse> stocks
) {
}
