package com.pg.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
public class HomeController {

    /** 루트(/) 리다이렉트는 RootRedirectFilter 가 처리 (환영 페이지 406 방지) */

    @GetMapping("/main")
    public String main() {
        return "redirect:/index.html";
    }

    @GetMapping("/transactions")
    public String transactions() {
        return "transactions";
    }

    @GetMapping("/merchants")
    public String merchantsRedirect() {
        return "redirect:/org/6";
    }

    @GetMapping("/pay/{compId}")
    public String payByComp(@PathVariable("compId") String compId) {
        String enc = URLEncoder.encode(compId != null ? compId : "", StandardCharsets.UTF_8);
        return "redirect:/pay.html?m=" + enc;
    }

    /** 챗봇·임베드용 공개 진입 URL (결제 화면은 동일 셸, 구분은 쿼리로 확장 가능) */
    @GetMapping("/chatbot-pay/{compId}")
    public String chatbotPayByComp(@PathVariable("compId") String compId) {
        String enc = URLEncoder.encode(compId != null ? compId : "", StandardCharsets.UTF_8);
        return "redirect:/chatbot-pay.html?m=" + enc;
    }
}
