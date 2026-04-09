package com.pg.service;

import com.pg.api.dto.PageResult;
import com.pg.entity.MerchantNotifyUrl;
import com.pg.entity.OrgUnit;
import com.pg.repository.MerchantNotifyUrlRepository;
import com.pg.repository.OrgUnitRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotifyUrlService {

    private final MerchantNotifyUrlRepository merchantNotifyUrlRepository;
    private final OrgUnitRepository orgUnitRepository;

    public NotifyUrlService(MerchantNotifyUrlRepository merchantNotifyUrlRepository,
                            OrgUnitRepository orgUnitRepository) {
        this.merchantNotifyUrlRepository = merchantNotifyUrlRepository;
        this.orgUnitRepository = orgUnitRepository;
    }

    public PageResult<Map<String, Object>> searchPayUrl(String searchCompId, String urlType, int page, int size) {
        List<MerchantNotifyUrl> all = merchantNotifyUrlRepository.findAll();
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (MerchantNotifyUrl n : all) {
            OrgUnit ou = orgUnitRepository.findById(n.getOrgUnitId()).orElse(null);
            if (ou == null) continue;
            String compId = ou.getCode();
            if (searchCompId != null && !searchCompId.trim().isEmpty() && !compId.contains(searchCompId.trim()))
                continue;
            if (urlType != null && !urlType.trim().isEmpty()) {
                String ut = urlType.trim().toUpperCase();
                if ("PAY".equals(ut)) {
                    if (!"BACKGROUND".equals(n.getUrlType()) && !"RESULT".equals(n.getUrlType())
                            && !"MIDDLEWARE".equals(n.getUrlType())) continue;
                } else if (!ut.equals(n.getUrlType())) continue;
            }
            Map<String, Object> m = new HashMap<>();
            m.put("compId", compId);
            m.put("urlType", "BACKGROUND".equals(n.getUrlType()) ? "URL Background"
                    : "RESULT".equals(n.getUrlType()) ? "URL Result"
                    : "MIDDLEWARE".equals(n.getUrlType()) ? "PG Middleware callback" : n.getUrlType());
            m.put("notiUrl", n.getNotiUrl());
            m.put("useYn", n.getUseYn());
            filtered.add(m);
        }
        int start = (page - 1) * size;
        int end = Math.min(start + size, filtered.size());
        List<Map<String, Object>> list = start < filtered.size() ? filtered.subList(start, end) : new ArrayList<>();
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setList(list);
        pr.setPage(page);
        pr.setSize(size);
        pr.setTotalElements(filtered.size());
        pr.setTotalPages((filtered.size() + size - 1) / size);
        return pr;
    }
}
