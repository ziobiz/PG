package com.pg.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/settle")
public class SettleController {

    @GetMapping("/list")
    public String list(Model model) {
        model.addAttribute("title", "가맹점 정산");
        model.addAttribute("activeMenu", "settle");
        return "placeholder-list";
    }

    @GetMapping("/groupList")
    public String groupList(Model model) {
        model.addAttribute("title", "총판 정산");
        model.addAttribute("activeMenu", "settleGroup");
        return "placeholder-list";
    }

    @GetMapping("/report")
    public String report(Model model) {
        model.addAttribute("title", "정산 리포트");
        model.addAttribute("activeMenu", "settleReport");
        return "placeholder-list";
    }
}
