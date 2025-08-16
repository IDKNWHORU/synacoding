package com.platform.coding.controller.refund;

import com.platform.coding.domain.user.User;
import com.platform.coding.service.refund.RefundService;
import com.platform.coding.service.refund.dto.RefundRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/payments/{paymentId}/refund-request")
@RequiredArgsConstructor
public class RefundController {
    private final RefundService refundService;

    @PostMapping
    public ResponseEntity<Void> requestRefund(
            @PathVariable Long paymentId,
            @Valid @RequestBody RefundRequest request,
            @AuthenticationPrincipal User parent
            ) {
        Long refundId = refundService.requestRefund(paymentId, request, parent);
        return ResponseEntity.created(URI.create("/api/refunds/" + refundId)).build();
    }
}
