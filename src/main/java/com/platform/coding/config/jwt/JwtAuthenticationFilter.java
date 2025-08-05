package com.platform.coding.config.jwt;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Log4j2
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
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
                // 토큰이 유효하지 않을 경우의 처리 (예: 로깅)
                log.error("e", e);
                // 여기서는 아무것도 하지 않고 다음 필터로 넘깁니다. (인증 실패)
            }
        }

        // 다음 필터로 요청과 응답을 전달합니다.
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
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