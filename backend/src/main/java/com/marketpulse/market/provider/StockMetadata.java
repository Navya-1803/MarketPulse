package com.marketpulse.market.provider;

public record StockMetadata(
        String symbol,
        String displaySymbol,
        String description,
        String type,
        String currency,
        String mic,
        String exchange
) {
    public StockMetadata(String symbol, String description) {
        this(symbol, symbol, description, "Common Stock", "USD", null, "US");
    }
}
