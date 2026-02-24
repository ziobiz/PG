package com.pg.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/terminal")
public class TerminalController {

    @GetMapping("/list")
    public String list(Model model) {
        model.addAttribute("title", "터미널 관리");
        model.addAttribute("activeMenu", "terminal");
        return "placeholder-list";
    }
}
