package com.marketpulse.market.provider;

import com.marketpulse.market.dto.StockHistoryResponse;
import java.util.List;
import java.util.Optional;

public interface MarketDataProvider {
    Optional<MarketQuote> getLatestQuote(String symbol);

    default List<StockMetadata> getStockUniverse() {
        return List.of();
    }

    default Optional<StockHistoryResponse> getStockHistory(String symbol, String range) {
        return Optional.empty();
    }
}
