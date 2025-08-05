package com.platform.coding.domain.course;

import lombok.Builder;

import java.math.BigDecimal;

// 강의 목록 조회용 DTO
public record SimpleCourseResponse(
        Long courseId,
        String title,
        String instructorName,
        BigDecimal price
) {
    @Builder
    public SimpleCourseResponse {}

    public static SimpleCourseResponse fromEntity(Course course) {
        return SimpleCourseResponse.builder()
                .courseId(course.getId())
                .title(course.getTitle())
                .instructorName(course.getAdmin().getUserName())
                .price(course.getPrice())
                .build();
    }
}
