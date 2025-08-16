package com.platform.coding.service.reward;

import com.platform.coding.domain.payment.RewardRepository;
import com.platform.coding.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminRewardService {
    private final RewardRepository rewardRepository;
    private final UserRepository userRepository;
}
