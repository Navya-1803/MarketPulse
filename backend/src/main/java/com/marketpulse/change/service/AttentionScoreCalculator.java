package com.marketpulse.change.service;

import com.marketpulse.change.entity.ChangeSeverity;
import com.marketpulse.common.config.MarketProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AttentionScoreCalculator {

    private final MarketProperties.Change thresholds;

    public AttentionScoreCalculator(MarketProperties properties) {
        this.thresholds = properties.change();
    }

    public ChangeSeverity severityFor(double absPercent) {
        return severityFor(absPercent, null);
    }

    public ChangeSeverity severityFor(double absPercent, Double userThreshold) {
        double significant = (userThreshold != null && userThreshold > 0) ? userThreshold : thresholds.significantPercent();
        double critical = significant * 2.5;
        double notable = significant * 0.66;

        if (absPercent >= critical) {
            return ChangeSeverity.CRITICAL;
        }
        if (absPercent >= significant) {
            return ChangeSeverity.SIGNIFICANT;
        }
        if (absPercent >= notable) {
            return ChangeSeverity.NOTABLE;
        }
        return ChangeSeverity.NORMAL;
    }

    public int score(
            double absPricePercent,
            boolean volumeAnomaly,
            boolean nearHigh,
            boolean nearLow,
            long hoursSinceCheck
    ) {
        int score = (int) Math.round(Math.min(absPricePercent * 10, 70));
        if (volumeAnomaly) {
            score += 20;
        }
        if (nearHigh || nearLow) {
            score += 15;
        }
        score += (int) Math.min(hoursSinceCheck, 12);
        return Math.min(score, 100);
    }

    public boolean isVolumeAnomaly(Long previousVolume, Long currentVolume) {
        if (previousVolume == null || currentVolume == null || previousVolume == 0) {
            return false;
        }
        return currentVolume >= previousVolume * thresholds.volumeAnomalyMultiplier();
    }

    public boolean isNearExtreme(BigDecimal price, BigDecimal extreme) {
        if (price == null || extreme == null || extreme.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }
        BigDecimal distance = price.subtract(extreme).abs()
                .divide(extreme, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        return distance.doubleValue() <= thresholds.nearExtremePercent();
    }

    public List<String> explanations(
            String symbol,
            double changePercent,
            ChangeSeverity severity,
            boolean volumeAnomaly,
            Double volumeMultiplier,
            boolean nearHigh,
            boolean nearLow
    ) {
        return explanations(symbol, changePercent, severity, volumeAnomaly, volumeMultiplier, nearHigh, nearLow, null);
    }

    public List<String> explanations(
            String symbol,
            double changePercent,
            ChangeSeverity severity,
            boolean volumeAnomaly,
            Double volumeMultiplier,
            boolean nearHigh,
            boolean nearLow,
            Double userThreshold
    ) {
        double threshold = (userThreshold != null && userThreshold > 0) ? userThreshold : thresholds.significantPercent();
        List<String> reasons = new ArrayList<>();
        String direction = changePercent >= 0 ? "increased" : "decreased";
        reasons.add(symbol + " price " + direction + " " + format(Math.abs(changePercent)) + "% since your last check.");
        if (severity.ordinal() >= ChangeSeverity.SIGNIFICANT.ordinal()) {
            reasons.add("Move exceeds your configured " + format(threshold) + "% threshold.");
        }
        if (volumeAnomaly && volumeMultiplier != null) {
            reasons.add("Volume is " + format(volumeMultiplier) + "× its recent average.");
        }
        if (nearHigh) {
            reasons.add("Price is near today's high.");
        }
        if (nearLow) {
            reasons.add("Price is near today's low.");
        }
        return reasons;
    }

    private String format(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
