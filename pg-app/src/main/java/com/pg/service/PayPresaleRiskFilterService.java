package com.pg.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.HqRiskCardPolicy;
import com.pg.entity.PayRiskFilterEvent;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.PayRiskFilterEventRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.util.PayCardBrandDetector;
import com.pg.util.PayCardPanHashUtil;
import com.pg.util.PayPresaleRiskFilterCodes;
import com.pg.util.PayPresaleRiskFilterI18n;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * JPAY·ChillPay 송부 전 사전 리스크 필터.
 */
@Service
public class PayPresaleRiskFilterService {

    private final HqRiskCardPolicyService hqRiskCardPolicyService;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final PayRiskFilterEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public PayPresaleRiskFilterService(HqRiskCardPolicyService hqRiskCardPolicyService,
                                       PgTrnsctnRepository pgTrnsctnRepository,
                                       PayRiskFilterEventRepository eventRepository,
                                       ObjectMapper objectMapper) {
        this.hqRiskCardPolicyService = hqRiskCardPolicyService;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    public Optional<PresaleRiskBlock> evaluate(Long orgUnitId,
                                               String merchantId,
                                               String pgVendor,
                                               Map<String, Object> body) {
        HqRiskCardPolicy policy = hqRiskCardPolicyService.getOrCreate();
        if (!"Y".equalsIgnoreCase(trim(policy.getPresaleFilterEnabledYn()))) {
            return Optional.empty();
        }
        String lang = resolveLang(body);
        String email = str(body.get("payEmailAddress"));
        if (email.isEmpty()) {
            email = str(body.get("custEmail"));
        }
        String phone = str(body.get("payTelephone"));
        if (phone.isEmpty()) {
            phone = str(body.get("phoneNumber"));
        }
        String first = str(body.get("payFirstname"));
        if (first.isEmpty()) {
            first = str(body.get("firstName"));
        }
        String last = str(body.get("payLastname"));
        if (last.isEmpty()) {
            last = str(body.get("lastName"));
        }
        String holderName = joinName(first, last);
        String pan = str(body.get("payCardno"));
        String cardHash = pan.length() >= 10 ? PayCardPanHashUtil.hashPan(PayCardBrandDetector.normalizePan(pan)) : "";
        String clientIp = str(body.get("_payerClientIp"));

        if ("Y".equalsIgnoreCase(trim(policy.getFilterEmailInvalidYn())) && !email.isEmpty()
                && PayPresaleRiskFilterCodes.isInvalidEmail(email)) {
            return Optional.of(block(PayPresaleRiskFilterCodes.EMAIL_INVALID, lang, Map.of()));
        }
        if ("Y".equalsIgnoreCase(trim(policy.getFilterPhoneInvalidYn())) && !phone.isEmpty()
                && PayPresaleRiskFilterCodes.isInvalidPhone(phone)) {
            return Optional.of(block(PayPresaleRiskFilterCodes.PHONE_INVALID, lang, Map.of()));
        }

        if ("Y".equalsIgnoreCase(trim(policy.getFilterHolderNameYn()))
                && PayPresaleRiskFilterCodes.isSuspiciousHolderName(holderName)) {
            return Optional.of(block(PayPresaleRiskFilterCodes.HOLDER_NAME_SUSPICIOUS, lang, Map.of()));
        }

        if ("Y".equalsIgnoreCase(trim(policy.getFilterVelocityCardYn())) && !cardHash.isEmpty()) {
            int windowMin = intOr(policy.getVelocityCardWindowMinutes(),
                    intOr(policy.getVelocityWindowMinutes(), 10));
            int maxAttempts = intOr(policy.getVelocityCardMaxAttempts(),
                    intOr(policy.getVelocityMaxAttempts(), 3));
            LocalDateTime since = LocalDateTime.now().minusMinutes(Math.max(1, windowMin));
            long cnt = pgTrnsctnRepository.countRecentByCardPanHash(cardHash, merchantId, since);
            if (cnt >= maxAttempts) {
                return Optional.of(block(PayPresaleRiskFilterCodes.VELOCITY_CARD, lang, Map.of()));
            }
        }
        if ("Y".equalsIgnoreCase(trim(policy.getFilterVelocityEmailYn())) && !email.isEmpty()) {
            int windowMin = intOr(policy.getVelocityEmailWindowMinutes(), 30);
            int maxAttempts = intOr(policy.getVelocityEmailMaxAttempts(), 5);
            LocalDateTime since = LocalDateTime.now().minusMinutes(Math.max(1, windowMin));
            long cnt = pgTrnsctnRepository.countRecentByCustomerEmail(
                    merchantId, PayPresaleRiskFilterCodes.normalizeEmail(email), since);
            if (cnt >= maxAttempts) {
                return Optional.of(block(PayPresaleRiskFilterCodes.VELOCITY_EMAIL, lang, Map.of()));
            }
        }
        if ("Y".equalsIgnoreCase(trim(policy.getFilterVelocityIpYn())) && !clientIp.isEmpty()) {
            int windowMin = intOr(policy.getVelocityIpWindowMinutes(), 15);
            int maxAttempts = intOr(policy.getVelocityIpMaxAttempts(), 10);
            LocalDateTime since = LocalDateTime.now().minusMinutes(Math.max(1, windowMin));
            long cnt = pgTrnsctnRepository.countRecentByPayerIp(merchantId, clientIp, since);
            if (cnt >= maxAttempts) {
                return Optional.of(block(PayPresaleRiskFilterCodes.VELOCITY_IP, lang, Map.of()));
            }
        }

        if ("Y".equalsIgnoreCase(trim(policy.getFilterBuyerContactMismatchYn())) && !cardHash.isEmpty()) {
            Optional<PresaleRiskBlock> mismatch = checkBuyerMismatch(merchantId, lang, email, phone, holderName, cardHash);
            if (mismatch.isPresent()) {
                return mismatch;
            }
        }
        return Optional.empty();
    }

    private Optional<PresaleRiskBlock> checkBuyerMismatch(String merchantId,
                                                          String lang,
                                                          String email,
                                                          String phone,
                                                          String holderName,
                                                          String cardHash) {
        List<PgTrnsctn> recent = pgTrnsctnRepository.findRecentBuyerContextByCardPanHash(
                cardHash, merchantId, PageRequest.of(0, 1));
        if (recent.isEmpty()) {
            return Optional.empty();
        }
        PgTrnsctn prev = recent.get(0);
        String prevEmail = prev.getCustomerId() != null ? prev.getCustomerId() : "";
        String prevPhone = prev.getCustomerTel() != null ? prev.getCustomerTel() : "";
        String prevName = prev.getCustomerNm() != null ? prev.getCustomerNm() : "";

        if (!email.isEmpty() && !prevEmail.isEmpty()
                && !PayPresaleRiskFilterCodes.normalizeEmail(email)
                .equals(PayPresaleRiskFilterCodes.normalizeEmail(prevEmail))) {
            Map<String, String> params = Map.of("prevEmail", PayPresaleRiskFilterCodes.maskEmail(prevEmail));
            return Optional.of(block(PayPresaleRiskFilterCodes.BUYER_EMAIL_MISMATCH, lang, params));
        }
        if (!phone.isEmpty() && !prevPhone.isEmpty()
                && !PayPresaleRiskFilterCodes.normalizePhone(phone)
                .equals(PayPresaleRiskFilterCodes.normalizePhone(prevPhone))) {
            Map<String, String> params = Map.of("prevPhone", PayPresaleRiskFilterCodes.maskPhone(prevPhone));
            return Optional.of(block(PayPresaleRiskFilterCodes.BUYER_PHONE_MISMATCH, lang, params));
        }
        if (!holderName.isEmpty() && !prevName.isEmpty()
                && !PayPresaleRiskFilterCodes.namesEquivalent(holderName, prevName)) {
            Map<String, String> params = Map.of("prevName", maskName(prevName));
            return Optional.of(block(PayPresaleRiskFilterCodes.BUYER_NAME_MISMATCH, lang, params));
        }
        return Optional.empty();
    }

    @Transactional
    public void recordEvent(Long orgUnitId,
                            String merchantId,
                            String orderNo,
                            String trnId,
                            String pgVendor,
                            PresaleRiskBlock block) {
        if (block == null) {
            return;
        }
        PayRiskFilterEvent row = new PayRiskFilterEvent();
        row.setOrgUnitId(orgUnitId);
        row.setMerchantId(merchantId);
        row.setOrderNo(orderNo);
        row.setTrnId(trnId);
        row.setPgVendor(pgVendor);
        row.setFilterCode(block.filterCode());
        row.setFilterDesc(PayPresaleRiskFilterI18n.filterLabelKo(block.filterCode()));
        try {
            row.setDetailJson(objectMapper.writeValueAsString(block.detail()));
        } catch (Exception ignored) {
            row.setDetailJson("{}");
        }
        eventRepository.save(row);
    }

    private PresaleRiskBlock block(String code, String lang, Map<String, String> params) {
        Map<String, String> p = params != null ? params : Map.of();
        String msg = PayPresaleRiskFilterI18n.message(lang, code, p);
        Map<String, String> messages = PayPresaleRiskFilterI18n.allLang(code, p);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("filterCode", code);
        detail.putAll(p);
        return new PresaleRiskBlock(code, msg, messages, detail);
    }

    private static String maskName(String name) {
        String n = PayPresaleRiskFilterCodes.normalizeName(name);
        if (n.length() <= 2) {
            return n + "**";
        }
        return n.substring(0, 2) + "**";
    }

    private static String resolveLang(Map<String, Object> body) {
        String l = str(body.get("payLanguage"));
        if (l.isEmpty()) {
            l = str(body.get("langCode"));
        }
        return l.isEmpty() ? "KOR" : l;
    }

    private static String joinName(String first, String last) {
        if (first.isEmpty()) {
            return last;
        }
        if (last.isEmpty()) {
            return first;
        }
        return first + " " + last;
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static int intOr(Integer v, int def) {
        return v != null && v > 0 ? v : def;
    }

    public record PresaleRiskBlock(String filterCode,
                                   String message,
                                   Map<String, String> messages,
                                   Map<String, Object> detail) {
    }
}
