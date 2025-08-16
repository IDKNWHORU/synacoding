package com.platform.coding.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.platform.coding.config.jwt.JwtAuthenticationFilter;
import com.platform.coding.config.jwt.JwtUtil;
import com.platform.coding.domain.course.Chapter;
import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.course.CourseRepository;
import com.platform.coding.domain.course.Lecture;
import com.platform.coding.domain.submission.Assignment;
import com.platform.coding.domain.submission.AssignmentRepository;
import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserRepository;
import com.platform.coding.domain.user.UserType;
import com.platform.coding.service.admin.dto.AssignmentCreateRequest;
import com.platform.coding.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class AdminAssignmentControllerTest extends IntegrationTestSupport {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private AssignmentRepository assignmentRepository;


    private String adminToken;
    private Lecture existingLecture;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());

        assignmentRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        User admin = userRepository.save(User.builder()
                .email("admin@example.com")
                .passwordHash("password_hash123")
                .userName("관리자")
                .userType(UserType.SUPER_ADMIN)
                .build());
        adminToken = jwtUtil.createAccessToken(admin);

        Course course = Course.builder().admin(admin).title("강의").price(BigDecimal.ZERO).build();
        Chapter chapter = Chapter.builder().title("챕터").order(1).build();
        Lecture lecture = Lecture.builder().title("렉처").order(1).build();

        chapter.addLecture(lecture);
        course.addChapter(chapter);
        Course savedCourse = courseRepository.save(course);
        existingLecture = savedCourse.getChapters().get(0).getLectures().get(0);
    }

    @Test
    @DisplayName("관리자는 특정 강의(Lecture)에 새 과제를 추가할 수 있다.")
    void createAssignmentSuccess() throws Exception {
        // given
        AssignmentCreateRequest request = new AssignmentCreateRequest("새로운 과제", "과제 내용입니다.", LocalDateTime.now().plusDays(7));
        Long lectureId = existingLecture.getId();

        // when & then
        mockMvc.perform(post("/api/admin/lectures/{lectureId}/assignments", lectureId)
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));

        // DB 검증
        List<Assignment> assignments = assignmentRepository.findByLecture(existingLecture);
        assertThat(assignments).hasSize(1);
        assertThat(assignments.get(0).getTitle()).isEqualTo("새로운 과제");
    }
}