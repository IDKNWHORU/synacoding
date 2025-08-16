package com.platform.coding.service.admin.dto;

import com.platform.coding.domain.rewardpolicy.PolicyKey;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

public record RewardPolicyUpdateRequest(
        @NotNull(message = "정책 키는 필수입니다.")
        PolicyKey key,
        @NotBlank(message = "정책 값은 비워둘 수 없습니다.")
        String value
) {
    @Builder
    public RewardPolicyUpdateRequest {}
}
