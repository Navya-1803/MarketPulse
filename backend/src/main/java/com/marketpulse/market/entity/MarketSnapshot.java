package com.marketpulse.market.entity;

import com.marketpulse.market.provider.MarketStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "market_snapshots")
@Getter
@Setter
@NoArgsConstructor
public class MarketSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 16)
    private String symbol;

    @Column(precision = 18, scale = 4)
    private BigDecimal price;

    @Column(name = "change_percent", precision = 10, scale = 4)
    private BigDecimal changePercent;

    private Long volume;

    @Column(name = "day_high", precision = 18, scale = 4)
    private BigDecimal dayHigh;

    @Column(name = "day_low", precision = 18, scale = 4)
    private BigDecimal dayLow;

    @Enumerated(EnumType.STRING)
    @Column(name = "market_status")
    private MarketStatus marketStatus;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Column(name = "user_id")
    private Long userId;
}
