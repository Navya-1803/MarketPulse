package com.marketpulse.dashboard.controller;

import com.marketpulse.change.service.ChangeDetectionService;
import com.marketpulse.common.security.AuthenticatedUser;
import com.marketpulse.dashboard.dto.DashboardResponse;
import com.marketpulse.user.service.UserService;
import com.marketpulse.watchlist.service.WatchlistService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final UserService userService;
    private final WatchlistService watchlistService;
    private final ChangeDetectionService changeDetectionService;

    public DashboardController(
            UserService userService,
            WatchlistService watchlistService,
            ChangeDetectionService changeDetectionService
    ) {
        this.userService = userService;
        this.watchlistService = watchlistService;
        this.changeDetectionService = changeDetectionService;
    }

    @GetMapping
    public DashboardResponse get(@AuthenticationPrincipal AuthenticatedUser user) {
        return new DashboardResponse(
                userService.toDto(userService.getById(user.getId())),
                changeDetectionService.detect(user.getId()),
                watchlistService.list(user.getId())
        );
    }
}
