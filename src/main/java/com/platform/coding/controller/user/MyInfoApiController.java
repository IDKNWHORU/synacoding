package com.platform.coding.controller.user;

import com.platform.coding.domain.user.User;
import com.platform.coding.service.user.MyInfoService;
import com.platform.coding.service.user.dto.PasswordUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/my-info")
@RequiredArgsConstructor
public class MyInfoApiController {

    private final MyInfoService myInfoService;

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody PasswordUpdateRequest request) {
        myInfoService.changePassword(user, request);
        return ResponseEntity.ok().build();
    }
}