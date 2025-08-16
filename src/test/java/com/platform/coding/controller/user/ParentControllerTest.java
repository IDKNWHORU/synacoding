package com.platform.coding.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.coding.config.jwt.JwtAuthenticationFilter;
import com.platform.coding.config.jwt.JwtUtil;
import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserRepository;
import com.platform.coding.domain.user.UserType;
import com.platform.coding.service.user.dto.ChildAccountCreateRequest;
import com.platform.coding.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class ParentControllerTest extends IntegrationTestSupport {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String parentToken;
    private String studentToken;
    private User parent;

    @BeforeEach
    void setUp() {
        userRepository.deleteAllInBatch();

        parent = userRepository.save(User.builder()
                .email("parent@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .userName("학부모")
                .userType(UserType.PARENT)
                .build());
        User studentUser = userRepository.save(User.builder()
                .email("student@example.com")
                .passwordHash(passwordEncoder.encode("password456"))
                .userName("기존학생")
                .userType(UserType.SUPER_ADMIN)
                .parent(parent)
                .build());

        parentToken = jwtUtil.createAccessToken(parent);
        studentToken = jwtUtil.createAccessToken(studentUser);
    }

    @Test
    @DisplayName("학부모가 자신의 자녀 계정을 성공적으로 생성한다.")
    void createChildAccountSuccess() throws Exception {
        // given
        ChildAccountCreateRequest request = ChildAccountCreateRequest.builder()
                .userName("새로운자녀")
                .email("new.child@example.com")
                .password("password123")
                .build();
        long initialUserCount = userRepository.count();

        // when & then
        mockMvc.perform(post("/api/parents/me/children")
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.userName").value("새로운자녀"))
                .andExpect(jsonPath("$.email").value("new.child@example.com"))
                .andReturn();

        // DB 검증
        User newChild = userRepository.findByEmail(request.email()).orElseThrow();
        assertThat(newChild.getUserType()).isEqualTo(UserType.STUDENT);
        assertThat(newChild.getParent().getId()).isEqualTo(parent.getId());
    }

    @Test
    @DisplayName("이미 존재하는 이메일로 자녀 계정 생성을 시도하면 실패(400 Bad Request)해야 한다.")
    void createChildAccountFailWithDuplicateEmail() throws Exception {
        // given
        ChildAccountCreateRequest request = ChildAccountCreateRequest.builder()
                .userName("중복된자녀")
                .email("student@example.com")
                .password("password123")
                .build();

        // when & then
        mockMvc.perform(post("/api/parents/me/children")
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미 가입된 이메일입니다."));
    }

    @Test
    @DisplayName("학부모가 아닌 사용자(학생)가 자녀 계정 생성을 시도하면 실패(403 Forbidden)해야 한다.")
    void createChildAccountFailWithStudentRole() throws Exception {
        // given
        ChildAccountCreateRequest request = ChildAccountCreateRequest.builder()
                .userName("권한없는자녀")
                .email("no.auth@example.com")
                .password("password123")
                .build();

        // when & then
        mockMvc.perform(post("/api/parents/me/children")
                        // 학생 토큰 사용
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("학부모는 자신의 자녀 목록을 성공적으로 조회할 수 있다.")
    void getMyChildrenSuccess() throws Exception {
        // given: beforeEach에서 이미 '기존학생' 1명이 parentUser의 자녀로 등록됨

        // when & then
        mockMvc.perform(get("/api/parents/me/children")
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + parentToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userName").value("기존학생"))
                .andExpect(jsonPath("$[0].email").value("student@example.com"));
    }
}
