package com.platform.coding.domain.refund;

import com.platform.coding.domain.payment.Payment;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "refunds", schema = "platform")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Refund {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_id")
    private Long id;

    // 어떤 결제 건에 대한 환불인지 (1:1 관계)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private Payment payment;

    @Column(name = "refund_amount", nullable = false, precision = 10, scale = 2)
    // 환불될 금액
    private BigDecimal refundAmount;
    
    @Lob
    @Column(name = "reason", columnDefinition = "TEXT")
    // 환불 사유
    private String reason;

    @Column(name = "refunded_at", nullable = false, updatable = false)
    private Instant refundedAt;

    @Builder
    public Refund(Payment payment, BigDecimal refundAmount, String reason) {
        this.payment = payment;
        this.refundAmount = refundAmount;
        this.reason = reason;
        this.refundedAt = Instant.now();
    }
}
