package com.platform.coding.controller.user;

import com.platform.coding.domain.user.User;
import com.platform.coding.service.user.UserService;
import com.platform.coding.service.user.dto.UserLoginRequest;
import com.platform.coding.service.user.dto.UserLoginResponse;
import com.platform.coding.service.user.dto.UserSignUpRequest;
import com.platform.coding.service.user.dto.UserSignupResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    // POST /api/users/signup 요청을 처리함
    @PostMapping("/signup")
    public ResponseEntity<UserSignupResponse> signup(@Valid @RequestBody UserSignUpRequest request) {
        // `@Valid` 어노테이션으로 UserSignUpRequest의 유효성 검사를 자동으로 수행함.
        // `@RequestBody` 는 HTTP 요청의 본문(JSON)을 자바 객체로 변환함.
        UserSignupResponse response = userService.signUp(request);

        // 성공시, HTTP 상태 코드 201(Created)와 함께 응답 데이터를 반환합니다.
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<UserLoginResponse> login(@Valid @RequestBody UserLoginRequest request) {
        UserLoginResponse response = userService.login(request);

        // 성공 시 200 OK와 함계 토큰 반환
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserSignupResponse> getMyInfo(@AuthenticationPrincipal User user) {
        // '@AuthenticationPrincipal' 어노테이션을 사용하면,
        // SecurityContext에 저장된 인증 객체(Principal)를 직접 주입받을 수 있습니다.
        UserSignupResponse response = UserSignupResponse.fromEntity(user);
        return ResponseEntity.ok(response);
    }
}
