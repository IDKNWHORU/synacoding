package com.platform.coding.controller.course;

import com.platform.coding.domain.course.*;
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
public class CourseControllerTest extends IntegrationTestSupport {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    private User admin;
    private Course publishedCourse1;

    @BeforeEach
    void setUp() {
        userRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();

        // 관리자 생성
        admin = userRepository.save(User.builder()
                .email("admin@example.com")
                .passwordHash("password_hash")
                .userName("관리자")
                .userType(UserType.CONTENT_MANAGER)
                .build());

        publishedCourse1 = createCourse("스프링 부트", CourseStatus.PUBLISHED);
        createCourse("JPA 기초", CourseStatus.PUBLISHED);
        createCourse("리액트", CourseStatus.DRAFT);
    }

    private Course createCourse(String title, CourseStatus status) {
        Course course = Course.builder()
                .admin(admin)
                .title(title)
                .description(title + "강의 설명")
                .price(new BigDecimal("50000"))
                .build();
        course.setStatus(status);

        Chapter chapter = Chapter.builder().title("1챕터").order(1).build();
        chapter.addLecture(Lecture.builder().title("1-1강").order(1).isSample(true).build());
        course.addChapter(chapter);

        return courseRepository.save(course);
    }

    @Test
    @DisplayName("판매 중인 전체 강의 목록을 페이지네이션으로 조회하면, PUBLISHED 상태의 강의만 응답해야 한다.")
    void getAllCourseWithPagination() throws Exception {
        // when & then
        mockMvc.perform(get("/api/courses").param("size", "5"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].title").value("스프링 부트"));
    }

    @Test
    @DisplayName("특정 강의의 상세 정보를 조회하면, 커리큘럼을 포함한 모든 정보가 응답해야 한다.")
    void getCourseDetailsSuccess() throws Exception {
        // given: 조회할 강의의 ID
        Long courseId = publishedCourse1.getId();

        // when & then
        mockMvc.perform(get("/api/courses/{courseId}", courseId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(courseId))
                .andExpect(jsonPath("$.title").value("스프링 부트"))
                .andExpect(jsonPath("$.instructorName").value("관리자"))
                .andExpect(jsonPath("$.curriculum").isArray())
                .andExpect(jsonPath("$.curriculum[0].title").value("1챕터"))
                .andExpect(jsonPath("$.curriculum[0].lectures[0].title").value("1-1강"))
                .andExpect(jsonPath("$.curriculum[0].lectures[0].isSample").value(true));
    }

    @Test
    @DisplayName("존재하지 않는 강의 ID로 상세 정보를 요청하면 실패(400 Bad Request)해야 한다.")
    void getCourseDetailsFailsWithInvalidId() throws Exception {
        // given: 존재하지 않는 ID
        Long invalidCourseId = 9999L;

        // when & then
        mockMvc.perform(get("/api/courses/{courseId}", invalidCourseId))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 강의입니다."));
    }
}
