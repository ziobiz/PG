package com.pg.service;

import com.pg.entity.PayCardFailCooldown;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PayCardFailCooldownRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.util.NotifyToTxnStatusMerge;
import com.pg.util.PayCardBrandDetector;
import com.pg.util.PayCardFailOutcomeRules;
import com.pg.util.PayCardMaskKeyUtil;
import com.pg.util.PayCardPanHashUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
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
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;

    public PayCardFailCooldownService(HqRiskCardPolicyService hqRiskCardPolicyService,
                                      PayCardFailCooldownRepository cooldownRepository,
                                      @Lazy PayCardPolicyService payCardPolicyService,
                                      PgTrnsctnRepository pgTrnsctnRepository,
                                      OrgUnitRepository orgUnitRepository) {
        this.hqRiskCardPolicyService = hqRiskCardPolicyService;
        this.cooldownRepository = cooldownRepository;
        this.payCardPolicyService = payCardPolicyService;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
    }

    public Optional<Map<String, Object>> checkBlocked(String pgVendorRaw, String panRaw, String lang, Long orgUnitId) {
        return checkBlocked(pgVendorRaw, panRaw, lang, orgUnitId, null);
    }

    public Optional<Map<String, Object>> checkBlocked(String pgVendorRaw, String panRaw, String lang, Long orgUnitId,
                                                      String holderName) {
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
        int triggerTier = policy.autoBlacklistTriggerTier();
        // N차 트리거(1·2·3·4 동일): 비성공 N회 완료 직후 등록 → (N+1)번째 시도부터 차단
        if (shouldRegisterAutoBlacklistAfterFailures(failCount, triggerTier)) {
            registerAutoBlacklistOnAttempt(pg, pan, hash, row, policy, orgUnitId, holderName, "THRESHOLD");
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
        recordQualifyingFailure(pgVendorRaw, panRaw, outcomeCode, outcomeMsg, orgUnitId, null);
    }

    @Transactional
    public void recordQualifyingFailure(String pgVendorRaw, String panRaw, String outcomeCode, String outcomeMsg,
                                        Long orgUnitId, String holderName) {
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
        applyFailure(pg, hash, PayCardMaskKeyUtil.maskKeyFromPan(pan), orgUnitId, policy, outcomeCode, holderName);
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
        recordFromTxnHash(pgVendorRaw, cardPanHash, panMaskKey, outcomeCode, outcomeMsg, orgUnitId, null);
    }

    @Transactional
    public void recordFromTxnHash(String pgVendorRaw, String cardPanHash, String panMaskKey,
                                  String outcomeCode, String outcomeMsg, Long orgUnitId, String holderName) {
        if (cardPanHash == null || cardPanHash.isBlank()) {
            return;
        }
        CardRiskPolicyEffective policy = hqRiskCardPolicyService.resolveForOrgUnit(orgUnitId);
        if (!policy.enabled() || !PayCardFailOutcomeRules.shouldCountQualifyingFailure(outcomeCode, outcomeMsg)) {
            return;
        }
        String pg = payCardPolicyService.normalizePgVendor(pgVendorRaw);
        applyFailure(pg, cardPanHash.trim(), panMaskKey, orgUnitId, policy, outcomeCode, holderName);
    }

    private void applyFailure(String pg, String hash, String panMaskKey, Long orgUnitId,
                              CardRiskPolicyEffective policy, String outcomeCode, String holderName) {
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

        if (shouldRegisterAutoBlacklistAfterFailures(nextCount, policy.autoBlacklistTriggerTier())) {
            registerAutoBlacklistOnAttempt(pg, null, hash, row, policy, orgUnitId, holderName, "COMPLETE");
        }

        int minutes = policy.tierMinutes(Math.min(nextCount, 4));
        int triggerTier = policy.autoBlacklistTriggerTier();
        if (minutes <= 0 && !shouldRegisterAutoBlacklistAfterFailures(nextCount, triggerTier)) {
            minutes = 1;
        }
        if (shouldRegisterAutoBlacklistAfterFailures(nextCount, triggerTier)) {
            row.setBlockedUntil(LocalDateTime.now().plusYears(10));
        } else if (minutes > 0) {
            row.setBlockedUntil(LocalDateTime.now().plusMinutes(minutes));
        }
        cooldownRepository.save(row);
    }

    /**
     * 자동등록 트리거 N차(1·2·3·4 동일): 비성공 N회가 누적 완료되면 즉시 비활성 등록.
     * 2차→1·2회 후 즉시 등록·3번째 시도부터 차단, 3차→1·2·3회 후 즉시 등록·4번째 시도부터 차단.
     */
    static boolean shouldRegisterAutoBlacklistAfterFailures(int completedFailureCount,
                                                           int autoBlacklistTriggerTier) {
        if (autoBlacklistTriggerTier < 1) {
            return false;
        }
        return completedFailureCount >= autoBlacklistTriggerTier;
    }

    private void registerAutoBlacklistOnAttempt(String pg, String pan, String panHash, PayCardFailCooldown row,
                                              CardRiskPolicyEffective policy, Long orgUnitId, String holderName,
                                              String reasonKind) {
        String mk = row.getPanMaskKey();
        if ((mk == null || mk.isBlank()) && pan != null && !pan.isBlank()) {
            mk = PayCardMaskKeyUtil.maskKeyFromPan(PayCardBrandDetector.normalizePan(pan));
            if (mk != null && !mk.isBlank()) {
                row.setPanMaskKey(mk.trim());
            }
        }
        String resolvedHolder = resolveHolderName(holderName, panHash != null ? panHash : row.getPanHash(), orgUnitId);
        if (mk != null && !mk.isBlank()) {
            String kind = reasonKind != null && !reasonKind.isBlank() ? reasonKind.trim() : "ATTEMPT";
            payCardPolicyService.addBlacklistAutoMask(pg, mk,
                    "AUTO_FAIL_COOLDOWN(" + policy.autoBlacklistTriggerTier() + "차 " + kind + ")",
                    orgUnitId, resolvedHolder);
        }
        row.setBlockedUntil(LocalDateTime.now().plusYears(10));
        cooldownRepository.save(row);
    }

    private String resolveHolderName(String holderName, String panHash, Long orgUnitId) {
        if (holderName != null && !holderName.isBlank()) {
            return holderName.trim();
        }
        if (panHash == null || panHash.isBlank() || orgUnitId == null) {
            return null;
        }
        return orgUnitRepository.findById(orgUnitId)
                .map(ou -> ou.getCode())
                .filter(code -> code != null && !code.isBlank())
                .flatMap(code -> pgTrnsctnRepository
                        .findRecentWithCustomerNmByCardPanHashAndMerchantId(
                                panHash.trim(), code.trim(), PageRequest.of(0, 1))
                        .stream()
                        .findFirst()
                        .map(PgTrnsctn::getCustomerNm)
                        .filter(nm -> nm != null && !nm.isBlank()))
                .map(String::trim)
                .orElse(null);
    }

    private Optional<PayCardFailCooldown> findRow(String pg, String hash, Long orgUnitId) {
        if (orgUnitId != null) {
            return cooldownRepository.findByPgVendorAndPanHashAndOrgUnitId(pg, hash, orgUnitId);
        }
        return cooldownRepository.findByPgVendorAndPanHashAndOrgUnitIdIsNull(pg, hash);
    }
}
