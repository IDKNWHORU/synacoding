package com.platform.coding.controller.submission;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.coding.config.jwt.JwtAuthenticationFilter;
import com.platform.coding.config.jwt.JwtUtil;
import com.platform.coding.domain.course.Chapter;
import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.course.CourseRepository;
import com.platform.coding.domain.course.Lecture;
import com.platform.coding.domain.enrollment.Enrollment;
import com.platform.coding.domain.enrollment.EnrollmentRepository;
import com.platform.coding.domain.submission.*;
import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserRepository;
import com.platform.coding.domain.user.UserType;
import com.platform.coding.service.submission.dto.SubmissionRequest;
import com.platform.coding.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
public class SubmissionControllerTest extends IntegrationTestSupport {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private AssignmentRepository assignmentRepository;
    @Autowired
    private SubmissionRepository submissionRepository;
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    @Autowired
    private FeedbackRepository feedbackRepository;

    private User student, otherStudent, admin;
    private String studentToken, otherStudentToken;
    private Assignment assignment;
    private Submission mySubmission;

    @BeforeEach
    void setUp() {
        assignmentRepository.deleteAllInBatch();
        submissionRepository.deleteAllInBatch();
        enrollmentRepository.deleteAllInBatch();
        feedbackRepository.deleteAllInBatch();
        
        admin = userRepository.save(User.builder()
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
        otherStudent = userRepository.save(User.builder()
                .email("other.student@example.com")
                .passwordHash("password_hash456")
                .userName("다른학생")
                .userType(UserType.STUDENT)
                .build());
        studentToken = jwtUtil.createAccessToken(student);
        otherStudentToken = jwtUtil.createAccessToken(otherStudent);

        Course course = Course.builder()
                .admin(admin)
                .title("강의")
                .description("강의 설명")
                .price(BigDecimal.ZERO)
                .build();
        Chapter chapter = Chapter.builder()
                .title("챕터")
                .order(1)
                .build();
        Lecture lecture = Lecture.builder()
                .title("렉처")
                .order(1)
                .build();

        chapter.addLecture(lecture);
        course.addChapter(chapter);
        Course newCourse = courseRepository.save(course);

        assignment = assignmentRepository.save(Assignment.builder()
                .lecture(newCourse.getChapters().get(0).getLectures().get(0))
                .title("과제")
                .content("과제 내용")
                .build());
        enrollmentRepository.save(new Enrollment(student, course));

        mySubmission = submissionRepository.save(Submission.builder()
                .assignment(assignment)
                .student(student)
                .textContent("내 제출물")
                .build());
    }

    @Test
    @DisplayName("수강생이 과제를 성공적으로 제출하면 201 Created를 응답한다.")
    void submitAssignmentSuccess() throws Exception {
        // given: 중복 제출 방지를 위해 기존 제출물 삭제
        submissionRepository.deleteAllInBatch();
        SubmissionRequest request = new SubmissionRequest("저의 과제입니다. 확인 부탁드립니다.");

        // when & then
        mockMvc.perform(post("/api/assignments/{assignmentId}/submissions", assignment.getId())
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + studentToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));
    }

    @Test
    @DisplayName("수강하지 않은 강의의 과제를 제출하려고 하면 실패(400 Bad Request)해야 한다.")
    void submitAssignmentFailWhenNotEnrolled() throws Exception {
        // given
        SubmissionRequest request = new SubmissionRequest("수강 안했지만 제출합니다.");

        // when & then
        mockMvc.perform(post("/api/assignments/{assignmentId}/submissions", assignment.getId())
                        // 수강하지 않은 학생 토큰 사용
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + otherStudentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("수강 중인 과제의 과제만 제출할 수 있습니다."));
    }

    @Test
    @DisplayName("수강생은 피드백이 없는 자신의 제출물을 조회할 수 있다.")
    void getMySubmissionWithoutFeedback() throws Exception {
        // given: 피드백이 없는 제출물을 미리 생성
        submissionRepository.deleteAllInBatch();
        Submission submission = submissionRepository.save(Submission.builder()
                .assignment(assignment)
                .student(student)
                .textContent("피드백 없는 제출물")
                .build());

        // when & then
        mockMvc.perform(get("/api/submissions/{submissionId}", submission.getId())
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + studentToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submissionId").value(submission.getId()))
                .andExpect(jsonPath("$.myContent").value("피드백 없는 제출물"))
                .andExpect(jsonPath("$.feedback").doesNotExist());
    }

    @Test
    @DisplayName("수강생은 피드백이 달린 자신의 제출물을 조회할 수 있다.")
    void getMySubmissionWithFeedback() throws Exception {
        // given: 피드백이 있는 제출물을 미리 생성
        submissionRepository.deleteAllInBatch();
        Submission submission = submissionRepository.save(Submission.builder()
                .assignment(assignment)
                .student(student)
                .textContent("피드백 있는 제출물")
                .build());
        Feedback feedback = feedbackRepository.save(Feedback.builder()
                .submission(submission)
                .admin(admin)
                .content("피드백입니다.")
                .build());
        submission.setFeedback(feedback);

        // when & then
        mockMvc.perform(get("/api/submissions/{submissionId}", submission.getId())
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + studentToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submissionId").value(submission.getId()))
                .andExpect(jsonPath("$.feedback").exists())
                .andExpect(jsonPath("$.feedback.content").value("피드백입니다."));
    }

    @Test
    @DisplayName("다른 학생의 제출물을 조회하려고 하면 실패(400 Bad Request)해야 한다.")
    void getSubmissionFailForOtherStudent() throws Exception {
        // given: 피드백이 있는 제출물을 미리 생성
        submissionRepository.deleteAllInBatch();
        SubmissionRequest request = new SubmissionRequest("수강 안했지만 제출합니다.");

        // when & then
        mockMvc.perform(get("/api/submissions/{submissionId}", mySubmission.getId())
                        // 다른 학생 토큰 사용
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + otherStudentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("자신의 제출물만 조회할 수 있습니다."));
    }
}
