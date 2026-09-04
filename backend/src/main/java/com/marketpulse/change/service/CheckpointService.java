package com.marketpulse.change.service;

import com.marketpulse.change.dto.CheckpointResponse;
import com.marketpulse.change.entity.UserCheckpoint;
import com.marketpulse.change.repository.ChangeEventRepository;
import com.marketpulse.change.repository.UserCheckpointRepository;
import com.marketpulse.market.service.MarketDataService;
import com.marketpulse.watchlist.service.WatchlistService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckpointService {

    private final UserCheckpointRepository checkpointRepository;
    private final ChangeEventRepository changeEventRepository;
    private final WatchlistService watchlistService;
    private final MarketDataService marketDataService;

    public CheckpointService(
            UserCheckpointRepository checkpointRepository,
            ChangeEventRepository changeEventRepository,
            WatchlistService watchlistService,
            MarketDataService marketDataService
    ) {
        this.checkpointRepository = checkpointRepository;
        this.changeEventRepository = changeEventRepository;
        this.watchlistService = watchlistService;
        this.marketDataService = marketDataService;
    }

    // Race condition prevention: Two rapid "Mark as checked" clicks from the same user could both attempt to
    // create duplicate Checkpoint records and redundant MarketSnapshot baselines. Wrapping in a single @Transactional
    // method with idempotent 2-second throttling safely returns the existing checkpoint instead of creating duplicates.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CheckpointResponse acknowledge(Long userId) {
        Instant now = Instant.now();
        Optional<UserCheckpoint> existing = checkpointRepository.findByUserId(userId);
        if (existing.isPresent() && existing.get().getLastCheckedAt() != null
                && existing.get().getLastCheckedAt().isAfter(now.minusSeconds(2))) {
            return new CheckpointResponse(existing.get().getLastCheckedAt(), "Checkpoint updated after a successful review.");
        }

        List<String> symbols = watchlistService.symbolsForUser(userId);
        marketDataService.persistSnapshots(userId, symbols);
        UserCheckpoint checkpoint = existing.orElseGet(() -> {
            UserCheckpoint created = new UserCheckpoint();
            created.setUserId(userId);
            return created;
        });
        checkpoint.setLastCheckedAt(now);
        try {
            checkpointRepository.saveAndFlush(checkpoint);
        } catch (DataIntegrityViolationException ex) {
            checkpoint = checkpointRepository.findByUserId(userId).orElse(checkpoint);
        }
        changeEventRepository.findByUserIdAndAcknowledgedFalseOrderByAttentionScoreDesc(userId)
                .forEach(event -> event.setAcknowledged(true));
        return new CheckpointResponse(now, "Checkpoint updated after a successful review.");
    }
}
