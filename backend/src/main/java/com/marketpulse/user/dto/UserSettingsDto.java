package com.marketpulse.user.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record UserSettingsDto(
        @NotNull(message = "Threshold percent is required")
        @DecimalMin(value = "0.1", message = "Threshold must be at least 0.1%")
        @DecimalMax(value = "50.0", message = "Threshold cannot exceed 50.0%")
        Double thresholdPercent
) {
}
