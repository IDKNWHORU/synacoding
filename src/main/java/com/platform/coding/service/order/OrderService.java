package com.platform.coding.service.order;

import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.course.CourseRepository;
import com.platform.coding.domain.payment.Order;
import com.platform.coding.domain.payment.OrderRepository;
import com.platform.coding.domain.user.User;
import com.platform.coding.service.order.dto.OrderCreateRequest;
import com.platform.coding.service.order.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request, User parent) {
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의입니다."));
        
        // TODO: 이미 수강 중인 강의인지 확인하는 로직 추가 가능

        Order newOrder = new Order(parent, List.of(course));
        Order savedOrder = orderRepository.save(newOrder);

        return OrderResponse.fromEntity(savedOrder);
    }
}
