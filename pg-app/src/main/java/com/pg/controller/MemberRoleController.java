package com.pg.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/memberRole")
public class MemberRoleController {

    @GetMapping("/list")
    public String list(Model model) {
        model.addAttribute("title", "권한(역할) 관리");
        model.addAttribute("activeMenu", "memberRole");
        return "placeholder-list";
    }
}
