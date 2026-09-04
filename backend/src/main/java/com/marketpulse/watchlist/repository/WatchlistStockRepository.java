package com.marketpulse.watchlist.repository;

import com.marketpulse.watchlist.entity.WatchlistStock;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistStockRepository extends JpaRepository<WatchlistStock, Long> {
    Optional<WatchlistStock> findByWatchlistIdAndSymbol(Long watchlistId, String symbol);
    List<WatchlistStock> findByWatchlistUserId(Long userId);
    boolean existsByWatchlistIdAndSymbol(Long watchlistId, String symbol);
}
