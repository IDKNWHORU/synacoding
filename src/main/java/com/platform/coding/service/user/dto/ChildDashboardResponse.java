package com.platform.coding.service.user.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

public record ChildDashboardResponse(
        Long childId,
        String childName,
        List<CourseProgressDto> courses
) {
    @Builder
    public ChildDashboardResponse {}

    /**
     * 자녀가 수강 중인 개별 강의의 진행 상태
     */
    public record CourseProgressDto(
            Long courseId,
            String courseTitle,
            // 진도율
            BigDecimal progressRate,
            List<AssignmentStatusDto> assignments
    ) {
        @Builder
        public CourseProgressDto {}
    }

    /**
     * 강의에 포함된 과제의 제출 상태
     */
    public record AssignmentStatusDto(
            Long assignmentId,
            String assignmentTitle,
            // "제출 완료", "미제출"
            String submissionStatus
    ) {
        @Builder
        public AssignmentStatusDto {}
    }
}
