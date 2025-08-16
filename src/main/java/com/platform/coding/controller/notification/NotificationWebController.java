package com.platform.coding.controller.notification;

import com.platform.coding.domain.user.User;
import com.platform.coding.service.notification.NotificationService;
import com.platform.coding.service.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationWebController {

    private final NotificationService notificationService;

    /**
     * 사용자의 전체 알림 목록 페이지를 보여줍니다.
     * 이 페이지에 접근하면 모든 알림이 '읽음' 상태로 처리됩니다.
     */
    @GetMapping
    public String notificationsPage(@AuthenticationPrincipal User user,
                                    @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                                    Model model) {
        // 1. 사용자의 모든 알림을 페이지네이션하여 조회합니다.
        Page<NotificationResponse> notificationPage = notificationService.getMyNotifications(user, pageable);
        model.addAttribute("notificationPage", notificationPage);

        // 2. 페이지네이션 UI를 위한 시작/끝 페이지 번호를 계산합니다.
        int nowPage = notificationPage.getPageable().getPageNumber() + 1;
        int startPage = Math.max(nowPage - 4, 1);
        int endPage = Math.min(nowPage + 4, notificationPage.getTotalPages());
        model.addAttribute("nowPage", nowPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage == 0 ? 1 : endPage);

        // 3. (중요) 이 페이지를 조회한 후에는 모든 알림을 읽음 처리하여 헤더의 카운트를 0으로 만듭니다.
        notificationService.markAllAsRead(user);

        return "notifications/list";
    }
}