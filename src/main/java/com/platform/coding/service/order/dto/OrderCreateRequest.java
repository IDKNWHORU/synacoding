package com.platform.coding.service.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

public record OrderCreateRequest(
        @NotNull(message = "강의 ID는 필수입니다.")
        Long courseId
) {
    @Builder
    public OrderCreateRequest {}
}
