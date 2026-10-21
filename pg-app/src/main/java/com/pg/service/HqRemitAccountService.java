package com.pg.service;

import com.pg.entity.HqRemitAccount;
import com.pg.repository.HqRemitAccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 본사 송금 수취계좌. ElementPay에 이미 있는 개인 계좌를 기본 계좌로 가져오고,
 * 새 계좌는 계좌관리에서만 등록합니다.
 */
@Service
public class HqRemitAccountService {

    private final HqRemitAccountRepository repository;
    private final ElementPayClientApiService elementPay;

    public HqRemitAccountService(HqRemitAccountRepository repository, ElementPayClientApiService elementPay) {
        this.repository = repository;
        this.elementPay = elementPay;
    }

    @Transactional
    public Map<String, Object> list(Authentication authentication) {
        String syncNote = "";
        try {
            syncFromElementPay(authentication);
        } catch (RuntimeException ex) {
            syncNote = ex.getMessage() != null ? ex.getMessage() : "ElementPay 계좌를 가져오지 못했습니다.";
        }
        ensureDefault();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (HqRemitAccount a : repository.findByActiveYnOrderByDefaultYnDescIdAsc("Y")) {
            rows.add(toRow(a));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("accounts", rows);
        if (!syncNote.isEmpty()) {
            out.put("syncNote", syncNote);
        }
        return out;
    }

    @Transactional
    public Map<String, Object> create(Authentication authentication, Map<String, String> body) {
        Map<String, Object> created = elementPay.createPersonalRecipient(authentication, body);
        String recipientId = String.valueOf(created.getOrDefault("id", "")).trim();
        if (recipientId.isEmpty()) {
            throw new IllegalStateException("수취계좌를 만들었지만 식별번호가 없습니다.");
        }
        HqRemitAccount row = repository.findByRecipientId(recipientId).orElseGet(HqRemitAccount::new);
        boolean fresh = row.getId() == null;
        applyCard(row, created, "HQ");
        row.setActiveYn("Y");
        boolean wantDefault = body != null && "Y".equalsIgnoreCase(trim(body.get("defaultYn")));
        if (wantDefault || repository.countByDefaultYnAndActiveYn("Y", "Y") == 0) {
            clearDefaults();
            row.setDefaultYn("Y");
        } else if (fresh) {
            row.setDefaultYn("N");
        }
        repository.save(row);
        Map<String, Object> out = toRow(row);
        out.put("message", "수취계좌를 등록했습니다.");
        return out;
    }

    @Transactional
    public Map<String, Object> markDefault(Authentication authentication, long id) {
        elementPay.assertRemittanceOperator(authentication);
        HqRemitAccount row = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("계좌를 찾을 수 없습니다."));
        if (!"Y".equals(row.getActiveYn())) {
            throw new IllegalArgumentException("사용 중지된 계좌는 기본으로 둘 수 없습니다.");
        }
        clearDefaults();
        row.setDefaultYn("Y");
        repository.save(row);
        Map<String, Object> out = toRow(row);
        out.put("message", "기본 수취계좌로 지정했습니다.");
        return out;
    }

    private void syncFromElementPay(Authentication authentication) {
        for (Map<String, Object> card : elementPay.listRecipientCards(authentication)) {
            String recipientId = String.valueOf(card.getOrDefault("id", "")).trim();
            if (recipientId.isEmpty()) {
                continue;
            }
            HqRemitAccount row = repository.findByRecipientId(recipientId).orElseGet(HqRemitAccount::new);
            boolean fresh = row.getId() == null;
            String source = fresh ? "EP" : row.getSourceCd();
            applyCard(row, card, source);
            boolean active = Boolean.TRUE.equals(card.get("active"));
            row.setActiveYn(active ? "Y" : "N");
            if (fresh) {
                row.setDefaultYn("N");
            }
            repository.save(row);
        }
    }

    private void ensureDefault() {
        if (repository.countByDefaultYnAndActiveYn("Y", "Y") > 0) {
            return;
        }
        List<HqRemitAccount> active = repository.findByActiveYnOrderByDefaultYnDescIdAsc("Y");
        if (active.isEmpty()) {
            return;
        }
        HqRemitAccount first = null;
        for (HqRemitAccount a : active) {
            if ("company".equalsIgnoreCase(a.getRecipientType())) {
                first = a;
                break;
            }
        }
        if (first == null) {
            first = active.get(0);
        }
        first.setDefaultYn("Y");
        repository.save(first);
    }

    private void clearDefaults() {
        for (HqRemitAccount a : repository.findByActiveYnOrderByDefaultYnDescIdAsc("Y")) {
            if ("Y".equals(a.getDefaultYn())) {
                a.setDefaultYn("N");
                repository.save(a);
            }
        }
    }

    private static void applyCard(HqRemitAccount row, Map<String, Object> card, String source) {
        row.setRecipientId(String.valueOf(card.getOrDefault("id", "")).trim());
        row.setHolderName(clip(String.valueOf(card.getOrDefault("name", "")), 200));
        row.setAccountLast(clip(String.valueOf(card.getOrDefault("lastDigits", "")), 8));
        row.setCountry(clip(String.valueOf(card.getOrDefault("country", "")), 8));
        row.setRecipientType(clip(String.valueOf(card.getOrDefault("type", "")), 20));
        if (source != null && !source.isBlank()) {
            row.setSourceCd(source);
        }
    }

    private static Map<String, Object> toRow(HqRemitAccount a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("recipientId", a.getRecipientId());
        m.put("holderName", a.getHolderName() != null ? a.getHolderName() : "");
        m.put("accountLast", a.getAccountLast() != null ? a.getAccountLast() : "");
        m.put("country", a.getCountry() != null ? a.getCountry() : "");
        m.put("recipientType", a.getRecipientType() != null ? a.getRecipientType() : "");
        m.put("defaultYn", "Y".equals(a.getDefaultYn()) ? "Y" : "N");
        m.put("sourceCd", a.getSourceCd() != null ? a.getSourceCd() : "EP");
        String last = a.getAccountLast() != null ? a.getAccountLast() : "";
        String name = a.getHolderName() != null ? a.getHolderName() : "";
        String label = name;
        if (!last.isEmpty()) {
            label = label.isEmpty() ? ("****" + last) : (name + " · ****" + last);
        }
        if ("Y".equals(a.getDefaultYn())) {
            label = label + " · 기본";
        }
        m.put("label", label);
        return m;
    }

    private static String clip(String raw, int max) {
        if (raw == null || "null".equals(raw)) {
            return "";
        }
        String s = raw.trim();
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String trim(String raw) {
        return raw == null ? "" : raw.trim();
    }
}
