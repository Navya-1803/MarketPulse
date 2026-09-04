package com.marketpulse.user.controller;

import com.marketpulse.common.security.AuthenticatedUser;
import com.marketpulse.user.dto.UserDto;
import com.marketpulse.user.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return userService.toDto(userService.getById(currentUser.getId()));
    }
}
