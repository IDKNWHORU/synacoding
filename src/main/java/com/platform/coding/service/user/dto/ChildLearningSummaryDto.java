package com.platform.coding.service.user.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * 학부모의 '자녀 학습관리' 대시보드 뷰를 위한 DTO.
 * 각 자녀의 정보와 그들의 학습 진행 상황 요약을 담습니다.
 */
public record ChildLearningSummaryDto(
        Long childId,
        String childName,
        List<CourseProgressSummaryDto> courses
) {
    @Builder
    public ChildLearningSummaryDto {}

    /**
     * 자녀가 수강 중인 개별 강의의 진행 요약 정보.
     */
    public record CourseProgressSummaryDto(
            Long courseId,
            String courseTitle,
            BigDecimal progressRate
    ) {
        @Builder
        public CourseProgressSummaryDto {}
    }
}