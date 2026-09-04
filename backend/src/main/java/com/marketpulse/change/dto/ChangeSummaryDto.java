package com.marketpulse.change.dto;

import java.time.Instant;
import java.util.List;

public record ChangeSummaryDto(
        Instant lastCheckedAt,
        int meaningfulChangeCount,
        List<AttentionItemDto> items
) {
}
