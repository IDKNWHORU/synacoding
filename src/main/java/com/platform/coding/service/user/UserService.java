package com.platform.coding.service.user;

import com.platform.coding.config.jwt.JwtProperties;
import com.platform.coding.config.jwt.JwtUtil;
import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserRepository;
import com.platform.coding.service.user.dto.UserLoginRequest;
import com.platform.coding.service.user.dto.UserLoginResponse;
import com.platform.coding.service.user.dto.UserSignUpRequest;
import com.platform.coding.service.user.dto.UserSignupResponse;
import com.platform.coding.util.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor // final 필드에 대한 생성자를 자동으로 만들어줌
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final CookieUtil cookieUtil;

    // 이 메소드는 하나의 트랜잭션으로 묶어 처리함
    @Transactional
    public UserSignupResponse signUp(UserSignUpRequest request) {
        // 이메일 중복 확인
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        // 비밀번호 암호화
        String encryptedPassword = passwordEncoder.encode(request.password());

        // DTO를 엔티티로 변환하고 저장
        User newUser = request.toEntity(encryptedPassword);
        User savedUser = userRepository.save(newUser);

        // 엔티티를 응답 DTO로 변환하여 반환
        return UserSignupResponse.fromEntity(savedUser);
    }

    // 읽기 전용 트랜잭션
    @Transactional
    public UserLoginResponse login(UserLoginRequest request, HttpServletResponse response) {
        // 이메일로 사용자 조회
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        // 비밀번호 일치 여부 확인
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("잘못된 비밀번호입니다.");
        }

        // JWT 생성
        String accessToken = jwtUtil.createAccessToken(user);
        String refreshToken = jwtUtil.createRefreshToken(user);

        // DB에 리프레시 토큰 저장
        user.updateRefreshToken(refreshToken);
        userRepository.save(user);

        cookieUtil.createAccessTokenCookie(response, accessToken, jwtProperties.getAccessTokenValidityInSeconds());
        cookieUtil.createRefreshTokenCookie(response, refreshToken, jwtProperties.getRefreshTokenValidityInSeconds());

        return new UserLoginResponse(accessToken, refreshToken);
    }

    @Transactional
    public void logout(User user, HttpServletResponse response) {
        // DB에서 사용자 조회 및 리프레시 토큰 제거
        userRepository.findById(user.getId()).ifPresent(u -> {
            u.updateRefreshToken(null);
            userRepository.save(u);
        });

        // 쿠키 삭제
        cookieUtil.deleteCookie(response, CookieUtil.ACCESS_TOKEN_COOKIE_NAME);
        cookieUtil.deleteCookie(response, CookieUtil.REFRESH_TOKEN_COOKIE_NAME);
    }

    @Transactional
    public void refreshAccessToken(HttpServletRequest request, HttpServletResponse response) {
        // 1. 쿠키에서 리프레시 토큰 가져오기
        String refreshToken = cookieUtil.getCookie(request, CookieUtil.REFRESH_TOKEN_COOKIE_NAME)
                .map(Cookie::getValue)
                .orElseThrow(() -> new IllegalArgumentException("리프레시 토큰이 없습니다."));

        // 2. 리프레시 토큰 검증 (유효기간, 서명 등)
        jwtUtil.verify(refreshToken);

        // 3. 토큰에서 사용자 ID 추출하여 DB에서 사용자 조회
        Long userId = jwtUtil.verify(refreshToken).getClaim("id").asLong();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 4. DB에 저장된 리프레시 토큰과 일치하는지 확인 (보안 강화)
        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }

        // 5. 새로운 액세스 토큰 생성
        String newAccessToken = jwtUtil.createAccessToken(user);

        // 6. 새로운 액세스 토큰을 쿠키에 저장
        cookieUtil.createAccessTokenCookie(response, newAccessToken, jwtProperties.getAccessTokenValidityInSeconds());
    }
}
