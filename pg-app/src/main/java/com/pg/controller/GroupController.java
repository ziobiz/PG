package com.pg.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/group")
public class GroupController {

    @GetMapping("/list")
    public String list(Model model) {
        model.addAttribute("title", "총판 관리");
        model.addAttribute("activeMenu", "group");
        return "placeholder-list";
    }
}
