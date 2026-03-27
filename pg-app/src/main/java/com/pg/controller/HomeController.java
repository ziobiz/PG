package com.pg.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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
        return "redirect:/pay.html?m=" + compId;
    }
}
