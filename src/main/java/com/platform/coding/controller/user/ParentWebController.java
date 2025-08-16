package com.platform.coding.controller.user;

import com.platform.coding.domain.user.User;
import com.platform.coding.service.user.ParentDashboardService;
import com.platform.coding.service.user.ParentService;
import com.platform.coding.service.user.dto.ChildAccountCreateRequest;
import com.platform.coding.service.user.dto.ChildDashboardResponse;
import com.platform.coding.service.user.dto.ChildLearningSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/my-profile")
@RequiredArgsConstructor
public class ParentWebController {
    private final ParentService parentService;
    private final ParentDashboardService parentDashboardService;

    @GetMapping("/children")
    public String childManagementPage(@AuthenticationPrincipal User parent, Model model) {
        // 1. [변경] 새로운 서비스 메소드를 호출하여 모든 자녀의 학습 요약 정보를 가져옵니다.
        List<ChildLearningSummaryDto> learningSummaries = parentDashboardService.getChildrenLearningSummary(parent);
        model.addAttribute("learningSummaries", learningSummaries);

        // 2. [유지] 폼 바인딩을 위한 빈 객체를 전달합니다.
        model.addAttribute("childRequest", new ChildAccountCreateRequest("", "", ""));

        return "user/children";
    }

    /**
     * 특정 자녀의 학습 현황 대시보드 페이지를 반환한다.
     * @param childId   조회할 자녀의 ID
     * @param parent    현재 로그인한 학부모 사용자
     * @param model     뷰에 데이터를 전달할 모델 객체
     * @return          자녀 학습 대시보드 뷰 템플릿 경로
     */
    @GetMapping("/children/{childId}/dashboard")
    public String childDashboardPage(
            @PathVariable Long childId,
            @AuthenticationPrincipal User parent,
            Model model
    ) {
        ChildDashboardResponse dashboardData = parentDashboardService.getChildDashboard(childId, parent);

        model.addAttribute("dashboard", dashboardData);
        return "user/child_dashboard";
    }
}
