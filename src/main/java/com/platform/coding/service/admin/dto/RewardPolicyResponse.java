package com.platform.coding.service.admin.dto;

import com.platform.coding.domain.rewardpolicy.PolicyKey;
import com.platform.coding.domain.rewardpolicy.RewardPolicy;
import lombok.Builder;

public record RewardPolicyResponse(
        PolicyKey key,
        String value,
        String description
) {
    @Builder
    public RewardPolicyResponse {}

    public static RewardPolicyResponse fromEntity(RewardPolicy policy) {
        return RewardPolicyResponse.builder()
                .key(policy.getPolicyKey())
                .value(policy.getPolicyValue())
                .description(policy.getDescription())
                .build();
    }
}
