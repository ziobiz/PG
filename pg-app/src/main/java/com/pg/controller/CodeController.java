package com.pg.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/code")
public class CodeController {

    @GetMapping("/list")
    public String list(Model model) {
        model.addAttribute("title", "공통코드 관리");
        model.addAttribute("activeMenu", "code");
        return "placeholder-list";
    }
}
