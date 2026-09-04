package com.marketpulse.change.repository;

import com.marketpulse.change.entity.ChangeEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChangeEventRepository extends JpaRepository<ChangeEvent, Long> {
    List<ChangeEvent> findByUserIdAndAcknowledgedFalseOrderByAttentionScoreDesc(Long userId);
    void deleteByUserIdAndAcknowledgedFalse(Long userId);
}
