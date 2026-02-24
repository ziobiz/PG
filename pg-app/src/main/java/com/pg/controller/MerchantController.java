package com.pg.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/merchant")
public class MerchantController {

    @GetMapping("/list")
    public String list(Model model) {
        model.addAttribute("title", "가맹점 관리");
        model.addAttribute("activeMenu", "merchant");
        return "placeholder-list";
    }
}
