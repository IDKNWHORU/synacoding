package com.platform.coding.controller.lecture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.coding.config.jwt.JwtAuthenticationFilter;
import com.platform.coding.config.jwt.JwtUtil;
import com.platform.coding.domain.course.Chapter;
import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.course.CourseRepository;
import com.platform.coding.domain.course.Lecture;
import com.platform.coding.domain.enrollment.Enrollment;
import com.platform.coding.domain.enrollment.EnrollmentRepository;
import com.platform.coding.domain.enrollment.EnrollmentStatus;
import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserRepository;
import com.platform.coding.domain.user.UserType;
import com.platform.coding.service.lecture.dto.ProgressUpdateRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class LectureControllerTest extends IntegrationTestSupport {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private ObjectMapper objectMapper;

    private User student;
    private String studentToken;
    private Lecture lecture1, lecture2, sampleLecture;
    private Enrollment enrollment;

    @BeforeEach
    void setUp() {
        // 초기화
        userRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();
        enrollmentRepository.deleteAllInBatch();

        // 사용자, 강의, 수강 등록 데이터 생성
        User admin = userRepository.save(User.builder()
                .email("admin@example.com")
                .passwordHash("password_hash123")
                .userName("관리자")
                .userType(UserType.SUPER_ADMIN)
                .build());
        student = userRepository.save(User.builder()
                .email("student@example.com")
                .passwordHash("password_hash456")
                .userName("학생")
                .userType(UserType.STUDENT)
                .build());
        studentToken = jwtUtil.createAccessToken(student);

        Course course = Course.builder()
                .admin(admin)
                .title("강의")
                .price(BigDecimal.ZERO)
                .build();
        Chapter chapter = Chapter.builder()
                .title("챕터")
                .order(1)
                .build();

        // 각 렉처에 총 재생 시간(duration)을 설정해야 진도율 계산이 가능
        lecture1 = Lecture.builder()
                .title("1강")
                .order(1)
                .isSample(false)
                .durationSeconds(100)
                .build();
        lecture2 = Lecture.builder()
                .title("2강")
                .order(2)
                .isSample(false)
                .durationSeconds(100)
                .build();
        sampleLecture = Lecture.builder()
                .title("맛보기 강의")
                .order(0)
                .isSample(true)
                .durationSeconds(60)
                .build();

        chapter.addLecture(sampleLecture);
        chapter.addLecture(lecture1);
        chapter.addLecture(lecture2);
        course.addChapter(chapter);

        Course savedCourse = courseRepository.save(course);
        Chapter savedChapter = savedCourse.getChapters().get(0);

        this.sampleLecture = savedChapter.getLectures().get(0);
        this.lecture1 = savedChapter.getLectures().get(1);
        this.lecture2 = savedChapter.getLectures().get(2);

        enrollment = enrollmentRepository.save(new Enrollment(student, course));
    }

    @Test
    @DisplayName("수강생이 동영상 시청을 시작하면, 마지막 시청 시간 0초와 함께 강의 정보를 받는다.")
    void getLectureForViewingFirstTime() throws Exception {
        mockMvc.perform(get("/api/lectures/{lectureId}", lecture1.getId())
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + studentToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastViewedSeconds").value(0));
    }

    @Test
    @DisplayName("동영상 시청 중 진도율을 업데이트하면, 전체 강의의 진도율도 함께 업데이트되어야 한다.")
    void updateProgressAndOverallProgress() throws Exception {
        // given: 1강을 95초(95%) 시청하여 완료 처리되도록 요청
        ProgressUpdateRequest request = new ProgressUpdateRequest(95);

        // when
        mockMvc.perform(post("/api/lectures/{lectureId}/progress", lecture1.getId())
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + studentToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());
        
        // then: 전체 3개의 강의 중 1개를 완료했으므로, 전체 진도율은 33.33%가 되어야 함
        Enrollment updatedEnrollment = enrollmentRepository.findById(enrollment.getId()).orElseThrow();
        assertThat(updatedEnrollment.getProgressRate()).isEqualByComparingTo("33.33");
        
        // when 2: 2강도 마저 완료
        mockMvc.perform(post("/api/lectures/{lectureId}/progress", lecture2.getId())
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());

        // then 2: 전체 3개의 강의 중 2개를 완료했으므로, 전체 진도율은 66.67%가 되어야 함.
        Enrollment updatedEnrollment2 = enrollmentRepository.findById(enrollment.getId()).orElseThrow();
        assertThat(updatedEnrollment2.getProgressRate()).isEqualByComparingTo("66.67");

        assertThat(updatedEnrollment2.getStatus()).isEqualTo(EnrollmentStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("비로그인 사용자가 맛보기 강의를 조회하면 성공(200 OK)해야 한다.")
    void getSampleLectureWithoutAuthSuccess() throws Exception {
        mockMvc.perform(get("/api/lectures/{lectureId}", sampleLecture.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lectureId").value(sampleLecture.getId()))
                .andExpect(jsonPath("$.title").value("맛보기 강의"))
                .andExpect(jsonPath("$.lastViewedSeconds").value(0));
    }

    @Test
    @DisplayName("비로그인 사용자가 일반 강의를 조회하면 실패(400 Bad Request)해야 한다.")
    void getNormalLectureWithoutAuthFail() throws Exception {
        mockMvc.perform(get("/api/lectures/{lectureId}", lecture1.getId()))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("로그인이 필요한 서비스입니다."));
    }

    @Test
    @DisplayName("로그인했지만 수강하지 않는 사용자가 일반 강의를 조회하면 실패(400 Bad Request)해야 한다.")
    void getNormalLectureWithAuthButNotEnrolledFail() throws Exception {
        // given: 새로운 학생 생성 (이 학생은 강의를 수강하지 않음)
        User newStudent = userRepository.save(User.builder()
                .email("new.student@example.com")
                .passwordHash("password_hash")
                .userName("새로운학생")
                .userType(UserType.STUDENT)
                .build());
        String newStudentToken = jwtUtil.createAccessToken(newStudent);

        // when & then
        mockMvc.perform(get("/api/lectures/{lectureId}", lecture1.getId())
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + newStudentToken))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("수강 중인 강의가 아닙니다."));
    }
}
