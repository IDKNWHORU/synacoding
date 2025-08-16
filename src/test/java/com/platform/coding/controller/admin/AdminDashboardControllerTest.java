package com.platform.coding.controller.admin;

import com.platform.coding.config.jwt.JwtAuthenticationFilter;
import com.platform.coding.config.jwt.JwtUtil;
import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.course.CourseRepository;
import com.platform.coding.domain.enrollment.Enrollment;
import com.platform.coding.domain.enrollment.EnrollmentRepository;
import com.platform.coding.domain.payment.*;
import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserRepository;
import com.platform.coding.domain.user.UserType;
import com.platform.coding.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class AdminDashboardControllerTest extends IntegrationTestSupport {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;
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

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception{
        // DB 초기화
        enrollmentRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        User admin = userRepository.save(User.builder()
                .email("admin@example.com")
                .passwordHash("password_hash123")
                .userName("관리자")
                .userType(UserType.SUPER_ADMIN)
                .build());
        adminToken = jwtUtil.createAccessToken(admin);
        
        // 오늘 가입한 사용자 2명
        userRepository.save(User.builder()
                .email("today1@example.com")
                .userName("오늘가입사용자1")
                .passwordHash("password_hash456")
                .userType(UserType.PARENT)
                .build());
        userRepository.save(User.builder()
                .email("today2@example.com")
                .userName("오늘가입사용자2")
                .passwordHash("password_hash789")
                .userType(UserType.PARENT)
                .build());
        // 어제 가입한 사용자 1명
        User yesterdayUser = userRepository.save(User.builder()
                .email("yesterday@example.com")
                .userName("어제가입사용자")
                .passwordHash("password_hash1234")
                .userType(UserType.PARENT)
                .build());
        setEntityCreatingTimestamp(yesterdayUser, Instant.now().minus(1, ChronoUnit.DAYS));
        userRepository.save(yesterdayUser);

        Course courseA = courseRepository.save(Course.builder()
                .admin(admin)
                .title("A코스")
                .price(new BigDecimal("10000.00"))
                .build());
        Course courseB = courseRepository.save(Course.builder()
                .admin(admin)
                .title("B코스")
                .price(new BigDecimal("20000.00"))
                .build());
        Course courseC = courseRepository.save(Course.builder()
                .admin(admin)
                .title("C코스")
                .price(new BigDecimal("5000.00"))
                .build());

        User parent = userRepository.save(User.builder()
                .email("parent@example.com")
                .userName("학부모")
                .passwordHash("password_hash6789")
                .userType(UserType.PARENT)
                .build());
     
        User student1 = userRepository.save(User.builder()
                .email("studen1t@example.com")
                .userName("학생1")
                .passwordHash("password_hash5678")
                .userType(UserType.STUDENT)
                .parent(parent)
                .build()
        );

        User student2 = userRepository.save(User.builder()
                .email("student2@example.com")
                .userName("학생2")
                .passwordHash("password_hash5678")
                .userType(UserType.STUDENT)
                .parent(parent)
                .build()
        );

        // 오늘 결제 2건 (A코스, B코스)
        createPaymentAndEnrollment(parent, student1, courseA, new BigDecimal("10000.00"), Instant.now());
        createPaymentAndEnrollment(parent, student1, courseB, new BigDecimal("20000.00"), Instant.now());
        // 3일 전 결제 1건 (B코스)
        createPaymentAndEnrollment(parent, student2, courseB, new BigDecimal("20000.00"), Instant.now().minus(3, ChronoUnit.DAYS));
        // 8일 전 결제 1건 (C코스)
        createPaymentAndEnrollment(parent, student1, courseC, new BigDecimal("5000.00"), Instant.now().minus(8, ChronoUnit.DAYS));
    }

    private void createPaymentAndEnrollment(User parent, User student, Course course, BigDecimal amount, Instant paidAt) throws Exception {
        Order order = orderRepository.save(new Order(parent, List.of(course)));
        order.completeOrder();
        Payment payment = Payment.builder()
                .order(order)
                .amount(amount)
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();
        setEntityCreatingTimestamp(payment, paidAt, "paidAt");
        paymentRepository.save(payment);
        enrollmentRepository.save(new Enrollment(student, course));
    }

    private void setEntityCreatingTimestamp(Object entity, Instant timestamp, String fieldName) throws Exception {
        Field createdAtField = entity.getClass().getDeclaredField(fieldName);
        createdAtField.setAccessible(true);
        createdAtField.set(entity, timestamp);
    }

    private void setEntityCreatingTimestamp(Object entity, Instant timestamp) throws Exception {
        Field createdAtField = entity.getClass().getDeclaredField("createdAt");
        createdAtField.setAccessible(true);
        createdAtField.set(entity, timestamp);
    }

    @Test
    @DisplayName("관리자 대시보드 통계 API를 호출하면, 일별/주별 매출, 신규 가입자, 인기 강좌 정보가 정확히 반환된다.")
    void getDashboardStatsSuccess() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/stats")
                    .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + adminToken))
                .andDo(print())
                .andExpect(status().isOk())
                // 매출 검증
                .andExpect(jsonPath("$.dailyRevenue").value("30000.0"))
                .andExpect(jsonPath("$.weeklyRevenue").value("50000.0"))
                // 신규 가입자 검증
                .andExpect(jsonPath("$.newUsersToday").value(7))
                .andExpect(jsonPath("$.popularCourses").isArray())
                .andExpect(jsonPath("$.popularCourses.length()").value(3))
                .andExpect(jsonPath("$.popularCourses[0].courseTitle").value("B코스"))
                .andExpect(jsonPath("$.popularCourses[0].enrollmentCount").value(2));
    }
}
