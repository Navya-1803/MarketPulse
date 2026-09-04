package com.marketpulse.watchlist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AddStockRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z.]{1,10}$", message = "must be a valid ticker symbol")
        String symbol
) {
}
