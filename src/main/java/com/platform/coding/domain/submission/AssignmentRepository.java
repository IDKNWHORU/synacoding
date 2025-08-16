package com.platform.coding.domain.submission;

import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.course.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    // 특정 강의(Course)에 포함된 모든 과제(Assignment)를 조회
    List<Assignment> findAllByLecture_Chapter_Course(Course course);
    List<Assignment> findByLecture(Lecture lecture);
}
