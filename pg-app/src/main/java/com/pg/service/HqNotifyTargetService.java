package com.pg.service;

import com.pg.entity.HqNotifyTarget;
import com.pg.repository.HqNotifyTargetRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class HqNotifyTargetService {
    private final HqNotifyTargetRepository repository;
    private final HqNotifyEnvService hqNotifyEnvService;

    public HqNotifyTargetService(HqNotifyTargetRepository repository, HqNotifyEnvService hqNotifyEnvService) {
        this.repository = repository;
        this.hqNotifyEnvService = hqNotifyEnvService;
    }

    public List<Map<String, Object>> list() {
        return repository.findAllByOrderByIdDesc().stream().map(this::toMap).toList();
    }

    @Transactional
    public Map<String, Object> create(String targetName, HttpServletRequest req) {
        String name = targetName == null ? "" : targetName.trim();
        if (name.isEmpty()) throw new IllegalArgumentException("노티 대상 이름을 입력하세요.");
        String code = ("NT" + System.currentTimeMillis()).toUpperCase(Locale.ROOT);
        while (repository.findByTargetCode(code).isPresent()) {
            code = "NT" + (System.currentTimeMillis() + (long)(Math.random() * 1000));
        }
        var cfg = hqNotifyEnvService.getOrCreate();
        String base = hqNotifyEnvService.buildNotifyIngressUrl(cfg, req);
        String url = base + "/" + code;
        HqNotifyTarget t = new HqNotifyTarget();
        t.setTargetCode(code);
        t.setTargetName(name);
        t.setTargetUrl(url);
        t.setUseYn("Y");
        repository.save(t);
        return toMap(t);
    }

    @Transactional
    public void delete(Long id) {
        HqNotifyTarget t = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("노티 대상을 찾을 수 없습니다."));
        repository.delete(t);
    }

    private Map<String, Object> toMap(HqNotifyTarget t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("targetCode", t.getTargetCode());
        m.put("targetName", t.getTargetName());
        m.put("targetUrl", t.getTargetUrl());
        m.put("useYn", t.getUseYn());
        m.put("createdAt", t.getCreatedAt() != null ? t.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
        return m;
    }
}

