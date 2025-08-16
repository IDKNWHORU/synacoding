package com.platform.coding.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.coding.config.jwt.JwtUtil;
import com.platform.coding.domain.rewardpolicy.PolicyKey;
import com.platform.coding.domain.rewardpolicy.RewardPolicy;
import com.platform.coding.domain.rewardpolicy.RewardPolicyRepository;
import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserRepository;
import com.platform.coding.domain.user.UserType;
import com.platform.coding.service.admin.dto.RewardPolicyUpdateRequest;
import com.platform.coding.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AdminRewardPolicyControllerTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RewardPolicyRepository rewardPolicyRepository;

    private String superAdminToken;
    private String contentManagerToken;

    @BeforeEach
    void setUp() throws Exception {
        User superAdmin = userRepository.save(User.builder()
                .email("admin@.com")
                .passwordHash("password_hash123")
                .userName("sa")
                .userType(UserType.SUPER_ADMIN)
                .build());
        User contentManager = userRepository.save(User.builder()
                .email("content@example.com")
                .passwordHash("password_hash456")
                .userName("cm")
                .userType(UserType.CONTENT_MANAGER)
                .build());
        superAdminToken = jwtUtil.createAccessToken(superAdmin);
        contentManagerToken = jwtUtil.createAccessToken(contentManager);

        RewardPolicy minLengthPolicy = createPolicy(PolicyKey.REVIEW_REWARD_MIN_LENGTH, "50", "리뷰 최소 글자 수");
        RewardPolicy pointAmountPolicy = createPolicy(PolicyKey.REVIEW_REWARD_POINT_AMOUNT, "1000", "지급 포인트");

        rewardPolicyRepository.saveAll(List.of(minLengthPolicy, pointAmountPolicy));
    }

    // RewardPolicy 엔티티를 생성하는 헬퍼 메소드
    private RewardPolicy createPolicy(PolicyKey key, String value, String description) throws Exception {
        Constructor<RewardPolicy> constructor = RewardPolicy.class.getDeclaredConstructor();        constructor.setAccessible(true); // 접근 제한을 해제
        constructor.setAccessible(true); // 접근 제한을 해제
        RewardPolicy policy = constructor.newInstance(); // 인스턴스 생성

        // 리플렉션을 사용해 private 필드에 값 설정
        setField(policy, "policyKey", key);
        setField(policy, "policyValue", value);
        setField(policy, "description", description);
        return policy;
    }

    // Reflection을 사용하여 필드 값을 설정하는 헬퍼 메소드
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    @DisplayName("관리자는 현재 설정된 보상 정책 목록을 조회할 수 있다.")
    void getRewardPolicies() throws Exception {
        mockMvc.perform(get("/api/admin/reward-policies")
                        .header("Authorization", "Bearer " + contentManagerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.key=='REVIEW_REWARD_MIN_LENGTH')]").exists());
    }

    @Test
    @DisplayName("최고 관리자(SUPER_ADMIN)는 보상 정책을 수정할 수 있다.")
    void updateRewardPoliciesBySuperAdmin() throws Exception {
        // given
        var requests = List.of(
                new RewardPolicyUpdateRequest(PolicyKey.REVIEW_REWARD_MIN_LENGTH, "100"),
                new RewardPolicyUpdateRequest(PolicyKey.REVIEW_REWARD_POINT_AMOUNT, "1500")
        );

        // when & then
        mockMvc.perform(put("/api/admin/reward-policies")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isNoContent());

        // DB 검증
        assertThat(rewardPolicyRepository.findByPolicyKey(PolicyKey.REVIEW_REWARD_MIN_LENGTH).get().getPolicyValue()).isEqualTo("100");
        assertThat(rewardPolicyRepository.findByPolicyKey(PolicyKey.REVIEW_REWARD_POINT_AMOUNT).get().getPolicyValue()).isEqualTo("1500");
    }

    @Test
    @DisplayName("콘텐츠 매니저는 보상 정책을 수정할 수 없다 (403 Forbidden).")
    void updateRewardPoliciesByContentManager() throws Exception {
        // given
        var requests = List.of(new RewardPolicyUpdateRequest(PolicyKey.REVIEW_REWARD_MIN_LENGTH, "10"));

        // when & then
        mockMvc.perform(put("/api/admin/reward-policies")
                        .header("Authorization", "Bearer " + contentManagerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isForbidden());
    }
}