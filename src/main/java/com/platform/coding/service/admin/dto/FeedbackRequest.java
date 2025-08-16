package com.platform.coding.service.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

public record FeedbackRequest(
        @NotBlank(message = "피드백 내용을 입력해주세요")
        String content
) {
    @Builder
    public FeedbackRequest {}
}
