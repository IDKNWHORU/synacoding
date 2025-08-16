package com.platform.coding.service.admin.dto;

import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.user.User;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

public record CourseCreateRequest(
        @NotBlank(message = "강의 제목은 필수입니다.")
        String title,

        String description,

        @NotNull(message = "가격을 입력해주세요.")
        @DecimalMin(value = "0.0", message = "가격은 0 이상이어야 합니다.")
        BigDecimal price
) {
    @Builder
    public CourseCreateRequest {}

    public Course toEntity(User admin) {
        return Course.builder()
                .title(this.title)
                .description(this.description)
                .price(this.price)
                .admin(admin)
                .build();
    }
}
