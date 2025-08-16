package com.platform.coding.service.submission;

import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.enrollment.EnrollmentRepository;
import com.platform.coding.domain.submission.Assignment;
import com.platform.coding.domain.submission.AssignmentRepository;
import com.platform.coding.domain.submission.Submission;
import com.platform.coding.domain.submission.SubmissionRepository;
import com.platform.coding.domain.user.User;
import com.platform.coding.service.submission.dto.SubmissionRequest;
import com.platform.coding.service.submission.dto.SubmissionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubmissionService {
    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final EnrollmentRepository enrollmentRepository;

    // 과제 제출
    @Transactional
    public Long submitAssignment(Long assignmentId, SubmissionRequest request, User student) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 과제입니다."));

        // 권한 검증 1. 이 과제가 포함된 강의를 수강 중인 학생인지 확인
        enrollmentRepository.findByStudentAndCourse(student, assignment.getLecture().getChapter().getCourse())
                .orElseThrow(() -> new IllegalArgumentException("수강 중인 과제의 과제만 제출할 수 있습니다."));
        
        // 중복 제출 방지. 이미 제출한 과제인지 확인
        submissionRepository.findByStudentAndAssignment(student, assignment)
                // 요구사항에 따라 재제출 허용 로직으로 변경 가능
                .ifPresent(s -> { throw new IllegalArgumentException("이미 제출한 과제입니다."); });

        Submission newSubmission = Submission.builder()
                .assignment(assignment)
                .student(student)
                .textContent(request.content())
                .build();
        Submission savedSubmission = submissionRepository.save(newSubmission);

        return savedSubmission.getId();
    }

    /**
     * 과제 제출 및 수정을 함께 처리하는 메소드
     */
    @Transactional
    public Long submitOrUpdateAssignment(Long assignmentId, SubmissionRequest request, User student) {
        Assignment assignment = findAssignmentById(assignmentId);
        validateEnrollment(student, assignment.getLecture().getChapter().getCourse());

        // 마감일 확인
        if (assignment.getDeadline() != null && assignment.getDeadline().isBefore(Instant.now())) {
            throw new IllegalStateException("과제 제출 마감일이 지났습니다.");
        }

        // 기존 제출 내역이 있는지 확인
        Submission submission = submissionRepository.findByStudentAndAssignment(student, assignment)
                .map(existingSubmission -> {
                    // 있다면 내용 업데이트 (재제출)
                    existingSubmission.updateContent(request.content());
                    return existingSubmission;
                })
                .orElseGet(() -> {
                    // 없다면 새로 생성 (최초 제출)
                    return Submission.builder()
                            .assignment(assignment)
                            .student(student)
                            .textContent(request.content())
                            .build();
                });

        Submission savedSubmission = submissionRepository.save(submission);
        return savedSubmission.getId();
    }

    // 내 제출물 상세 조회
    @Transactional(readOnly = true)
    public SubmissionResponse getMySubmission(Long submissionId, User student) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 제출물입니다."));
        
        // 권한 검증: 자신의 제출물만 조회할 수 있도록 확인
        if(!submission.getStudent().getId().equals(student.getId())) {
            throw new IllegalArgumentException("자신의 제출물만 조회할 수 있습니다.");
        }

        return SubmissionResponse.fromEntity(submission);
    }

    /**
     * 웹 페이지에서 과제 정보를 표시하기 위해 과제 엔티티를 조회하고 권한을 검증하는 메소드
     */
    @Transactional(readOnly = true)
    public Assignment getAssignmentForSubmission(Long assignmentId, User student) {
        Assignment assignment = findAssignmentById(assignmentId);
        validateEnrollment(student, assignment.getLecture().getChapter().getCourse());
        return assignment;
    }

    /**
     * 특정 과제에 대한 나의 제출 내역을 조회하는 메소드 (재제출 시 기존 내용 로딩용)
     */
    @Transactional(readOnly = true)
    public Optional<Submission> findMySubmissionByAssignment(User student, Assignment assignment) {
        return submissionRepository.findByStudentAndAssignment(student, assignment);
    }

    // --- Helper Methods ---
    private Assignment findAssignmentById(Long assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 과제입니다."));
    }

    private void validateEnrollment(User student, Course course) {
        enrollmentRepository.findByStudentAndCourse(student, course)
                .orElseThrow(() -> new AccessDeniedException("이 강의를 수강하고 있지 않습니다."));
    }
}