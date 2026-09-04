package com.marketpulse.change.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record NotificationDto(
        Long id,
        String symbol,
        String changeType,
        BigDecimal changePercent,
        String severity,
        int attentionScore,
        List<String> reasons,
        Instant detectedAt,
        boolean acknowledged
) {
}
