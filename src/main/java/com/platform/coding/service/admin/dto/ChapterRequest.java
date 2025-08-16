package com.platform.coding.service.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

public record ChapterRequest(
        @NotBlank(message = "챕터 제목은 필수입니다.")
        String title,

        @Positive(message = "순서는 양수여야 합니다.")
        int order
) {
    @Builder
    public ChapterRequest {}
}
