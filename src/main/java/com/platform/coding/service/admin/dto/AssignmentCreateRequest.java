package com.platform.coding.service.admin.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.time.LocalDateTime;

public record AssignmentCreateRequest(
        @NotBlank(message = "과제 제목은 필수입니다.")
        String title,
        String content,
        @FutureOrPresent(message = "마감일은 현재 또는 미래의 시간이어야 합니다.")
        LocalDateTime deadline
) {
    @Builder
    public AssignmentCreateRequest {}
}