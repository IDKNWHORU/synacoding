package com.platform.coding.service.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

public record CourseUpdateRequest(
        @NotBlank(message = "강의 제목은 필수입니다.")
        String title,
        String description,
        @NotNull(message = "가격을 입력해주세요.")
        @DecimalMin(value = "0.0")
        BigDecimal price
) {
    @Builder
    public CourseUpdateRequest {}
}
