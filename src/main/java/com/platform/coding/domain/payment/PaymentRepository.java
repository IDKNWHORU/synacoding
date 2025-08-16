package com.platform.coding.domain.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    /**
     * 특정 기간 동안의 총 매출액을 계산한다.
     */
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.paidAt BETWEEN :start AND :end")
    Optional<BigDecimal> findTotalAmountByPaidAtBetween(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * 가장 인기 있는 강좌(수강 등록 기준) 목록을 조회한다.
     * @param limit 조회할 개수
     * @return [과목 ID, 과목명, 수강생 수] 목록
     */
    @Query("SELECT e.course.id, e.course.title, COUNT(e) as enrollmentCount " +
           "FROM Enrollment e " +
           "GROUP BY e.course.id, e.course.title " +
           "ORDER BY enrollmentCount DESC " +
           "LIMIT :limit")
    List<Object[]> findPopularCoursesByEnrollment(@Param("limit") int limit);
}
