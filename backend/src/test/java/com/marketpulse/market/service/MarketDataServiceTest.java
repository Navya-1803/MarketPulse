package com.marketpulse.market.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketpulse.common.config.MarketProperties;
import com.marketpulse.market.dto.MarketQuoteDto;
import com.marketpulse.market.provider.MarketDataProvider;
import com.marketpulse.market.provider.MarketQuote;
import com.marketpulse.market.provider.MarketStatus;
import com.marketpulse.market.repository.MarketSnapshotRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MarketDataServiceTest {

    private MarketDataProvider marketDataProvider;
    private MarketSnapshotRepository snapshotRepository;
    private MarketDataService marketDataService;

    @BeforeEach
    void setUp() {
        marketDataProvider = mock(MarketDataProvider.class);
        snapshotRepository = mock(MarketSnapshotRepository.class);
        MarketProperties properties = new MarketProperties(
                "mock",
                30,
                15,
                new MarketProperties.Finnhub("test-key", "https://finnhub.io/api/v1"),
                new MarketProperties.Change(1.5, 3.0, 7.0, 1.8, 1.0)
        );
        marketDataService = new MarketDataService(marketDataProvider, properties, snapshotRepository);
    }

    @Test
    void gracefulFailureWhenProviderThrowsException() {
        when(marketDataProvider.getLatestQuote("TSLA"))
                .thenThrow(new RuntimeException("API connection timeout or rate limit reached"));

        MarketQuoteDto dto = marketDataService.getQuote("TSLA");

        assertNotNull(dto);
        assertEquals("TSLA", dto.symbol());
        assertEquals(MarketStatus.UNAVAILABLE, dto.status());
        assertNotNull(dto.message());
        assertTrue(dto.message().contains("Temporarily unavailable"));
    }

    @Test
    void gracefulFailureWhenProviderReturnsEmpty() {
        when(marketDataProvider.getLatestQuote("INVALID"))
                .thenReturn(Optional.empty());

        MarketQuoteDto dto = marketDataService.getQuote("INVALID");

        assertNotNull(dto);
        assertEquals("INVALID", dto.symbol());
        assertEquals(MarketStatus.UNAVAILABLE, dto.status());
        assertNotNull(dto.message());
    }

    @Test
    void staleDataDetectedWhenCapturedAtExceedsFreshnessThreshold() {
        // Captured 25 minutes ago (> 15 minutes staleAfterMinutes)
        Instant oldTimestamp = Instant.now().minus(Duration.ofMinutes(25));
        MarketQuote oldQuote = new MarketQuote(
                "AAPL",
                "Apple Inc.",
                BigDecimal.valueOf(230.50),
                BigDecimal.valueOf(1.50),
                BigDecimal.valueOf(0.65),
                BigDecimal.valueOf(232.00),
                BigDecimal.valueOf(229.00),
                45000000L,
                oldTimestamp,
                MarketStatus.LIVE
        );

        when(marketDataProvider.getLatestQuote("AAPL"))
                .thenReturn(Optional.of(oldQuote));

        MarketQuoteDto dto = marketDataService.getQuote("AAPL");

        assertNotNull(dto);
        assertEquals(MarketStatus.STALE, dto.status());
        assertNotNull(dto.message());
        assertTrue(dto.message().contains("freshness threshold"));
    }

    @Test
    void cachingReusesQuoteWithinTtlWithoutRequeryingProvider() {
        MarketQuote quote = new MarketQuote(
                "NVDA",
                "NVIDIA Corporation",
                BigDecimal.valueOf(180.00),
                BigDecimal.valueOf(5.00),
                BigDecimal.valueOf(2.85),
                BigDecimal.valueOf(182.00),
                BigDecimal.valueOf(175.00),
                50000000L,
                Instant.now(),
                MarketStatus.LIVE
        );

        when(marketDataProvider.getLatestQuote("NVDA"))
                .thenReturn(Optional.of(quote));

        // First call fetches from provider
        Optional<MarketQuote> firstCall = marketDataService.fetchQuote("NVDA");
        assertTrue(firstCall.isPresent());

        // Second call within TTL (30 seconds) should return cached quote
        Optional<MarketQuote> secondCall = marketDataService.fetchQuote("NVDA");
        assertTrue(secondCall.isPresent());

        // Provider must only be called once
        verify(marketDataProvider, times(1)).getLatestQuote("NVDA");
    }
}
