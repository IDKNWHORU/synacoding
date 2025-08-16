package com.platform.coding.service.user.dto;

import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * 자녀 계정 생성을 위한 요청 DTO
 */
public record ChildAccountCreateRequest(
        @NotBlank(message = "자녀의 닉네임을 입력해주세요")
        @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하로 입력해주세요")
        String userName,

        @NotBlank(message = "자녀의 이메일을 입력해주세요.")
        @Email(message = "유효한 이메일 형식이 아닙니다.")
        String email,
        
        @NotBlank(message = "비밀번호를 입력해주세요")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
        String password
) {
    @Builder
    public ChildAccountCreateRequest {}

    public User toEntity(String encryptedPassword, User parent) {
        return User.builder()
                .userName(this.userName)
                .email(this.email)
                .passwordHash(encryptedPassword)
                .userType(UserType.STUDENT)
                .parent(parent)
                .isActive(true)
                .build();
    }
}
