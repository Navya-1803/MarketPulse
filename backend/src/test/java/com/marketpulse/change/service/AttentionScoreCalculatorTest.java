package com.marketpulse.change.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.marketpulse.change.entity.ChangeSeverity;
import com.marketpulse.common.config.MarketProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttentionScoreCalculatorTest {

    private AttentionScoreCalculator calculator;

    @BeforeEach
    void setUp() {
        MarketProperties properties = new MarketProperties(
                "mock",
                30,
                15,
                new MarketProperties.Finnhub("", ""),
                new MarketProperties.Change(2.0, 3.0, 8.0, 2.0, 1.0)
        );
        calculator = new AttentionScoreCalculator(properties);
    }

    @Test
    void classifiesSeverityByConfiguredThresholds() {
        assertThat(calculator.severityFor(0.8)).isEqualTo(ChangeSeverity.NORMAL);
        assertThat(calculator.severityFor(2.1)).isEqualTo(ChangeSeverity.NOTABLE);
        assertThat(calculator.severityFor(4.5)).isEqualTo(ChangeSeverity.SIGNIFICANT);
        assertThat(calculator.severityFor(10.2)).isEqualTo(ChangeSeverity.CRITICAL);
    }

    @Test
    void scoresPriceVolumeAndExtremes() {
        int score = calculator.score(6.67, true, false, true, 18);
        assertThat(score).isGreaterThan(80);
        assertThat(score).isLessThanOrEqualTo(100);
    }
}
