package com.platform.coding.controller.order;

import com.platform.coding.domain.user.User;
import com.platform.coding.service.order.OrderService;
import com.platform.coding.service.order.dto.OrderCreateRequest;
import com.platform.coding.service.order.dto.OrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderCreateRequest request,
            @AuthenticationPrincipal User parent) {
        OrderResponse response = orderService.createOrder(request, parent);
        return ResponseEntity.ok(response);
    }
}
