package com.platform.coding.service.reward.dto;

import com.platform.coding.domain.payment.RewardType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminRewardCreateRequest(
        @NotNull(message = "사용자 ID는 필수입니다.")
        Long userId,

        @NotNull(message = "보상 타입은 필수입니다.")
        RewardType rewardType,

        @NotNull(message = "보상 금액은 필수입니다.")
        @DecimalMin(value = "0", inclusive = false, message = "보상 금액은 0보다 커야 합니다.")
        BigDecimal amount,
        
        // 선택적
        @Future(message = "만료이은 현재 시간 이후여야 합니다.")
        Instant expiresAt

) {
    @Builder
    public AdminRewardCreateRequest {}
}
