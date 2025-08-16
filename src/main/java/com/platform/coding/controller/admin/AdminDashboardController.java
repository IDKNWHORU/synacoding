package com.platform.coding.controller.admin;

import com.platform.coding.domain.user.User;
import com.platform.coding.service.admin.AdminDashboardService;
import com.platform.coding.service.admin.dto.DashboardStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {
    private final AdminDashboardService adminDashboardService;

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getStats(
            @AuthenticationPrincipal User admin
            ) {
        DashboardStatsResponse stats = adminDashboardService.getDashboardStats();

        return ResponseEntity.ok(stats);
    }
}
