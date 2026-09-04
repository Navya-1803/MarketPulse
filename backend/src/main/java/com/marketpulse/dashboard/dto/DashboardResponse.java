package com.marketpulse.dashboard.dto;

import com.marketpulse.change.dto.ChangeSummaryDto;
import com.marketpulse.user.dto.UserDto;
import com.marketpulse.watchlist.dto.WatchlistResponse;
import java.util.List;

public record DashboardResponse(
        UserDto user,
        ChangeSummaryDto changes,
        List<WatchlistResponse> watchlists
) {
}
