package com.platform.coding.controller.user;

import com.platform.coding.domain.user.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/my-info")
public class MyInfoWebController {

    @GetMapping
    public String myInfoPage(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("user", user);
        return "user/my-info";
    }
}