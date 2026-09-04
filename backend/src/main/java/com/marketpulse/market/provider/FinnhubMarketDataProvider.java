package com.marketpulse.market.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.marketpulse.common.config.MarketProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;
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

    public FinnhubMarketDataProvider(MarketProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(4000);
        this.restClient = RestClient.builder()
                .baseUrl(properties.finnhub().baseUrl())
                .requestFactory(factory)
                .build();
    }

    @Override
    public Optional<MarketQuote> getLatestQuote(String symbol) {
        String apiKey = properties.finnhub().apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        try {
            QuoteResponse quote = restClient.get()
                    .uri(uri -> uri.path("/quote").queryParam("symbol", symbol).queryParam("token", apiKey).build())
                    .retrieve()
                    .body(QuoteResponse.class);
            ProfileResponse profile = restClient.get()
                    .uri(uri -> uri.path("/stock/profile2").queryParam("symbol", symbol).queryParam("token", apiKey).build())
                    .retrieve()
                    .body(ProfileResponse.class);
            if (quote == null || quote.c() == null || quote.c() == 0) {
                return Optional.empty();
            }
            String name = profile != null && profile.name() != null ? profile.name() : symbol;
            Instant capturedAt = quote.t() != null && quote.t() > 0 ? Instant.ofEpochSecond(quote.t()) : Instant.now();
            return Optional.of(new MarketQuote(
                    symbol.toUpperCase(),
                    name,
                    scale(quote.c()),
                    scale(quote.d()),
                    scale(quote.dp()),
                    scale(quote.h()),
                    scale(quote.l()),
                    null,
                    capturedAt,
                    MarketStatus.DELAYED
            ));
        } catch (Exception ex) {
            log.warn("Finnhub quote failed for {}: {}", symbol, ex.getMessage());
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
    public record QuoteResponse(Double c, Double d, Double dp, Double h, Double l, Long t) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProfileResponse(String name) {
    }
}
