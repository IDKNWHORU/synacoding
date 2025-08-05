package com.platform.coding.service.user.dto;

import lombok.Builder;

public record UserLoginResponse(
        String accessToken,
        String refreshToken
) {
    @Builder
    public UserLoginResponse {}
}
