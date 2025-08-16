package com.platform.coding.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final NotificationInterceptor notificationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(notificationInterceptor)
                // 모든 경로에 인터셉터를 적용
                .addPathPatterns("/**")
                // 단, 정적 리소스(css, js 등) 경로는 제외하여 불필요한 DB 조회를 방지
                .excludePathPatterns("/css/**", "/js/**", "/images/**", "/favicon.ico");
    }
}