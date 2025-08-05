package com.platform.coding.domain.user;

import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.course.CourseRepository;
import com.platform.coding.domain.enrollment.Enrollment;
import com.platform.coding.domain.enrollment.EnrollmentRepository;
import com.platform.coding.domain.enrollment.EnrollmentStatus;
import com.platform.coding.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class EnrollmentRepositoryTest extends IntegrationTestSupport {
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CourseRepository courseRepository;

    private User student;
    private Course course;

    @BeforeEach
    void setUp() {
        User parent = userRepository.save(User.builder()
                .email("parent@example.com")
                .passwordHash("hashed_password_123")
                .userName("보호자")
                .userType(UserType.PARENT)
                .build());
        student = userRepository.save(User.builder()
                .email("student@example.com")
                .passwordHash("hashed_password_456")
                .userName("수강생")
                .userType(UserType.STUDENT)
                .parent(parent)
                .build());
        User admin = userRepository.save(User.builder()
                .email("admin@example.com")
                .passwordHash("admin_pass")
                .userName("최고 관리자")
                .userType(UserType.SUPER_ADMIN)
                .build());
        course = courseRepository.save(Course.builder()
                .admin(admin)
                .title("Test Course")
                .price(BigDecimal.TEN)
                .build());
    }

    @Test
    @DisplayName("학생이 강의를 수강 신청하면, 초기 상태는 '수강 중'이고 진도율은 0%여야 한다.")
    void createEnrollment() {
        // given: 학생과 강의가 준비된 상태에서

        // when: 수강 정보를 생성하고 저장한다.
        Enrollment newEnrollment = new Enrollment(student, course);
        enrollmentRepository.save(newEnrollment);

        // then: 저장된 수강 정보를 조회하여 초기 상태를 검증한다.
        Enrollment foundEnrollment = enrollmentRepository.findById(newEnrollment.getId()).orElseThrow();

        assertThat(foundEnrollment.getId()).isNotNull();
        assertThat(foundEnrollment.getStudent().getId()).isEqualTo(student.getId());
        assertThat(foundEnrollment.getCourse().getId()).isEqualTo(course.getId());
        assertThat(foundEnrollment.getStatus()).isEqualTo(EnrollmentStatus.IN_PROGRESS);
        assertThat(foundEnrollment.getProgressRate()).isEqualByComparingTo("0.00");
        assertThat(foundEnrollment.getEnrolledAt()).isNotNull();
        assertThat(foundEnrollment.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("수강 중인 강의의 진도율을 업데이트하면 DB에 반영되어야 한다.")
    void updateProgressRate() {
        // given: 수강 정보가 미리 저장되어 있다.
        Enrollment enrollment = enrollmentRepository.save(new Enrollment(student, course));

        // when: 엔티티의 비즈니스 메소드를 호출하여 진도율을 업데이트한다.
        enrollment.updateProgress(new BigDecimal("55.50"));
        // @Transactional 덕분에 변경된 내용은 테스트 메소드 종료 시점에 DB에 반영(flush)된다.

        // then: DB에서 다시 조회하여 진도율이 반영되었는지 확인한다.
        Enrollment foundEnrollment = enrollmentRepository.findById(enrollment.getId()).orElseThrow();

        assertThat(foundEnrollment.getProgressRate()).isEqualByComparingTo("55.50");
        assertThat(foundEnrollment.getStatus()).isEqualTo(EnrollmentStatus.IN_PROGRESS);
        assertThat(foundEnrollment.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("진도율이 100%가 되면, 수강 상태는 '완료'로 변경되고 완료 시간이 기록되어야 한다.")
    void completeCourseWhenProgressIs100() {
        // given: 수강 정보가 미리 저장되어 있다.
        Enrollment enrollment = enrollmentRepository.save(new Enrollment(student, course));

        // when: 엔티티의 비즈니스 메소드를 호출하여 진도율을 업데이트한다.
        enrollment.updateProgress(new BigDecimal("100.00"));

        // then: DB에서 다시 조회하여 상태와 완료 시간을 검증한다.
        Enrollment foundEnrollment = enrollmentRepository.findById(enrollment.getId()).orElseThrow();

        assertThat(foundEnrollment.getProgressRate()).isEqualByComparingTo("100.00");
        assertThat(foundEnrollment.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
        assertThat(foundEnrollment.getCompletedAt()).isNotNull();
    }
}
