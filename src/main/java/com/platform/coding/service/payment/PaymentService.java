package com.platform.coding.service.payment;

import com.platform.coding.domain.enrollment.Enrollment;
import com.platform.coding.domain.enrollment.EnrollmentRepository;
import com.platform.coding.domain.payment.*;
import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserRepository;
import com.platform.coding.service.payment.dto.PaymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RewardRepository rewardRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    @Transactional
    public Long processPayment(PaymentRequest request, User parent) {
        // 1. 주문 정보 조회 및 검증
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다."));

        if (!order.getParent().getId().equals(parent.getId())) {
            throw new IllegalArgumentException("자신의 주문만 결제할 수 있습니다.");
        }

        // 2. 할인 적용 및 최정 결제 금액 계산
        BigDecimal originalPrice = order.getTotalPrice();
        BigDecimal finalPrice = originalPrice;
        Reward usedReward = null;

        // 핵심 로직: 포인트 또는 쿠폰 사용 처리
        if (request.pointId() != null && request.couponId() != null) {
            throw new IllegalArgumentException("포인트와 쿠폰은 동시에 사용할 수 없습니다.");
        }

        if (request.pointId() != null) {
            usedReward = validateAndApplyReward(request.pointId(), parent, RewardType.POINT);
            finalPrice = originalPrice.subtract(usedReward.getAmount());
            if (finalPrice.compareTo(BigDecimal.ZERO) < 0) {
                finalPrice = BigDecimal.ZERO; // 최종 가격이 0보다 작을 수 없음
            }
        } else if (request.couponId() != null) {
            usedReward = validateAndApplyReward(request.couponId(), parent, RewardType.COUPON);
            // 여기서는 쿠폰을 정액 할인으로 가정. (정책에 따라 정률 할인 등으로 변경 가능)
            finalPrice = originalPrice.subtract(usedReward.getAmount());
            if (finalPrice.compareTo(BigDecimal.ZERO) < 0) {
                finalPrice = BigDecimal.ZERO;
            }
        }

        // 3. 외부 PG사 연동 (여기서는 성공했다고 가정)
        // String pgTransactionId = pgApi.requestPayment(order.getOrderUid(), finalPrice, ...);
        String pgTransactionId = "pg_mock_" + System.currentTimeMillis();

        // 4. 결제 정보 생성 및 저장
        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(request.paymentMethod())
                .amount(finalPrice)
                .pgTransactionId(pgTransactionId)
                .usedReward(usedReward)
                .build();
        paymentRepository.save(payment);

        // 5. 사용한 보상(포인트/쿠폰) 상태 변경
        if (usedReward != null) {
            usedReward.use();
        }

        // 6. 주문 상태 변경
        order.completeOrder();

        // 7. 수강 등록 처리 (결제자의 자녀에게)
        User student = userRepository.findById(request.studentId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 학생 ID입니다."));

        if (student.getParent() == null || !student.getParent().getId().equals(parent.getId())) {
            throw new IllegalArgumentException("자신의 자녀에 대한 강의만 결제할 수 있습니다.");
        }

        order.getOrderItems().forEach(item -> {
            // 이미 수강 중인지 확인
            if (enrollmentRepository.findByStudentAndCourse(student, item.getCourse()).isPresent()) {
                log.info("Student {} is already enrolled in course {}", student.getId(), item.getCourse().getId());
            } else {
                Enrollment enrollment = new Enrollment(student, item.getCourse());
                enrollmentRepository.save(enrollment);
            }
        });

        return payment.getId();
    }

    private Reward validateAndApplyReward(Long rewardId, User user, RewardType expectedType) {
        Reward reward = rewardRepository.findById(rewardId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 보상 ID 입니다."));

        if (!reward.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("자신의 보상만 사용할 수 있습니다.");
        }
        if (reward.getRewardType() != expectedType) {
            throw new IllegalArgumentException("잘못된 보상 타입입니다.");
        }
        if (reward.isUsed()) {
            throw new IllegalArgumentException("이미 사용된 보상입니다.");
        }
        if (reward.getExpiresAt() != null && reward.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("만료된 보상입니다.");
        }

        return reward;
    }
}
