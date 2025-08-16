package com.platform.coding.service.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import org.hibernate.validator.constraints.URL;

public record LectureRequest(
        @NotBlank(message = "강의 제목은 필수입니다.")
        String title,
        @URL(message = "유효한 URL 형식이 아닙니다.")
        String videoUrl,
        @Positive(message = "순서는 양수여야 합니다.")
        int order,
        boolean sample,
        Integer durationSeconds
) {
    @Builder
    public LectureRequest {}
}
