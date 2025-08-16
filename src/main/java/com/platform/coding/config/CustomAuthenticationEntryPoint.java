package com.platform.coding.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        log.warn("인증되지 않은 접근입니다. URI: {}, Error: {}", request.getRequestURI(), authException.getMessage());
        // 인증이 필요한 페이지에 비인증 상태로 접근 시 로그인 페이지로 리디렉션
        response.sendRedirect("/login?error=unauthorized");
    }
}