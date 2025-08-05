package com.platform.coding.service.user;

import com.platform.coding.config.jwt.JwtUtil;
import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserRepository;
import com.platform.coding.service.user.dto.UserLoginRequest;
import com.platform.coding.service.user.dto.UserLoginResponse;
import com.platform.coding.service.user.dto.UserSignUpRequest;
import com.platform.coding.service.user.dto.UserSignupResponse;
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
    @Transactional(readOnly = true)
    public UserLoginResponse login(UserLoginRequest request) {
        // 이메일로 사용자 조회
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 이메일입니다."));

        // 비밀번호 일치 여부 확인
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("잘못된 비밀번호입니다.");
        }

        // JWT 생성
        String accessToken = jwtUtil.createAccessKey(user);
        String refreshToken = jwtUtil.createRefreshToken(user);

        return new UserLoginResponse(accessToken, refreshToken);
    }
}
