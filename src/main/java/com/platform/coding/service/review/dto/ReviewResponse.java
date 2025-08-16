package com.platform.coding.service.review.dto;

import com.platform.coding.domain.review.Review;
import lombok.Builder;

import java.time.Instant;

public record ReviewResponse(
        String studentName,
        Integer rating,
        String content,
        Instant createdAt
) {
    @Builder
    public ReviewResponse {}

    public static ReviewResponse fromEntity(Review review) {
        return ReviewResponse.builder()
                .studentName(review.getStudent().getUserName())
                .rating(review.getRating())
                .content(review.getContent())
                .createdAt(review.getCreatedAt())
                .build();
    }
}