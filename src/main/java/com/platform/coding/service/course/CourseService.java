package com.platform.coding.service.course;

import com.platform.coding.domain.course.*;
import com.platform.coding.domain.enrollment.EnrollmentRepository;
import com.platform.coding.domain.enrollment.EnrollmentStatus;
import com.platform.coding.domain.review.ReviewRepository;
import com.platform.coding.domain.submission.Assignment;
import com.platform.coding.domain.submission.AssignmentRepository;
import com.platform.coding.domain.submission.Submission;
import com.platform.coding.domain.submission.SubmissionRepository;
import com.platform.coding.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final ReviewRepository reviewRepository;
    private final SubmissionRepository submissionRepository;

    // 전체 강의 목록 조회 (페이지네이션 적용)
    @Transactional(readOnly = true)
    public Page<SimpleCourseResponse> getAllPublishedCourses(Pageable pageable) {
        return courseRepository.findByStatus(CourseStatus.PUBLISHED, pageable)
                .map(SimpleCourseResponse::fromEntity);
    }
    
    // 강의 상세 정보 조회
    @Transactional(readOnly = true)
    public DetailedCourseResponse getCourseDetails(Long courseId) {
        // ID로 강의를 찾고, PUBLISHED가 아니면 예외 발생
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의입니다."));

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            // TODO: 관리자나 구매한 사용자는 볼 수 있도록 예외 처리 필요
            throw new IllegalArgumentException("현재 판매 중인 강의가 아닙니다.");
        }

        Map<Long, List<Assignment>> assignmentsByLecture = getAssignmentsMapForCourse(course);

        return DetailedCourseResponse.fromEntity(course, assignmentsByLecture, Collections.emptyMap());
    }

    /**
     * 수강생이 자신의 강의 커리큘럼을 조회한다.
     * 수강 중인 강의인지 권한을 확인하는 로직이 포함되어있음.
     */
    @Transactional(readOnly = true)
    public DetailedCourseResponse getEnrolledCourseDetails(Long courseId, User student) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의입니다."));
        
        // 이 학생이 이 강의를 수강 중인지 확인
        enrollmentRepository.findByStudentAndCourse(student, course)
                .orElseThrow(() -> new AccessDeniedException("이 강의를 수강하고 있지 않습니다."));

        // 강의에 속한 모든 과제를 Lecture ID 기준으로 그룹화
        Map<Long, List<Assignment>> assignmentsByLecture = getAssignmentsMapForCourse(course);

        // 강의에 속한 모든 과제에 대한 학생의 제출 정보를 조회하여 Map으로 변환 (과제 ID -> Submission)
        List<Assignment> allAssignments = assignmentsByLecture.values().stream()
                .flatMap(List::stream)
                .toList();
        Map<Long, Submission> submissionsByAssignment = submissionRepository.findByStudentAndAssignmentIn(student, allAssignments).stream()
                .collect(Collectors.toMap(s -> s.getAssignment().getId(), Function.identity()));


        // DTO 생성 시 학생의 제출 정보(submissionsByAssignment)를 함께 전달
        return DetailedCourseResponse.fromEntity(course, assignmentsByLecture, submissionsByAssignment);
    }

    @Transactional(readOnly = true)
    public boolean isEligibleToWriteReview(Long courseId, User student) {
        if (student == null) {
            return false;
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의입니다."));

        // 완강했는지 확인
        boolean isCompleted = enrollmentRepository.findByStudentAndCourse(student, course)
                .map(enrollment -> enrollment.getStatus() == EnrollmentStatus.COMPLETED)
                .orElse(false);

        if (!isCompleted) {
            return false;
        }

        // 이미 리뷰를 작성했는지 확인
        boolean alreadyReviewed = reviewRepository.findByStudentAndCourse(student, course).isPresent();

        return !alreadyReviewed;
    }

    /**
     * 특정 강의에 포함된 모든 과제를 조회하여 Lecture ID를 키로 하는 Map으로 반환하는 헬퍼 메소드
     */
    private Map<Long, List<Assignment>> getAssignmentsMapForCourse(Course course) {
        Map<Long, List<Assignment>> assignmentsByLecture = new HashMap<>();
        course.getChapters().forEach(chapter ->
                chapter.getLectures().forEach(lecture -> {
                    List<Assignment> assignments = assignmentRepository.findByLecture(lecture);
                    assignmentsByLecture.put(lecture.getId(), assignments);
                })
        );
        return assignmentsByLecture;
    }
}
