package com.marketpulse.change.repository;

import com.marketpulse.change.entity.UserCheckpoint;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCheckpointRepository extends JpaRepository<UserCheckpoint, Long> {
    Optional<UserCheckpoint> findByUserId(Long userId);
}
