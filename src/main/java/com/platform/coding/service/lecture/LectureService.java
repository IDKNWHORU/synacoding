package com.platform.coding.service.lecture;

import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.course.Lecture;
import com.platform.coding.domain.course.LectureRepository;
import com.platform.coding.domain.enrollment.Enrollment;
import com.platform.coding.domain.enrollment.EnrollmentRepository;
import com.platform.coding.domain.lectureprogress.LectureProgress;
import com.platform.coding.domain.lectureprogress.LectureProgressRepository;
import com.platform.coding.domain.user.User;
import com.platform.coding.service.lecture.dto.LectureViewResponse;
import com.platform.coding.service.lecture.dto.LectureWebResponse;
import com.platform.coding.service.lecture.dto.ProgressUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LectureService {
    private final LectureRepository lectureRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LectureProgressRepository lectureProgressRepository;

    @Transactional(readOnly = true)
    public LectureViewResponse getLectureForViewing(Long lectureId, User student) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의입니다."));

        // 맛보기 강의일 경우, 인증/수강 여부와 관계 없이 반환
        if (lecture.isSample()) {
            int lastViewedSeconds = 0;
            // 만약 로그인한 사용자라면, 시청 기록을 가져옴
            if (student != null) {
                lastViewedSeconds = lectureProgressRepository.findByStudentAndLecture(student, lecture)
                        .map(LectureProgress::getLastViewedSeconds)
                        .orElse(0);
            }
            return LectureViewResponse.of(lecture, lastViewedSeconds);
        }

        // 일반 강의(맛보기X)에 대한 기존 로직
        // 일반 강의는 로그인이 반드시 필요함
        if (student == null) {
            throw new IllegalArgumentException("로그인이 필요한 서비스입니다.");
        }

        // 이 강의를 수강 중인지 확인
        enrollmentRepository.findByStudentAndCourse(student, lecture.getChapter().getCourse())
                .orElseThrow(() -> new IllegalArgumentException("수강 중인 강의가 아닙니다."));

        int lastViewedSeconds = lectureProgressRepository.findByStudentAndLecture(student, lecture)
                .map(LectureProgress::getLastViewedSeconds)
                .orElse(0);

        return LectureViewResponse.of(lecture, lastViewedSeconds);
    }

    @Transactional
    public void updateLectureProgress(Long lectureId, ProgressUpdateRequest request, User student) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의입니다."));

        // 개별 렉처 진행률 업데이트 또는 생성
        LectureProgress progress = lectureProgressRepository.findByStudentAndLecture(student, lecture)
                .orElseGet(() -> new LectureProgress(student, lecture));
        progress.updateProgress(request.currentViewedSeconds());
        lectureProgressRepository.save(progress);

        // 전체 강의 진도율 재계산 및 업데이트
        updateOverallCourseProgress(lecture.getChapter().getCourse(), student);
    }

    private void updateOverallCourseProgress(Course course, User student) {
        Enrollment enrollment = enrollmentRepository.findByStudentAndCourse(student, course)
                .orElseThrow(() -> new IllegalArgumentException("수강 정보가 존재하지 않습니다."));
        
        // 해당 강의의 모든 렉처 목록을 가져옴
        List<Lecture> allLecturesInCourse = course.getChapters().stream()
                .flatMap(chapter -> chapter.getLectures().stream())
                .toList();

        if (allLecturesInCourse.isEmpty()) {
            // 강의가 없으면 진도율 계산 불가
            return;
        }

        // 학생이 완료한 렉처의 수를 셈
        long completedLectureCount = allLecturesInCourse.stream()
                .filter(lec -> lectureProgressRepository.findByStudentAndLecture(student, lec)
                    .map(LectureProgress::isCompleted)
                    .orElse(false))
                .count();

        // 진도율 계산
        BigDecimal newProgressRate = BigDecimal.valueOf(completedLectureCount)
                .multiply(new BigDecimal(100))
                .divide(BigDecimal.valueOf(allLecturesInCourse.size()), 2, RoundingMode.HALF_UP);

        enrollment.updateProgress(newProgressRate);
    }

    @Transactional(readOnly = true)
    public LectureWebResponse getLectureForWeb(Long lectureId, User student) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의입니다."));

        // SecurityConfig에서 인증을 보장하므로, student 객체는 null이 아님이 보장됨.
        // 따라서 '맛보기 강의'가 아닐 경우, '수강 중'인지 여부만 확인하면 됨.
        boolean isEnrolled = enrollmentRepository.findByStudentAndCourse(student, lecture.getChapter().getCourse()).isPresent();
        
        if (!lecture.isSample() && !isEnrolled) {
            throw new AccessDeniedException("이 강의를 수강하고 있지 않습니다.");
        }

        // 시청 기록은 로그인한 사용자에게만 조회
        int lastViewedSeconds = 0;
        if (student != null) {
            lastViewedSeconds= lectureProgressRepository.findByStudentAndLecture(student, lecture)
                    .map(LectureProgress::getLastViewedSeconds)
                    .orElse(0);
        }
        
        // 이전/다음 강의 ID 조회
        Course course = lecture.getChapter().getCourse();
        List<Lecture> allLectures = course.getChapters().stream()
                .flatMap(chapter -> chapter.getLectures().stream())
                .toList();

        Long previousLectureId = null;
        Long nextLectureId = null;

        for (int i = 0; i < allLectures.size(); i++) {
            if (allLectures.get(i).getId().equals(lectureId)) {
                if (i > 0) {
                    previousLectureId = allLectures.get(i - 1).getId();
                }

                if (i < allLectures.size() - 1) {
                    nextLectureId = allLectures.get(i + 1).getId();
                }
                break;
            }
        }

        return LectureWebResponse.builder()
                .lectureId(lecture.getId())
                .lectureTitle(lecture.getTitle())
                .videoUrl(lecture.getVideoUrl())
                .lastViewedSeconds(lastViewedSeconds)
                .courseId(lecture.getChapter().getCourse().getId())
                .courseTitle(lecture.getChapter().getCourse().getTitle())
                .previousLectureId(previousLectureId)
                .nextLectureId(nextLectureId)
                .build();

    }
}
