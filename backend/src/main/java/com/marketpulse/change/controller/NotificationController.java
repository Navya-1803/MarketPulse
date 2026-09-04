package com.marketpulse.change.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketpulse.change.dto.NotificationDto;
import com.marketpulse.change.entity.ChangeEvent;
import com.marketpulse.change.repository.ChangeEventRepository;
import com.marketpulse.common.exception.ResourceNotFoundException;
import com.marketpulse.common.security.AuthenticatedUser;
import java.util.Collections;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final ChangeEventRepository changeEventRepository;
    private final ObjectMapper objectMapper;

    public NotificationController(ChangeEventRepository changeEventRepository, ObjectMapper objectMapper) {
        this.changeEventRepository = changeEventRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public List<NotificationDto> list(@AuthenticationPrincipal AuthenticatedUser user) {
        List<ChangeEvent> events = changeEventRepository
                .findByUserIdAndAcknowledgedFalseOrderByAttentionScoreDesc(user.getId());

        return events.stream().map(this::toDto).toList();
    }

    @PostMapping("/{id}/acknowledge")
    @Transactional
    public ResponseEntity<Void> acknowledgeSingle(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id
    ) {
        ChangeEvent event = changeEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!event.getUserId().equals(user.getId())) {
            throw new ResourceNotFoundException("Notification not found");
        }
        event.setAcknowledged(true);
        changeEventRepository.save(event);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/acknowledge-all")
    @Transactional
    public ResponseEntity<Void> acknowledgeAll(@AuthenticationPrincipal AuthenticatedUser user) {
        List<ChangeEvent> events = changeEventRepository
                .findByUserIdAndAcknowledgedFalseOrderByAttentionScoreDesc(user.getId());
        for (ChangeEvent e : events) {
            e.setAcknowledged(true);
        }
        changeEventRepository.saveAll(events);
        return ResponseEntity.noContent().build();
    }

    private NotificationDto toDto(ChangeEvent event) {
        List<String> reasons = Collections.emptyList();
        if (event.getReasonsJson() != null && !event.getReasonsJson().isBlank()) {
            try {
                reasons = objectMapper.readValue(event.getReasonsJson(), new TypeReference<List<String>>() {});
            } catch (Exception ignored) {
            }
        }
        return new NotificationDto(
                event.getId(),
                event.getSymbol(),
                event.getChangeType() != null ? event.getChangeType().name() : "PRICE_MOVEMENT",
                event.getChangePercent(),
                event.getSeverity() != null ? event.getSeverity().name() : "NORMAL",
                event.getAttentionScore(),
                reasons,
                event.getDetectedAt(),
                event.isAcknowledged()
        );
    }
}
