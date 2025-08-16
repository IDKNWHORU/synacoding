package com.platform.coding.controller.admin;

import com.platform.coding.domain.user.User;
import com.platform.coding.service.admin.AdminRewardPolicyService;
import com.platform.coding.service.admin.dto.RewardPolicyResponse;
import com.platform.coding.service.admin.dto.RewardPolicyUpdateForm;
import com.platform.coding.service.admin.dto.RewardPolicyUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/reward-policies")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminRewardPolicyWebController {

    private final AdminRewardPolicyService adminRewardPolicyService;

    @GetMapping
    public String rewardPolicyManagementPage(Model model) {
        List<RewardPolicyResponse> policies = adminRewardPolicyService.getPolicies();
        model.addAttribute("policies", policies);

        RewardPolicyUpdateForm form = new RewardPolicyUpdateForm();
        // [수정] 폼 객체 초기화 로직 변경
        form.setPolicies(policies.stream()
                .map(p -> {
                    RewardPolicyUpdateForm.PolicyField field = new RewardPolicyUpdateForm.PolicyField();
                    field.setKey(p.key());
                    field.setValue(p.value());
                    return field;
                })
                .collect(Collectors.toList()));
        model.addAttribute("policyForm", form);

        return "admin/reward_policy_management";
    }

    /**
     * 보상 정책 수정을 처리합니다.
     */
    @PostMapping("/update")
    public String updateRewardPolicies(@Valid @ModelAttribute("policyForm") RewardPolicyUpdateForm form,
                                       @AuthenticationPrincipal User admin,
                                       RedirectAttributes redirectAttributes) {

        // [핵심] 폼 데이터를 담은 mutable DTO를 불변(immutable) record DTO 리스트로 변환합니다.
        List<RewardPolicyUpdateRequest> updateRequests = form.getPolicies().stream()
                .map(field -> new RewardPolicyUpdateRequest(field.getKey(), field.getValue()))
                .collect(Collectors.toList());

        // 서비스 계층에는 record 타입의 DTO를 전달합니다.
        adminRewardPolicyService.updatePolicies(updateRequests, admin);

        redirectAttributes.addFlashAttribute("successMessage", "보상 정책이 성공적으로 업데이트되었습니다.");
        return "redirect:/admin/reward-policies";
    }
}