package com.marketpulse.change.dto;

import java.time.Instant;

public record CheckpointResponse(Instant lastCheckedAt, String message) {
}
