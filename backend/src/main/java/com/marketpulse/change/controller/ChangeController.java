package com.marketpulse.change.controller;

import com.marketpulse.change.dto.ChangeSummaryDto;
import com.marketpulse.change.dto.CheckpointResponse;
import com.marketpulse.change.service.ChangeDetectionService;
import com.marketpulse.change.service.CheckpointService;
import com.marketpulse.common.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ChangeController {

    private final ChangeDetectionService changeDetectionService;
    private final CheckpointService checkpointService;

    public ChangeController(ChangeDetectionService changeDetectionService, CheckpointService checkpointService) {
        this.changeDetectionService = changeDetectionService;
        this.checkpointService = checkpointService;
    }

    @GetMapping("/changes")
    public ChangeSummaryDto changes(@AuthenticationPrincipal AuthenticatedUser user) {
        return changeDetectionService.detect(user.getId());
    }

    @PostMapping("/checkpoints")
    public CheckpointResponse checkpoint(@AuthenticationPrincipal AuthenticatedUser user) {
        return checkpointService.acknowledge(user.getId());
    }
}
