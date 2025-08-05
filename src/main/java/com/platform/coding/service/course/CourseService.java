package com.platform.coding.service.course;

import com.platform.coding.domain.course.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository courseRepository;

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

        return DetailedCourseResponse.fromEntity(course);
    }
}
