package com.platform.coding.service.admin;

import com.platform.coding.domain.payment.PaymentRepository;
import com.platform.coding.domain.user.UserRepository;
import com.platform.coding.service.admin.dto.DashboardStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        Instant todayStart = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant now = Instant.now();
        // 오늘을 포함한 최근 7일
        Instant weekStart = todayStart.minus(6, ChronoUnit.DAYS);

        BigDecimal dailyRevenue = paymentRepository.findTotalAmountByPaidAtBetween(todayStart, now)
                .orElse(BigDecimal.ZERO);

        BigDecimal weeklyRevenue = paymentRepository.findTotalAmountByPaidAtBetween(weekStart, now)
                .orElse(BigDecimal.ZERO);

        long newUsersToday = userRepository.countByCreatedAtBetween(todayStart, now);

        // 인기 강좌 목록 조회 (JPQL 쿼리 결과)
        List<Object[]> popularCourseResult = paymentRepository.findPopularCoursesByEnrollment(5);
        List<DashboardStatsResponse.PopularCourseDto> popularCourses = popularCourseResult.stream()
                .map(result -> DashboardStatsResponse.PopularCourseDto.builder()
                        .courseId((Long) result[0])
                        .courseTitle((String) result[1])
                        .enrollmentCount((Long) result[2])
                        .build())
                .toList();

        return DashboardStatsResponse.builder()
                .dailyRevenue(dailyRevenue)
                .weeklyRevenue(weeklyRevenue)
                .newUsersToday(newUsersToday)
                .popularCourses(popularCourses)
                .build();
    }
}
