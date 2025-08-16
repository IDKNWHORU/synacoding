package com.platform.coding.controller.refund;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.coding.config.jwt.JwtAuthenticationFilter;
import com.platform.coding.config.jwt.JwtUtil;
import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.course.CourseRepository;
import com.platform.coding.domain.enrollment.Enrollment;
import com.platform.coding.domain.enrollment.EnrollmentRepository;
import com.platform.coding.domain.enrollment.EnrollmentStatus;
import com.platform.coding.domain.payment.*;
import com.platform.coding.domain.refund.Refund;
import com.platform.coding.domain.refund.RefundRepository;
import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserRepository;
import com.platform.coding.domain.user.UserType;
import com.platform.coding.service.refund.dto.RefundRequest;
import com.platform.coding.support.IntegrationTestSupport;
import org.aspectj.weaver.ast.Or;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class RefundControllerTest extends IntegrationTestSupport {
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    @Autowired
    private RefundRepository refundRepository;

    private User parent, otherParent;
    private String parentToken;
    private Payment validPayment, oldPayment, progressedPayment;
    private Enrollment noProgressEnrollment, progressEnrollment;

    @BeforeEach
    void setUp() throws Exception {
        // DB 초기화
        userRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        enrollmentRepository.deleteAllInBatch();
        refundRepository.deleteAllInBatch();
        
        // 사용자, 강의, 주문, 결제 데이터 생성
        parent = userRepository.save(User.builder()
                .email("parent@example.com")
                .passwordHash("password_hash123")
                .userName("학부모")
                .userType(UserType.PARENT)
                .build());
        otherParent = userRepository.save(User.builder()
                .email("other@example.com")
                .passwordHash("password_hash1234")
                .userName("학부모2")
                .userType(UserType.PARENT)
                .build());
        User student = userRepository.save(User.builder()
                .email("student@example.com")
                .passwordHash("password_hash456")
                .userName("학생")
                .userType(UserType.STUDENT)
                .parent(parent)
                .build());
        User admin = userRepository.save(User.builder()
                .email("admin@example.com")
                .passwordHash("password_hash789")
                .userName("관리자")
                .userType(UserType.SUPER_ADMIN)
                .build());
        parentToken = jwtUtil.createAccessToken(parent);

        // 환불 가능한 정상 결제 건
        Course validCourse = courseRepository.save(Course.builder()
                .admin(admin)
                .title("환불가능 강의")
                .price(BigDecimal.TEN)
                .build());
        Order validOrder = new Order(parent, List.of(validCourse));
        validOrder.completeOrder();
        orderRepository.save(validOrder);
        validPayment = paymentRepository.save(Payment.builder()
                .order(validOrder)
                .amount(validOrder.getTotalPrice())
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build());
        noProgressEnrollment = enrollmentRepository.save(Enrollment.builder()
                .student(student)
                .course(validCourse)
                .build());

        // 환불 기간(7일)이 지난 결제 건
        Course oldCourse = courseRepository.save(Course.builder()
                .admin(admin)
                .title("오래된 강의")
                .price(BigDecimal.TEN)
                .build());
        Order oldOrder = new Order(parent, List.of(oldCourse));
        oldOrder.completeOrder();
        orderRepository.save(oldOrder);
        Payment tempOldPayment = paymentRepository.save(Payment.builder()
                .order(oldOrder)
                .amount(oldOrder.getTotalPrice())
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build());
        enrollmentRepository.save(Enrollment.builder()
                .student(student)
                .course(oldCourse)
                .build());
        
        // Reflection을 사용해 'paidAt' 필드 값을 8일 전으로 수정
        Field paidAtField = tempOldPayment.getClass().getDeclaredField("paidAt");
        paidAtField.setAccessible(true);
        paidAtField.set(tempOldPayment, Instant.now().minus(8, ChronoUnit.DAYS));
        oldPayment = paymentRepository.save(tempOldPayment);

        // 수강 이력이 있는 결제 건
        Course progressedCourse = courseRepository.save(Course.builder()
                .admin(admin)
                .title("진행중 강의")
                .price(BigDecimal.TEN)
                .build());
        Order progressOrder = new Order(parent, List.of(progressedCourse));
        progressOrder.completeOrder();
        orderRepository.save(progressOrder);
        progressedPayment = paymentRepository.save(Payment.builder()
                .order(progressOrder)
                .amount(progressOrder.getTotalPrice())
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build());
        progressEnrollment = enrollmentRepository.save(Enrollment.builder()
                .student(student)
                .course(progressedCourse)
                .build());
        // 진도율 10%
        progressEnrollment.updateProgress(new BigDecimal("10.00"));
    }

    @Test
    @DisplayName("정책에 부합하는(7일 이내, 수강이력 없음) 환불 요청 시 성공(201 Created)한다.")
    void requestRefundSuccess() throws Exception {
        // given
        RefundRequest request = new RefundRequest("단순 변심");
        long initialRefundCount = refundRepository.count();

        // when & then
        mockMvc.perform(post("/api/payments/{paymentId}/refund-request", validPayment.getId())
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + parentToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated());
        
        // DB 검증
        assertThat(refundRepository.count()).isEqualTo(initialRefundCount + 1);
        Enrollment updatedEnrollment = enrollmentRepository.findById(noProgressEnrollment.getId()).orElseThrow();
        assertThat(updatedEnrollment.getStatus()).isEqualTo(EnrollmentStatus.REFUND_REQUESTED);
        Order updatedOrder = orderRepository.findById(validPayment.getOrder().getId()).orElseThrow();
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("자신의 결제가 아닌 경우, 환불 요청 시 실패(400 Bad Request)한다.")
    void requestRefundFailsWhenNotMyParent() throws Exception {
        // given
        RefundRequest request = new RefundRequest("다른 사람 결제 환불 시도");
        String otherParentToken = jwtUtil.createAccessToken(otherParent);

        // when & then
        mockMvc.perform(post("/api/payments/{paymentId}/refund-request", validPayment.getId())
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + otherParentToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("자신의 결제 건에 대해서만 환불을 요청할 수 있습니다."));
    }

    @Test
    @DisplayName("환불 기간(7일)이 지난 결제 건에 대해 환불 요청 시 실패(400 Bad Request)한다.")
    void requestRefundFailsWhenTimeLimitExceeded() throws Exception {
        // given
        RefundRequest request = new RefundRequest("기간 지남");

        // when & then
        mockMvc.perform(post("/api/payments/{paymentId}/refund-request", oldPayment.getId())
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + parentToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("전액 환불 기간(7일)이 지났습니다."));
    }

    @Test
    @DisplayName("수강 이력이 있는 강의에 대해 환불 요청 시 실패(400 Bad Request)한다.")
    void requestRefundFailsWhenCourseInProgress() throws Exception {
        // given
        RefundRequest request = new RefundRequest("강의 들었음");

        // when & then
        mockMvc.perform(post("/api/payments/{paymentId}/refund-request", progressedPayment.getId())
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + parentToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("수강 이력이 있는 강의(진행중 강의)는 환불할 수 없습니다."));
    }

    @Test
    @DisplayName("이미 환불 요청이 접수된 결제 건에 대해 중복 요청 시 실패(400 Bad Request)한다.")
    void requestRefundFailsWhenAlreadyRequested() throws Exception {
        // given
        // 첫 번째 요청을 미리 보낸다.
        refundRepository.save(Refund.builder()
                .payment(validPayment)
                .refundAmount(BigDecimal.TEN)
                .reason("첫 요청")
                .build());
        RefundRequest request = new RefundRequest("중복 요청");

        // when & then
        mockMvc.perform(post("/api/payments/{paymentId}/refund-request", validPayment.getId())
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + parentToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미 환불 요청이 접수된 결제입니다."));
    }
}
