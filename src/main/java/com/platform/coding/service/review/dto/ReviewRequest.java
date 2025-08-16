package com.platform.coding.service.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

public record ReviewRequest(
        @NotBlank(message = "리뷰 내용을 입력해주세요.")
        String content,

        @NotNull(message = "평점을 입력해주세요.")
        @Min(value = 1, message = "평점은 1점 이상이어야 합니다.")
        @Max(value = 5, message = "평점은 5점 이하이어야 합니다.")
        int rating
) {
    @Builder
    public ReviewRequest {}
}
