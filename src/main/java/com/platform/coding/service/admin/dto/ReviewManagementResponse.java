package com.platform.coding.service.admin.dto;

import com.platform.coding.domain.review.Review;
import lombok.Builder;

import java.time.Instant;

public record ReviewManagementResponse(
        Long reviewId,
        String courseTitle,
        String studentName,
        String content,
        Integer rating,
        String status,
        boolean isBest,
        Instant createdAt
) {
    @Builder
    public ReviewManagementResponse {}

    public static ReviewManagementResponse fromEntity(Review review) {
        return ReviewManagementResponse.builder()
                .reviewId(review.getId())
                .courseTitle(review.getCourse().getTitle())
                .studentName(review.getStudent().getUserName())
                .content(review.getContent())
                .rating(review.getRating())
                .status(review.getStatus().name())
                .isBest(review.isBest())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
