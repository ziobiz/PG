package com.pg.service;

import com.pg.entity.AppUser;
import com.pg.entity.HqViewCustomColumn;
import com.pg.repository.HqViewCustomColumnRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class HqViewCustomColumnService {

    private final HqViewCustomColumnRepository repository;
    private final OrgViewColumnAllowanceService allowanceService;

    public HqViewCustomColumnService(HqViewCustomColumnRepository repository,
                                    OrgViewColumnAllowanceService allowanceService) {
        this.repository = repository;
        this.allowanceService = allowanceService;
    }

    public List<Map<String, Object>> list(String pageUrl, AppUser actor) {
        requireManage(actor);
        String p = safe(pageUrl);
        if (p.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (HqViewCustomColumn row : repository.findByPageUrlOrderBySortOrderAscIdAsc(p)) {
            out.add(toMap(row));
        }
        return out;
    }

    @Transactional
    public Map<String, Object> add(String pageUrl, String displayName, AppUser actor) {
        requireManage(actor);
        String p = safe(pageUrl);
        String name = safe(displayName);
        if (p.isEmpty()) {
            throw new IllegalArgumentException("화면 경로(pageUrl)가 필요합니다.");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("항목 표시명을 입력하세요.");
        }
        if (name.length() > 200) {
            throw new IllegalArgumentException("표시명은 200자 이내입니다.");
        }
        String key = "hqExt_" + UUID.randomUUID().toString().replace("-", "");
        int order = (int) repository.countByPageUrl(p);
        Instant now = Instant.now();
        HqViewCustomColumn row = new HqViewCustomColumn();
        row.setPageUrl(p);
        row.setColumnKey(key);
        row.setDisplayName(name);
        row.setSortOrder(order);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        repository.save(row);
        return toMap(row);
    }

    @Transactional
    public Map<String, Object> update(long id, String displayName, AppUser actor) {
        requireManage(actor);
        String name = safe(displayName);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("항목 표시명을 입력하세요.");
        }
        if (name.length() > 200) {
            throw new IllegalArgumentException("표시명은 200자 이내입니다.");
        }
        HqViewCustomColumn row = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("항목을 찾을 수 없습니다."));
        row.setDisplayName(name);
        row.setUpdatedAt(Instant.now());
        repository.save(row);
        return toMap(row);
    }

    @Transactional
    public void delete(long id, AppUser actor) {
        requireManage(actor);
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("항목을 찾을 수 없습니다.");
        }
        repository.deleteById(id);
    }

    private void requireManage(AppUser actor) {
        if (!allowanceService.canManageOrgViewAllowance(actor)) {
            throw new IllegalArgumentException("총본사(또는 ADMIN)만 관리할 수 있습니다.");
        }
    }

    private static Map<String, Object> toMap(HqViewCustomColumn row) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", row.getId());
        m.put("pageUrl", row.getPageUrl());
        m.put("columnKey", row.getColumnKey());
        m.put("displayName", row.getDisplayName());
        m.put("sortOrder", row.getSortOrder());
        m.put("createdAt", row.getCreatedAt() != null ? row.getCreatedAt().toString() : "");
        m.put("updatedAt", row.getUpdatedAt() != null ? row.getUpdatedAt().toString() : "");
        return m;
    }

    private static String safe(String v) {
        return v == null ? "" : v.trim();
    }
}
