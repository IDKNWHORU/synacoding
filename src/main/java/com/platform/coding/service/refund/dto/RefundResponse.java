package com.platform.coding.service.refund.dto;

import com.platform.coding.domain.payment.OrderStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

public record RefundResponse(
        Long refundId,
        Long orderId,
        BigDecimal refundAmount,
        OrderStatus orderStatus,
        Instant refundedAt
) {
    @Builder
    public RefundResponse {}
}
