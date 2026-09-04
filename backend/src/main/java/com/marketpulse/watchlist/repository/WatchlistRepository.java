package com.marketpulse.watchlist.repository;

import com.marketpulse.watchlist.entity.Watchlist;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {
    @EntityGraph(attributePaths = "stocks")
    List<Watchlist> findByUserIdOrderByCreatedAtAsc(Long userId);

    @EntityGraph(attributePaths = "stocks")
    Optional<Watchlist> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = "stocks")
    Optional<Watchlist> findById(Long id);

    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);
}
