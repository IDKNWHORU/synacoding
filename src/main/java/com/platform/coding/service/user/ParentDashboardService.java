package com.platform.coding.service.user;

import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.enrollment.Enrollment;
import com.platform.coding.domain.enrollment.EnrollmentRepository;
import com.platform.coding.domain.submission.Assignment;
import com.platform.coding.domain.submission.AssignmentRepository;
import com.platform.coding.domain.submission.SubmissionRepository;
import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserRepository;
import com.platform.coding.service.user.dto.ChildDashboardResponse;
import com.platform.coding.service.user.dto.ChildLearningSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParentDashboardService {
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;

    /**
     * [신규 추가]
     * 학부모에게 속한 모든 자녀의 학습 현황 요약 정보를 조회합니다.
     * @param parent 현재 로그인한 학부모 사용자
     * @return 모든 자녀의 학습 현황 요약 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<ChildLearningSummaryDto> getChildrenLearningSummary(User parent) {
        // 1. 학부모에게 속한 모든 자녀 계정을 조회합니다.
        List<User> children = userRepository.findByParent(parent);

        // 2. 각 자녀별로 학습 현황 DTO를 생성하여 리스트로 만듭니다.
        return children.stream()
                .map(child -> {
                    // 3. (성능 최적화) Fetch Join을 사용하여 자녀의 모든 수강 정보를 한 번의 쿼리로 가져옵니다.
                    List<Enrollment> enrollments = enrollmentRepository.findByStudent(child);

                    // 4. 각 수강 정보를 '강의 진행 요약 DTO'로 변환합니다.
                    List<ChildLearningSummaryDto.CourseProgressSummaryDto> courseSummaries = enrollments.stream()
                            .map(enrollment -> ChildLearningSummaryDto.CourseProgressSummaryDto.builder()
                                    .courseId(enrollment.getCourse().getId())
                                    .courseTitle(enrollment.getCourse().getTitle())
                                    .progressRate(enrollment.getProgressRate())
                                    .build())
                            .collect(Collectors.toList());

                    // 5. 최종적으로 자녀 정보와 강의 요약 리스트를 합쳐 반환합니다.
                    return ChildLearningSummaryDto.builder()
                            .childId(child.getId())
                            .childName(child.getUserName())
                            .courses(courseSummaries)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChildDashboardResponse getChildDashboard(Long childId, User parent) {
        // 자녀 정보 조회 및 요청한 학부모의 자녀가 맞는지 권한 검증
        User child = userRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 자녀입니다."));

        if (child.getParent() == null || !child.getParent().getId().equals(parent.getId())) {
            throw new IllegalArgumentException("자신의 자녀 정보만 조회할 수 있습니다.");
        }
        
        // 자녀의 모든 수강 정보 조회
        List<Enrollment> enrollments = enrollmentRepository.findByStudent(child);
        
        // 각 수강 정보(강의)를 순회하며 상세 정보 가공
        List<ChildDashboardResponse.CourseProgressDto> courseProgressDtos = enrollments.stream()
                .map(enrollment -> {
                    Course course = enrollment.getCourse();

                    // 해당 강의의 모든 과제 목록 조회
                    List<Assignment> assignments = assignmentRepository.findAllByLecture_Chapter_Course(course);
                    
                    // 각 과제에 대한 제출 상태 확인
                    List<ChildDashboardResponse.AssignmentStatusDto> assignmentStatusDtos = assignments.stream()
                            .map(assignment -> {
                                boolean submitted = submissionRepository.findByStudentAndAssignment(child, assignment).isPresent();
                                return ChildDashboardResponse.AssignmentStatusDto.builder()
                                        .assignmentId(assignment.getId())
                                        .assignmentTitle(assignment.getTitle())
                                        .submissionStatus(submitted ? "제출 완료": "미제출")
                                        .build();
                            })
                            .collect(Collectors.toList());

                    return ChildDashboardResponse.CourseProgressDto.builder()
                            .courseId(course.getId())
                            .courseTitle(course.getTitle())
                            .progressRate(enrollment.getProgressRate())
                            .assignments(assignmentStatusDtos)
                            .build();
                })
                .collect(Collectors.toList());

        // 최종 응답 DTO 조립
        return ChildDashboardResponse.builder()
                .childId(child.getId())
                .childName(child.getUserName())
                .courses(courseProgressDtos)
                .build();
    }
}
