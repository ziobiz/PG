package com.pg.service;

import com.pg.entity.PayCardFailCooldown;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.PayCardFailCooldownRepository;
import com.pg.util.NotifyToTxnStatusMerge;
import com.pg.util.PayCardBrandDetector;
import com.pg.util.PayCardFailOutcomeRules;
import com.pg.util.PayCardMaskKeyUtil;
import com.pg.util.PayCardPanHashUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class PayCardFailCooldownService {

    public static final String ERROR_CODE = "CARD_COOLDOWN";

    private final HqRiskCardPolicyService hqRiskCardPolicyService;
    private final PayCardFailCooldownRepository cooldownRepository;
    private final PayCardPolicyService payCardPolicyService;

    public PayCardFailCooldownService(HqRiskCardPolicyService hqRiskCardPolicyService,
                                      PayCardFailCooldownRepository cooldownRepository,
                                      @Lazy PayCardPolicyService payCardPolicyService) {
        this.hqRiskCardPolicyService = hqRiskCardPolicyService;
        this.cooldownRepository = cooldownRepository;
        this.payCardPolicyService = payCardPolicyService;
    }

    public Optional<Map<String, Object>> checkBlocked(String pgVendorRaw, String panRaw, String lang, Long orgUnitId) {
        CardRiskPolicyEffective policy = hqRiskCardPolicyService.resolveForOrgUnit(orgUnitId);
        if (!policy.enabled()) {
            return Optional.empty();
        }
        String pan = PayCardBrandDetector.normalizePan(panRaw);
        if (pan.length() < 10) {
            return Optional.empty();
        }
        String pg = payCardPolicyService.normalizePgVendor(pgVendorRaw);
        String hash = PayCardPanHashUtil.hashPan(pan);
        if (hash.isEmpty()) {
            return Optional.empty();
        }
        String langNorm = lang != null ? lang.trim() : "KO";

        if (payCardPolicyService.findBlacklistHit(pan, pg).isPresent()) {
            return Optional.of(inactiveCardBlock(langNorm));
        }

        Optional<PayCardFailCooldown> rowOpt = findRow(pg, hash, orgUnitId);
        if (rowOpt.isEmpty()) {
            return Optional.empty();
        }
        PayCardFailCooldown row = rowOpt.get();
        int failCount = row.getFailCount();
        // 자동등록 트리거: N차 결제 시도 시점(이전 실패 N-1건 후 재시도)에 발동 — N번째 실패 후가 아님
        if (shouldBlockOnAttemptTrigger(failCount, policy.autoBlacklistTriggerTier())) {
            registerAutoBlacklistOnAttempt(pg, pan, row, policy, orgUnitId);
            return Optional.of(inactiveCardBlock(langNorm));
        }

        LocalDateTime until = row.getBlockedUntil();
        if (until == null) {
            return Optional.empty();
        }
        LocalDateTime now = LocalDateTime.now();
        if (!now.isBefore(until)) {
            return Optional.empty();
        }
        long remainMin = Math.max(1, ChronoUnit.MINUTES.between(now, until)
                + (ChronoUnit.SECONDS.between(now, until) % 60 > 0 ? 1 : 0));
        String messageKey = PayCardPolicyI18n.tierCooldownMessageKey(failCount);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("valid", false);
        m.put("errorCode", messageKey);
        m.put("messageKey", messageKey);
        m.put("message", PayCardPolicyI18n.format(langNorm, messageKey, remainMin));
        m.put("messages", PayCardPolicyI18n.allLang(messageKey, remainMin));
        m.put("blockedUntil", until.toString());
        m.put("remainingMinutes", remainMin);
        m.put("cooldownTier", Math.min(Math.max(failCount, 1), 4));
        return Optional.of(m);
    }

    private static Map<String, Object> inactiveCardBlock(String lang) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("valid", false);
        m.put("errorCode", "INACTIVE_CARD");
        m.put("messageKey", "INACTIVE_CARD");
        m.put("message", PayCardPolicyI18n.format(lang, "INACTIVE_CARD"));
        m.put("messages", PayCardPolicyI18n.allLang("INACTIVE_CARD"));
        return m;
    }

    @Transactional
    public void recordQualifyingFailure(String pgVendorRaw, String panRaw, String outcomeCode, String outcomeMsg,
                                        Long orgUnitId) {
        CardRiskPolicyEffective policy = hqRiskCardPolicyService.resolveForOrgUnit(orgUnitId);
        if (!policy.enabled() || !PayCardFailOutcomeRules.shouldCountQualifyingFailure(outcomeCode, outcomeMsg)) {
            return;
        }
        String pan = PayCardBrandDetector.normalizePan(panRaw);
        if (pan.length() < 10) {
            return;
        }
        String pg = payCardPolicyService.normalizePgVendor(pgVendorRaw);
        String hash = PayCardPanHashUtil.hashPan(pan);
        if (hash.isEmpty()) {
            return;
        }
        applyFailure(pg, hash, PayCardMaskKeyUtil.maskKeyFromPan(pan), orgUnitId, policy, outcomeCode);
    }

    @Transactional
    public void clearOnSuccess(String pgVendorRaw, String panRaw, Long orgUnitId) {
        String pan = PayCardBrandDetector.normalizePan(panRaw);
        if (pan.length() < 10) {
            return;
        }
        String pg = payCardPolicyService.normalizePgVendor(pgVendorRaw);
        String hash = PayCardPanHashUtil.hashPan(pan);
        findRow(pg, hash, orgUnitId).ifPresent(row -> {
            row.setFailCount(0);
            row.setBlockedUntil(null);
            cooldownRepository.save(row);
        });
    }

    @Transactional
    public void clearOnSuccessByHash(String pgVendorRaw, String cardPanHash, Long orgUnitId) {
        if (cardPanHash == null || cardPanHash.isBlank()) {
            return;
        }
        String pg = payCardPolicyService.normalizePgVendor(pgVendorRaw);
        findRow(pg, cardPanHash.trim(), orgUnitId).ifPresent(row -> {
            row.setFailCount(0);
            row.setBlockedUntil(null);
            cooldownRepository.save(row);
        });
    }

    @Transactional
    public void recordFromTxnHash(String pgVendorRaw, String cardPanHash, String panMaskKey,
                                  String outcomeCode, String outcomeMsg, Long orgUnitId) {
        if (cardPanHash == null || cardPanHash.isBlank()) {
            return;
        }
        CardRiskPolicyEffective policy = hqRiskCardPolicyService.resolveForOrgUnit(orgUnitId);
        if (!policy.enabled() || !PayCardFailOutcomeRules.shouldCountQualifyingFailure(outcomeCode, outcomeMsg)) {
            return;
        }
        String pg = payCardPolicyService.normalizePgVendor(pgVendorRaw);
        applyFailure(pg, cardPanHash.trim(), panMaskKey, orgUnitId, policy, outcomeCode);
    }

    private void applyFailure(String pg, String hash, String panMaskKey, Long orgUnitId,
                              CardRiskPolicyEffective policy, String outcomeCode) {
        PayCardFailCooldown row = findRow(pg, hash, orgUnitId).orElseGet(PayCardFailCooldown::new);
        row.setPgVendor(pg);
        row.setPanHash(hash);
        row.setOrgUnitId(orgUnitId);
        if (panMaskKey != null && !panMaskKey.isBlank()) {
            row.setPanMaskKey(panMaskKey.trim());
        }
        int nextCount = row.getFailCount() + 1;
        row.setFailCount(nextCount);
        row.setLastFailAt(LocalDateTime.now());
        row.setLastOutcomeCode(outcomeCode != null ? outcomeCode.trim() : null);

        if (nextCount >= policy.autoBlacklistTriggerTier()) {
            registerAutoBlacklistOnAttempt(pg, null, row, policy, orgUnitId);
        }

        int minutes = policy.tierMinutes(Math.min(nextCount, 4));
        if (minutes <= 0 && nextCount < policy.autoBlacklistTriggerTier()) {
            minutes = 1;
        }
        if (minutes > 0) {
            row.setBlockedUntil(LocalDateTime.now().plusMinutes(minutes));
        } else if (nextCount >= policy.autoBlacklistTriggerTier()) {
            row.setBlockedUntil(LocalDateTime.now().plusYears(10));
        }
        cooldownRepository.save(row);
    }

    /** 다음 결제 시도 회차(이전 실패 건수 + 1)가 자동등록 트리거 회차 이상이면 해당 시도에서 차단 */
    static boolean shouldBlockOnAttemptTrigger(int failCount, int autoBlacklistTriggerTier) {
        if (autoBlacklistTriggerTier < 1) {
            return false;
        }
        return failCount + 1 >= autoBlacklistTriggerTier;
    }

    private void registerAutoBlacklistOnAttempt(String pg, String pan, PayCardFailCooldown row,
                                              CardRiskPolicyEffective policy, Long orgUnitId) {
        String mk = row.getPanMaskKey();
        if ((mk == null || mk.isBlank()) && pan != null && !pan.isBlank()) {
            mk = PayCardMaskKeyUtil.maskKeyFromPan(PayCardBrandDetector.normalizePan(pan));
            if (mk != null && !mk.isBlank()) {
                row.setPanMaskKey(mk.trim());
            }
        }
        if (mk != null && !mk.isBlank()) {
            payCardPolicyService.addBlacklistAutoMask(pg, mk,
                    "AUTO_FAIL_COOLDOWN(" + policy.autoBlacklistTriggerTier() + "차 ATTEMPT)", orgUnitId);
        }
        row.setBlockedUntil(LocalDateTime.now().plusYears(10));
        cooldownRepository.save(row);
    }

    private Optional<PayCardFailCooldown> findRow(String pg, String hash, Long orgUnitId) {
        if (orgUnitId != null) {
            return cooldownRepository.findByPgVendorAndPanHashAndOrgUnitId(pg, hash, orgUnitId);
        }
        return cooldownRepository.findByPgVendorAndPanHashAndOrgUnitIdIsNull(pg, hash);
    }
}
