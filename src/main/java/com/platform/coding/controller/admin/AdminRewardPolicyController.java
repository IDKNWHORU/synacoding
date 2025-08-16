package com.platform.coding.controller.admin;

import com.platform.coding.domain.user.User;
import com.platform.coding.service.admin.AdminRewardPolicyService;
import com.platform.coding.service.admin.dto.RewardPolicyResponse;
import com.platform.coding.service.admin.dto.RewardPolicyUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reward-policies")
@RequiredArgsConstructor
public class AdminRewardPolicyController {
    private final AdminRewardPolicyService adminRewardPolicyService;

    @GetMapping
    public ResponseEntity<List<RewardPolicyResponse>> getRewardPolicies() {
        return ResponseEntity.ok(adminRewardPolicyService.getPolicies());
    }

    @PutMapping
    // 최고 관리자만 수정 가능하도록 설정
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> updateRewardPolicies(
            @Valid @RequestBody List<RewardPolicyUpdateRequest> requests,
            @AuthenticationPrincipal User admin) {
        adminRewardPolicyService.updatePolicies(requests, admin);
        return ResponseEntity.noContent().build();
    }
}
