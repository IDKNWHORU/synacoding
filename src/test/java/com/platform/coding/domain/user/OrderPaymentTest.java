package com.platform.coding.domain.user;

import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.course.CourseRepository;
import com.platform.coding.domain.enrollment.Enrollment;
import com.platform.coding.domain.enrollment.EnrollmentRepository;
import com.platform.coding.domain.payment.*;
import com.platform.coding.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class OrderPaymentTest extends IntegrationTestSupport {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    // 수강 등록 확인용
    @Autowired
    private EnrollmentRepository enrollmentRepository;

    private User parent;
    private User student;
    private Course springCourse;
    private Course jpaCourse;

    @BeforeEach
    void setUp() {
        // 사용자 준비
        parent = userRepository.save(User.builder()
                .email("parent@example.com")
                .passwordHash("hashed_password_123")
                .userName("보호자")
                .userType(UserType.PARENT)
                .build());
        student = userRepository.save(User.builder()
                .email("student@example.com")
                .passwordHash("hashed_password_456")
                .userName("수강생")
                .userType(UserType.STUDENT)
                .parent(parent)
                .build());
        User admin = userRepository.save(User.builder()
                .email("admin@example.com")
                .passwordHash("admin_pass")
                .userName("최고 관리자")
                .userType(UserType.SUPER_ADMIN)
                .build());

        // 강의 준비
        springCourse = courseRepository.save(Course.builder()
                .admin(admin)
                .title("Spring")
                .price(new BigDecimal("10000.00"))
                .build());
        jpaCourse = courseRepository.save(Course.builder()
                .admin(admin)
                .title("JPA")
                .price(new BigDecimal("20000.00"))
                .build());
    }

    @Test
    @DisplayName("사용자가 여러 강의를 선택하여 주문을 생성하고 결제에 성공하면, 주문 상태가 COMPLETED로 변경되고 수강 등록이 완료되어야 한다.")
    void createOrderAndCompletePayment() {
        // given: 구매할 강의 목록으로 주문(Order)을 발생시킨다. (상태: PENDING)
        Order newOrder = new Order(parent, List.of(springCourse, jpaCourse));
        orderRepository.save(newOrder);

        // when: 외부 PG 결제가 성공했다고 가정하고, 결제(Payment) 정보를 생성하고 주문 상태를 변경한다.
        // (실제 애플리케이션에서는 이 로직이 별도의 PaymentService 안에 있어야 됨)

        // 결제 정보 생성
        Payment payment = Payment.builder()
                .order(newOrder)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .amount(newOrder.getTotalPrice())
                .pgTransactionId("pg_trans_12345")
                .build();
        paymentRepository.save(payment);

        // 주문 상태 변경
        newOrder.completeOrder();

        // 주문된 강의들에 대해 수강 등록(Enrollment) 처리
        newOrder.getOrderItems().forEach(item -> {
            Enrollment enrollment = new Enrollment(student, item.getCourse());
            enrollmentRepository.save(enrollment);
        });

        // then: 모든 상태가 올바르게 변경되었는지 검증한다.
        Order foundOrder = orderRepository.findById(newOrder.getId()).orElseThrow();
        
        // 주문 상태 검증
        assertThat(foundOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(foundOrder.getTotalPrice()).isEqualTo(new BigDecimal("30000.00"));
        assertThat(foundOrder.getOrderItems()).hasSize(2);

        // 결제 기록 검증
        List<Payment> payments = paymentRepository.findAll();
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getOrder().getId()).isEqualTo(foundOrder.getId());
        assertThat(payments.get(0).getPgTransactionId()).isEqualTo("pg_trans_12345");

        // 수강 등록 검증
        List<Enrollment> enrollments = enrollmentRepository.findAll();
        assertThat(enrollments).hasSize(2);
        assertThat(enrollments.stream().map(e -> e.getCourse().getTitle())).containsExactlyInAnyOrder("Spring", "JPA");
        assertThat(enrollments.stream().map(e -> e.getStudent().getId()).allMatch(id -> id.equals(student.getId()))).isTrue();
    }
}
