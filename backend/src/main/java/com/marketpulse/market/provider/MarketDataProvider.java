package com.marketpulse.market.provider;

import java.util.Optional;

public interface MarketDataProvider {
    Optional<MarketQuote> getLatestQuote(String symbol);
}
