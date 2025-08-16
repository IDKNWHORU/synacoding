package com.platform.coding.domain.payment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments", schema = "platform")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    // 실제 결제된 금액
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    // PG사 거래 ID
    @Column(name = "pg_transaction_id")
    private String pgTransactionId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "used_reward_id")
    private Reward usedReward;

    @Column(name = "paid_at", nullable = false, updatable = false)
    private Instant paidAt;

    @Builder
    public Payment(Order order, PaymentMethod paymentMethod, BigDecimal amount, String pgTransactionId, Reward usedReward) {
        this.order = order;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.pgTransactionId = pgTransactionId;
        this.usedReward = usedReward;
        this.paidAt = Instant.now();
    }
}