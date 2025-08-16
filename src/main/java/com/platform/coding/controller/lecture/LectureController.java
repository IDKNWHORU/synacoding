package com.platform.coding.controller.lecture;

import com.platform.coding.domain.user.User;
import com.platform.coding.service.lecture.LectureService;
import com.platform.coding.service.lecture.dto.LectureViewResponse;
import com.platform.coding.service.lecture.dto.ProgressUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lectures")
@RequiredArgsConstructor
public class LectureController {
    private final LectureService lectureService;

    // 동영상 시청 페이지 조회 API
    @GetMapping("/{lectureId}")
    public ResponseEntity<LectureViewResponse> getLecture(
            @PathVariable Long lectureId,
            @AuthenticationPrincipal User user
            ) {
        LectureViewResponse response = lectureService.getLectureForViewing(lectureId, user);

        return ResponseEntity.ok(response);
    }

    // 진도율 업데이트 API
    @PostMapping("/{lectureId}/progress")
    public ResponseEntity<Void> updateProgress(
            @PathVariable Long lectureId,
            @Valid @RequestBody ProgressUpdateRequest request,
            @AuthenticationPrincipal User user
            ) {
        lectureService.updateLectureProgress(lectureId, request, user);

        return ResponseEntity.ok().build();
    }
}
