package com.platform.coding.service.admin.dto;

import com.platform.coding.domain.rewardpolicy.PolicyKey;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * [수정] 보상 정책 관리 폼 데이터를 받기 위한 전용 Form-backing Object
 * 이 클래스와 내부 PolicyField 클래스는 mutable(변경 가능)합니다.
 */
@Getter
@Setter
public class RewardPolicyUpdateForm {

    @Valid
    private List<PolicyField> policies = new ArrayList<>();

    /**
     * 각 정책의 key와 value 필드를 담는 내부 클래스.
     * Spring이 데이터를 바인딩할 수 있도록 기본 생성자와 getter/setter를 가집니다.
     */
    @Getter
    @Setter
    public static class PolicyField {
        @NotNull
        private PolicyKey key;

        @NotBlank
        private String value;
    }
}