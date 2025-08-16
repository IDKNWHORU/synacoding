package com.platform.coding.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.coding.config.jwt.JwtAuthenticationFilter;
import com.platform.coding.config.jwt.JwtUtil;
import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.course.CourseRepository;
import com.platform.coding.domain.payment.*;
import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserRepository;
import com.platform.coding.domain.user.UserType;
import com.platform.coding.service.payment.dto.PaymentRequest;
import com.platform.coding.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
public class PaymentControllerTest extends IntegrationTestSupport {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private RewardRepository rewardRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CourseRepository courseRepository;

    private User parent, student, parent2, otherStudent;
    private String parentToken, parent2Token;
    private Order order;
    private Reward point, coupon, usedPoint, expiredPoint;
    
    @BeforeEach
    void setUp() {
        // DB 초기화
        userRepository.deleteAllInBatch();
        rewardRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();
        
        parent = userRepository.save(User.builder()
                .email("parent@example.com")
                .passwordHash("password_hash123")
                .userName("학부모")
                .userType(UserType.PARENT)
                .build());
        parent2 = userRepository.save(User.builder()
                .email("parent2@example.com")
                .passwordHash("password_hash789")
                .userName("학부모2")
                .userType(UserType.PARENT)
                .build());
        // 자녀 생성 및 부모와 연결
        student = userRepository.save(User.builder()
                .email("student@example.com")
                .passwordHash("password_hash456")
                .userName("학생")
                .userType(UserType.STUDENT)
                .parent(parent)
                .build());
        otherStudent = userRepository.save(User.builder()
                .email("other.student@example.com")
                .passwordHash("password_hash456")
                .userName("다른학생")
                .userType(UserType.STUDENT)
                .parent(parent2)
                .build());

        parent.addChild(student);
        parent2.addChild(otherStudent);

        parentToken = jwtUtil.createAccessToken(parent);
        parent2Token = jwtUtil.createAccessToken(parent2);

        Course course = courseRepository.save(Course.builder()
                .admin(parent)
                .title("강의")
                .price(new BigDecimal("50000"))
                .build());
        order = orderRepository.save(new Order(parent, List.of(course)));
        
        // 테스트용 보상 생성
        point = rewardRepository.save(Reward.builder()
                .user(parent)
                .rewardType(RewardType.POINT)
                .amount(new BigDecimal("1000"))
                .build());
        coupon = rewardRepository.save(Reward.builder()
                .user(parent)
                .rewardType(RewardType.COUPON)
                .amount(new BigDecimal("5000"))
                .build());
        
        // 미리 사용된 포인트 생성
        Reward tempUsedPoint = Reward.builder()
                .user(parent)
                .rewardType(RewardType.POINT)
                .amount(new BigDecimal("100"))
                .build();
        tempUsedPoint.use();
        usedPoint = rewardRepository.save(tempUsedPoint);

        // 만료된 포인트 생성
        expiredPoint = rewardRepository.save(Reward.builder()
                .user(parent)
                .rewardType(RewardType.POINT)
                .amount(new BigDecimal("100"))
                .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build());
    }

    @Test
    @DisplayName("아무런 할인을 적용하지 않고 결제하면 원가 그대로 결제된다.")
    void processPaymentWithoutDiscount() throws Exception {
        // given
        PaymentRequest request = PaymentRequest.builder()
                .orderId(order.getId())
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .studentId(student.getId())
                .build();

        // when & then
        mockMvc.perform(post("/api/payments")
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + parentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated());

        Payment payment = paymentRepository.findAll().get(0);
        assertThat(payment.getAmount()).isEqualByComparingTo("50000.00");
        assertThat(payment.getUsedReward()).isNull();
    }

    @Test
    @DisplayName("포인트를 사용하여 결제하면 최종 금액이 할인되고 포인트는 사용 처리되어야 한다.")
    void processPaymentWithPoint() throws Exception {
        // given
        PaymentRequest request = PaymentRequest.builder()
                .orderId(order.getId())
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .studentId(student.getId())
                .pointId(point.getId())
                .build();

        // when
        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated());

        // then
        // 1. 결제 정보 검증
        Payment payment = paymentRepository.findAll().get(0);
        // 50000 - 1000
        assertThat(payment.getAmount()).isEqualByComparingTo("49000.00");
        assertThat(payment.getUsedReward().getId()).isEqualTo(point.getId());

        // 2. 주문 상태 검증
        Order completedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(completedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);

        // 3. 사용된 포인트 상태 검증
        Reward usedPoint = rewardRepository.findById(point.getId()).orElseThrow();
        assertThat(usedPoint.isUsed()).isTrue();
    }

    @Test
    @DisplayName("포인트와 쿠폰을 동시에 사용하려고 시도하면 실패(400 Bad Request)해야 한다.")
    void processPaymentFailWithPointAndCoupon() throws Exception {
        // given
        PaymentRequest request = PaymentRequest.builder()
                .orderId(order.getId())
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .studentId(student.getId())
                // 동시 사용 시도
                .pointId(point.getId())
                .couponId(coupon.getId())
                .build();

        // when & then
        mockMvc.perform(post("/api/payments")
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + parentToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("포인트와 쿠폰은 동시에 사용할 수 없습니다."));
    }

    @Test
    @DisplayName("보유 포인트가 결제 금액보다 클 경우, 최종 결제 금액은 0원이 되어야 한다.")
    void processPaymentWithExceedingPoint() throws Exception {
        // given: 결제 금액(50000)보다 더 큰 포인트를 생성
        Reward bigPoint = rewardRepository.save(Reward.builder()
                .user(parent)
                .rewardType(RewardType.POINT)
                .amount(new BigDecimal("60000"))
                .build());

        PaymentRequest request = PaymentRequest.builder()
                .orderId(order.getId())
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .studentId(student.getId())
                .pointId(bigPoint.getId())
                .build();

        // when & then
        mockMvc.perform(post("/api/payments")
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + parentToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated());

        Payment payment = paymentRepository.findAll().get(0);
        assertThat(payment.getAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("다른 사람의 포인트를 사용하려고 시도하면 실패(400 Bad Request)해야 한다.")
    void processPaymentFailWithOthersPoint() throws Exception {
        // given: parent2의 토큰으로, parent의 포인트를 사용하려고 시도
        PaymentRequest request = PaymentRequest.builder()
                .orderId(order.getId())
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .studentId(student.getId())
                .pointId(point.getId())
                .build();

        // when & then
        mockMvc.perform(post("/api/payments")
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + parent2Token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                // 주문 권한에서 먼저 걸림
                .andExpect(jsonPath("$.message").value("자신의 주문만 결제할 수 있습니다."));
    }

    @Test
    @DisplayName("이미 사용된 포인트를 다시 사용하려고 시도하면 실패(400 Bad Request)해야 한다.")
    void processPaymentFailWithUsedPoint() throws Exception {
        // given: 미리 '사용 처리된' 포인트를 사용
        PaymentRequest request = PaymentRequest.builder()
                .orderId(order.getId())
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .studentId(student.getId())
                .pointId(usedPoint.getId())
                .build();

        // mock & then
        mockMvc.perform(post("/api/payments")
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + parentToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("이미 사용된 보상입니다."));
    }

    @Test
    @DisplayName("만료된 포인트를 사용하려고 시도하면 실패(400 Bad Request)해야 한다.")
    void processPaymentFailWithExpiredPoint() throws Exception {
        // given
        PaymentRequest request = PaymentRequest.builder()
                .orderId(order.getId())
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .studentId(student.getId())
                .pointId(expiredPoint.getId())
                .build();

        // when & then
        mockMvc.perform(post("/api/payments")
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + parentToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("만료된 보상입니다."));
    }

    @Test
    @DisplayName("자신의 자녀가 아닌 다른 학생의 강의를 결제하려고 시도하면 실패(400 Bad Request)해야 한다.")
    void processPaymentFailForOtherParentsChild() throws Exception {
        // given: parentToken을 사용해 parent2의 자녀(otherStudent) 강의를 결제 시도
        PaymentRequest request = PaymentRequest.builder()
                .orderId(order.getId())
                .studentId(otherStudent.getId()) // 다른 학부모의 자녀 ID
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();

        // when & then
        mockMvc.perform(post("/api/payments")
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("자신의 자녀에 대한 강의만 결제할 수 있습니다."));
    }
}
