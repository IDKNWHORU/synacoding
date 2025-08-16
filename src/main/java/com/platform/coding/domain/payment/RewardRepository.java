package com.platform.coding.domain.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface RewardRepository extends JpaRepository<Reward, Long> {
    /**
     * 만료되고 사용되지 않은 모든 보상을 삭제함.
     * 이 메소드는 스케줄러에 의해 주기적으로 호출됨.
     * @param now 현재 시간
     */
    @Modifying
    @Query("DELETE FROM Reward r WHERE r.expiresAt < :now AND r.isUsed = false")
    void deleteExpiredAndUnusedRewards(@Param("now")Instant now);
}
