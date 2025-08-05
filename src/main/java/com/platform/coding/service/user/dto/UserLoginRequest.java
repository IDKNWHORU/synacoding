package com.platform.coding.service.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

public record UserLoginRequest(
        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "유효한 이메일 형식이 아닙니다.")
        String email,
        @NotBlank(message = "비밀번호를 입력해주세요")
        String password
) {
    @Builder
    public UserLoginRequest {}
}
