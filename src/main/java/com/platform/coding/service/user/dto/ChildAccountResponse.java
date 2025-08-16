package com.platform.coding.service.user.dto;

import com.platform.coding.domain.user.User;
import lombok.Builder;

/**
 * 생성된 자녀 계정 정보를 반환하기 위한 응답 DTO
 */
@Builder
public record ChildAccountResponse(
        Long userId,
        String userName,
        String email
) {
    public static ChildAccountResponse fromEntity(User user) {
        return ChildAccountResponse.builder()
                .userId(user.getId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .build();
    }
}
