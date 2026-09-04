package com.marketpulse.watchlist.controller;

import com.marketpulse.common.security.AuthenticatedUser;
import com.marketpulse.watchlist.dto.AddStockRequest;
import com.marketpulse.watchlist.dto.WatchlistRequest;
import com.marketpulse.watchlist.dto.WatchlistResponse;
import com.marketpulse.watchlist.service.WatchlistService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/watchlists")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping
    public List<WatchlistResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return watchlistService.list(user.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WatchlistResponse create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody WatchlistRequest request
    ) {
        return watchlistService.create(user.getId(), request);
    }

    @GetMapping("/{id}")
    public WatchlistResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        return watchlistService.get(user.getId(), id);
    }

    @PutMapping("/{id}")
    public WatchlistResponse rename(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id,
            @Valid @RequestBody WatchlistRequest request
    ) {
        return watchlistService.rename(user.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
        watchlistService.delete(user.getId(), id);
    }

    @PostMapping("/{id}/stocks")
    public WatchlistResponse addStock(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id,
            @Valid @RequestBody AddStockRequest request
    ) {
        return watchlistService.addStock(user.getId(), id, request);
    }

    @DeleteMapping("/{id}/stocks/{symbol}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeStock(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id,
            @PathVariable String symbol
    ) {
        watchlistService.removeStock(user.getId(), id, symbol);
    }
}
