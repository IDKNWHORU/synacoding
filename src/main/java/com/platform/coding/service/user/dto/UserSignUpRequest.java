package com.platform.coding.service.user.dto;

import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

public record UserSignUpRequest(
        @NotBlank(message = "이름을 입력해주세요")
        String userName,

        @NotBlank(message = "이메일을 입력해주세요")
        @Email(message = "유효한 이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "비밀번호를 입력해주세요")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 합니다.")
        String password
) {
    // 빌더 패턴을 사용한다.
    @Builder
    public UserSignUpRequest {}

    // DTO를 User 엔티티로 변환하는 메소드 (비밀번호 암호화는 서비스 계층에서 분리)
    public User toEntity(String encryptedPasswrod) {
        return User.builder()
                .userName(this.userName)
                .email(this.email)
                .passwordHash(encryptedPasswrod)
                .userType(UserType.PARENT)
                .isActive(true)
                .build();
    }
}