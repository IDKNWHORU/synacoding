package com.platform.coding.config;

import com.platform.coding.config.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Bcrypt는 현재 가장 널리 사용되는 해싱 알고리즘 중 하나임.
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        // CSRF, Form Login, HTTP Basic 비활성화
        http.csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            // 세션을 사용하지 않는 Stateless 서버로 설정
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // API 경로별 접근 권한 설정
            .authorizeHttpRequests(authz -> authz
                    // '/api/users/signup', '/api/users/login' 등은 누구나 접근 가능
                    .requestMatchers("/api/users/signup", "/api/users/login", "/api/courses", "/api/courses/{courseId}").permitAll()
                    // '/api/admin/**' 경로는 SUPER_ADMIN 또는 CONTENT_MANAGER 권한 필요
                    .requestMatchers("/api/admin/**").hasAnyRole("SUPER_ADMIN", "CONTENT_MANAGER")
                    // 그 외 모든 요청은 인증된 사용자만 접근 가능
                    .anyRequest().authenticated()

            )
            // 우리가 만든 JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
