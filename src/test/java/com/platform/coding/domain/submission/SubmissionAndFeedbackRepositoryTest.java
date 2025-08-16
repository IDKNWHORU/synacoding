package com.platform.coding.domain.submission;

import com.platform.coding.domain.course.Chapter;
import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.course.CourseRepository;
import com.platform.coding.domain.course.Lecture;
import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserRepository;
import com.platform.coding.domain.user.UserType;
import com.platform.coding.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

public class SubmissionAndFeedbackRepositoryTest extends IntegrationTestSupport {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private AssignmentRepository assignmentRepository;
    @Autowired
    private SubmissionRepository submissionRepository;
    @Autowired
    private FeedbackRepository feedbackRepository;

    private User student;
    private User admin;
    private Assignment assignment;

    @BeforeEach
    void setUp() {
        // DB 초기화
        feedbackRepository.deleteAllInBatch();
        submissionRepository.deleteAllInBatch();
        assignmentRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        
        // 테스트 데이터 생성
        admin = userRepository.save(User.builder()
                .email("admin@example.com")
                .passwordHash("password_hash123")
                .userName("관리자")
                .userType(UserType.SUPER_ADMIN)
                .build());
        student = userRepository.save(User.builder()
                .email("student@example.com")
                .passwordHash("password_hash456")
                .userName("학생")
                .userType(UserType.STUDENT)
                .build());

        Course course = Course.builder()
                .admin(admin)
                .title("강의")
                .price(BigDecimal.ZERO)
                .build();
        Chapter chapter = Chapter.builder()
                .title("챕터")
                .order(1)
                .build();
        Lecture lecture = Lecture.builder()
                .title("강의")
                .order(1)
                .build();

        chapter.addLecture(lecture);
        course.addChapter(chapter);
        courseRepository.save(course);

        assignment = assignmentRepository.save(Assignment.builder()
                .lecture(lecture)
                .title("과제1")
                .content("과제 내용")
                .build());
    }

    @Test
    @DisplayName("학생이 과제를 제출하면 Submission이 생성되고 상태는 SUBMITTED여아 한다.")
    void createSubmission() {
        // given
        String submissionContent = "이것은 저의 과제 제출물입니다.";

        // when
        Submission submission = Submission.builder()
                .assignment(assignment)
                .student(student)
                .textContent(submissionContent)
                .build();
        submissionRepository.save(submission);

        // then
        Submission foundSubmission = submissionRepository.findById(submission.getId()).orElseThrow();

        assertThat(foundSubmission.getId()).isNotNull();
        assertThat(foundSubmission.getTextContent()).isEqualTo(submissionContent);
        assertThat(foundSubmission.getStudent().getId()).isEqualTo(student.getId());
        assertThat(foundSubmission.getAssignment().getId()).isEqualTo(assignment.getId());
        assertThat(foundSubmission.getStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
        assertThat(foundSubmission.getFeedback()).isNull();
    }

    @Test
    @DisplayName("관리자가 제출물에 피드백을 남기면 Feedback이 생성되고 Submission 상태가 GRADED로 변경되어야 한다.")
    void createFeedbackForSubmission() {
        // given: 학생이 과제를 미리 제출한 상태
        Submission submission = submissionRepository.save(
                Submission.builder()
                        .assignment(assignment)
                        .student(student)
                        .textContent("제출합니다.")
                        .build()
        );

        String feedbackContent = "아주 잘했습니다. 이 부분은 이렇게 수정하면 더 좋을 것 같아요.";

        // when: 관리자가 피드백을 생성한다.
        Feedback feedback = Feedback.builder()
                .submission(submission)
                .admin(admin)
                .content(feedbackContent)
                .build();

        // Submission에 Feedback을 생성하고, Submission의 상태 변경 로직을 호출한다.
        submission.setFeedback(feedback);
        // 피드백 저장
        feedbackRepository.save(feedback);

        // then
        // 1. 피드백 검증
        Feedback foundFeedback = feedbackRepository.findById(feedback.getId()).orElseThrow();
        assertThat(foundFeedback.getContent()).isEqualTo(feedbackContent);
        assertThat(foundFeedback.getAdmin().getId()).isEqualTo(admin.getId());
        assertThat(foundFeedback.getSubmission().getId()).isEqualTo(submission.getId());

        // 2. 제출물 상태 변경 검증
        Submission gradedSubmission = submissionRepository.findById(submission.getId()).orElseThrow();
        assertThat(gradedSubmission.getStatus()).isEqualTo(SubmissionStatus.GRADED);
        assertThat(gradedSubmission.getFeedback()).isNotNull();
        assertThat(gradedSubmission.getFeedback().getId()).isEqualTo(foundFeedback.getId());
    }

    @Test
    @DisplayName("과제 제출물을 삭제하면, 연관된 피드백도 함께 삭제되어야 한다.")
    void deleteSubmissionAlsoDeleteFeedback() {
        // given: 피드백까지 완료된 제출물
        Submission submission = submissionRepository.save(Submission.builder()
                .assignment(assignment)
                .student(student)
                .textContent("제출합니다.")
                .build());
        Feedback feedback = feedbackRepository.save(Feedback.builder()
                .submission(submission)
                .admin(admin)
                .content("잘 했어요.")
                .build());
        submission.setFeedback(feedback);

        Long submissionId = submission.getId();
        Long feedbackId = feedback.getId();

        // when: 제출물을 삭제한다.
        submissionRepository.deleteById(submissionId);
        // DB에 즉시 반영
        submissionRepository.flush();

        // then: 제출물과 피드백 모두 존재하지 않아야 한다.
        boolean submissionExists = submissionRepository.existsById(submissionId);
        boolean feedbackExists = feedbackRepository.existsById(feedbackId);

        assertThat(submissionExists).isFalse();
        // Feedback 엔티티의 submission 필드에 @OneToOne(mappedBy = "submission", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true) 가
        // Submission 엔티티에 있어야 연쇄 삭제가 되지만, 여기서는 Submission이 삭제되었으므로 Feedback도  삭제되어야 한다. (FK 제약조건)
        // submission이 삭제되었으므로 피드백도 삭제되었는지 화인
        assertThat(feedbackExists).isFalse();
    }
}
