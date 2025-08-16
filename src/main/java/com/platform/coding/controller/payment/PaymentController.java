package com.platform.coding.controller.payment;

import com.platform.coding.domain.user.User;
import com.platform.coding.service.payment.PaymentService;
import com.platform.coding.service.payment.dto.PaymentRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<Void> processPayment(
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal User parent
            ) {
        Long paymentId = paymentService.processPayment(request, parent);
        return ResponseEntity.created(URI.create("/api/payments/" + paymentId)).build();
    }
}
