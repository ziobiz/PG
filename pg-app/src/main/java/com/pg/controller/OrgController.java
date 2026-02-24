package com.pg.controller;

import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.OrgUnitRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/org")
public class OrgController {

    private final OrgUnitRepository orgUnitRepository;

    public OrgController(OrgUnitRepository orgUnitRepository) {
        this.orgUnitRepository = orgUnitRepository;
    }

    @GetMapping("/{levelCode}")
    public String list(@PathVariable int levelCode, Model model) {
        OrgLevel level = OrgLevel.fromCode(levelCode);
        if (level == null) {
            return "redirect:/main";
        }
        List<OrgUnit> list = orgUnitRepository.findByOrgLevelOrderByCodeAsc(level);
        model.addAttribute("level", level);
        model.addAttribute("levelCode", levelCode);
        model.addAttribute("list", list);
        model.addAttribute("activeMenu", "org" + levelCode);
        return "org-level";
    }
}
