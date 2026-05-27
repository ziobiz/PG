package com.pg.service;

import com.pg.entity.MerchantCreditToken;
import com.pg.repository.MerchantCreditTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class MerchantCreditTokenService {

    private static final DateTimeFormatter REQ_EXPIRE_FMT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

    private final MerchantCreditTokenRepository repository;
    private final ChillPayService chillPayService;

    public MerchantCreditTokenService(MerchantCreditTokenRepository repository,
                                      ChillPayService chillPayService) {
        this.repository = repository;
        this.chillPayService = chillPayService;
    }

    @Transactional
    public void upsertToken(Long orgUnitId, String pgCd, String customerId, String creditToken,
                            String cardMask, String cardBrand) {
        if (orgUnitId == null || pgCd == null || pgCd.isBlank()
                || customerId == null || customerId.isBlank()
                || creditToken == null || creditToken.isBlank()) {
            return;
        }
        String pg = pgCd.trim();
        String cust = customerId.trim();
        String tok = creditToken.trim();
        Optional<MerchantCreditToken> existing =
                repository.findFirstByOrgUnitIdAndPgCdAndCustomerIdAndCreditToken(orgUnitId, pg, cust, tok);
        MerchantCreditToken row = existing.orElseGet(MerchantCreditToken::new);
        if (row.getId() == null) {
            row.setOrgUnitId(orgUnitId);
            row.setPgCd(pg);
            row.setCustomerId(cust);
            row.setCreditToken(tok);
        }
        row.setActiveYn("Y");
        if (cardMask != null && !cardMask.isBlank()) {
            row.setCardMask(cardMask.trim());
        }
        if (cardBrand != null && !cardBrand.isBlank()) {
            row.setCardBrand(cardBrand.trim());
        }
        row.setUpdatedAt(LocalDateTime.now());
        repository.save(row);
    }

    @Transactional
    public void markUsed(Long orgUnitId, String pgCd, String customerId, String creditToken) {
        if (orgUnitId == null || pgCd == null || creditToken == null || customerId == null) {
            return;
        }
        repository.findFirstByOrgUnitIdAndPgCdAndCustomerIdAndCreditToken(
                        orgUnitId, pgCd.trim(), customerId.trim(), creditToken.trim())
                .ifPresent(row -> {
                    row.setLastUsedAt(LocalDateTime.now());
                    repository.save(row);
                });
    }

    public List<Map<String, Object>> listForCardSelect(Long orgUnitId, String pgCd, String customerId) {
        if (orgUnitId == null || pgCd == null || pgCd.isBlank()
                || customerId == null || customerId.isBlank()) {
            return List.of();
        }
        List<MerchantCreditToken> rows = repository.findByOrgUnitIdAndPgCdAndCustomerIdAndActiveYnOrderByUpdatedAtDesc(
                orgUnitId, pgCd.trim(), customerId.trim(), "Y");
        String expire = requestExpireDateUtcPlusMinutes(30);
        List<Map<String, Object>> out = new ArrayList<>();
        for (MerchantCreditToken row : rows) {
            if (row.getCreditToken() == null || row.getCreditToken().isBlank()) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("creditToken", row.getCreditToken().trim());
            m.put("cardMask", row.getCardMask() != null ? row.getCardMask().trim() : "");
            m.put("cardBrand", row.getCardBrand() != null ? row.getCardBrand().trim() : "");
            m.put("requestExpireDate", expire);
            m.put("merchantSecurityCheck", chillPayService.computeMerchantSecurityCheck(
                    orgUnitId, ChillPayService.UrlPayBindingScope.REPAY, expire, row.getCreditToken().trim()));
            out.add(m);
        }
        return out;
    }

    public static String requestExpireDateUtcPlusMinutes(int minutes) {
        long epoch = System.currentTimeMillis() + Math.max(1, minutes) * 60_000L;
        return REQ_EXPIRE_FMT.format(java.time.Instant.ofEpochMilli(epoch));
    }

    public static String normalizeCustomerId(String email, String orderNo, String explicit) {
        if (explicit != null && !explicit.isBlank()) {
            return explicit.trim();
        }
        if (email != null && !email.isBlank()) {
            return email.trim();
        }
        if (orderNo != null && !orderNo.isBlank()) {
            return orderNo.trim();
        }
        return "guest";
    }

    public static String pickNotifyField(Map<String, String> fields, String... keys) {
        if (fields == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key == null) {
                continue;
            }
            for (Map.Entry<String, String> e : fields.entrySet()) {
                if (e.getKey() != null && key.equalsIgnoreCase(e.getKey().trim())) {
                    String v = e.getValue();
                    if (v != null && !v.isBlank()) {
                        return v.trim();
                    }
                }
            }
        }
        return null;
    }
}
