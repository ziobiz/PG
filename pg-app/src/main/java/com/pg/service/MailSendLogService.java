package com.pg.service;

import com.pg.api.dto.PageResult;
import com.pg.entity.MailSendLog;
import com.pg.repository.MailSendLogRepository;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MailSendLogService {

    public static final String KIND_VOID_TEST = "VOID_TEST";
    public static final String KIND_VOID_TXN = "VOID_TXN";
    public static final String KIND_SPLIT_PAY_D_MINUS1 = "SPLIT_PAY_D_MINUS1";
    public static final String KIND_SPLIT_PAY_D0 = "SPLIT_PAY_D0";
    public static final String KIND_SPLIT_PAY_D1 = "SPLIT_PAY_D1";
    public static final String KIND_SPLIT_PAY_D2 = "SPLIT_PAY_D2";
    public static final String KIND_SPLIT_PAY_D3 = "SPLIT_PAY_D3";
    public static final String KIND_SPLIT_PAY_CREATE = "SPLIT_PAY_CREATE";
    public static final String KIND_SPLIT_PAY_TEST = "SPLIT_PAY_TEST";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAIL = "FAIL";

    private static final int PREVIEW_MAX = 800;

    private final MailSendLogRepository mailSendLogRepository;

    public MailSendLogService(MailSendLogRepository mailSendLogRepository) {
        this.mailSendLogRepository = mailSendLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void append(String mailKind, String status, String toAddress, String subject, String body,
                       String errorMessage, String pgTrnId, String actorUsername) {
        MailSendLog row = new MailSendLog();
        row.setMailKind(mailKind != null ? mailKind.trim() : "");
        row.setStatus(status != null ? status.trim() : STATUS_FAIL);
        row.setToAddress(toAddress != null ? trimTo(toAddress, 500) : "");
        row.setSubject(subject != null ? trimTo(subject, 500) : "");
        row.setBodyPreview(preview(body));
        row.setErrorMessage(errorMessage != null ? trimTo(errorMessage, 4000) : null);
        row.setPgTrnId(pgTrnId != null ? trimTo(pgTrnId, 32) : null);
        row.setActorUsername(actorUsername != null ? trimTo(actorUsername, 128) : null);
        mailSendLogRepository.save(row);
    }

    @Transactional(readOnly = true)
    public PageResult<Map<String, Object>> search(int page1Based, int size,
                                                  String mailKind, String status,
                                                  LocalDate fromDate, LocalDate toDate) {
        int pageIdx = Math.max(page1Based, 1) - 1;
        int sz = size <= 0 ? 20 : Math.min(size, 200);
        Specification<MailSendLog> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (mailKind != null && !mailKind.isBlank()) {
                ps.add(cb.equal(cb.upper(root.get("mailKind")), mailKind.trim().toUpperCase(Locale.ROOT)));
            }
            if (status != null && !status.isBlank()) {
                ps.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase(Locale.ROOT)));
            }
            if (fromDate != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), LocalDateTime.of(fromDate, LocalTime.MIN)));
            }
            if (toDate != null) {
                ps.add(cb.lessThanOrEqualTo(root.get("createdAt"), LocalDateTime.of(toDate, LocalTime.MAX)));
            }
            if (ps.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(ps.toArray(Predicate[]::new));
        };
        Page<MailSendLog> p = mailSendLogRepository.findAll(spec,
                PageRequest.of(pageIdx, sz, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResult.of(p, this::toRow);
    }

    private Map<String, Object> toRow(MailSendLog e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("mailKind", e.getMailKind());
        m.put("status", e.getStatus());
        m.put("toAddress", e.getToAddress());
        m.put("subject", e.getSubject());
        m.put("bodyPreview", e.getBodyPreview());
        m.put("errorMessage", e.getErrorMessage());
        m.put("pgTrnId", e.getPgTrnId());
        m.put("actorUsername", e.getActorUsername());
        m.put("createdAt", e.getCreatedAt() != null ? e.getCreatedAt().toString().replace('T', ' ') : "");
        return m;
    }

    private static String preview(String body) {
        if (body == null || body.isEmpty()) {
            return null;
        }
        String t = body.replace("\r\n", "\n").trim();
        if (t.length() <= PREVIEW_MAX) {
            return t;
        }
        return t.substring(0, PREVIEW_MAX) + "…";
    }

    private static String trimTo(String s, int max) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
