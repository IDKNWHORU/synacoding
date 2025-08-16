package com.platform.coding.domain.refund;

import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.course.CourseRepository;
import com.platform.coding.domain.payment.*;
import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserRepository;
import com.platform.coding.domain.user.UserType;
import com.platform.coding.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RefundRepositoryTest extends IntegrationTestSupport {
    @Autowired
    private RefundRepository refundRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private UserRepository userRepository;

    private Payment payment;

    @BeforeEach
    void setUp() {
        // DB 초기화
        refundRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        User parent = userRepository.save(User.builder()
                .email("parent@example.com")
                .passwordHash("password_hash123")
                .userName("학부모")
                .userType(UserType.PARENT)
                .build());
        User admin = userRepository.save(User.builder()
                .email("admin@example.com")
                .passwordHash("password_hash456")
                .userName("관리자")
                .userType(UserType.SUPER_ADMIN)
                .build());

        Course course = courseRepository.save(Course.builder()
                .admin(admin)
                .title("강의")
                .price(new BigDecimal("50000.00"))
                .build());
        Order order = orderRepository.save(new Order(parent, List.of(course)));
        payment = paymentRepository.save(Payment.builder()
                .order(order)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .amount(order.getTotalPrice())
                .build());
    }

    @Test
    @DisplayName("환불 정보 저장 및 조회 테스트")
    void createRefundRequest() {
        // given
        String reason = "강의 내용이 생각과 다릅니다.";
        BigDecimal refundAmount = payment.getAmount();

        // when
        Refund refund = Refund.builder()
                .payment(payment)
                .refundAmount(refundAmount)
                .reason(reason)
                .build();
        refundRepository.save(refund);

        // then
        Refund foundRefund = refundRepository.findById(refund.getId()).orElseThrow();
        assertThat(foundRefund.getId()).isNotNull();
        assertThat(foundRefund.getReason()).isEqualTo(reason);
        assertThat(foundRefund.getRefundAmount()).isEqualByComparingTo(refundAmount);
        assertThat(foundRefund.getPayment().getId()).isEqualTo(payment.getId());
    }

    @Test
    @DisplayName("결제 ID로 환불 정보가 존재하는지 조회한다.")
    void existsByPaymentId() {
        // given
        Refund refund = Refund.builder()
                .payment(payment)
                .refundAmount(payment.getAmount())
                .reason("단순 변심")
                .build();

        refundRepository.save(refund);

        // when
        boolean exists = refundRepository.existsByPaymentId(payment.getId());
        // 존재하지 않는 결제 ID
        boolean notExists = refundRepository.existsByPaymentId(999L);

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
}
