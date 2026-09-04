package com.marketpulse.change.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "change_events")
@Getter
@Setter
@NoArgsConstructor
public class ChangeEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 16)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false)
    private ChangeType changeType;

    @Column(name = "previous_value", precision = 18, scale = 4)
    private BigDecimal previousValue;

    @Column(name = "current_value", precision = 18, scale = 4)
    private BigDecimal currentValue;

    @Column(name = "change_percent", precision = 10, scale = 4)
    private BigDecimal changePercent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChangeSeverity severity;

    @Column(name = "attention_score", nullable = false)
    private int attentionScore;

    @Lob
    @Column(name = "reasons", columnDefinition = "TEXT")
    private String reasonsJson;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(nullable = false)
    private boolean acknowledged;
}
