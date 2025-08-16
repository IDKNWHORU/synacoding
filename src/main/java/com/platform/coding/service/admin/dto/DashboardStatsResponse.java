package com.platform.coding.service.admin.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class DashboardStatsResponse {
    private final BigDecimal dailyRevenue;
    private final BigDecimal weeklyRevenue;
    private final long newUsersToday;
    private final List<PopularCourseDto> popularCourses;

    @Getter
    @Builder
    public static class PopularCourseDto {
        private final Long courseId;
        private final String courseTitle;
        private final long enrollmentCount;
    }
}
