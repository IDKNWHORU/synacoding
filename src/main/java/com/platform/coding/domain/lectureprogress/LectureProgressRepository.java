package com.platform.coding.domain.lectureprogress;

import com.platform.coding.domain.course.Lecture;
import com.platform.coding.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LectureProgressRepository extends JpaRepository<LectureProgress, Long> {
    Optional<LectureProgress> findByStudentAndLecture(User student, Lecture lecture);
}
