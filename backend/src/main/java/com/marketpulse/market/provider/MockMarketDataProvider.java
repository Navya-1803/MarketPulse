package com.marketpulse.market.provider;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public class MockMarketDataProvider implements MarketDataProvider {

    private static final Map<String, CompanySeed> SEEDS = new LinkedHashMap<>();

    static {
        // Tech & Cloud
        register("NVDA", "NVIDIA Corporation", 178.42, 48_200_000, "NASDAQ");
        register("AAPL", "Apple Inc.", 231.21, 52_100_000, "NASDAQ");
        register("MSFT", "Microsoft Corporation", 511.30, 22_400_000, "NASDAQ");
        register("GOOGL", "Alphabet Inc.", 214.10, 28_900_000, "NASDAQ");
        register("AMZN", "Amazon.com Inc.", 186.50, 41_000_000, "NASDAQ");
        register("META", "Meta Platforms, Inc.", 512.40, 18_700_000, "NASDAQ");
        register("TSLA", "Tesla, Inc.", 340.10, 89_000_000, "NASDAQ");
        register("NFLX", "Netflix, Inc.", 701.20, 4_200_000, "NASDAQ");
        register("AMD", "Advanced Micro Devices, Inc.", 148.50, 45_000_000, "NASDAQ");
        register("INTC", "Intel Corporation", 22.80, 58_000_000, "NASDAQ");
        register("CRM", "Salesforce, Inc.", 312.40, 6_800_000, "NYSE");
        register("ORCL", "Oracle Corporation", 168.20, 11_300_000, "NYSE");
        register("ADBE", "Adobe Inc.", 495.10, 3_900_000, "NASDAQ");
        register("CSCO", "Cisco Systems, Inc.", 56.70, 21_000_000, "NASDAQ");
        register("QCOM", "QUALCOMM Incorporated", 162.80, 8_900_000, "NASDAQ");
        register("AVGO", "Broadcom Inc.", 172.90, 14_100_000, "NASDAQ");
        register("IBM", "International Business Machines", 224.50, 4_800_000, "NYSE");
        register("NOW", "ServiceNow, Inc.", 945.00, 1_600_000, "NYSE");
        register("UBER", "Uber Technologies, Inc.", 74.30, 19_500_000, "NYSE");
        register("ABNB", "Airbnb, Inc.", 134.80, 5_400_000, "NASDAQ");

        // Finance & Payments
        register("JPM", "JPMorgan Chase & Co.", 218.40, 9_500_000, "NYSE");
        register("V", "Visa Inc.", 284.10, 6_200_000, "NYSE");
        register("MA", "Mastercard Incorporated", 488.70, 2_800_000, "NYSE");
        register("BAC", "Bank of America Corporation", 40.25, 34_000_000, "NYSE");
        register("WFC", "Wells Fargo & Company", 58.60, 18_400_000, "NYSE");
        register("GS", "The Goldman Sachs Group, Inc.", 492.30, 2_100_000, "NYSE");
        register("MS", "Morgan Stanley", 102.10, 7_600_000, "NYSE");
        register("AXP", "American Express Company", 256.40, 3_100_000, "NYSE");
        register("BLK", "BlackRock, Inc.", 962.80, 850_000, "NYSE");
        register("PYPL", "PayPal Holdings, Inc.", 76.50, 12_800_000, "NASDAQ");

        // Consumer, Retail & Entertainment
        register("WMT", "Walmart Inc.", 78.90, 16_500_000, "NYSE");
        register("COST", "Costco Wholesale Corporation", 894.20, 2_300_000, "NASDAQ");
        register("TGT", "Target Corporation", 152.40, 4_100_000, "NYSE");
        register("HD", "The Home Depot, Inc.", 398.60, 3_800_000, "NYSE");
        register("NKE", "NIKE, Inc.", 84.10, 10_200_000, "NYSE");
        register("MCD", "McDonald's Corporation", 298.50, 2_900_000, "NYSE");
        register("SBUX", "Starbucks Corporation", 95.80, 7_300_000, "NASDAQ");
        register("KO", "The Coca-Cola Company", 68.40, 13_200_000, "NYSE");
        register("PEP", "PepsiCo, Inc.", 174.10, 5_600_000, "NASDAQ");
        register("DIS", "The Walt Disney Company", 96.30, 9_800_000, "NYSE");

        // Healthcare & Biotech
        register("LLY", "Eli Lilly and Company", 924.50, 3_100_000, "NYSE");
        register("JNJ", "Johnson & Johnson", 162.30, 7_400_000, "NYSE");
        register("UNH", "UnitedHealth Group Incorporated", 572.10, 3_200_000, "NYSE");
        register("ABBV", "AbbVie Inc.", 194.80, 4_600_000, "NYSE");
        register("MRK", "Merck & Co., Inc.", 118.20, 8_100_000, "NYSE");
        register("PFE", "Pfizer Inc.", 29.40, 28_000_000, "NYSE");
        register("TMO", "Thermo Fisher Scientific Inc.", 596.30, 1_400_000, "NYSE");
        register("ABT", "Abbott Laboratories", 114.70, 5_800_000, "NYSE");
        register("AMGN", "Amgen Inc.", 324.90, 2_200_000, "NASDAQ");
        register("ISRG", "Intuitive Surgical, Inc.", 472.60, 1_700_000, "NASDAQ");

        // Energy, Industrials & Materials
        register("XOM", "Exxon Mobil Corporation", 118.40, 14_500_000, "NYSE");
        register("CVX", "Chevron Corporation", 146.90, 8_100_000, "NYSE");
        register("COP", "ConocoPhillips", 108.30, 6_200_000, "NYSE");
        register("CAT", "Caterpillar Inc.", 386.40, 3_400_000, "NYSE");
        register("BA", "The Boeing Company", 158.20, 7_900_000, "NYSE");
        register("GE", "General Electric Company", 188.50, 5_300_000, "NYSE");
        register("HON", "Honeywell International Inc.", 208.70, 2_800_000, "NASDAQ");
        register("UNP", "Union Pacific Corporation", 242.10, 2_400_000, "NYSE");
        register("UPS", "United Parcel Service, Inc.", 132.80, 4_100_000, "NYSE");
        register("LMT", "Lockheed Martin Corporation", 574.30, 1_100_000, "NYSE");
    }

    private static void register(String symbol, String name, double basePrice, long baseVolume, String exchange) {
        SEEDS.put(symbol, new CompanySeed(name, basePrice, baseVolume, exchange));
    }

    @Override
    public List<StockMetadata> getStockUniverse() {
        List<StockMetadata> list = new ArrayList<>(SEEDS.size());
        for (Map.Entry<String, CompanySeed> entry : SEEDS.entrySet()) {
            list.add(new StockMetadata(
                    entry.getKey(),
                    entry.getKey(),
                    entry.getValue().name(),
                    "Common Stock",
                    "USD",
                    null,
                    entry.getValue().exchange()
            ));
        }
        return list;
    }

    @Override
    public Optional<MarketQuote> getLatestQuote(String symbol) {
        String key = symbol.toUpperCase(Locale.ROOT);
        CompanySeed seed = SEEDS.getOrDefault(key, new CompanySeed(key + " Inc.", 80 + Math.abs(key.hashCode() % 120), 8_000_000L, "US"));
        double wave = Math.sin((Instant.now().getEpochSecond() / 180.0) + key.hashCode());
        double extraMove = switch (key) {
            case "NVDA" -> -0.062;
            case "TSLA" -> 0.051;
            case "AAPL" -> 0.008;
            default -> wave * 0.018;
        };
        double price = seed.basePrice() * (1 + extraMove + wave * 0.004);
        double open = seed.basePrice();
        double previousClose = open * 0.995;
        double changeAmount = price - previousClose;
        double changePercent = (changeAmount / previousClose) * 100;
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
                scale(open),
                scale(previousClose),
                volume,
                "USD",
                seed.exchange(),
                Instant.now(),
                MarketStatus.DELAYED
        ));
    }

    @Override
    public Optional<com.marketpulse.market.dto.StockHistoryResponse> getStockHistory(String symbol, String range) {
        String key = symbol.toUpperCase(Locale.ROOT);
        CompanySeed seed = SEEDS.getOrDefault(key, new CompanySeed(key + " Inc.", 80 + Math.abs(key.hashCode() % 120), 8_000_000L, "US"));
        String cleanRange = (range != null && !range.isBlank()) ? range.toUpperCase(Locale.ROOT) : "1D";

        int count;
        long stepSeconds;
        switch (cleanRange) {
            case "1W" -> {
                count = 28;
                stepSeconds = 6 * 3600;
            }
            case "1M" -> {
                count = 30;
                stepSeconds = 24 * 3600;
            }
            case "3M" -> {
                count = 90;
                stepSeconds = 24 * 3600;
            }
            default -> { // "1D"
                count = 24;
                stepSeconds = 3600;
            }
        }

        Instant now = Instant.now();
        List<com.marketpulse.market.dto.StockHistoryPointDto> points = new ArrayList<>(count);
        double currentPrice = seed.basePrice();

        for (int i = count - 1; i >= 0; i--) {
            Instant timestamp = now.minusSeconds(i * stepSeconds);
            double progress = (double) (count - 1 - i) / count;
            double wave = Math.sin((progress * Math.PI * 4) + key.hashCode() * 0.1);
            double noise = ((Math.abs((key.hashCode() + i * 31) % 100)) - 50) / 1500.0;
            double priceAtTime = seed.basePrice() * (1.0 + (wave * 0.035) + noise + (progress * 0.02));
            double open = priceAtTime * 0.998;
            double high = Math.max(open, priceAtTime) * 1.006;
            double low = Math.min(open, priceAtTime) * 0.994;
            long volume = Math.round(seed.baseVolume() / (double) count * (0.8 + Math.abs(wave) * 0.4));

            points.add(new com.marketpulse.market.dto.StockHistoryPointDto(
                    timestamp,
                    scale(priceAtTime),
                    scale(open),
                    scale(high),
                    scale(low),
                    volume
            ));
        }

        return Optional.of(new com.marketpulse.market.dto.StockHistoryResponse(key, cleanRange, points));
    }

    private BigDecimal scale(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private record CompanySeed(String name, double basePrice, long baseVolume, String exchange) {
    }
}
