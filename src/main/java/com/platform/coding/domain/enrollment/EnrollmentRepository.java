package com.platform.coding.domain.enrollment;

import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    Optional<Enrollment> findByStudentAndCourse(User student, Course course);
}
