package com.platform.coding.controller.mycourse;

import com.platform.coding.domain.user.User;
import com.platform.coding.service.mycourse.MyCourseService;
import com.platform.coding.service.mycourse.dto.MyCourseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/my-courses")
@RequiredArgsConstructor
public class MyCourseController {
    private final MyCourseService myCourseService;

    @GetMapping
    public ResponseEntity<List<MyCourseResponse>> getMyCourses(@AuthenticationPrincipal User user) {
        // @AuthenticationPrincipal로 현재 로그인한 사용자 정보를 가져옴
        List<MyCourseResponse> myCourses = myCourseService.getMyCourses(user);

        return ResponseEntity.ok(myCourses);
    }
}
