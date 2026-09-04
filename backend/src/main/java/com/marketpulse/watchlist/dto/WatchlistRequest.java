package com.marketpulse.watchlist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WatchlistRequest(
        @NotBlank @Size(min = 2, max = 80) String name
) {
}
