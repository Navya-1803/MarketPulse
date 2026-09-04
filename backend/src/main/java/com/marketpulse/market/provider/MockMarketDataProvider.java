package com.marketpulse.market.provider;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public class MockMarketDataProvider implements MarketDataProvider {

    private static final Map<String, CompanySeed> SEEDS = Map.ofEntries(
            Map.entry("NVDA", new CompanySeed("NVIDIA Corporation", 178.42, 48_200_000)),
            Map.entry("AAPL", new CompanySeed("Apple Inc.", 231.21, 52_100_000)),
            Map.entry("MSFT", new CompanySeed("Microsoft Corporation", 511.30, 22_400_000)),
            Map.entry("GOOGL", new CompanySeed("Alphabet Inc.", 214.10, 28_900_000)),
            Map.entry("AMZN", new CompanySeed("Amazon.com Inc.", 186.50, 41_000_000)),
            Map.entry("TSLA", new CompanySeed("Tesla, Inc.", 340.10, 89_000_000)),
            Map.entry("META", new CompanySeed("Meta Platforms, Inc.", 512.40, 18_700_000)),
            Map.entry("NFLX", new CompanySeed("Netflix, Inc.", 701.20, 4_200_000))
    );

    @Override
    public Optional<MarketQuote> getLatestQuote(String symbol) {
        String key = symbol.toUpperCase();
        CompanySeed seed = SEEDS.getOrDefault(key, new CompanySeed(key + " Inc.", 80 + Math.abs(key.hashCode() % 120), 8_000_000L));
        double wave = Math.sin((Instant.now().getEpochSecond() / 180.0) + key.hashCode());
        double extraMove = switch (key) {
            case "NVDA" -> -0.062;
            case "TSLA" -> 0.051;
            case "AAPL" -> 0.008;
            default -> wave * 0.018;
        };
        double price = seed.basePrice() * (1 + extraMove + wave * 0.004);
        double open = seed.basePrice();
        double changeAmount = price - open;
        double changePercent = (changeAmount / open) * 100;
        double high = Math.max(open, price) * 1.012;
        double low = Math.min(open, price) * 0.988;
        long volume = Math.round(seed.baseVolume() * (1 + Math.abs(extraMove) * 8 + ThreadLocalRandom.current().nextDouble(0, 0.15)));

        return Optional.of(new MarketQuote(
                key,
                seed.name(),
                scale(price),
                scale(changeAmount),
                scale(changePercent),
                scale(high),
                scale(low),
                volume,
                Instant.now(),
                MarketStatus.DELAYED
        ));
    }

    private BigDecimal scale(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private record CompanySeed(String name, double basePrice, long baseVolume) {
    }
}
