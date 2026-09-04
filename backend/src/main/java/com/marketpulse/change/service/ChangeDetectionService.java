package com.marketpulse.change.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketpulse.change.dto.AttentionItemDto;
import com.marketpulse.change.dto.ChangeSummaryDto;
import com.marketpulse.change.entity.ChangeEvent;
import com.marketpulse.change.entity.ChangeSeverity;
import com.marketpulse.change.entity.ChangeType;
import com.marketpulse.change.entity.UserCheckpoint;
import com.marketpulse.change.repository.ChangeEventRepository;
import com.marketpulse.change.repository.UserCheckpointRepository;
import com.marketpulse.market.dto.MarketQuoteDto;
import com.marketpulse.market.entity.MarketSnapshot;
import com.marketpulse.market.provider.MarketQuote;
import com.marketpulse.market.service.MarketDataService;
import com.marketpulse.user.repository.UserRepository;
import com.marketpulse.watchlist.service.WatchlistService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangeDetectionService {

    private final WatchlistService watchlistService;
    private final MarketDataService marketDataService;
    private final UserCheckpointRepository checkpointRepository;
    private final ChangeEventRepository changeEventRepository;
    private final AttentionScoreCalculator scoreCalculator;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    public ChangeDetectionService(
            WatchlistService watchlistService,
            MarketDataService marketDataService,
            UserCheckpointRepository checkpointRepository,
            ChangeEventRepository changeEventRepository,
            AttentionScoreCalculator scoreCalculator,
            ObjectMapper objectMapper,
            UserRepository userRepository
    ) {
        this.watchlistService = watchlistService;
        this.marketDataService = marketDataService;
        this.checkpointRepository = checkpointRepository;
        this.changeEventRepository = changeEventRepository;
        this.scoreCalculator = scoreCalculator;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
    }

    @Transactional
    public ChangeSummaryDto detect(Long userId) {
        Instant lastCheckedAt = checkpointRepository.findByUserId(userId)
                .map(UserCheckpoint::getLastCheckedAt)
                .orElse(null);
        Double userThreshold = userRepository.findById(userId)
                .map(u -> u.getThresholdPercent() != null ? u.getThresholdPercent() : 3.0)
                .orElse(3.0);

        List<String> symbols = watchlistService.symbolsForUser(userId);
        List<AttentionItemDto> items = new ArrayList<>();

        for (String symbol : symbols) {
            Optional<MarketQuote> current = marketDataService.fetchQuote(symbol);
            if (current.isEmpty() || current.get().price() == null) {
                continue;
            }
            MarketQuote quote = current.get();
            Optional<MarketSnapshot> previous = marketDataService.lastSnapshot(userId, symbol);
            BigDecimal previousPrice = previous.map(MarketSnapshot::getPrice).orElseGet(() -> inferOpen(quote));
            Long previousVolume = previous.map(MarketSnapshot::getVolume).orElse(quote.volume());
            detectForSymbol(userId, quote, previousPrice, previousVolume, lastCheckedAt, userThreshold).ifPresent(items::add);
        }

        items.sort(Comparator.comparingInt(AttentionItemDto::attentionScore).reversed());
        replaceOpenEvents(userId, items);
        long meaningful = items.stream()
                .filter(item -> item.severity() != ChangeSeverity.NORMAL)
                .count();
        return new ChangeSummaryDto(lastCheckedAt, (int) meaningful, items);
    }

    private Optional<AttentionItemDto> detectForSymbol(
            Long userId,
            MarketQuote current,
            BigDecimal previousPrice,
            Long previousVolume,
            Instant lastCheckedAt,
            Double userThreshold
    ) {
        if (previousPrice == null || previousPrice.compareTo(BigDecimal.ZERO) == 0) {
            return Optional.empty();
        }
        BigDecimal changePercent = current.price().subtract(previousPrice)
                .divide(previousPrice, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        double abs = Math.abs(changePercent.doubleValue());
        ChangeSeverity severity = scoreCalculator.severityFor(abs, userThreshold);
        boolean volumeAnomaly = scoreCalculator.isVolumeAnomaly(previousVolume, current.volume());
        boolean nearHigh = scoreCalculator.isNearExtreme(current.price(), current.dayHigh());
        boolean nearLow = scoreCalculator.isNearExtreme(current.price(), current.dayLow());
        Double volumeMultiplier = null;
        if (volumeAnomaly && previousVolume != null && previousVolume > 0) {
            volumeMultiplier = current.volume() / (double) previousVolume;
        }
        long hours = lastCheckedAt == null ? 24 : Math.max(Duration.between(lastCheckedAt, Instant.now()).toHours(), 0);
        int score = scoreCalculator.score(abs, volumeAnomaly, nearHigh, nearLow, hours);
        List<String> reasons = scoreCalculator.explanations(
                current.symbol(),
                changePercent.doubleValue(),
                severity,
                volumeAnomaly,
                volumeMultiplier,
                nearHigh,
                nearLow,
                userThreshold
        );
        ChangeType type = ChangeType.PRICE_MOVEMENT;
        if (volumeAnomaly && abs < 2) {
            type = ChangeType.VOLUME_ANOMALY;
        } else if (nearHigh) {
            type = ChangeType.NEAR_DAY_HIGH;
        } else if (nearLow) {
            type = ChangeType.NEAR_DAY_LOW;
        }
        MarketQuoteDto dto = marketDataService.toDto(current.symbol(), Optional.of(current));
        return Optional.of(new AttentionItemDto(
                current.symbol(),
                current.companyName(),
                type,
                severity,
                score,
                previousPrice,
                current.price(),
                changePercent.setScale(2, RoundingMode.HALF_UP),
                lastCheckedAt,
                reasons,
                dto
        ));
    }

    private BigDecimal inferOpen(MarketQuote quote) {
        if (quote.changePercent() == null || quote.price() == null) {
            return quote.price();
        }
        BigDecimal factor = BigDecimal.ONE.add(quote.changePercent().divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        if (factor.compareTo(BigDecimal.ZERO) == 0) {
            return quote.price();
        }
        return quote.price().divide(factor, 4, RoundingMode.HALF_UP);
    }

    private void replaceOpenEvents(Long userId, List<AttentionItemDto> items) {
        changeEventRepository.deleteByUserIdAndAcknowledgedFalse(userId);
        Instant now = Instant.now();
        for (AttentionItemDto item : items) {
            if (item.severity() == ChangeSeverity.NORMAL) {
                continue;
            }
            ChangeEvent event = new ChangeEvent();
            event.setUserId(userId);
            event.setSymbol(item.symbol());
            event.setChangeType(item.changeType());
            event.setPreviousValue(item.previousPrice());
            event.setCurrentValue(item.currentPrice());
            event.setChangePercent(item.changePercent());
            event.setSeverity(item.severity());
            event.setAttentionScore(item.attentionScore());
            event.setDetectedAt(now);
            event.setAcknowledged(false);
            try {
                event.setReasonsJson(objectMapper.writeValueAsString(item.reasons()));
            } catch (JsonProcessingException e) {
                event.setReasonsJson("[]");
            }
            changeEventRepository.save(event);
        }
    }
}
