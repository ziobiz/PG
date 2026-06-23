package com.pg.service;

import com.pg.api.dto.PageResult;
import com.pg.entity.MerchantNotifyOutboundLog;
import com.pg.entity.OrgUnit;
import com.pg.repository.MerchantNotifyOutboundLogRepository;
import com.pg.repository.OrgUnitRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class MerchantNotifyOutboundLogService {

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAIL = "FAIL";
    private static final int PAYLOAD_PREVIEW_MAX = 500;

    private final MerchantNotifyOutboundLogRepository repository;
    private final OrgUnitRepository orgUnitRepository;

    public MerchantNotifyOutboundLogService(MerchantNotifyOutboundLogRepository repository,
                                            OrgUnitRepository orgUnitRepository) {
        this.repository = repository;
        this.orgUnitRepository = orgUnitRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void append(String compId, Long orgUnitId, String trnId, String orderNo,
                       String urlType, String targetUrl, String notifyChannel,
                       String payloadBody,
                       boolean success, int attempts, Integer httpStatus, String errorMessage) {
        MerchantNotifyOutboundLog row = new MerchantNotifyOutboundLog();
        row.setCompId(compId != null ? compId.trim() : "");
        row.setOrgUnitId(orgUnitId);
        row.setTrnId(trimTo(trnId, 32));
        row.setOrderNo(trimTo(orderNo, 64));
        row.setUrlType(urlType != null ? urlType.trim() : "");
        row.setTargetUrl(trimTo(targetUrl, 1000));
        row.setNotifyChannel(trimTo(notifyChannel, 32));
        row.setPayloadBody(payloadBody);
        row.setResultStatus(success ? STATUS_SUCCESS : STATUS_FAIL);
        row.setHttpStatus(httpStatus);
        int retry = attempts > 0 ? Math.max(0, attempts - 1) : 0;
        row.setRetryCnt(retry);
        row.setErrorMessage(errorMessage != null ? trimTo(errorMessage, 4000) : null);
        repository.save(row);
    }

    @Transactional(readOnly = true)
    public PageResult<Map<String, Object>> search(int page1Based, int size,
                                                  LocalDate fromDate, LocalDate toDate,
                                                  String searchCompNm) {
        int pageIdx = Math.max(page1Based, 1) - 1;
        int sz = size <= 0 ? 20 : Math.min(size, 200);
        List<String> compIds = resolveCompIdsByName(searchCompNm);
        if (compIds != null && compIds.isEmpty()) {
            PageResult<Map<String, Object>> empty = PageResult.empty(page1Based, sz);
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("sendTotal", 0L);
            meta.put("sendSuccess", 0L);
            meta.put("sendFail", 0L);
            empty.setMeta(meta);
            return empty;
        }
        Specification<MerchantNotifyOutboundLog> spec = buildSpec(fromDate, toDate, compIds);
        Page<MerchantNotifyOutboundLog> page = repository.findAll(spec,
                PageRequest.of(pageIdx, sz, Sort.by(Sort.Direction.DESC, "sentAt", "id")));
        Map<String, String> compNmCache = new HashMap<>();
        PageResult<Map<String, Object>> result = PageResult.of(page, row -> toRow(row, compNmCache));
        Map<String, Object> meta = new LinkedHashMap<>();
        long total = repository.count(spec);
        long success = repository.count(buildSpec(fromDate, toDate, compIds)
                .and((root, query, cb) -> cb.equal(cb.upper(root.get("resultStatus")), STATUS_SUCCESS)));
        meta.put("sendTotal", total);
        meta.put("sendSuccess", success);
        meta.put("sendFail", Math.max(0L, total - success));
        result.setMeta(meta);
        return result;
    }

    private Specification<MerchantNotifyOutboundLog> buildSpec(LocalDate fromDate, LocalDate toDate,
                                                                 List<String> compIds) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (fromDate != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("sentAt"), LocalDateTime.of(fromDate, LocalTime.MIN)));
            }
            if (toDate != null) {
                ps.add(cb.lessThanOrEqualTo(root.get("sentAt"), LocalDateTime.of(toDate, LocalTime.MAX)));
            }
            if (compIds != null) {
                ps.add(root.get("compId").in(compIds));
            }
            if (ps.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(ps.toArray(Predicate[]::new));
        };
    }

    private List<String> resolveCompIdsByName(String searchCompNm) {
        if (searchCompNm == null || searchCompNm.isBlank()) {
            return null;
        }
        String q = searchCompNm.trim().toLowerCase(Locale.ROOT);
        List<String> ids = new ArrayList<>();
        for (OrgUnit ou : orgUnitRepository.findAll()) {
            if (ou.getCode() == null || ou.getCode().isBlank()) {
                continue;
            }
            String nm = ou.getName() != null ? ou.getName().trim().toLowerCase(Locale.ROOT) : "";
            if (nm.contains(q)) {
                ids.add(ou.getCode().trim());
            }
        }
        return ids;
    }

    private Map<String, Object> toRow(MerchantNotifyOutboundLog e, Map<String, String> compNmCache) {
        Map<String, Object> m = new LinkedHashMap<>();
        String cid = e.getCompId() != null ? e.getCompId().trim() : "";
        m.put("compId", cid);
        m.put("compNm", resolveCompNm(cid, compNmCache));
        m.put("sendDt", e.getSentAt() != null ? e.getSentAt().toString().replace('T', ' ').substring(0, 19) : "");
        m.put("result", e.getResultStatus());
        m.put("retryCnt", e.getRetryCnt());
        m.put("urlType", displayUrlType(e.getUrlType()));
        m.put("targetUrl", e.getTargetUrl() != null ? e.getTargetUrl() : "");
        m.put("webhookPayload", e.getPayloadBody() != null ? e.getPayloadBody() : "");
        m.put("webhookPayloadPreview", previewPayload(e.getPayloadBody()));
        m.put("trnId", e.getTrnId());
        m.put("orderNo", e.getOrderNo());
        m.put("httpStatus", e.getHttpStatus());
        m.put("errorMessage", e.getErrorMessage());
        return m;
    }

    private static String displayUrlType(String urlType) {
        if (urlType == null || urlType.isBlank()) {
            return "";
        }
        return switch (urlType.trim().toUpperCase(Locale.ROOT)) {
            case "BACKGROUND" -> "URL Background";
            case "RESULT" -> "URL Result";
            case "MIDDLEWARE" -> "PG Middleware";
            default -> urlType.trim();
        };
    }

    private static String previewPayload(String body) {
        if (body == null || body.isEmpty()) {
            return "";
        }
        String t = body.trim();
        if (t.length() <= PAYLOAD_PREVIEW_MAX) {
            return t;
        }
        return t.substring(0, PAYLOAD_PREVIEW_MAX) + "…";
    }

    private String resolveCompNm(String compId, Map<String, String> cache) {
        if (compId == null || compId.isBlank()) {
            return "";
        }
        if (cache.containsKey(compId)) {
            return cache.get(compId);
        }
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(compId);
        if (ou.isEmpty()) {
            ou = orgUnitRepository.findByCodeIgnoreCase(compId);
        }
        String nm = ou.map(OrgUnit::getName).orElse("");
        cache.put(compId, nm != null ? nm : "");
        return nm != null ? nm : "";
    }

    private static String trimTo(String s, int max) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
