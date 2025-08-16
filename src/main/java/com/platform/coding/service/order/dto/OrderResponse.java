package com.platform.coding.service.order.dto;

import com.platform.coding.domain.payment.Order;
import lombok.Builder;

import java.math.BigDecimal;

public record OrderResponse(
        Long orderId,
        String orderUid,
        BigDecimal totalPrice
) {
    @Builder
    public OrderResponse {}

    public static OrderResponse fromEntity(Order order) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .orderUid(order.getOrderUid())
                .totalPrice(order.getTotalPrice())
                .build();
    }
}
