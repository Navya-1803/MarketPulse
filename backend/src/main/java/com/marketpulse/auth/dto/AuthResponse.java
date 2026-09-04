package com.marketpulse.auth.dto;

import com.marketpulse.user.dto.UserDto;

public record AuthResponse(String token, String tokenType, UserDto user) {
    public static AuthResponse bearer(String token, UserDto user) {
        return new AuthResponse(token, "Bearer", user);
    }
}
