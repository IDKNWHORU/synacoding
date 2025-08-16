package com.platform.coding.service.scheduler;

import com.platform.coding.domain.payment.RewardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class RewardScheduler {
    private final RewardRepository rewardRepository;

    /**
     * 매일 자정에 만료되었지만 사용되지 않은 보상(포인트/쿠폰)을 삭제
     * Cron 표현식: "0 0 0 * * * (초, 분, 시, 일, 월, 요일)
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void cleanupExpiredRewards() {
        log.info("만료된 보상 정리 작업을 시작합니다.");
        final Instant now = Instant.now();
        rewardRepository.deleteExpiredAndUnusedRewards(now);
        log.info("만료된 보상 정리 작업을 완료했습니다.");
    }
}
