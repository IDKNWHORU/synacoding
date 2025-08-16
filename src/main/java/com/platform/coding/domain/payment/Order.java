package com.platform.coding.domain.payment;

import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders", schema = "platform")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    // 비즈니스적으로 사용할 수 있는 고유 주문 ID
    @Column(name = "order_uid", nullable = false, unique = true)
    private String orderUid;

    // 구매자인 부모
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", nullable = false)
    private User parent;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "total_price", nullable = false, precision = 0, scale = 2)
    private BigDecimal totalPrice;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    public Order(User parent, List<Course> courses) {
        this.orderUid = UUID.randomUUID().toString();
        this.parent = parent;
        this.status = OrderStatus.PENDING;
        this.createdAt = Instant.now();
        this.totalPrice = courses.stream()
                                 .map(Course::getPrice)
                                 .reduce(BigDecimal.ZERO, BigDecimal::add);
        courses.forEach(course -> this.addOrderItem(new OrderItem(this, course)));
    }

    public void addOrderItem(OrderItem orderItem) {
        this.orderItems.add(orderItem);
    }

    // 주문 처리 완료
    public void completeOrder() {
        this.status = OrderStatus.COMPLETED;
    }

    // 주문 취소(환불) 처리
    public void cancel() {
        if (this.status != OrderStatus.COMPLETED) {
            throw new IllegalStateException("완료된 주문만 취소할 수 있습니다. 현재 상태: " + this.status);
        }
        this.status = OrderStatus.CANCELLED;
    }
}
