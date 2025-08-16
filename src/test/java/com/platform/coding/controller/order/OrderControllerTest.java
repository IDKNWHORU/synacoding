package com.platform.coding.controller.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.coding.config.jwt.JwtAuthenticationFilter;
import com.platform.coding.config.jwt.JwtUtil;
import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.course.CourseRepository;
import com.platform.coding.domain.payment.OrderRepository;
import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserRepository;
import com.platform.coding.domain.user.UserType;
import com.platform.coding.service.order.dto.OrderCreateRequest;
import com.platform.coding.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class OrderControllerTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private OrderRepository orderRepository;

    private User parent;
    private String parentToken;
    private Course course;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        parent = userRepository.save(User.builder()
                .email("parent@example.com")
                .passwordHash("password_hash123")
                .userName("학부모")
                .userType(UserType.PARENT)
                .build());
        parentToken = jwtUtil.createAccessToken(parent);

        User admin = userRepository.save(User.builder()
                .email("admin@example.com")
                .passwordHash("password_hash456")
                .userName("관리자")
                .userType(UserType.SUPER_ADMIN)
                .build());

        course = courseRepository.save(Course.builder()
                .admin(admin)
                .title("테스트 강의")
                .price(new BigDecimal("50000.00"))
                .build());
    }

    @Test
    @DisplayName("로그인한 학부모는 성공적으로 강의 주문을 생성할 수 있다.")
    void createOrderSuccess() throws Exception {
        // given
        OrderCreateRequest request = new OrderCreateRequest(course.getId());
        long initialOrderCount = orderRepository.count();

        // when & then
        mockMvc.perform(post("/api/orders")
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.orderUid").exists())
                .andExpect(jsonPath("$.totalPrice").value(50000.00));

        // DB 검증
        assertThat(orderRepository.count()).isEqualTo(initialOrderCount + 1);
    }

    @Test
    @DisplayName("존재하지 않는 강의 ID로 주문을 생성하면 실패(400 Bad Request)해야 한다.")
    void createOrderFailWithInvalidCourseId() throws Exception {
        // given
        long invalidCourseId = 9999L;
        OrderCreateRequest request = new OrderCreateRequest(invalidCourseId);

        // when & then
        mockMvc.perform(post("/api/orders")
                        .header(JwtAuthenticationFilter.AUTHORIZATION_HEADER, JwtAuthenticationFilter.BEARER_PREFIX + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("존재하지 않는 강의입니다."));
    }
}