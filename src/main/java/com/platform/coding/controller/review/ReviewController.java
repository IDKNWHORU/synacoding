package com.platform.coding.controller.review;

import com.platform.coding.domain.user.User;
import com.platform.coding.service.review.ReviewService;
import com.platform.coding.service.review.dto.ReviewRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/courses/{courseId}")
    public ResponseEntity<Void> createReview(
            @PathVariable Long courseId,
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal User student
    ) {
        Long reviewId = reviewService.createReviewAndProvidePoints(courseId, request, student);
        // 생성된 리소스의 URI를 반환 (예: /api/reviews/{reviewId})
        return ResponseEntity.created(URI.create("/api/reviews/" + reviewId)).build();
    }

    /**
     * 특정 리뷰를 신고하는 API
     */
    @PostMapping("/{reviewId}/report")
    public ResponseEntity<Void> reportReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal User user
    ) {
        reviewService.reportReview(reviewId, user);

        return ResponseEntity.noContent().build();
    }
}
