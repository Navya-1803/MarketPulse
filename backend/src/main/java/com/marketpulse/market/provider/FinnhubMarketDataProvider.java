package com.marketpulse.market.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.marketpulse.common.config.MarketProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class FinnhubMarketDataProvider implements MarketDataProvider {

    private static final Logger log = LoggerFactory.getLogger(FinnhubMarketDataProvider.class);

    private final MarketProperties properties;
    private final RestClient restClient;
    private final Map<String, StockMetadata> metadataCache = new ConcurrentHashMap<>();

    public FinnhubMarketDataProvider(MarketProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(4000);
        factory.setReadTimeout(5000);
        this.restClient = RestClient.builder()
                .baseUrl(properties.finnhub().baseUrl())
                .requestFactory(factory)
                .build();
    }

    @Override
    public List<StockMetadata> getStockUniverse() {
        String apiKey = properties.finnhub().apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return List.of();
        }
        try {
            FinnhubSymbolDto[] symbols = restClient.get()
                    .uri(uri -> uri.path("/stock/symbol")
                            .queryParam("exchange", "US")
                            .queryParam("token", apiKey)
                            .build())
                    .retrieve()
                    .body(FinnhubSymbolDto[].class);

            if (symbols == null || symbols.length == 0) {
                return List.of();
            }

            List<StockMetadata> universe = new ArrayList<>();
            for (FinnhubSymbolDto s : symbols) {
                if (s.symbol() == null || s.symbol().isBlank()) {
                    continue;
                }
                // Filter for common stock / equities with USD currency
                String type = s.type() != null ? s.type() : "";
                boolean isCommonStock = type.equalsIgnoreCase("Common Stock")
                        || type.equalsIgnoreCase("EQS")
                        || type.isBlank();

                boolean isUsd = s.currency() == null || s.currency().equalsIgnoreCase("USD");

                // Filter out warrants, rights, preferred shares with dots/special symbols if needed
                String sym = s.symbol().trim();
                boolean isCleanSymbol = !sym.contains(".") && !sym.contains("+") && !sym.contains("*") && !sym.contains("^") && sym.length() <= 5;

                if (isCommonStock && isUsd && isCleanSymbol) {
                    StockMetadata meta = new StockMetadata(
                            sym.toUpperCase(Locale.ROOT),
                            s.displaySymbol() != null ? s.displaySymbol() : sym,
                            s.description() != null ? s.description() : sym,
                            s.type() != null ? s.type() : "Common Stock",
                            s.currency() != null ? s.currency() : "USD",
                            s.mic(),
                            "US"
                    );
                    universe.add(meta);
                    metadataCache.put(meta.symbol(), meta);
                }
            }
            log.info("Fetched {} US common stocks from Finnhub symbol universe", universe.size());
            return universe;
        } catch (Exception ex) {
            log.warn("Failed to fetch Finnhub stock universe: {}", ex.getMessage());
            return List.of();
        }
    }

    @Override
    public Optional<MarketQuote> getLatestQuote(String symbol) {
        String apiKey = properties.finnhub().apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        String cleanSymbol = symbol.toUpperCase(Locale.ROOT);
        try {
            QuoteResponse quote = restClient.get()
                    .uri(uri -> uri.path("/quote").queryParam("symbol", cleanSymbol).queryParam("token", apiKey).build())
                    .retrieve()
                    .body(QuoteResponse.class);

            if (quote == null || quote.c() == null || quote.c() == 0) {
                return Optional.empty();
            }

            String companyName = cleanSymbol;
            StockMetadata cachedMeta = metadataCache.get(cleanSymbol);
            if (cachedMeta != null && cachedMeta.description() != null && !cachedMeta.description().isBlank()) {
                companyName = cachedMeta.description();
            } else {
                try {
                    ProfileResponse profile = restClient.get()
                            .uri(uri -> uri.path("/stock/profile2").queryParam("symbol", cleanSymbol).queryParam("token", apiKey).build())
                            .retrieve()
                            .body(ProfileResponse.class);
                    if (profile != null && profile.name() != null && !profile.name().isBlank()) {
                        companyName = profile.name();
                    }
                } catch (Exception ignored) {
                    // Fall back to symbol as name if profile fails
                }
            }

            Instant capturedAt = quote.t() != null && quote.t() > 0 ? Instant.ofEpochSecond(quote.t()) : Instant.now();
            return Optional.of(new MarketQuote(
                    cleanSymbol,
                    companyName,
                    scale(quote.c()),
                    scale(quote.d()),
                    scale(quote.dp()),
                    scale(quote.h()),
                    scale(quote.l()),
                    scale(quote.o()),
                    scale(quote.pc()),
                    null,
                    "USD",
                    "US",
                    capturedAt,
                    MarketStatus.DELAYED
            ));
        } catch (Exception ex) {
            log.warn("Finnhub quote failed or rate-limited for {}: {}", cleanSymbol, ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<com.marketpulse.market.dto.StockHistoryResponse> getStockHistory(String symbol, String range) {
        String apiKey = properties.finnhub().apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        String cleanSymbol = symbol.toUpperCase(Locale.ROOT);
        String cleanRange = (range != null && !range.isBlank()) ? range.toUpperCase(Locale.ROOT) : "1D";

        long to = Instant.now().getEpochSecond();
        long from;
        String resolution;

        switch (cleanRange) {
            case "1W" -> {
                from = to - (7 * 24 * 3600);
                resolution = "60";
            }
            case "1M" -> {
                from = to - (30 * 24 * 3600);
                resolution = "D";
            }
            case "3M" -> {
                from = to - (90 * 24 * 3600);
                resolution = "D";
            }
            default -> { // "1D"
                from = to - (24 * 3600);
                resolution = "15";
            }
        }

        try {
            CandleResponse candle = restClient.get()
                    .uri(uri -> uri.path("/stock/candle")
                            .queryParam("symbol", cleanSymbol)
                            .queryParam("resolution", resolution)
                            .queryParam("from", from)
                            .queryParam("to", to)
                            .queryParam("token", apiKey)
                            .build())
                    .retrieve()
                    .body(CandleResponse.class);

            if (candle == null || !"ok".equalsIgnoreCase(candle.s()) || candle.t() == null || candle.t().isEmpty()) {
                return Optional.empty();
            }

            List<com.marketpulse.market.dto.StockHistoryPointDto> points = new ArrayList<>();
            int size = candle.t().size();
            for (int i = 0; i < size; i++) {
                Instant timestamp = Instant.ofEpochSecond(candle.t().get(i));
                Double close = (candle.c() != null && candle.c().size() > i) ? candle.c().get(i) : null;
                Double open = (candle.o() != null && candle.o().size() > i) ? candle.o().get(i) : close;
                Double high = (candle.h() != null && candle.h().size() > i) ? candle.h().get(i) : close;
                Double low = (candle.l() != null && candle.l().size() > i) ? candle.l().get(i) : close;
                Long volume = (candle.v() != null && candle.v().size() > i) ? candle.v().get(i) : null;

                if (close != null) {
                    points.add(new com.marketpulse.market.dto.StockHistoryPointDto(
                            timestamp,
                            scale(close),
                            scale(open),
                            scale(high),
                            scale(low),
                            volume
                    ));
                }
            }

            return Optional.of(new com.marketpulse.market.dto.StockHistoryResponse(cleanSymbol, cleanRange, points));
        } catch (Exception ex) {
            log.warn("Finnhub candle query failed for {}: {}", cleanSymbol, ex.getMessage());
            return Optional.empty();
        }
    }

    private BigDecimal scale(Double value) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QuoteResponse(Double c, Double d, Double dp, Double h, Double l, Double o, Double pc, Long t) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProfileResponse(String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CandleResponse(
            List<Double> c,
            List<Double> h,
            List<Double> l,
            List<Double> o,
            String s,
            List<Long> t,
            List<Long> v
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FinnhubSymbolDto(
            String symbol,
            String displaySymbol,
            String description,
            String type,
            String currency,
            String mic
    ) {
    }
}
