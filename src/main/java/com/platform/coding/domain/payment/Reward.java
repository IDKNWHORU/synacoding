package com.platform.coding.domain.payment;

import com.platform.coding.domain.user.User;
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
@Table(name = "rewards", schema = "platform")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reward {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reward_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "reward_type", nullable = false)
    private RewardType rewardType;
    
    // 포인트 금액 또는 쿠폰 할인액
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "is_used", nullable = false)
    private boolean isUsed = false;

    @Builder
    public Reward(User user, RewardType rewardType, BigDecimal amount, Instant expiresAt) {
        this.user = user;
        this.rewardType = rewardType;
        this.amount = amount;
        this.expiresAt = expiresAt;
    }

    public void use() {
        if (this.isUsed) {
            throw new IllegalStateException("이미 사용된 보상입니다.");
        }
        if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
            throw new IllegalStateException("만료된 보상입니다.");
        }
        this.isUsed = true;
    }
}
