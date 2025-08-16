package com.platform.coding.domain.submission;

import com.platform.coding.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    Optional<Submission> findByStudentAndAssignment(User student, Assignment assignment);
    Page<Submission> findByStatus(SubmissionStatus status, Pageable pageable);

    // 특정 학생과 여러 과제 목록을 기반으로 제출물 목록을 한 번에 조회한다.
    List<Submission> findByStudentAndAssignmentIn(User student, List<Assignment> allAssignments);
}
