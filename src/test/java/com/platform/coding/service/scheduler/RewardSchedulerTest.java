package com.platform.coding.service.scheduler;

import com.platform.coding.domain.payment.Reward;
import com.platform.coding.domain.payment.RewardRepository;
import com.platform.coding.domain.payment.RewardType;
import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserRepository;
import com.platform.coding.domain.user.UserType;
import com.platform.coding.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RewardSchedulerTest extends IntegrationTestSupport {
    @Autowired
    private RewardScheduler rewardScheduler;

    @Autowired
    private RewardRepository rewardRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        rewardRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        user = userRepository.save(User.builder()
                .email("test@example.com")
                .passwordHash("password_hash123")
                .userName("테스트유저")
                .userType(UserType.PARENT)
                .build());
    }

    @Test
    @DisplayName("스케줄러는 만료되고 사용되지 않은 보상만 정확히 삭제해야 한다.")
    void cleanupExpiredRewards() {
        // given: 4가지 종류의 보상 데이터 생성
        // 1. 만료되고 사용되지 않은 보상 (삭제 대상)
        rewardRepository.save(Reward.builder()
                .user(user)
                .rewardType(RewardType.POINT)
                .amount(BigDecimal.TEN)
                // 어제 만료
                .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build());

        // 2. 만료되었지만 이미 사용된 보상 (삭제 대상 아님)
        Reward usedAndExpiredReward = Reward.builder()
                .user(user)
                .rewardType(RewardType.COUPON)
                .amount(BigDecimal.TEN)
                .expiresAt(Instant.now().minus(2, ChronoUnit.DAYS))
                .build();
        try {
            Field isUsedField = Reward.class.getDeclaredField("isUsed");
            isUsedField.setAccessible(true);
            isUsedField.set(usedAndExpiredReward, true);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("테스트 데이터 설정 실패: 'isUsed' 필드 접근 불가", e);
        }
        rewardRepository.save(usedAndExpiredReward);

        // 3. 만료되지 않은 보상 (삭제 대상 아님)
        rewardRepository.save(Reward.builder()
                .user(user)
                .rewardType(RewardType.POINT)
                .amount(BigDecimal.TEN)
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .build());

        // 4. 만료일이 없는 영구 보상 (삭제 대상 아님)
        rewardRepository.save(Reward.builder()
                .user(user)
                .rewardType(RewardType.POINT)
                .amount(BigDecimal.TEN)
                .expiresAt(null)
                .build());

        assertThat(rewardRepository.count()).isEqualTo(4);

        // when: 스케줄러의 public 메소드를 직접 호출하여 로직을 테스트
        rewardScheduler.cleanupExpiredRewards();

        // then: 삭제 대상인 1개의 보상만 삭제되고 3개가 남아있어야 함
        List<Reward> remainingRewards = rewardRepository.findAll();
        assertThat(remainingRewards).hasSize(3);

        // 남은 보상들이 올바른 보상인지 확인
        assertThat(remainingRewards).filteredOn(Reward::isUsed)
                .as("사용된 보상은 삭제되지 않아야 합니다.")
                .hasSize(1); // 2번
        assertThat(remainingRewards).filteredOn(r -> r.getExpiresAt() != null && !r.isUsed())
                .as("만료되지 않은 보상은 삭제되지 않아야 합니다.")
                .allMatch(r -> r.getExpiresAt().isAfter(Instant.now()));

        assertThat(remainingRewards).filteredOn(r -> r.getExpiresAt() == null)
                .as("만료일이 없는 보상은 삭제되지 않아야 합니다.")
                .hasSize(1);
    }
}
