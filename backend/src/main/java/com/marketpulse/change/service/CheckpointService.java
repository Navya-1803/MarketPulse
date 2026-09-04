package com.marketpulse.change.service;

import com.marketpulse.change.dto.CheckpointResponse;
import com.marketpulse.change.entity.UserCheckpoint;
import com.marketpulse.change.repository.ChangeEventRepository;
import com.marketpulse.change.repository.UserCheckpointRepository;
import com.marketpulse.market.service.MarketDataService;
import com.marketpulse.watchlist.service.WatchlistService;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
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

    @Transactional
    public CheckpointResponse acknowledge(Long userId) {
        List<String> symbols = watchlistService.symbolsForUser(userId);
        marketDataService.persistSnapshots(userId, symbols);
        Instant now = Instant.now();
        UserCheckpoint checkpoint = checkpointRepository.findByUserId(userId).orElseGet(() -> {
            UserCheckpoint created = new UserCheckpoint();
            created.setUserId(userId);
            return created;
        });
        checkpoint.setLastCheckedAt(now);
        checkpointRepository.save(checkpoint);
        changeEventRepository.findByUserIdAndAcknowledgedFalseOrderByAttentionScoreDesc(userId)
                .forEach(event -> event.setAcknowledged(true));
        return new CheckpointResponse(now, "Checkpoint updated after a successful review.");
    }
}
