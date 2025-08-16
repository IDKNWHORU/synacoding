package com.platform.coding.controller.user;

import com.platform.coding.domain.user.User;
import com.platform.coding.service.user.ParentDashboardService;
import com.platform.coding.service.user.ParentService;
import com.platform.coding.service.user.dto.ChildAccountCreateRequest;
import com.platform.coding.service.user.dto.ChildAccountResponse;
import com.platform.coding.service.user.dto.ChildDashboardResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/parents/me/children")
@RequiredArgsConstructor
public class ParentController {
    private final ParentService parentService;
    private final ParentDashboardService parentDashboardService;

    /**
     * 현재 로그인한 학부모 계정에 자녀 계정을 생성하고 연동한다.
     * POST /api/parents/me/children
     */
    @PostMapping
    public ResponseEntity<ChildAccountResponse> createChildAccount(
            @Valid @RequestBody ChildAccountCreateRequest request,
            @AuthenticationPrincipal User parent
            ) {
        ChildAccountResponse response = parentService.createChildAccount(request, parent);
        return ResponseEntity.created(URI.create("/api/users" + response.userId())).body(response);
    }

    /**
     * 현재 로그인한 학부모의 모든 자녀 계정 목록을 조회한다.
     * GET /api/parents/me/children
     */
    @GetMapping
    public ResponseEntity<List<ChildAccountResponse>> getMyChildren(
            @AuthenticationPrincipal User parent
    ) {
        List<ChildAccountResponse> children = parentService.getMyChildren(parent);

        return ResponseEntity.ok(children);
    }

    /**
     * 특정 자녀의 학습 현황 대시보드를 조회한다.
     * GET /api/parents/me/children/{childId}/dashboard
     */
    @GetMapping("/{childId}/dashboard")
    public ResponseEntity<ChildDashboardResponse> getChildDashboard(
            @PathVariable Long childId,
            @AuthenticationPrincipal User parent
    ) {
        ChildDashboardResponse dashboard = parentDashboardService.getChildDashboard(childId, parent);

        return ResponseEntity.ok(dashboard);
    }
}
