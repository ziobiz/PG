package com.pg.service;

import com.pg.entity.UserViewSetting;
import com.pg.repository.UserViewSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class UserViewSettingService {

    private final UserViewSettingRepository repository;

    public UserViewSettingService(UserViewSettingRepository repository) {
        this.repository = repository;
    }

    public Map<String, Object> get(String username, String pageUrl) {
        String u = safe(username);
        String p = safe(pageUrl);
        var opt = repository.findByUsernameAndPageUrl(u, p);
        String selected = opt.map(UserViewSetting::getSelectedKeysJson).orElse("[]");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("username", u);
        m.put("pageUrl", p);
        m.put("hasSetting", opt.isPresent());
        m.put("selectedKeysJson", selected != null ? selected : "[]");
        return m;
    }

    @Transactional
    public Map<String, Object> save(String username, String pageUrl, String selectedKeysJson) {
        String u = safe(username);
        String p = safe(pageUrl);
        String json = selectedKeysJson == null || selectedKeysJson.isBlank() ? "[]" : selectedKeysJson.trim();
        UserViewSetting row = repository.findByUsernameAndPageUrl(u, p).orElseGet(UserViewSetting::new);
        row.setUsername(u);
        row.setPageUrl(p);
        row.setSelectedKeysJson(json);
        repository.save(row);
        return get(u, p);
    }

    private static String safe(String v) {
        return v == null ? "" : v.trim();
    }
}
