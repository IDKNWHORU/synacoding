package com.platform.coding.domain.enrollment;

import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    Optional<Enrollment> findByStudentAndCourse(User student, Course course);

    /**
     * N+1 문제 해결을 위해 Fetch Join 적용
     * 학생이 수강하는 모든 정보를 조회할 때,
     * 연관된 Course(c)와 Course의 admin(a) 정보를 함께 조회함.
     */
    @Query("SELECT e FROM Enrollment e JOIN FETCH e.course c JOIN FETCH c.admin a WHERE e.student = :student")
    List<Enrollment> findByStudent(@Param("student") User student);
}
