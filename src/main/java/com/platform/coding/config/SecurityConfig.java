package com.platform.coding.config;

import com.platform.coding.config.jwt.JwtAuthenticationFilter;
import com.platform.coding.domain.user.UserType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
// @PreAuthorize 어노테이션을 사용하기 위해 추가
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Bcrypt는 현재 가장 널리 사용되는 해싱 알고리즘 중 하나임.
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        // CSRF, Form Login, HTTP Basic 비활성화
        http.csrf(csrf -> csrf.disable())
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            // 세션을 사용하지 않는 Stateless 서버로 설정
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // API 경로별 접근 권한 설정
            .authorizeHttpRequests(authz -> authz
                    // '/api/users/signup', '/api/users/login' 등은 누구나 접근 가능
                    .requestMatchers("/", "/login", "/signup", "/courses", "/courses/{courseId}",
                            "/.well-known/**", "/css/**", "/js/**", "/images/**", "/favicon.ico",
                            "/api/users/signup", "/api/users/login", "/api/users/logout", "/api/users/refresh",
                            "/api/courses", "/api/courses/{courseId}", "/api/lectures/{lectureId}",
                            "/?error=**", "/login?error=**").permitAll()
                    // 관리자 웹 페이지 경로 추가
                    .requestMatchers("/admin/**").hasAnyRole(UserType.SUPER_ADMIN.name(), UserType.CONTENT_MANAGER.name())
                    // '/api/admin/reward-policy' GET 요청은 모든 관리자가 가능
                    .requestMatchers(HttpMethod.GET, "/api/admin/reward-policies").hasAnyRole(UserType.SUPER_ADMIN.name(), UserType.CONTENT_MANAGER.name())
                    // '/api/admin/**' 경로는 SUPER_ADMIN 또는 CONTENT_MANAGER 권한 필요
                    .requestMatchers("/api/admin/**").hasAnyRole(UserType.SUPER_ADMIN.name(), UserType.CONTENT_MANAGER.name())
                    // 학부모 관련 API 권한 설정 추가
                    .requestMatchers("/my-profile/**", "/api/parents/**").hasRole(UserType.PARENT.name())
                    // 그 외 모든 요청은 인증된 사용자만 접근 가능
                    .anyRequest().authenticated()

            )
            // [핵심] 예외 처리 핸들러 등록
            .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint(customAuthenticationEntryPoint) // 인증 실패 시 (401)
                    .accessDeniedHandler(customAccessDeniedHandler)           // 인가 실패 시 (403)
            )
            // 우리가 만든 JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 추가
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
