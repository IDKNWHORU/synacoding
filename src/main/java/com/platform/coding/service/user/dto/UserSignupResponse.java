package com.platform.coding.service.user.dto;

import com.platform.coding.domain.user.User;
import lombok.Builder;

public record UserSignupResponse(
        Long userId,
        String userName,
        String email
) {
    @Builder
    public UserSignupResponse {}

    // User 엔티티를 DTO로 변환하는 정적 팩토리 메소드
    public static UserSignupResponse fromEntity(User user) {
        return UserSignupResponse.builder()
                .userId(user.getId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .build();
    }
}
