package com.platform.coding.service.admin.dto;

import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserType;
import lombok.Builder;

import java.time.Instant;

/**
 * 회원 관리 페이지 응답용 DTO
 */
public record UserManagementResponse(
        Long userId,
        String email,
        String userName,
        UserType userType,
        boolean isActive,
        Instant createdAt
) {
    @Builder
    public UserManagementResponse {}

    public static UserManagementResponse fromEntity(User user) {
        return UserManagementResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .userName(user.getUserName())
                .userType(user.getUserType())
                .isActive(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}