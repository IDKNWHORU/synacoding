package com.platform.coding.config.jwt;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserRepository;
import com.platform.coding.util.CookieUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final CookieUtil cookieUtil;
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 1. 요청 헤더에서 토큰을 가져옵니다.
        String token = resolveToken(request);

        // 2. 토큰이 유효한지 검증합니다.
        if (StringUtils.hasText(token)) {
            try {
                DecodedJWT decodedJWT = jwtUtil.verify(token);
                // 3. 토큰이 유효하면 사용자 정보를 조회하고 인증 객체를 생성합니다.
                Long userId = decodedJWT.getClaim("id").asLong();
                // 4. Spring Security 컨텍스트에 인증 정보를 등록합니다.
                userRepository.findById(userId).ifPresent(this::saveAuthentication);
            } catch (JWTVerificationException e) {
                // 토큰이 유효하지 않으면(만료, 위조 등) SecurityContext를 비운다.
                // 그러면 해당 요청은 '인증되지 않은' 요청으로 처리되며,
                // 뒤이은 Spring Security 필터 체인에서 이를 감지하고
                // 우리가 등록한 CustomAuthenticationEntryPoint를 호출하게 된다.
                log.warn("JWT 토큰 검증 실패: {}, URI: {}", e.getMessage(), request.getRequestURI());
                SecurityContextHolder.clearContext();
            }
        }

        // 다음 필터로 요청과 응답을 전달합니다.
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        // 1. 헤더에서 토큰 찾기 (API 호출용)
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        // 2. 헤더에 없으면 쿠키에서 토큰 찾기 (웹 페이지 접근용)
        return cookieUtil.getCookie(request, CookieUtil.ACCESS_TOKEN_COOKIE_NAME)
                .map(Cookie::getValue)
                .orElse(null);
    }

    private void saveAuthentication(User user) {
        // 인증된 사용자 정보와 권한을 담은 Authentication 객체 생성
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user, // Principal (주로 사용자 객체 자체를 넣음)
                null, // Credentials (비밀번호, 보통 null 처리)
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getUserType().name())) // Authorities (권한 목록)
        );
        // SecurityContext에 인증 정보를 저장합니다.
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}