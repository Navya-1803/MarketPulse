package com.marketpulse.change.dto;

import com.marketpulse.change.entity.ChangeSeverity;
import com.marketpulse.change.entity.ChangeType;
import com.marketpulse.market.dto.MarketQuoteDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AttentionItemDto(
        String symbol,
        String companyName,
        ChangeType changeType,
        ChangeSeverity severity,
        int attentionScore,
        BigDecimal previousPrice,
        BigDecimal currentPrice,
        BigDecimal changePercent,
        Instant lastCheckedAt,
        List<String> reasons,
        MarketQuoteDto quote
) {
}
