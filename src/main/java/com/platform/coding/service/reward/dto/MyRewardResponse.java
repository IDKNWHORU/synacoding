package com.platform.coding.service.reward.dto;

import com.platform.coding.domain.payment.Reward;
import com.platform.coding.domain.payment.RewardType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

public record MyRewardResponse (
        Long rewardId,
        RewardType rewardType,
        BigDecimal amount,
        Instant expiresAt
){
    @Builder
    public MyRewardResponse {}

    public static MyRewardResponse fromEntity(Reward reward) {
        return MyRewardResponse.builder()
                .rewardId(reward.getId())
                .rewardType(reward.getRewardType())
                .amount(reward.getAmount())
                .expiresAt(reward.getExpiresAt())
                .build();
    }
}
