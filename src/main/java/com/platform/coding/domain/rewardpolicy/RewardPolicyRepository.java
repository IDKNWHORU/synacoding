package com.platform.coding.domain.rewardpolicy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RewardPolicyRepository extends JpaRepository<RewardPolicy, Long> {
    Optional<RewardPolicy> findByPolicyKey(PolicyKey key);
}
