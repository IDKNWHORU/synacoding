package com.platform.coding.controller.user;

import com.platform.coding.config.jwt.JwtAuthenticationFilter;
import com.platform.coding.config.jwt.JwtUtil;
import com.platform.coding.domain.course.Chapter;
import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.course.CourseRepository;
import com.platform.coding.domain.course.Lecture;
import com.platform.coding.domain.enrollment.Enrollment;
import com.platform.coding.domain.enrollment.EnrollmentRepository;
import com.platform.coding.domain.submission.Assignment;
import com.platform.coding.domain.submission.AssignmentRepository;
import com.platform.coding.domain.submission.Submission;
import com.platform.coding.domain.submission.SubmissionRepository;
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
public class ParentControllerDashboardTest extends IntegrationTestSupport {
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
    @Autowired
    private AssignmentRepository assignmentRepository;
    @Autowired
    private SubmissionRepository submissionRepository;

    private User parent, otherParent, child;
    private String parentToken, otherParentToken;
    private Course course;
    private Assignment assignment1, assignment2;

    @BeforeEach
    void setUp() {
        // DB 초기화
        userRepository.deleteAllInBatch();

        // 사용자 생성
        parent = userRepository.save(User.builder()
                .email("parent@example.com")
                .passwordHash("password_hash123")
                .userName("학부모")
                .userType(UserType.PARENT)
                .build());
        otherParent = userRepository.save(User.builder()
                .email("other@example.com")
                .passwordHash("password_hash456")
                .userName("다른학부모")
                .userType(UserType.PARENT)
                .build());
        child = userRepository.save(User.builder()
                .email("child@example.com")
                .passwordHash("password_hash789")
                .userName("자녀")
                .userType(UserType.STUDENT)
                .parent(parent)
                .build());
        User admin = userRepository.save(User.builder()
                .email("admin@example.com")
                .passwordHash("password_hash1234")
                .userName("관리자")
                .userType(UserType.SUPER_ADMIN)
                .build());

        parentToken = jwtUtil.createAccessToken(parent);
        otherParentToken = jwtUtil.createAccessToken(otherParent);
        
        // 강의 및 과제 데이터 생성
        course = Course.builder()
                .admin(admin)
                .title("코딩 기초")
                .price(BigDecimal.ZERO)
                .build();
        Chapter chapter = Chapter.builder()
                .title("1챕터")
                .order(1)
                .build();
        Lecture lecture1 = Lecture.builder()
                .title("1-1강")
                .order(1)
                .build();
        Lecture lecture2 = Lecture.builder()
                .title("1-2강")
                .order(2)
                .build();

        chapter.addLecture(lecture1);
        chapter.addLecture(lecture2);
        course.addChapter(chapter);
        courseRepository.save(course);
        
        assignment1 = assignmentRepository.save(Assignment.builder()
                .lecture(lecture1)
                .title("1번 과제")
                .build());
        assignment2 = assignmentRepository.save(Assignment.builder()
                .lecture(lecture2)
                .title("2번 과제")
                .build());
        
        // 수강 정보 및 진도율, 제출 정보 생성
        Enrollment enrollment = enrollmentRepository.save(new Enrollment(child, course));
        enrollment.updateProgress(new BigDecimal("35.50"));
        
        // 자녀는 1번 과제만 제출한 상태
        submissionRepository.save(Submission.builder()
                .student(child)
                .assignment(assignment1)
                .textContent("제출합니다.")
                .build());
    }

    @Test
    @DisplayName("학부모는 자신의 자녀 학습 대시보드를 성공적으로 조회할 수 있다.")
    void getChildDashboardSuccess() throws Exception {
        // when & then
        mockMvc.perform(get("/api/parents/me/children/{childId}/dashboard", child.getId())
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + parentToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.childId").value(child.getId()))
                .andExpect(jsonPath("$.childName").value("자녀"))
                .andExpect(jsonPath("$.courses").isArray())
                .andExpect(jsonPath("$.courses.length()").value(1))
                .andExpect(jsonPath("$.courses[0].courseTitle").value("코딩 기초"))
                .andExpect(jsonPath("$.courses[0].progressRate").value(35.50))
                .andExpect(jsonPath("$.courses[0].assignments").isArray())
                .andExpect(jsonPath("$.courses[0].assignments.length()").value(2))
                // 1번 과제는 '제출 완료'
                .andExpect(jsonPath("$.courses[0].assignments[?(@.assignmentTitle=='1번 과제')].submissionStatus").value("제출 완료"))
                // 2번 과제는 '미제출'
                .andExpect(jsonPath("$.courses[0].assignments[?(@.assignmentTitle=='2번 과제')].submissionStatus").value("미제출"));
    }

    @Test
    @DisplayName("다른 학부모의 자녀 대시보드 조회를 시도하면 실패(400 Bad Request)해야 한다.")
    void  getChildDashboardFailWithWrongParent() throws Exception {
        // when & then
        mockMvc.perform(get("/api/parents/me/children/{childId}/dashboard", child.getId())
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + otherParentToken))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("자신의 자녀 정보만 조회할 수 있습니다."));
    }

    @Test
    @DisplayName("존재하지 않는 자녀 ID로 대시보드 조회를 시도하면 실패(400 Bad Request)해야 한다.")
    void getChildDashboardFailWithInvalidChildId() throws Exception {
        // given
        long invalidChildId = 9999L;

        // when & then
        mockMvc.perform(get("/api/parents/me/children/{childId}/dashboard", invalidChildId)
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + parentToken))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("존재하지 않는 자녀입니다."));
    }
}
