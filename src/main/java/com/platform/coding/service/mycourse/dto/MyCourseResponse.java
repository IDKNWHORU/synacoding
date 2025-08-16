package com.platform.coding.service.mycourse.dto;

import com.platform.coding.domain.enrollment.Enrollment;
import lombok.Builder;

import java.math.BigDecimal;

public record MyCourseResponse(
        Long enrollmentId,
        Long courseId,
        String title,
        String instructorName,
        BigDecimal progressRate
) {
    @Builder
    public MyCourseResponse {}

    public static MyCourseResponse fromEntity(Enrollment enrollment) {
        return MyCourseResponse.builder()
                .enrollmentId(enrollment.getId())
                .courseId(enrollment.getCourse().getId())
                .title(enrollment.getCourse().getTitle())
                .instructorName(enrollment.getCourse().getAdmin().getUserName())
                .progressRate(enrollment.getProgressRate())
                .build();
    }
}
