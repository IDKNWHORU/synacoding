package com.platform.coding.service.refund;

import com.platform.coding.domain.enrollment.Enrollment;
import com.platform.coding.domain.enrollment.EnrollmentRepository;
import com.platform.coding.domain.payment.Order;
import com.platform.coding.domain.payment.OrderStatus;
import com.platform.coding.domain.payment.Payment;
import com.platform.coding.domain.payment.PaymentRepository;
import com.platform.coding.domain.refund.Refund;
import com.platform.coding.domain.refund.RefundRepository;
import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserRepository;
import com.platform.coding.service.refund.dto.RefundRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class RefundService {
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    @Transactional
    public Long requestRefund(Long paymentId, RefundRequest request, User parent) {
        // 1. 결제 정보 조회 및 권한 검증
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결제 내역입니다."));

        Order order = payment.getOrder();
        if (!order.getParent().getId().equals(parent.getId())) {
            throw new IllegalArgumentException("자신의 결제 건에 대해서만 환불을 요청할 수 있습니다.");
        }

        // 2. 중복 환불 요청 검증
        if (refundRepository.existsByPaymentId(paymentId)) {
            throw new IllegalArgumentException("이미 환불 요청이 접수된 결제입니다.");
        }
        
        // 3. 환불 정책 검증
        validateRefundPolicy(payment);

        // 4. 환불 금액 계산 (여기서는 전액 환불로 가정)
        BigDecimal refundAmount = payment.getAmount();

        // 5. Refund 엔티티 생성 및 저장
        Refund refund = Refund.builder()
                .payment(payment)
                .refundAmount(refundAmount)
                .reason(request.reason())
                .build();
        Refund savedRefund = refundRepository.save(refund);

        // 6. 관련 수강 정보(Enrollment) 상태 변경
        // 이 주문에 포함된 모든 강의에 대한 수강 정보를 찾아서 상태 변경
        User student = userRepository.findByParent(parent)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("환불 처리할 자녀 계정을 찾을 수 없습니다."));

        order.getOrderItems().forEach(item -> {
            enrollmentRepository.findByStudentAndCourse(student, item.getCourse())
                    .ifPresent(Enrollment::requestRefund);
        });

        // 7. 주문(Order) 상태 변경 (선택적)
        order.cancel();

        // TODO: 관리자에게 환불 요청 알림 발송

        return savedRefund.getId();
    }

    private void validateRefundPolicy(Payment payment) {
        Order order = payment.getOrder();
        
        // 정책 1: 강의 구매 후, 수강 이력이 없는 경우에만 신청 가능
        User student = userRepository.findByParent(order.getParent())
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("주문과 연결된 학생을 찾을 수 없습니다."));

        order.getOrderItems().forEach(item -> {
            Enrollment enrollment = enrollmentRepository.findByStudentAndCourse(student, item.getCourse())
                    .orElseThrow(() -> new IllegalStateException("수강 정보를 찾을 수 없습니다: " + item.getCourse().getTitle()));

            if (enrollment.getProgressRate().compareTo(BigDecimal.ZERO) > 0) {
                throw new IllegalArgumentException("수강 이력이 있는 강의(" + item.getCourse().getTitle() + ")는 환불할 수 없습니다.");
            }
        });
        
        // 정책 2: 전액 환불은 결제 후 7일 이내
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        if (payment.getPaidAt().isBefore(sevenDaysAgo)) {
            throw new IllegalArgumentException("전액 환불 기간(7일)이 지났습니다.");
        }
    }
}
