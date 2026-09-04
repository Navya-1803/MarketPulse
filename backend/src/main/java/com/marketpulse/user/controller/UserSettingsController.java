package com.marketpulse.user.controller;

import com.marketpulse.common.security.AuthenticatedUser;
import com.marketpulse.user.dto.UserSettingsDto;
import com.marketpulse.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class UserSettingsController {

    private final UserService userService;

    public UserSettingsController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserSettingsDto getSettings(@AuthenticationPrincipal AuthenticatedUser user) {
        return userService.getSettings(user.getId());
    }

    @PutMapping
    public UserSettingsDto updateSettings(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UserSettingsDto dto
    ) {
        return userService.updateSettings(user.getId(), dto);
    }
}
