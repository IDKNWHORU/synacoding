package com.platform.coding.config;

import com.platform.coding.domain.user.User;
import com.platform.coding.service.notification.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
@RequiredArgsConstructor
public class NotificationInterceptor implements HandlerInterceptor {

    private final NotificationService notificationService;

    /**
     * 컨트롤러가 실행된 후, View가 렌더링되기 전에 실행됩니다.
     * 모든 페이지에서 'unreadNotificationCount' 변수를 사용할 수 있도록 모델에 추가합니다.
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // ModelAndView가 null이 아니어야 (리다이렉트 등이 아님) 모델에 데이터를 추가할 수 있음
        if (modelAndView == null) {
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 인증된 사용자인지, 그리고 Principal이 User 타입인지 확인
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            User user = (User) authentication.getPrincipal();
            long unreadCount = notificationService.getUnreadNotificationCount(user);
            modelAndView.addObject("unreadNotificationCount", unreadCount);
        }
    }
}