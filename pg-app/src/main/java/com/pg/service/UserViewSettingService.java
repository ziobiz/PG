package com.pg.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.HqViewCustomColumn;
import com.pg.entity.UserViewSetting;
import com.pg.repository.HqViewCustomColumnRepository;
import com.pg.repository.UserViewSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class UserViewSettingService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final UserViewSettingRepository repository;
    private final OrgViewColumnAllowanceService allowanceService;
    private final HqViewCustomColumnRepository hqViewCustomColumnRepository;

    public UserViewSettingService(UserViewSettingRepository repository,
                                  OrgViewColumnAllowanceService allowanceService,
                                  HqViewCustomColumnRepository hqViewCustomColumnRepository) {
        this.repository = repository;
        this.allowanceService = allowanceService;
        this.hqViewCustomColumnRepository = hqViewCustomColumnRepository;
    }

    public Map<String, Object> get(String username, String pageUrl) {
        String u = safe(username);
        String p = safe(pageUrl);
        return buildResponse(u, p);
    }

    private Map<String, Object> buildResponse(String username, String pageUrl) {
        Optional<List<String>> restriction = allowanceService.getRestrictedAllowedKeys(username, pageUrl);
        var opt = repository.findByUsernameAndPageUrl(username, pageUrl);
        List<String> stored = parseJsonArray(opt.map(UserViewSetting::getSelectedKeysJson).orElse("[]"));

        List<String> effective;
        if (restriction.isPresent()) {
            Set<String> allow = new HashSet<>(restriction.get());
            effective = stored.stream().filter(allow::contains).collect(java.util.stream.Collectors.toList());
        } else {
            effective = stored;
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("username", username);
        m.put("pageUrl", pageUrl);
        m.put("hasSetting", opt.isPresent());
        m.put("selectedKeysJson", writeJsonArray(effective));
        m.put("columnAllowanceRestricted", restriction.isPresent());
        if (restriction.isPresent()) {
            m.put("allowedKeysJson", writeJsonArray(restriction.get()));
        } else {
            m.put("allowedKeysJson", null);
        }
        allowanceService.resolveRegionalAncestorOrgCode(username).ifPresent(code -> m.put("regionalScopeOrgCode", code));
        List<Map<String, Object>> customCols = new ArrayList<>();
        for (HqViewCustomColumn c : hqViewCustomColumnRepository.findByPageUrlOrderBySortOrderAscIdAsc(pageUrl)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", c.getId());
            row.put("columnKey", c.getColumnKey());
            row.put("displayName", c.getDisplayName());
            customCols.add(row);
        }
        m.put("customViewColumns", customCols);
        return m;
    }

    @Transactional
    public Map<String, Object> save(String username, String pageUrl, String selectedKeysJson) {
        String u = safe(username);
        String p = safe(pageUrl);
        List<String> keys = new ArrayList<>(parseJsonArray(selectedKeysJson == null || selectedKeysJson.isBlank() ? "[]" : selectedKeysJson));
        allowanceService.getRestrictedAllowedKeys(u, p).ifPresent(allowed -> {
            Set<String> as = new HashSet<>(allowed);
            keys.removeIf(k -> !as.contains(k));
        });
        String json = writeJsonArray(keys);
        UserViewSetting row = repository.findByUsernameAndPageUrl(u, p).orElseGet(UserViewSetting::new);
        row.setUsername(u);
        row.setPageUrl(p);
        row.setSelectedKeysJson(json);
        repository.save(row);
        return buildResponse(u, p);
    }

    private static String safe(String v) {
        return v == null ? "" : v.trim();
    }

    private static List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            List<String> list = MAPPER.readValue(json.trim(), new TypeReference<List<String>>() {});
            return list != null ? new ArrayList<>(list) : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static String writeJsonArray(List<String> keys) {
        try {
            return MAPPER.writeValueAsString(keys != null ? keys : List.of());
        } catch (Exception e) {
            return "[]";
        }
    }
}
