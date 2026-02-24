package com.pg.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/van")
public class VanController {

    @GetMapping("/list")
    public String list(Model model) {
        model.addAttribute("title", "VAN(PG) 관리");
        model.addAttribute("activeMenu", "van");
        return "placeholder-list";
    }
}
