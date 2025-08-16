package com.platform.coding.service.refund.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

public record RefundRequest(
        @Size(max = 500, message = "환불 사유는 500자를 넘을 수 없습니다.")
        String reason
) {
    @Builder
    public RefundRequest {}
}
