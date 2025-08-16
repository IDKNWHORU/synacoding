package com.platform.coding.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        log.warn("접근 거부됨: User='{}', URI='{}', Message='{}'",
                request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "Anonymous",
                request.getRequestURI(),
                accessDeniedException.getMessage());

        // 접근 거부 시 홈페이지로 리디렉션하며, 쿼리 파라미터로 에러 원인을 전달
        response.sendRedirect("/?error=access_denied");
    }
}