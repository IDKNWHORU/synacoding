package com.platform.coding.controller.admin;

import com.platform.coding.domain.review.ReviewStatus;
import com.platform.coding.domain.user.User;
import com.platform.coding.service.admin.AdminReviewService;
import com.platform.coding.service.admin.dto.ReviewManagementResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {
    private final AdminReviewService adminReviewService;

    /**
     * 리뷰 목록을 상태별로 조회합니다. (예: /api/admin/reviews?status=PENDING_APPROVAL)
     */
    @GetMapping
    public ResponseEntity<Page<ReviewManagementResponse>> getReviews(
            @RequestParam(required = false)ReviewStatus status,
            @PageableDefault(sort = "createdAt") Pageable pageable,
            @AuthenticationPrincipal User amdin
            ) {
        Page<ReviewManagementResponse> reviews = adminReviewService.getReviewsByStatus(status, pageable);
        return ResponseEntity.ok(reviews);
    }

    /**
     * 특정 리뷰를 승인(게시)합니다.
     */
    @PatchMapping("/{reviewId}/approve")
    public ResponseEntity<Void> approveReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal User admin
    ) {
        adminReviewService.approveReview(reviewId, admin);
        return ResponseEntity.noContent().build();
    }

    /**
     * 특정 리뷰를 숨김 처리합니다.
     */
    @PatchMapping("/{reviewId}/hide")
    public ResponseEntity<Void> hideReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal User admin
    ) {
        adminReviewService.hideReview(reviewId, admin);
        return ResponseEntity.noContent().build();
    }

    /**
     * 신고된 리뷰를 다시 게시 상태로 되돌림.
     */
    @PatchMapping("/{reviewId}/revert")
    public ResponseEntity<Void> revertReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal User admin
    ) {
        adminReviewService.reverReview(reviewId, admin);

        return ResponseEntity.noContent().build();
    }

    /**
     * 특정 리뷰를 '베스트 리뷰'로 선정합니다.
     */
    @PatchMapping("/{reviewId}/mark-as-best")
    public ResponseEntity<Void> markAsBest(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal User admin
    ) {
        adminReviewService.markAsBest(reviewId, admin);
        return ResponseEntity.noContent().build();
    }

    /**
     * 특정 리뷰의 '베스트 리뷰' 선정을 취소합니다.
     */
    @PatchMapping("/{reviewId}/unmark-as-best")
    public ResponseEntity<Void> unmarkAsBest(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal User admin
    ) {
        adminReviewService.unmarkAsBest(reviewId, admin);
        return ResponseEntity.noContent().build();
    }
}
