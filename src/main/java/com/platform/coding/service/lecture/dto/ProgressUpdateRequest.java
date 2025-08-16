package com.platform.coding.service.lecture.dto;

import jakarta.validation.constraints.Min;
import lombok.Builder;

public record ProgressUpdateRequest(
        @Min(value = 0, message = "시청 시간은 0 이상이어야 합니다.")
        int currentViewedSeconds
) {
    @Builder
    public ProgressUpdateRequest {}
}
