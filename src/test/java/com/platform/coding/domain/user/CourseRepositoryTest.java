package com.platform.coding.domain.user;

import com.platform.coding.domain.course.Chapter;
import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.course.CourseRepository;
import com.platform.coding.domain.course.Lecture;
import com.platform.coding.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class CourseRepositoryTest extends IntegrationTestSupport {
    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    private User courseAdmin;

    // 각 테스트가 실행되기 전에 관리자 계정을 미리 생성
    @BeforeEach
    void setUp() {
        courseAdmin = userRepository.save(User.builder()
                .email("admin@example.com")
                .passwordHash("admin_pass")
                .userName("강의관리자")
                .userType(UserType.CONTENT_MANAGER)
                .build());
    }

    @Test
    @DisplayName("강의(Course)를 생성할 때 챕터와 개별 강의(Lecture)가 함께 저장되어야 한다.")
    void createCourseWithChaptersAndLectures() {
        // given: 객체 그래프를 통해 강의, 챕터, 개별 강의를 모두 생성한다.
        Course course = Course.builder()
                .admin(courseAdmin)
                .title("초보자를 위한 스프링 부트")
                .description("스프링 부트의 기초부터 배포까지")
                .price(new BigDecimal("99000.00"))
                .build();

        Chapter chapter1 = Chapter.builder().title("1. 프로젝트 시작하기").order(1).build();
        Lecture lecture1_1 = Lecture.builder().title("1-1. Spring Initializer").order(1).build();
        Lecture lecture1_2 = Lecture.builder().title("1-2. Hello World").order(2).build();
        // 연관관계 편의 메소드를 사용하여 객체들을 연결
        chapter1.addLecture(lecture1_1);
        chapter1.addLecture(lecture1_2);

        Chapter chapter2 = Chapter.builder().title("2. JPA 기초").order(2).build();
        Lecture lecture2_1 = Lecture.builder().title("2-1. 엔티티 매핑").order(1).build();
        chapter2.addLecture(lecture2_1);

        course.addChapter(chapter1);
        course.addChapter(chapter2);

        // when: courseRepository.save()만 호출한다.
        // CascadeType.ALL 옵션 덕분에 Course에 속한 Chapter와 Lecture가 모두 저장된다.
        Course savedCourse = courseRepository.save(course);

        // then: 지정된 Course와 그 하위 요소들을 검증한다.
        assertThat(savedCourse.getId()).isNotNull();
        assertThat(savedCourse.getChapters()).hasSize(2);
        assertThat(savedCourse.getChapters().get(0).getTitle()).isEqualTo("1. 프로젝트 시작하기");
        assertThat(savedCourse.getChapters().get(0).getLectures()).hasSize(2);
        assertThat(savedCourse.getChapters().get(0).getLectures().get(0).getTitle()).isEqualTo("1-1. Spring Initializer");
        assertThat(savedCourse.getChapters().get(1).getLectures()).hasSize(1);
        assertThat(savedCourse.getChapters().get(1).getLectures().get(0).getChapter().getTitle()).isEqualTo("2. JPA 기초");
    }
}
