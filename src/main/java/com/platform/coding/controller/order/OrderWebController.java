package com.platform.coding.controller.order;

import com.platform.coding.domain.course.Course;
import com.platform.coding.domain.course.CourseRepository;
import com.platform.coding.domain.user.User;
import com.platform.coding.domain.user.UserRepository;
import com.platform.coding.service.user.dto.ChildAccountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderWebController {
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @GetMapping("/checkout/{courseId}")
    public String checkoutPage(
            @PathVariable Long courseId,
            @AuthenticationPrincipal User parent,
            Model model
    ) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 강의입니다."));

        // 현재 학부모의 자녀 목록을 가져옴
        List<User> children = userRepository.findByParent(parent);
        List<ChildAccountResponse> childDtos = children.stream()
                .map(ChildAccountResponse::fromEntity)
                .collect(Collectors.toList());

        model.addAttribute("course", course);
        model.addAttribute("children", childDtos);

        return "order/checkout";
    }
}
