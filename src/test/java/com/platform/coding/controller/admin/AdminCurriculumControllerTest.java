package com.platform.coding.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.coding.config.jwt.JwtAuthenticationFilter;
import com.platform.coding.config.jwt.JwtUtil;
import com.platform.coding.domain.course.*;
import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserRepository;
import com.platform.coding.domain.user.UserType;
import com.platform.coding.service.admin.dto.ChapterRequest;
import com.platform.coding.service.admin.dto.LectureRequest;
import com.platform.coding.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class AdminCurriculumControllerTest extends IntegrationTestSupport {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private ChapterRepository chapterRepository;
    @Autowired
    private LectureRepository lectureRepository;
    @Autowired
    private JwtUtil jwtUtil;

    private String adminToken;
    private String normalUserToken;
    private Course existingCourse;
    private Chapter existingChapter;
    private Lecture existingLecture;

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

        // 수정 테스트를 위한 기존 챕터 데이터 생성
        existingChapter = Chapter.builder()
                .title("1챕터")
                .order(1)
                .build();

        existingLecture = Lecture.builder()
                .title("1강")
                .order(1)
                .videoUrl("old_url")
                .isSample(true)
                .build();

        existingChapter.addLecture(existingLecture);
        existingCourse.addChapter(existingChapter);
        courseRepository.save(existingCourse);
    }

    @Test
    @DisplayName("관리자는 특정 강의에 새 챕터를 추가할 수 있다.")
    void addChapterSuccess() throws Exception {
        // given
        ChapterRequest request = ChapterRequest.builder()
                .title("새로운 챕터")
                .order(2)
                .build();

        Long courseId = existingCourse.getId();

        // when
        mockMvc.perform(post("/api/admin/courses/{courseId}/chapters", courseId)
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated());

        // then
        Course foundCourse = courseRepository.findById(courseId).orElseThrow();
        assertThat(foundCourse.getChapters()).hasSize(2);
        assertThat(foundCourse.getChapters().get(1).getTitle()).isEqualTo("새로운 챕터");
    }

    @Test
    @DisplayName("관리자는 기존 챕터의 제목과 순서를 변경할 수 있다.")
    public void updateChapterSuccess() throws Exception {
        // given
        ChapterRequest request = ChapterRequest.builder()
                .title("수정된 챕터 제목")
                .order(99)
                .build();

        Long chapterId = existingChapter.getId();

        // when
        mockMvc.perform(put("/api/admin/chapters/{chapterId}", chapterId)
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());

        // then
        Chapter chapter = chapterRepository.findById(chapterId).orElseThrow();
        assertThat(chapter.getTitle()).isEqualTo("수정된 챕터 제목");
        assertThat(chapter.getOrder()).isEqualTo(99);
    }

    @Test
    @DisplayName("관리자는 기존 챕터를 삭제할 수 있다.")
    void deleteChapterSuccess() throws Exception {
        // given: 챕터 ID
        Long chapterId = existingChapter.getId();

        // when
        mockMvc.perform(delete("/api/admin/chapters/{chapterId}", chapterId)
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + adminToken))
                .andDo(print())
                .andExpect(status().isNoContent());

        // then
        boolean exists = chapterRepository.existsById(chapterId);
        assertThat(exists).isFalse();
        // CascadeType.ALL + orphanRemoval=true 이므로, 챕터에 속한 렉처도 함께 삭제되어야 함.
        boolean lectureExists = lectureRepository.existsById(existingLecture.getId());
        assertThat(lectureExists).isFalse();
    }

    @Test
    @DisplayName("관리자는 특정 챕터에 새 렉처(강의)를 추가할 수 있다.")
    void addLectureSuccess() throws Exception {
        // given
        LectureRequest request = LectureRequest.builder()
                .title("2강")
                .order(2)
                .videoUrl("http://new.url")
                .sample(false)
                .build();

        Long chapterId = existingChapter.getId();

        mockMvc.perform(post("/api/admin/chapters/{chapterId}/lectures", chapterId)
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated());

        // then
        Chapter foundChapter = chapterRepository.findById(chapterId).orElseThrow();
        List<Lecture> lectures = foundChapter.getLectures();
        
        assertThat(lectures).hasSize(2);
        assertThat(lectures.get(1).getTitle()).isEqualTo("2강");
        assertThat(lectures.get(1).isSample()).isFalse();
    }

    @Test
    @DisplayName("관리자는 기존 렉처의 정보를 수정할 수 있다.")
    void updateLectureSuccess() throws Exception {
        // given
        LectureRequest request = LectureRequest.builder()
                .title("수정된 강의 제목")
                .order(10)
                .videoUrl("http://updated.url")
                .sample(false)
                .build();
        Long lectureId = existingLecture.getId();

        // when
        mockMvc.perform(put("/api/admin/lectures/{lectureId}", lectureId)
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());

        // then
        Lecture updatedLecture = lectureRepository.findById(lectureId).orElseThrow();
        assertThat(updatedLecture.getTitle()).isEqualTo("수정된 강의 제목");
        assertThat(updatedLecture.getVideoUrl()).isEqualTo("http://updated.url");
        assertThat(updatedLecture.isSample()).isFalse();
    }

    @Test
    @DisplayName("관리자는 기존 렉처를 삭제할 수 있다.")
    void deleteLectureSuccess() throws Exception {
        // given
        Long lectureId = existingLecture.getId();

        // when
        mockMvc.perform(delete("/api/admin/lectures/{lectureId}", lectureId)
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + adminToken))
                .andDo(print())
                .andExpect(status().isNoContent());

        // then
        boolean exists = lectureRepository.existsById(lectureId);
        assertThat(exists).isFalse();
    }
}
