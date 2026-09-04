package com.marketpulse.market.repository;

import com.marketpulse.market.entity.MarketSnapshot;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketSnapshotRepository extends JpaRepository<MarketSnapshot, Long> {
    Optional<MarketSnapshot> findTopByUserIdAndSymbolOrderByCapturedAtDesc(Long userId, String symbol);
    List<MarketSnapshot> findByUserIdAndSymbolIn(Long userId, Collection<String> symbols);
}
