package com.platform.coding.domain.review;

import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    // 특정 강의의 게시된(PUBLISHED) 리뷰 목록을 찾는 메소드
    List<Review> findByCourseIdAndStatus(Long courseId, ReviewStatus status);
    // 특정 학생이 특정 강의에 대해 작성한 리뷰를 찾는 메소드
    Optional<Review> findByStudentAndCourse(User student, Course course);
    Page<Review> findByStatus(ReviewStatus status, Pageable pageable);
}
