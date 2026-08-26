package com.pg.service;

import com.pg.entity.PayCardFailCooldown;
import com.pg.entity.PayCardFailRiskEvent;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PayCardFailCooldownRepository;
import com.pg.repository.PayCardFailRiskEventRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.util.CardRiskTrackPeriod;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class PayCardFailCooldownService {

    public static final String ERROR_CODE = "CARD_COOLDOWN";

    private final HqRiskCardPolicyService hqRiskCardPolicyService;
    private final PayCardFailCooldownRepository cooldownRepository;
    private final PayCardFailRiskEventRepository riskEventRepository;
    private final PayCardPolicyService payCardPolicyService;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;

    public PayCardFailCooldownService(HqRiskCardPolicyService hqRiskCardPolicyService,
                                      PayCardFailCooldownRepository cooldownRepository,
                                      PayCardFailRiskEventRepository riskEventRepository,
                                      @Lazy PayCardPolicyService payCardPolicyService,
                                      PgTrnsctnRepository pgTrnsctnRepository,
                                      OrgUnitRepository orgUnitRepository) {
        this.hqRiskCardPolicyService = hqRiskCardPolicyService;
        this.cooldownRepository = cooldownRepository;
        this.riskEventRepository = riskEventRepository;
        this.payCardPolicyService = payCardPolicyService;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
    }

  /** 만료 리스크 이벤트 정리 시 {@link PayCardFailRiskEventRepository} DELETE 가 동작하므로 트랜잭션 필수. */
    @Transactional
    public Optional<Map<String, Object>> checkBlocked(String pgVendorRaw, String panRaw, String lang, Long orgUnitId) {
        return checkBlocked(pgVendorRaw, panRaw, lang, orgUnitId, null);
    }

    @Transactional
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
        int failCount = syncAndGetFailureCount(pg, hash, orgUnitId, row, policy);
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
        m.put("skipPaymentListRecord", true);
        return Optional.of(m);
    }

    private static Map<String, Object> inactiveCardBlock(String lang) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("valid", false);
        m.put("errorCode", "INACTIVE_CARD");
        m.put("messageKey", "INACTIVE_CARD");
        m.put("message", PayCardPolicyI18n.format(lang, "INACTIVE_CARD"));
        m.put("messages", PayCardPolicyI18n.allLang("INACTIVE_CARD"));
        m.put("skipPaymentListRecord", true);
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
            clearRiskEvents(pg, hash, orgUnitId);
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
        String hash = cardPanHash.trim();
        findRow(pg, hash, orgUnitId).ifPresent(row -> {
            clearRiskEvents(pg, hash, orgUnitId);
            row.setFailCount(0);
            row.setBlockedUntil(null);
            cooldownRepository.save(row);
        });
    }

    /** JPAY 동기 승인·노티 등 — PAN 우선, 없으면 거래 cardPanHash 로 RESET */
    @Transactional
    public void clearOnSuccessForTxn(String pgVendorRaw, String panRaw, String cardPanHash, Long orgUnitId) {
        String pan = PayCardBrandDetector.normalizePan(panRaw);
        if (pan.length() >= 10) {
            clearOnSuccess(pgVendorRaw, pan, orgUnitId);
            return;
        }
        clearOnSuccessByHash(pgVendorRaw, cardPanHash, orgUnitId);
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
        LocalDateTime now = LocalDateTime.now();
        PayCardFailCooldown row = findRow(pg, hash, orgUnitId).orElseGet(PayCardFailCooldown::new);
        row.setPgVendor(pg);
        row.setPanHash(hash);
        row.setOrgUnitId(orgUnitId);
        if (panMaskKey != null && !panMaskKey.isBlank()) {
            row.setPanMaskKey(panMaskKey.trim());
        }

        appendRiskEvent(pg, hash, orgUnitId, outcomeCode, now);
        pruneExpiredRiskEvents(pg, hash, orgUnitId, policy, now);
        int failCount = countQualifyingFailures(pg, hash, orgUnitId, policy, now);
        row.setFailCount(failCount);
        row.setLastFailAt(now);
        row.setLastOutcomeCode(outcomeCode != null ? outcomeCode.trim() : null);

        if (shouldRegisterAutoBlacklistAfterFailures(failCount, policy.autoBlacklistTriggerTier())) {
            registerAutoBlacklistOnAttempt(pg, null, hash, row, policy, orgUnitId, holderName, "COMPLETE");
        }

        int minutes = policy.tierMinutes(Math.min(failCount, 4));
        int triggerTier = policy.autoBlacklistTriggerTier();
        if (minutes <= 0 && !shouldRegisterAutoBlacklistAfterFailures(failCount, triggerTier)) {
            minutes = 1;
        }
        if (shouldRegisterAutoBlacklistAfterFailures(failCount, triggerTier)) {
            row.setBlockedUntil(LocalDateTime.now().plusYears(10));
        } else if (minutes > 0) {
            row.setBlockedUntil(LocalDateTime.now().plusMinutes(minutes));
        }
        cooldownRepository.save(row);
    }

    private int syncAndGetFailureCount(String pg, String hash, Long orgUnitId,
                                       PayCardFailCooldown row, CardRiskPolicyEffective policy) {
        LocalDateTime now = LocalDateTime.now();
        pruneExpiredRiskEvents(pg, hash, orgUnitId, policy, now);
        int count = countQualifyingFailures(pg, hash, orgUnitId, policy, now);
        if (row.getFailCount() != count) {
            row.setFailCount(count);
            cooldownRepository.save(row);
        }
        return count;
    }

    private void appendRiskEvent(String pg, String hash, Long orgUnitId, String outcomeCode, LocalDateTime now) {
        PayCardFailRiskEvent ev = new PayCardFailRiskEvent();
        ev.setPgVendor(pg);
        ev.setPanHash(hash);
        ev.setOrgUnitId(orgUnitId);
        ev.setOutcomeCode(outcomeCode != null ? outcomeCode.trim() : null);
        ev.setOccurredAt(now);
        riskEventRepository.save(ev);
    }

    private void clearRiskEvents(String pg, String hash, Long orgUnitId) {
        riskEventRepository.deleteAllForCard(pg, hash, orgUnitId);
    }

    private void pruneExpiredRiskEvents(String pg, String hash, Long orgUnitId,
                                        CardRiskPolicyEffective policy, LocalDateTime now) {
        LocalDateTime start = CardRiskTrackPeriod.windowStart(
                policy.trackPeriodMode(), policy.trackPeriodValue(), now);
        if (start != null) {
            riskEventRepository.deleteOlderThanForCard(pg, hash, orgUnitId, start);
        }
    }

    private int countQualifyingFailures(String pg, String hash, Long orgUnitId,
                                        CardRiskPolicyEffective policy, LocalDateTime now) {
        LocalDateTime start = CardRiskTrackPeriod.windowStart(
                policy.trackPeriodMode(), policy.trackPeriodValue(), now);
        if (start == null) {
            return (int) riskEventRepository.countAllForCard(pg, hash, orgUnitId);
        }
        return (int) riskEventRepository.countSinceForCard(pg, hash, orgUnitId, start);
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

    @Transactional
    public void clearRiskStateOnInactiveCardRelease(String pgVendorRaw, String panHash, String panMaskKey) {
        String hash = panHash != null ? panHash.trim() : "";
        String mask = panMaskKey != null ? panMaskKey.trim() : "";
        if (hash.isEmpty() && mask.isEmpty()) {
            return;
        }
        for (String pg : pgScopesForInactiveCardRelease(pgVendorRaw)) {
            for (PayCardFailCooldown row : cooldownRepository.findAllByPgAndPanIdentity(pg, hash, mask)) {
                String rowHash = row.getPanHash();
                if (rowHash != null && !rowHash.isBlank()) {
                    riskEventRepository.deleteAllForCard(pg, rowHash.trim(), row.getOrgUnitId());
                }
                row.setFailCount(0);
                row.setBlockedUntil(null);
                row.setLastOutcomeCode(null);
                cooldownRepository.save(row);
            }
        }
    }

    private static List<String> pgScopesForInactiveCardRelease(String pgVendorRaw) {
        if (pgVendorRaw == null || pgVendorRaw.isBlank()) {
            return List.of(PgVendor.JPAY, PgVendor.CHILLPAY, PgVendor.EXIMBAY, PgVendor.ELEMENTPAY, PgVendor.ILK);
        }
        String pg = pgVendorRaw.trim().toUpperCase(Locale.ROOT);
        if (PgVendor.isJpayFamily(pg)) {
            return List.of(PgVendor.JPAY);
        }
        if (PgVendor.isChillPayFamily(pg)) {
            return List.of(PgVendor.CHILLPAY);
        }
        if (PgVendor.isEximbayFamily(pg)) {
            return List.of(PgVendor.EXIMBAY);
        }
        if (PgVendor.isElementPayFamily(pg)) {
            return List.of(PgVendor.ELEMENTPAY);
        }
        if (PgVendor.isIlkFamily(pg)) {
            return List.of(PgVendor.ILK);
        }
        return List.of(pg);
    }

    private Optional<PayCardFailCooldown> findRow(String pg, String hash, Long orgUnitId) {
        if (orgUnitId != null) {
            return cooldownRepository.findByPgVendorAndPanHashAndOrgUnitId(pg, hash, orgUnitId);
        }
        return cooldownRepository.findByPgVendorAndPanHashAndOrgUnitIdIsNull(pg, hash);
    }
}
