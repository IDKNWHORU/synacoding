package com.platform.coding.controller.mycourse;

import com.platform.coding.config.jwt.JwtAuthenticationFilter;
import com.platform.coding.config.jwt.JwtUtil;
import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.course.CourseRepository;
import com.platform.coding.domain.enrollment.Enrollment;
import com.platform.coding.domain.enrollment.EnrollmentRepository;
import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserRepository;
import com.platform.coding.domain.user.UserType;
import com.platform.coding.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class MyCourseApiControllerTest extends IntegrationTestSupport {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private EnrollmentRepository enrollmentRepository;

    private User student1;
    private User student2;
    private String student1Token;
    private String student2Token;

    @BeforeEach
    void setUp() {
        // 초기화
        userRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();
        enrollmentRepository.deleteAllInBatch();

        // 사용자 생성
        User admin = userRepository.save(User.builder()
                .email("admin@example.com")
                .passwordHash("password_hash123")
                .userName("관리자")
                .userType(UserType.SUPER_ADMIN)
                .build());
        student1 = userRepository.save(User.builder()
                .email("student1@example.com")
                .passwordHash("password_hash456")
                .userName("학생1")
                .userType(UserType.STUDENT)
                .build());
        student2 = userRepository.save(User.builder()
                .email("student2@example.com")
                .passwordHash("password_hash789")
                .userName("학생2")
                .userType(UserType.STUDENT)
                .build());
        // 토큰 발급
        student1Token = jwtUtil.createAccessToken(student1);
        student2Token = jwtUtil.createAccessToken(student2);

        // 강의 생성
        Course courseA = courseRepository.save(Course.builder()
                .admin(admin)
                .title("강의A")
                .price(BigDecimal.TEN)
                .build());
        Course courseB = courseRepository.save(Course.builder()
                .admin(admin)
                .title("강의B")
                .price(BigDecimal.TEN)
                .build());
        Course courseC = courseRepository.save(Course.builder()
                .admin(admin)
                .title("강의C")
                .price(BigDecimal.TEN)
                .build());

        // 수강 등록 데이터 생성
        // 학생1은 강의 A, B를 수강
        enrollmentRepository.save(new Enrollment(student1, courseA));
        enrollmentRepository.save(new Enrollment(student1, courseB));
        // 학생2는 강의 B, C를 수강
        enrollmentRepository.save(new Enrollment(student2, courseB));
        enrollmentRepository.save(new Enrollment(student2, courseC));
    }

    @Test
    @DisplayName("학생1로 로그인하여 내 강의실을 조회하면, 학생1이 수강하는 강의 2개만 보여야 한다.")
    void getMyCourseForStudent1() throws Exception {
        // when & then
        mockMvc.perform(get("/api/my-courses")
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + student1Token))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.title == '강의A')]").exists())
                .andExpect(jsonPath("$[?(@.title == '강의B')]").exists())
                .andExpect(jsonPath("$[?(@.title == '강의C')]").doesNotExist());
    }

    @Test
    @DisplayName("학생2로 로그인하여 내 강의실을 조회하면, 학생2가 수강하는 강의 2개만 보여야 한다.")
    void getMyCoursesForStudent2() throws Exception {
        // when & then
        mockMvc.perform(get("/api/my-courses")
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + student2Token))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.title == '강의A')]").doesNotExist())
                .andExpect(jsonPath("$[?(@.title == '강의B')]").exists())
                .andExpect(jsonPath("$[?(@.title == '강의C')]").exists());
    }

    @Test
    @DisplayName("로그인하지 않고 내 강의실을 조회하면 실패(403 Forbidden)해야 한다.")
    void getMyCoursesFailWithoutToken() throws Exception {
        // when & then
        mockMvc.perform(get("/api/my-courses"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
}
