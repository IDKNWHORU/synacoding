package com.platform.coding.service.payment.dto;

import com.platform.coding.domain.payment.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

public record PaymentRequest(
        @NotNull(message = "주문 ID는 필수입니다.")
        Long orderId,

        @NotNull(message = "결제 수단은 필수입니다.")
        PaymentMethod paymentMethod,

        // 어떤 학생에게 강의를 등록할지 ID를 받음
        @NotNull(message = "수강할 학생 ID는 필수입니다.")
        Long studentId,

        // 사용할 포인트 ID (선택 사항)
        Long pointId,

        // 사용할 쿠폰 ID (선택 사항)
        Long couponId
) {
    @Builder
    public PaymentRequest {}
}
