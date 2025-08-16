package com.platform.coding.controller.admin;

import com.platform.coding.domain.user.User;
import com.platform.coding.service.admin.AdminUserService;
import com.platform.coding.service.admin.dto.UserManagementResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminUserWebController {

    private final AdminUserService adminUserService;

    /**
     * 회원 목록 페이지. 검색 기능 포함.
     */
    @GetMapping
    public String listUsers(@RequestParam(required = false) String searchType,
                            @RequestParam(required = false) String keyword,
                            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                            Model model) {

        Page<UserManagementResponse> userPage = adminUserService.getUsers(searchType, keyword, pageable);
        model.addAttribute("userPage", userPage);
        model.addAttribute("searchType", searchType);
        model.addAttribute("keyword", keyword);

        // 페이징 UI를 위한 정보
        int nowPage = userPage.getPageable().getPageNumber() + 1;
        int startPage = Math.max(nowPage - 4, 1);
        int endPage = Math.min(nowPage + 4, userPage.getTotalPages());
        model.addAttribute("nowPage", nowPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage == 0 ? 1 : endPage);

        return "admin/user_management";
    }

    /**
     * 특정 회원을 비활성화(정지) 처리합니다.
     */
    @PostMapping("/{userId}/deactivate")
    public String deactivateUser(@PathVariable Long userId,
                                 @AuthenticationPrincipal User admin,
                                 RedirectAttributes redirectAttributes) {
        adminUserService.deactivateUser(userId, admin);
        redirectAttributes.addFlashAttribute("successMessage", "회원이 비활성화 처리되었습니다.");
        return "redirect:/admin/users";
    }

    /**
     * 특정 회원을 다시 활성화 처리합니다.
     */
    @PostMapping("/{userId}/activate")
    public String activateUser(@PathVariable Long userId,
                               @AuthenticationPrincipal User admin,
                               RedirectAttributes redirectAttributes) {
        adminUserService.activateUser(userId, admin);
        redirectAttributes.addFlashAttribute("successMessage", "회원이 활성화 처리되었습니다.");
        return "redirect:/admin/users";
    }
}