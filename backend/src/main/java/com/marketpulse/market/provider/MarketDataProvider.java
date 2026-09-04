package com.marketpulse.market.provider;

import java.util.List;
import java.util.Optional;

public interface MarketDataProvider {
    Optional<MarketQuote> getLatestQuote(String symbol);

    default List<StockMetadata> getStockUniverse() {
        return List.of();
    }
}
