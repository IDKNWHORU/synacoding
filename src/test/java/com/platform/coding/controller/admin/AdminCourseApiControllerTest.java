package com.platform.coding.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.coding.config.jwt.JwtAuthenticationFilter;
import com.platform.coding.config.jwt.JwtUtil;
import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.course.CourseRepository;
import com.platform.coding.domain.course.CourseStatus;
import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserRepository;
import com.platform.coding.domain.user.UserType;
import com.platform.coding.service.admin.dto.CourseCreateRequest;
import com.platform.coding.service.admin.dto.CourseUpdateRequest;
import com.platform.coding.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class AdminCourseApiControllerTest extends IntegrationTestSupport {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private JwtUtil jwtUtil;

    private String adminToken;
    private String normalUserToken;
    private Course existingCourse;

    @BeforeEach
    void setUp() {
        userRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();
        
        // 관리자, 일반 사용자 생성
        User adminUser = userRepository.save(User.builder()
                .email("admin@example.com")
                .passwordHash("password_123")
                .userName("관리자")
                .userType(UserType.CONTENT_MANAGER)
                .build());
        User normalUser = userRepository.save(User.builder()
                .email("user@example.com")
                .passwordHash("password_456")
                .userName("일반사용자")
                .userType(UserType.PARENT)
                .build());

        adminToken = jwtUtil.createAccessToken(adminUser);
        normalUserToken = jwtUtil.createAccessToken(normalUser);

        // 수정 테스트를 위한 기존 강의 데이터 생성
        existingCourse = Course.builder()
                .title("기존 강의")
                .description("수정 전 설명입니다.")
                .price(new BigDecimal("50000"))
                .admin(adminUser)
                .build();
        
        courseRepository.save(existingCourse);
    }

    @Test
    @DisplayName("관리자(CONTENT_MANAGER) 권한으로 강의 생성을 요청하면 성공해야 한다.")
    void createCourseSuccessWithAdminRole() throws Exception {
        // given
        CourseCreateRequest request = CourseCreateRequest.builder()
                .title("신규 강의")
                .description("신규 강의 설명입니다.")
                .price(new BigDecimal("1000000"))
                .build();

        mockMvc.perform(post("/api/admin/courses")
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("신규 강의"))
                .andExpect(jsonPath("$.instructorName").value("관리자"));
    }

    @Test
    @DisplayName("일반 사용자(PARENT) 권한으로 강의 생성을 요청하면 실패(403 Forbidden)해야한다.")
    void createCourseFailsWithNormalUserRole() throws Exception {
        // given
        CourseCreateRequest request = CourseCreateRequest.builder()
                .title("권한 없는 강의")
                .price(BigDecimal.ZERO)
                .build();

        // when & then
        mockMvc.perform(post("/api/admin/courses")
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + normalUserToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증 없이(토큰 없이) 강의 생성을 요청하면 실패(403 Forbidden)해야 한다.")
    void createCourseFailsWithoutToken() throws Exception {
        // given
        CourseCreateRequest request = CourseCreateRequest.builder()
                .title("권한 없는 강의")
                .price(BigDecimal.ZERO)
                .build();

        // when & then
        mockMvc.perform(post("/api/admin/courses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("관리자 권한으로 강의 수정을 요청하면 성공하고 내용이 변경되어야 한다.")
    void updateCourseSuccessWithAdminRole() throws Exception {
        // given
        CourseUpdateRequest request = CourseUpdateRequest.builder()
                .title("수정된 강의 제목")
                .description("수정된 설명입니다.")
                .price(new BigDecimal("75000.00"))
                .build();

        Long courseId = existingCourse.getId();

        // when & then
        mockMvc.perform(put("/api/admin/courses/{courseId}", courseId)
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정된 강의 제목"));
        
        // DB 에서 직접 조회하여 내용이 실제로 변경되었는지 확인
        Course updatedCourse = courseRepository.findById(courseId).orElseThrow();
        assertThat(updatedCourse.getTitle()).isEqualTo("수정된 강의 제목");
        assertThat(updatedCourse.getDescription()).isEqualTo("수정된 설명입니다.");
        assertThat(updatedCourse.getPrice()).isEqualByComparingTo("75000.00");
    }

    @Test
    @DisplayName("일반 사용자 권한으로 강의 수정을 요청하면 실패(403 Forbidden)해야 한다.")
    void updateCourseFailWithNormalUserRole() throws Exception {
        // given
        CourseUpdateRequest request = CourseUpdateRequest.builder()
                .title("수정 시도")
                .description("권한 없는 수정")
                .price(BigDecimal.ZERO)
                .build();

        Long courseId = existingCourse.getId();

        // when & then
        mockMvc.perform(put("/api/admin/courses/{courseId}", courseId)
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + normalUserToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("존재하지 않는 강의 ID로 수정을 요청하면 실패(400 Bad Request)해야 한다.")
    void updateCourseFailWithInvalidId() throws Exception {
        // given
        CourseUpdateRequest request = CourseUpdateRequest.builder()
                .title("아무 수정")
                .description("아무 내용")
                .price(BigDecimal.ZERO)
                .build();

        Long invalidCourseId = 9999L; // 존재하지 않는 ID

        // when & then
        mockMvc.perform(put("/api/admin/courses/{courseId}", invalidCourseId)
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("존재하지 않는 강의입니다."));
    }

    @Test
    @DisplayName("관리자 권한으로 강의 삭제(보관)를 요청하면 성공하고 상태가 ARCHIVED로 변경되어야 한다.")
    void archiveCourseSuccessWithAdminRole() throws Exception {
        // given: 삭제할 대상 강의 ID
        Long courseId = existingCourse.getId();

        // when & then
        mockMvc.perform(delete("/api/admin/courses/{courseId}", courseId)
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + adminToken))
                .andDo(print())
                .andExpect(status().isNoContent());

        // DB 에서 직접 조회하여 상태가 실제로 ARCHIVED로 변경되었는지 확인
        Course archivedCourse = courseRepository.findById(courseId).orElseThrow();
        assertThat(archivedCourse.getStatus()).isEqualTo(CourseStatus.ARCHIVED);
    }

    @Test
    @DisplayName("존재하지 않는 강의 ID로 삭제를 요청하면 실패(400 Bad Request)해야 한다")
    void deleteCourseFailWithInvalidId() throws Exception {
        // given: 존재하지 않는 ID
        Long invalidCourseId = 9999L;

        // when & then
        mockMvc.perform(delete("/api/admin/courses/{courseId}", invalidCourseId)
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + adminToken))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("존재하지 않는 강의입니다."));
    }
}
