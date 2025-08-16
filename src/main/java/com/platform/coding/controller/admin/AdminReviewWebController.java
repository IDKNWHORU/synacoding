package com.platform.coding.controller.admin;

import com.platform.coding.domain.review.ReviewStatus;
import com.platform.coding.domain.user.User;
import com.platform.coding.service.admin.AdminReviewService;
import com.platform.coding.service.admin.dto.ReviewManagementResponse;
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
@RequestMapping("/admin/reviews")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CONTENT_MANAGER')")
@RequiredArgsConstructor
public class AdminReviewWebController {

    private final AdminReviewService adminReviewService;

    @GetMapping
    public String listReviews(@RequestParam(required = false, defaultValue = "PENDING_APPROVAL") ReviewStatus status,
                              @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                              Model model) {

        Page<ReviewManagementResponse> reviewPage = adminReviewService.getReviewsByStatus(status, pageable);
        model.addAttribute("reviewPage", reviewPage);
        model.addAttribute("currentStatus", status.name());

        // Pagination UI logic
        int nowPage = reviewPage.getPageable().getPageNumber() + 1;
        int startPage = Math.max(nowPage - 4, 1);
        int endPage = Math.min(nowPage + 4, reviewPage.getTotalPages());
        model.addAttribute("nowPage", nowPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage == 0 ? 1 : endPage);

        return "admin/review_management";
    }

    @PostMapping("/{reviewId}/approve")
    public String approveReview(@PathVariable Long reviewId,
                                @RequestParam String status,
                                @AuthenticationPrincipal User admin,
                                RedirectAttributes redirectAttributes) {
        adminReviewService.approveReview(reviewId, admin);
        redirectAttributes.addFlashAttribute("successMessage", "리뷰가 성공적으로 승인되었습니다.");
        return "redirect:/admin/reviews?status=" + status;
    }

    @PostMapping("/{reviewId}/hide")
    public String hideReview(@PathVariable Long reviewId,
                             @RequestParam String status,
                             @AuthenticationPrincipal User admin,
                             RedirectAttributes redirectAttributes) {
        adminReviewService.hideReview(reviewId, admin);
        redirectAttributes.addFlashAttribute("successMessage", "리뷰가 숨김 처리되었습니다.");
        return "redirect:/admin/reviews?status=" + status;
    }

    @PostMapping("/{reviewId}/revert")
    public String revertReview(@PathVariable Long reviewId,
                               @RequestParam String status,
                               @AuthenticationPrincipal User admin,
                               RedirectAttributes redirectAttributes) {
        adminReviewService.reverReview(reviewId, admin);
        redirectAttributes.addFlashAttribute("successMessage", "신고된 리뷰가 다시 게시 상태로 변경되었습니다.");
        return "redirect:/admin/reviews?status=" + status;
    }

    @PostMapping("/{reviewId}/mark-as-best")
    public String markAsBest(@PathVariable Long reviewId,
                             @RequestParam String status,
                             @AuthenticationPrincipal User admin,
                             RedirectAttributes redirectAttributes) {
        adminReviewService.markAsBest(reviewId, admin);
        redirectAttributes.addFlashAttribute("successMessage", "베스트 리뷰로 선정되었습니다.");
        return "redirect:/admin/reviews?status=" + status;
    }

    @PostMapping("/{reviewId}/unmark-as-best")
    public String unmarkAsBest(@PathVariable Long reviewId,
                               @RequestParam String status,
                               @AuthenticationPrincipal User admin,
                               RedirectAttributes redirectAttributes) {
        adminReviewService.unmarkAsBest(reviewId, admin);
        redirectAttributes.addFlashAttribute("successMessage", "베스트 리뷰 선정이 취소되었습니다.");
        return "redirect:/admin/reviews?status=" + status;
    }
}