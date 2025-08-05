package com.platform.coding.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.coding.config.jwt.JwtAuthenticationFilter;
import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserRepository;
import com.platform.coding.service.user.dto.UserLoginRequest;
import com.platform.coding.service.user.dto.UserLoginResponse;
import com.platform.coding.service.user.dto.UserSignUpRequest;
import com.platform.coding.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@Testcontainers
@AutoConfigureMockMvc
public class UserControllerTest extends IntegrationTestSupport {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAllInBatch();

        // 로그인 및 내 정보 조회 테스트에 사용될 사용자를 미리 생성합니다.
        // 비밀번호는 반드시 암호화하여 저장해야 합니다.
        User loginUser = User.builder()
                .email("login@example.com")
                .passwordHash(passwordEncoder.encode("password_123"))
                .userName("로그인유저")
                .userType(com.platform.coding.domain.user.UserType.PARENT)
                .isActive(true)
                .build();
        userRepository.save(loginUser);
    }

    @Test
    @DisplayName("정상적인 정보로 학부모 회원가입을 요청하면 성공해야 한다.")
    void signUpSuccess() throws Exception {
        // given : 회원가입 요청 데이터 준비
        UserSignUpRequest request = UserSignUpRequest.builder()
                .userName("학부모")
                .email("test@example.com")
                .password("password123")
                .build();

        // when & then
        mockMvc.perform(post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.userName").value("학부모"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @DisplayName("유효하지 않은 정보(짧은 비밀번호)로 회원가입을 요청하면 실패(400 Bad Request)해야 한다")
    void signUpFailWithInvalidInput() throws Exception {
        // given : 유효하지 않은 요청 데이터 준비 (비밀번호가 8자 미만)
        UserSignUpRequest request = UserSignUpRequest.builder()
                .userName("실패유저")
                .email("test@example.com")
                .password("123")
                .build();

        // when & then
        mockMvc.perform(post("/api/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest()); // HTTP 상태 코드가 400인지 확인
    }

    @Test
    @DisplayName("인증되지 않은 사용자가 보호된 API(/me)를 조회하면 실패(403 Forbidden)해야 한다.")
    void getMyInfoFailWithoutToken() throws Exception {
        // given: 인증 토큰 없음

        // when & then
        mockMvc.perform(get("/api/users/me"))
                .andDo(print())
                .andExpect(status().isForbidden()); // Spring Security는 인증되지 않은 접근에 기본적으로 403을 반환
    }

    @Test
    @DisplayName("인증된 사용자가 보호된 API(/me)를 호출하면 자신의 정보를 응답받아야 한다.")
    void getMyInfoSuccessWithToken() throws Exception {
        // given: 먼저 로그인을 통해 JWT 토큰을 발급받는다.
        UserLoginRequest loginRequest = UserLoginRequest.builder()
                .email("login@example.com")
                .password("password_123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = loginResult.getResponse().getContentAsString();
        UserLoginResponse loginResponse = objectMapper.readValue(jsonResponse, UserLoginResponse.class);
        String accessToken = loginResponse.accessToken();

        // when & then: 발급받은 Access Token을 Authorization 헤더에 담아 API를 호출한다.
        mockMvc.perform(get("/api/users/me")
                .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("login@example.com"))
                .andExpect(jsonPath("$.userName").value("로그인유저"));
    }
}
