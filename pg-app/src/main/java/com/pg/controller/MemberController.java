package com.pg.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/member")
public class MemberController {

    @GetMapping("/list")
    public String list(Model model) {
        model.addAttribute("title", "멤버(관리자) 관리");
        model.addAttribute("activeMenu", "member");
        return "placeholder-list";
    }
}
