package com.platform.coding.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.coding.config.jwt.JwtAuthenticationFilter;
import com.platform.coding.config.jwt.JwtUtil;
import com.platform.coding.domain.course.Chapter;
import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.course.CourseRepository;
import com.platform.coding.domain.course.Lecture;
import com.platform.coding.domain.notification.NotificationRepository;
import com.platform.coding.domain.submission.*;
import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserRepository;
import com.platform.coding.domain.user.UserType;
import com.platform.coding.service.admin.dto.FeedbackRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class AdminSubmissionControllerTest extends IntegrationTestSupport {
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
    private NotificationRepository notificationRepository;

    private User admin;
    private User student;
    private String adminToken;
    private Submission submission;

    @BeforeEach
    void setUp() {
        userRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();
        assignmentRepository.deleteAllInBatch();
        submissionRepository.deleteAllInBatch();
        notificationRepository.deleteAllInBatch();

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
        adminToken = jwtUtil.createAccessToken(admin);

        Course course = courseRepository.save(Course.builder()
                .admin(admin)
                .title("강의")
                .price(BigDecimal.ZERO)
                .build());
        Chapter chapter = Chapter.builder()
                .course(course)
                .title("챕터")
                .order(1)
                .build();
        Lecture lecture = Lecture.builder()
                .chapter(chapter)
                .title("렉처")
                .order(1)
                .build();

        chapter.addLecture(lecture);
        course.addChapter(chapter);
        Course newCourse = courseRepository.save(course);

        Assignment assignment = assignmentRepository.save(Assignment.builder()
                .lecture(newCourse.getChapters().get(0).getLectures().get(0))
                .title("과제")
                .build());
        submission = submissionRepository.save(Submission.builder()
                .assignment(assignment)
                .student(student)
                .textContent("제출물 내용")
                .build());
    }

    @Test
    @DisplayName("관리자는 학생의 제출물에 성공적으로 피드백을 작성할 수 있으며, 학생에게 알림이 발송되어야 한다.")
    void createFeedbackSuccess() throws Exception {
        // given
        FeedbackRequest request = new FeedbackRequest("아주 훌륭한 과제입니다!");
        Long submissionId = submission.getId();
        long initialNotificationCount = notificationRepository.count();

        // when & then
        mockMvc.perform(post("/api/admin/submissions/{submissionId}/feedback", submissionId)
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());

        // 검증 강화: DB에서 Submission의 상태와 Feedback 존재 여부 확인
        Submission foundSubmission = submissionRepository.findById(submissionId).orElseThrow();
        assertThat(foundSubmission.getStatus()).isEqualTo(SubmissionStatus.GRADED);
        assertThat(foundSubmission.getFeedback()).isNotNull();
        assertThat(foundSubmission.getFeedback().getContent()).isEqualTo("아주 훌륭한 과제입니다!");
        assertThat(foundSubmission.getFeedback().getAdmin().getId()).isEqualTo(admin.getId());

        // 알림 생성 검증
        assertThat(notificationRepository.count()).isEqualTo(initialNotificationCount + 1);
        var notification = notificationRepository.findAll().get(0);
        assertThat(notification.getUser().getId()).isEqualTo(student.getId());
        assertThat(notification.getContent()).contains("'강의' 강의의 과제에 새로운 피드백이 도착했습니다.");
        assertThat(notification.getLinkUrl()).isEqualTo("/submissions/" + submissionId);
    }

    @Test
    @DisplayName("일반 사용자 권한으로 피드백 작성을 시도하면 실패(403 Forbidden)해야 한다.")
    void createFeedbackFailWithNormalUserRole() throws Exception {
        // given
        String normalUserToken = jwtUtil.createAccessToken(student);
        FeedbackRequest request = new FeedbackRequest("권한 없는 피드백");
        Long submissionId = submission.getId();

        // when & then
        mockMvc.perform(post("/api/admin/submissions/{submissionId}/feedback", submissionId)
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + normalUserToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
}
