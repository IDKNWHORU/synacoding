package com.platform.coding.service.admin;

import com.platform.coding.domain.rewardpolicy.RewardPolicy;
import com.platform.coding.domain.rewardpolicy.RewardPolicyRepository;
import com.platform.coding.domain.user.User;
import com.platform.coding.service.admin.dto.RewardPolicyResponse;
import com.platform.coding.service.admin.dto.RewardPolicyUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminRewardPolicyService {
    private final RewardPolicyRepository rewardPolicyRepository;

    @Transactional(readOnly = true)
    public List<RewardPolicyResponse> getPolicies() {
        return rewardPolicyRepository.findAll().stream()
                .map(RewardPolicyResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updatePolicies(List<RewardPolicyUpdateRequest> requests, User admin) {
        for (RewardPolicyUpdateRequest request : requests) {
            RewardPolicy policy = rewardPolicyRepository.findByPolicyKey(request.key())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 정책 키입니다: " + request.key()));

            // TODO: 값의 유효성 검증 (예: 숫자인지, 양수인지 등)
            policy.updateValue(request.value(), admin);
        }
    }
}
