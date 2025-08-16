package com.platform.coding.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {
    /**
     * @return 메인 페이지를 반환한다.
     */
    @GetMapping("/")
    public String mainPage() {
        return "index";
    }

    /**
     * @return 로그인 페이지를 반환한다.
     */
    @GetMapping("/login")
    public String loginPage() {
        return "user/login";
    }

    /**
     * @return 회원가입 페이지를 반환한다.
     */
    @GetMapping("/signup")
    public String signupPage() {
        return "user/signup";
    }
}
