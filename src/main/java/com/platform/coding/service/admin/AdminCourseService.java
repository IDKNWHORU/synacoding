package com.platform.coding.service.admin;

import com.platform.coding.domain.course.*;
import com.platform.coding.domain.submission.*;
import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserType;
import com.platform.coding.service.admin.dto.*;
import com.platform.coding.service.course.dto.CourseCurriculumResponse;
import com.platform.coding.service.notification.NotificationService;
import com.platform.coding.service.submission.dto.SubmissionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminCourseService {
    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final LectureRepository lectureRepository;
    private final SubmissionRepository submissionRepository;
    private final FeedbackRepository feedbackRepository;
    private final NotificationService notificationService;
    private final AssignmentRepository assignmentRepository;

    // 강의 생성
    @Transactional
    public SimpleCourseResponse createCourse(CourseCreateRequest request, User admin) {
        Course newCourse = request.toEntity(admin);
        Course savedCourse = courseRepository.save(newCourse);
        return SimpleCourseResponse.fromEntity(savedCourse);
    }
    
    // 강의 정보 수정
    @Transactional
    public SimpleCourseResponse updateCourse(Long courseId, CourseUpdateRequest request, User admin) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의입니다."));

        // [중요] 해당 강의를 수정할 권한이 있는지 확인 (자신이 생성한 강의인지 등)
        // 여기서는 간단하게 모든 관리자가 수정 가능하다고 가정함.

        course.updateDetails(request.title(), request.description(), request.price());
        return SimpleCourseResponse.fromEntity(course);
    }

    /**
     * 강의를 '게시' 상태로 변경 (DRAFT -> PUBLISH)
     */
    @Transactional
    public void publishCourse(Long courseId, User admin) {
        Course course = findCourseById(courseId);

        if (course.getStatus() != CourseStatus.DRAFT) {
            throw new IllegalArgumentException("초안 상태의 강의만 게시할 수 있습니다.");
        }
        course.setStatus(CourseStatus.PUBLISHED);
    }

    /**
     * 강의를 '초안' 상태로 되돌림. (PUBLISHED -> DRAFT)
     */
    @Transactional
    public void unpublishCourse(Long courseId, User admin) {
        Course course = findCourseById(courseId);

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new IllegalArgumentException("게시된 상태의 강의만 게시를 취소할 수 있습니다.");
        }

        course.setStatus(CourseStatus.DRAFT);
    }

    /**
     * 보관된 강의를 다시 '게시' 상태로 변경한다. (ARCHIVED -> PUBLISHED)
     */
    @Transactional
    public void republishCourse(Long courseId, User admin) {
        Course course = findCourseById(courseId);

        if (course.getStatus() != CourseStatus.ARCHIVED) {
            throw new IllegalArgumentException("보관된 상태의 강의만  재게시할 수 있습니다.");
        }
        course.setStatus(CourseStatus.PUBLISHED);
    }

    // 강의 논리적 삭제 (PUBLISHED -> ARCHIVED)
    @Transactional
    public void archiveCourse(Long courseId, User admin) {
        Course course = findCourseById(courseId);

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new IllegalStateException("게시된 상태의 강의만 보관할 수 있습니다.");
        }

        course.archive();
    }
    
    // 챕터 관리
    @Transactional
    public void addChapterToCourse(Long courseId, ChapterRequest request, User admin) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의입니다."));
        // TODO: 권한 검증 로직
        Chapter chapter = Chapter.builder()
                .title(request.title())
                .order(request.order())
                .build();

        course.addChapter(chapter);
    }

    @Transactional
    public void updateChapter(Long chapterId, ChapterRequest request, User admin) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 챕터입니다."));
        // TODO: 권한 검증 로직
        chapter.updateDetails(request.title(), request.order());
    }

    @Transactional
    public void deleteChapter(Long chapterId, User admin) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 챕터입니다."));
        // TODO: 권한 검증 로직
        chapter.getCourse().getChapters().remove(chapter);
    }

    @Transactional
    public void addLecture(Long chapterId, LectureRequest request, User admin) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 챕터입니다."));
        // TODO: 권한 검증 로직
        Lecture newLecture = Lecture.builder()
                .title(request.title())
                .order(request.order())
                .videoUrl(request.videoUrl())
                .isSample(request.sample())
                .durationSeconds(request.durationSeconds())
                .build();

        chapter.addLecture(newLecture);
    }

    @Transactional
    public void updateLecture(Long lectureId, LectureRequest request, User admin) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의입니다."));

        lecture.updateDetails(request.title(), request.order(), request.videoUrl(), request.sample(), request.durationSeconds());
    }

    @Transactional
    public void deleteLecture(Long lectureId, User admin) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의입니다."));
        // TODO: 권한 검증 로직
        lecture.getChapter().getLectures().remove(lecture);
    }

    @Transactional
    public void createFeedbackForSubmission(Long submissionId, FeedbackRequest request, User admin) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 제출물입니다."));

        // 권한 검증: 피드백을 남기려는 사용자가 관리자인지 확인
        if (admin.getUserType() != UserType.CONTENT_MANAGER && admin.getUserType() != UserType.SUPER_ADMIN) {
            throw new IllegalStateException("피드백을 작성할 권한이 없습니다.");
        }

        // 중복 방지: 이미 피드백이 존재하는지 확인
        if (submission.getFeedback() != null) {
            throw new IllegalArgumentException("이미 피드백이 등록된 제출물입니다.");
        }

        // 피드백 생성
        Feedback newFeedback = Feedback.builder()
                .submission(submission)
                .admin(admin)
                .content(request.content())
                .build();

        // 연관관계 설정 및 상태 변경
        submission.setFeedback(newFeedback);

        // 피드백 저장 (Submission의 CascadeType.ALL 덕분에 submission만 저장해도 되지만, 명시적으로 save 호출)
        feedbackRepository.save(newFeedback);

        // 피드백이 작성되었다는 알림을 학생에게 보냄
        String notificationContent = String.format(
                "'%s' 강의의 과제에 새로운 피드백이 도착했습니다.",
                submission.getAssignment().getLecture().getChapter().getCourse().getTitle()
        );
        String linkUrl = "/submissions/" + submission.getId();
        notificationService.createNotification(submission.getStudent(), notificationContent, linkUrl);
    }

    @Transactional(readOnly = true)
    public CourseCurriculumResponse getCourseCurriculum(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의입니다."));

        Map<Long, List<Assignment>> assignmentsByLecture = new HashMap<>();
        // 이 로직 전체가 하나의 트랜잭션 안에서 실행됨.
        course.getChapters().forEach(chapter ->
                chapter.getLectures().forEach(lecture -> {
                    List<Assignment> assignments = assignmentRepository.findByLecture(lecture);
                    assignmentsByLecture.put(lecture.getId(), assignments);
                }));

        return CourseCurriculumResponse.of(course, assignmentsByLecture);
    }

    @Transactional
    public Long createAssignmentForLecture(Long lectureId, AssignmentCreateRequest request, User admin) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 렉처입니다."));

        // TODO: 권한 검증 로직 (여기서는 모든 관리자가 가능하다고 가정)

        Assignment newAssignment = Assignment.builder()
                .lecture(lecture)
                .title(request.title())
                .content(request.content())
                .deadline(request.deadline() != null ? request.deadline().toInstant(ZoneOffset.UTC) : null)
                .build();

        Assignment savedAssignment = assignmentRepository.save(newAssignment);
        return savedAssignment.getId();
    }

    /**
     * 특정 과제의 정보를 수정합니다.
     */
    @Transactional
    public void updateAssignment(Long assignmentId, AssignmentUpdateRequest request, User admin) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 과제입니다."));
        // TODO: 권한 검증 로직

        assignment.updateDetails(
                request.title(),
                request.content(),
                request.deadline() != null ? request.deadline().toInstant(ZoneOffset.UTC) : null
        );
    }

    /**
     * 특정 과제를 삭제합니다.
     */
    @Transactional
    public void deleteAssignment(Long assignmentId, User admin) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 과제입니다."));
        // TODO: 권한 검증 로직
        assignmentRepository.delete(assignment);
    }

    /**
     * 특정 상태의 모든 제출물을 페이지네이션하여 조회한다.
     */
    @Transactional(readOnly = true)
    public Page<SubmissionSummaryResponse> getSubmissionsByStatus(SubmissionStatus status, Pageable pageable) {
        Page<Submission> submissions;
        if (status == null) {
            submissions = submissionRepository.findAll(pageable);
        } else {
            submissions = submissionRepository.findByStatus(status, pageable);
        }
        return submissions.map(SubmissionSummaryResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public SubmissionResponse getSubmissionForFeedback(Long submissionId, User admin) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 제출물입니다."));
        // TODO: 관리자가 해당 강의의 관리자인지 등 추가 권한 검증 로직
        return SubmissionResponse.fromEntity(submission);
    }

    private Course findCourseById(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의입니다."));
    }
}
