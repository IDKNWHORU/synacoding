package com.platform.coding.controller.admin;

import com.platform.coding.service.admin.AdminDashboardService;
import com.platform.coding.service.admin.dto.DashboardStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
// SUPER_ADMIN 또는 CONTENT_MANAGER 역할을 가진 사용자만 접근 가능
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_MANAGER')")
@RequiredArgsConstructor
public class AdminWebController {
    private final AdminDashboardService adminDashboardService;
    /**
     * 관리자 페이지의 메인 대시보드를 반환합니다.
     * @return 관리자 대시보드 뷰
     */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        //  서비스 레이어에서 통계 데이터 조회
        DashboardStatsResponse stats = adminDashboardService.getDashboardStats();
        // Model에 'stats'라는 이름으로 데이터 추가
        model.addAttribute("stats", stats);
        return "admin/dashboard";
    }
}