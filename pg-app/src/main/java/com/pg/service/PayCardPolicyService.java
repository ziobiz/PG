package com.pg.service;

import com.pg.entity.HqPayCardBlacklist;
import com.pg.entity.HqPayCardBlockPrefix;
import com.pg.entity.OrgUnit;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.HqPayCardBlacklistRepository;
import com.pg.repository.HqPayCardBlockPrefixRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.util.OpsInactiveCardPanRules;
import com.pg.util.PayCardBrand;
import com.pg.util.PayCardBrandDetector;
import com.pg.util.PayCardMaskKeyUtil;
import com.pg.util.PayCardPanHashUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * PG별 카드 브랜드·BIN 접두·블랙리스트 정책.
 * <p>JPAY: Visa/MC/JCB/UnionPay(62)/AMEX. ChillPay: AMEX 불가.
 */
@Service
public class PayCardPolicyService {

    public static final String MATCH_MODE_FULL_PAN = "FULL_PAN";
    public static final String MATCH_MODE_MASK_6_4 = "MASK_6_4";

    private static final Set<PayCardBrand> JPAY_ALLOWED = EnumSet.of(
            PayCardBrand.VISA, PayCardBrand.MASTERCARD, PayCardBrand.JCB,
            PayCardBrand.UNIONPAY, PayCardBrand.AMEX);
    private static final Set<PayCardBrand> CHILLPAY_ALLOWED = EnumSet.of(
            PayCardBrand.VISA, PayCardBrand.MASTERCARD, PayCardBrand.JCB, PayCardBrand.UNIONPAY);

    private final HqPayCardBlockPrefixRepository blockPrefixRepository;
    private final HqPayCardBlacklistRepository blacklistRepository;
    private final PayCardFailCooldownService payCardFailCooldownService;
    private final OrgUnitRepository orgUnitRepository;

    public PayCardPolicyService(HqPayCardBlockPrefixRepository blockPrefixRepository,
                                HqPayCardBlacklistRepository blacklistRepository,
                                @Lazy PayCardFailCooldownService payCardFailCooldownService,
                                OrgUnitRepository orgUnitRepository) {
        this.blockPrefixRepository = blockPrefixRepository;
        this.blacklistRepository = blacklistRepository;
        this.payCardFailCooldownService = payCardFailCooldownService;
        this.orgUnitRepository = orgUnitRepository;
    }

    public String normalizePgVendor(String pgCdOrVan) {
        if (pgCdOrVan == null || pgCdOrVan.isBlank()) {
            return PgVendor.CHILLPAY;
        }
        if (PgVendor.isJpayFamily(pgCdOrVan)) {
            return PgVendor.JPAY;
        }
        if (PgVendor.isChillPayFamily(pgCdOrVan)) {
            return PgVendor.CHILLPAY;
        }
        return pgCdOrVan.trim().toUpperCase(Locale.ROOT);
    }

    public Set<PayCardBrand> allowedBrandsForPg(String pgVendor) {
        String pg = normalizePgVendor(pgVendor);
        if (PgVendor.JPAY.equals(pg)) {
            return JPAY_ALLOWED;
        }
        return CHILLPAY_ALLOWED;
    }

    public boolean isBrandSelectEnabled(String pgVendor) {
        return PgVendor.JPAY.equals(normalizePgVendor(pgVendor));
    }

    /** 결제창·API 공용 정책 페이로드 */
    public Map<String, Object> buildClientPolicy(String pgVendorRaw) {
        String pg = normalizePgVendor(pgVendorRaw);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("pgVendor", pg);
        out.put("brandSelectEnabled", isBrandSelectEnabled(pg));
        List<String> brands = new ArrayList<>();
        for (PayCardBrand b : allowedBrandsForPg(pg)) {
            brands.add(b.name());
        }
        out.put("allowedBrands", brands);
        out.put("blockedPrefixes", loadActivePrefixDigits(pg));
        out.put("amexDigitLength", 15);
        out.put("defaultDigitLength", 16);
        out.put("unionPayValidPrefix", "62");
        Map<String, Map<String, String>> msg = new LinkedHashMap<>();
        msg.put("BLOCKED_PREFIX", PayCardPolicyI18n.allLang("BLOCKED_PREFIX", "{0}"));
        msg.put("BLACKLIST", PayCardPolicyI18n.allLang("INACTIVE_CARD"));
        msg.put("INACTIVE_CARD", PayCardPolicyI18n.allLang("INACTIVE_CARD"));
        msg.put("CARD_COOLDOWN", PayCardPolicyI18n.allLang("CARD_COOLDOWN", "{0}"));
        for (int tier = 1; tier <= 4; tier++) {
            String key = PayCardPolicyI18n.tierCooldownMessageKey(tier);
            msg.put(key, PayCardPolicyI18n.allLang(key, "{0}"));
        }
        msg.put("BRAND_NOT_ALLOWED", PayCardPolicyI18n.allLang("BRAND_NOT_ALLOWED", "{0}"));
        msg.put("UNION_NOT_62", PayCardPolicyI18n.allLang("UNION_NOT_62"));
        msg.put("UNION_60_81", PayCardPolicyI18n.allLang("UNION_60_81"));
        msg.put("AMEX_LEN", PayCardPolicyI18n.allLang("AMEX_LEN"));
        msg.put("CARD_LEN", PayCardPolicyI18n.allLang("CARD_LEN", "{0}"));
        msg.put("INVALID_PAN", PayCardPolicyI18n.allLang("INVALID_PAN"));
        msg.put("SELECT_BRAND", PayCardPolicyI18n.allLang("SELECT_BRAND"));
        out.put("messages", msg);
        return out;
    }

    public List<String> loadActivePrefixDigits(String pgVendor) {
        String pg = normalizePgVendor(pgVendor);
        List<String> out = new ArrayList<>();
        for (HqPayCardBlockPrefix row : blockPrefixRepository.findByPgVendorAndActiveYnOrderByPrefixDigitsAsc(pg, "Y")) {
            String p = row.getPrefixDigits() != null ? row.getPrefixDigits().trim() : "";
            if (!p.isEmpty()) {
                out.add(p);
            }
        }
        return out;
    }

    public Map<String, Object> validateForSale(String pgVendorRaw, String panRaw, String selectedBrandRaw, String lang) {
        return validateForSale(pgVendorRaw, panRaw, selectedBrandRaw, lang, null);
    }

    public Map<String, Object> validateForSale(String pgVendorRaw, String panRaw, String selectedBrandRaw, String lang,
                                               Long orgUnitId) {
        String pg = normalizePgVendor(pgVendorRaw);
        String pan = PayCardBrandDetector.normalizePan(panRaw);
        String langNorm = lang != null ? lang.trim() : "KO";

        if (pan.length() < 6) {
            return fail("INVALID_PAN", "INVALID_PAN", langNorm);
        }

        if (findBlacklistHit(pan, pg).isPresent()) {
            return fail("INACTIVE_CARD", "INACTIVE_CARD", langNorm);
        }

        Optional<Map<String, Object>> cooldown = payCardFailCooldownService.checkBlocked(pg, pan, langNorm, orgUnitId);
        if (cooldown.isPresent()) {
            return cooldown.get();
        }

        PayCardBrand detected = PayCardBrandDetector.detect(pan);
        PayCardBrand selected = PayCardBrandDetector.parseBrandKey(selectedBrandRaw);
        PayCardBrand brand = selected != null ? selected : detected;

        String blockedPrefix = matchBlockedPrefix(pg, pan);
        if (blockedPrefix != null) {
            return fail("BLOCKED_PREFIX", "BLOCKED_PREFIX", langNorm, blockedPrefix);
        }

        if (PgVendor.JPAY.equals(pg)) {
            if (pan.startsWith("60") || pan.startsWith("81")) {
                return fail("UNION_60_81", "UNION_60_81", langNorm);
            }
            if (detected == PayCardBrand.UNIONPAY && !pan.startsWith("62")) {
                return fail("UNION_NOT_62", "UNION_NOT_62", langNorm);
            }
        }

        if (!allowedBrandsForPg(pg).contains(brand)) {
            return fail("BRAND_NOT_ALLOWED", "BRAND_NOT_ALLOWED", langNorm,
                    PayCardPolicyI18n.brandLabelKo(brand));
        }
        if (detected != PayCardBrand.UNKNOWN && detected != brand && selected != null) {
            return fail("INVALID_PAN", "INVALID_PAN", langNorm);
        }

        int expected = PayCardBrandDetector.expectedLength(brand);
        if (pan.length() > 0 && pan.length() != expected && pan.length() >= expected - 2) {
            String key = brand == PayCardBrand.AMEX ? "AMEX_LEN" : "CARD_LEN";
            return fail(key, key, langNorm, expected);
        }
        if (pan.length() >= expected && pan.length() != expected) {
            String key = brand == PayCardBrand.AMEX ? "AMEX_LEN" : "CARD_LEN";
            return fail(key, key, langNorm, expected);
        }

        Map<String, Object> ok = new LinkedHashMap<>();
        ok.put("valid", true);
        ok.put("brand", brand.name());
        ok.put("expectedLength", expected);
        return ok;
    }

    public Optional<HqPayCardBlacklist> findBlacklistHit(String pan, String pgVendor) {
        String pg = normalizePgVendor(pgVendor);
        String norm = PayCardBrandDetector.normalizePan(pan);
        if (norm.length() >= 13) {
            Optional<HqPayCardBlacklist> full = blacklistRepository.findActiveHit(PayCardPanHashUtil.hashPan(norm), pg);
            if (full.isPresent()) {
                return full;
            }
        }
        if (norm.length() >= 10) {
            String maskKey = PayCardMaskKeyUtil.maskKeyFromPan(norm);
            if (!maskKey.isEmpty()) {
                return blacklistRepository.findActiveMaskDisplayHit(maskKey, pg);
            }
        }
        return Optional.empty();
    }

    private String matchBlockedPrefix(String pg, String pan) {
        for (String prefix : loadActivePrefixDigits(pg)) {
            if (!prefix.isEmpty() && pan.startsWith(prefix)) {
                return prefix;
            }
        }
        return null;
    }

    private static Map<String, Object> fail(String code, String messageKey, String lang, Object... args) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("valid", false);
        m.put("errorCode", code);
        m.put("messageKey", messageKey);
        m.put("message", PayCardPolicyI18n.format(lang, messageKey, args));
        m.put("messages", PayCardPolicyI18n.allLang(messageKey, args));
        return m;
    }

    public List<Map<String, Object>> listBlockPrefixesForAdmin() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (HqPayCardBlockPrefix row : blockPrefixRepository.findByActiveYnOrderByPgVendorAscPrefixDigitsAsc("Y")) {
            list.add(prefixToMap(row));
        }
        return list;
    }

    public List<Map<String, Object>> listBlacklistForAdmin() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (HqPayCardBlacklist row : blacklistRepository.findByActiveYnOrderByIdDesc("Y")) {
            list.add(blacklistToMap(row));
        }
        return list;
    }

    @Transactional
    public HqPayCardBlockPrefix addBlockPrefix(String pgVendor, String prefixDigits, String remark) {
        String pg = normalizePgVendor(pgVendor);
        String p = normalizePrefix(prefixDigits);
        if (p.isEmpty()) {
            throw new IllegalArgumentException("접두 숫자가 필요합니다.");
        }
        if (blockPrefixRepository.existsByPgVendorAndPrefixDigits(pg, p)) {
            throw new IllegalArgumentException("이미 등록된 접두입니다: " + p);
        }
        HqPayCardBlockPrefix row = new HqPayCardBlockPrefix();
        row.setPgVendor(pg);
        row.setPrefixDigits(p);
        row.setRemark(remark != null ? remark.trim() : null);
        row.setActiveYn("Y");
        return blockPrefixRepository.save(row);
    }

    @Transactional
    public void deleteBlockPrefix(Long id) {
        blockPrefixRepository.findById(id).ifPresent(row -> {
            row.setActiveYn("N");
            blockPrefixRepository.save(row);
        });
    }

    @Transactional
    public HqPayCardBlacklist addBlacklistManual(String pgVendor, String panRaw, String reason, String registeredBy) {
        return addBlacklistManual(pgVendor, panRaw, reason, registeredBy, null, null);
    }

    public HqPayCardBlacklist addBlacklistManual(String pgVendor, String panRaw, String reason, String registeredBy,
                                               String cardBrand) {
        return addBlacklistManual(pgVendor, panRaw, reason, registeredBy, cardBrand, null);
    }

    public HqPayCardBlacklist addBlacklistManual(String pgVendor, String panRaw, String reason, String registeredBy,
                                               String cardBrand, String holderName) {
        return addBlacklistManual(pgVendor, panRaw, reason, registeredBy, cardBrand, holderName, null, null, null);
    }

    public HqPayCardBlacklist addBlacklistManual(String pgVendor, String panRaw, String reason, String registeredBy,
                                               String cardBrand, String holderName,
                                               Long orgUnitId, String compId, String compNm) {
        String pg = pgVendor != null && !pgVendor.isBlank() ? normalizePgVendor(pgVendor) : null;
        String pgScope = pg != null ? pg : PgVendor.JPAY;

        if (PayCardMaskKeyUtil.isMaskInput(panRaw)) {
            String maskKey = PayCardMaskKeyUtil.normalizeMaskInput(panRaw);
            OpsInactiveCardPanRules.validateForMaskRegister(maskKey);
            if (blacklistRepository.findActiveMaskDisplayHit(maskKey, pgScope).isPresent()) {
                throw new IllegalArgumentException("이미 비활성 등록된 카드입니다.");
            }
            String hash = PayCardMaskKeyUtil.hashForMaskKey(maskKey);
            HqPayCardBlacklist row = new HqPayCardBlacklist();
            row.setPgVendor(pg);
            row.setPanHash(hash);
            row.setPanDisplay(maskKey);
            row.setMatchMode(MATCH_MODE_MASK_6_4);
            row.setHolderName(trimHolder(holderName));
            row.setSource("MANUAL");
            row.setReason(reason != null ? reason.trim() : null);
            row.setRegisteredBy(registeredBy != null && !registeredBy.isBlank() ? registeredBy.trim() : null);
            row.setActiveYn("Y");
            applyRegisteredMerchantSnapshot(row, orgUnitId, compId, compNm);
            return blacklistRepository.save(row);
        }

        String pan = PayCardBrandDetector.normalizePan(panRaw);
        OpsInactiveCardPanRules.validateForRegister(cardBrand, pan);
        String hash = PayCardPanHashUtil.hashPan(pan);
        if (blacklistRepository.findActiveHit(hash, pgScope).isPresent()) {
            throw new IllegalArgumentException("이미 비활성 등록된 카드입니다.");
        }
        String maskKey = PayCardMaskKeyUtil.maskKeyFromPan(pan);
        if (!maskKey.isEmpty() && blacklistRepository.findActiveMaskDisplayHit(maskKey, pgScope).isPresent()) {
            throw new IllegalArgumentException("이미 비활성 등록된 카드입니다.");
        }
        HqPayCardBlacklist row = new HqPayCardBlacklist();
        row.setPgVendor(pg);
        row.setPanHash(hash);
        row.setPanDisplay(maskKey.isEmpty() ? PayCardPanHashUtil.maskForDisplay(pan) : maskKey);
        row.setMatchMode(MATCH_MODE_FULL_PAN);
        row.setHolderName(trimHolder(holderName));
        row.setSource("MANUAL");
        row.setReason(reason != null ? reason.trim() : null);
        row.setRegisteredBy(registeredBy != null && !registeredBy.isBlank() ? registeredBy.trim() : null);
        row.setActiveYn("Y");
        row.setReleasedAt(null);
        row.setReleasedBy(null);
        applyRegisteredMerchantSnapshot(row, orgUnitId, compId, compNm);
        return blacklistRepository.save(row);
    }

    @Transactional
    public HqPayCardBlacklist addBlacklistAutoMask(String pgVendor, String maskKey, String reason) {
        return addBlacklistAutoMask(pgVendor, maskKey, reason, null);
    }

    @Transactional
    public HqPayCardBlacklist addBlacklistAutoMask(String pgVendor, String maskKey, String reason, Long orgUnitId) {
        String mk = PayCardMaskKeyUtil.normalizeMaskInput(maskKey);
        if (!PayCardMaskKeyUtil.isValidMaskKey(mk)) {
            return null;
        }
        String pg = normalizePgVendor(pgVendor);
        if (blacklistRepository.findActiveMaskDisplayHit(mk, pg).isPresent()) {
            return null;
        }
        HqPayCardBlacklist row = new HqPayCardBlacklist();
        row.setPgVendor(pg);
        row.setPanHash(PayCardMaskKeyUtil.hashForMaskKey(mk));
        row.setPanDisplay(mk);
        row.setMatchMode(MATCH_MODE_MASK_6_4);
        row.setSource("AUTO");
        row.setReason(reason != null ? reason.trim() : "AUTO_FAIL_COOLDOWN");
        row.setRegisteredOrgUnitId(orgUnitId);
        applyRegisteredMerchantSnapshot(row, orgUnitId, null, null);
        row.setActiveYn("Y");
        return blacklistRepository.save(row);
    }

    @Transactional
    public HqPayCardBlacklist releaseBlacklist(long id, String releasedBy) {
        HqPayCardBlacklist row = blacklistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("등록 건을 찾을 수 없습니다."));
        if (!"Y".equalsIgnoreCase(String.valueOf(row.getActiveYn()).trim())) {
            throw new IllegalStateException("이미 해지된 카드입니다.");
        }
        row.setActiveYn("N");
        row.setReleasedAt(java.time.LocalDateTime.now());
        row.setReleasedBy(releasedBy != null && !releasedBy.isBlank() ? releasedBy.trim() : null);
        return blacklistRepository.save(row);
    }

    @Transactional
    public HqPayCardBlacklist updateInactiveCardContent(long id, String pgVendor, String holderName, String reason,
                                                        Long orgUnitId, String compId, String compNm, String updatedBy) {
        HqPayCardBlacklist row = blacklistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("등록 건을 찾을 수 없습니다."));
        if (!"Y".equalsIgnoreCase(String.valueOf(row.getActiveYn()).trim())) {
            throw new IllegalStateException("해지된 카드는 수정할 수 없습니다.");
        }
        String pg = pgVendor != null && !pgVendor.isBlank() ? normalizePgVendor(pgVendor) : null;
        row.setPgVendor(pg);
        row.setHolderName(trimHolder(holderName));
        row.setReason(reason != null ? reason.trim() : null);
        String compIdVal = compId != null ? compId.trim() : "";
        String compNmVal = compNm != null ? compNm.trim() : "";
        if (compIdVal.isEmpty()) {
            row.setRegisteredOrgUnitId(null);
            row.setRegisteredCompId(null);
            row.setRegisteredCompNm(compNmVal.isEmpty() ? null
                    : (compNmVal.length() > 200 ? compNmVal.substring(0, 200) : compNmVal));
        } else {
            applyRegisteredMerchantSnapshot(row, orgUnitId, compIdVal, compNmVal);
        }
        row.setContentUpdatedAt(java.time.LocalDateTime.now());
        row.setContentUpdatedBy(updatedBy != null && !updatedBy.isBlank() ? updatedBy.trim() : null);
        return blacklistRepository.save(row);
    }

    @Transactional
    public HqPayCardBlacklist addBlacklistAuto(String pgVendor, String panRaw, String reason) {
        return addBlacklistAuto(pgVendor, panRaw, reason, null);
    }

    @Transactional
    public HqPayCardBlacklist addBlacklistAuto(String pgVendor, String panRaw, String reason, Long orgUnitId) {
        String pan = PayCardBrandDetector.normalizePan(panRaw);
        if (pan.length() < 10) {
            return null;
        }
        String pg = normalizePgVendor(pgVendor);
        String hash = PayCardPanHashUtil.hashPan(pan);
        if (blacklistRepository.findActiveHit(hash, pg).isPresent()) {
            return null;
        }
        String maskKey = PayCardMaskKeyUtil.maskKeyFromPan(pan);
        if (!maskKey.isEmpty() && blacklistRepository.findActiveMaskDisplayHit(maskKey, pg).isPresent()) {
            return null;
        }
        HqPayCardBlacklist row = new HqPayCardBlacklist();
        row.setPgVendor(pg);
        row.setPanHash(hash);
        row.setPanDisplay(maskKey.isEmpty() ? PayCardPanHashUtil.maskForDisplay(pan) : maskKey);
        row.setMatchMode(MATCH_MODE_FULL_PAN);
        row.setSource("AUTO");
        row.setReason(reason != null ? reason.trim() : "AUTO_FRAUD");
        row.setRegisteredOrgUnitId(orgUnitId);
        applyRegisteredMerchantSnapshot(row, orgUnitId, null, null);
        row.setActiveYn("Y");
        return blacklistRepository.save(row);
    }

    @Transactional
    public void deleteBlacklist(Long id) {
        blacklistRepository.findById(id).ifPresent(row -> {
            row.setActiveYn("N");
            blacklistRepository.save(row);
        });
    }

    private static String normalizePrefix(String raw) {
        return PayCardBrandDetector.normalizePan(raw);
    }

    private static String trimHolder(String holderName) {
        if (holderName == null) {
            return null;
        }
        String t = holderName.trim();
        return t.isEmpty() ? null : (t.length() > 100 ? t.substring(0, 100) : t);
    }

    private void applyRegisteredMerchantSnapshot(HqPayCardBlacklist row, Long orgUnitId, String compIdIn, String compNmIn) {
        if (row == null) {
            return;
        }
        String compId = compIdIn != null ? compIdIn.trim() : "";
        String compNm = compNmIn != null ? compNmIn.trim() : "";
        Long resolvedOrgId = orgUnitId;
        if (!compId.isEmpty()) {
            row.setRegisteredCompId(compId.length() > 32 ? compId.substring(0, 32) : compId);
            Optional<OrgUnit> ouOpt = orgUnitRepository.findByCodeIgnoreCase(compId);
            if (ouOpt.isPresent()) {
                resolvedOrgId = ouOpt.get().getId();
                if (compNm.isEmpty()) {
                    String nm = ouOpt.get().getName();
                    if (nm != null && !nm.isBlank()) {
                        compNm = nm.trim();
                    }
                }
            }
        } else if (resolvedOrgId != null) {
            Optional<OrgUnit> ouById = orgUnitRepository.findById(resolvedOrgId);
            if (ouById.isPresent()) {
                OrgUnit ou = ouById.get();
                if (ou.getCode() != null && !ou.getCode().isBlank()) {
                    row.setRegisteredCompId(ou.getCode().trim());
                }
                if (compNm.isEmpty() && ou.getName() != null && !ou.getName().isBlank()) {
                    compNm = ou.getName().trim();
                }
            }
        }
        if (resolvedOrgId != null) {
            row.setRegisteredOrgUnitId(resolvedOrgId);
        }
        if (!compNm.isEmpty()) {
            row.setRegisteredCompNm(compNm.length() > 200 ? compNm.substring(0, 200) : compNm);
        }
    }

    private static Map<String, Object> prefixToMap(HqPayCardBlockPrefix row) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", row.getId());
        m.put("pgVendor", row.getPgVendor());
        m.put("prefixDigits", row.getPrefixDigits());
        m.put("remark", row.getRemark());
        m.put("activeYn", row.getActiveYn());
        return m;
    }

    private static Map<String, Object> blacklistToMap(HqPayCardBlacklist row) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", row.getId());
        m.put("pgVendor", row.getPgVendor());
        m.put("panDisplay", row.getPanDisplay());
        m.put("holderName", row.getHolderName());
        m.put("matchMode", row.getMatchMode());
        m.put("source", row.getSource());
        m.put("reason", row.getReason());
        m.put("activeYn", row.getActiveYn());
        m.put("registeredBy", row.getRegisteredBy());
        m.put("registeredAt", row.getCreatedAt() != null ? row.getCreatedAt().toString() : null);
        m.put("releasedBy", row.getReleasedBy());
        m.put("releasedAt", row.getReleasedAt() != null ? row.getReleasedAt().toString() : null);
        return m;
    }
}
