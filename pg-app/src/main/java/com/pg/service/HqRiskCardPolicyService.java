package com.pg.service;

import com.pg.entity.HqRiskCardPolicy;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.HqPayCardBlacklistRepository;
import com.pg.repository.HqRiskCardPolicyRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.util.CardRiskTrackPeriod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class HqRiskCardPolicyService {

    public static final String MODE_FOLLOW_HQ = "FOLLOW_HQ";
    public static final String MODE_CUSTOM = "CUSTOM";
    public static final String MODE_DISABLED = "DISABLED";

    public static final String TRACK_POLICY_NONE = "NONE";
    public static final String TRACK_POLICY_FOLLOW_HQ = "FOLLOW_HQ";
    public static final String TRACK_POLICY_CUSTOM = "CUSTOM";

    private final HqRiskCardPolicyRepository riskCardPolicyRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final HqPayCardBlacklistRepository blacklistRepository;

    public HqRiskCardPolicyService(HqRiskCardPolicyRepository riskCardPolicyRepository,
                                   MerchantProfileRepository merchantProfileRepository,
                                   OrgUnitRepository orgUnitRepository,
                                   HqPayCardBlacklistRepository blacklistRepository) {
        this.riskCardPolicyRepository = riskCardPolicyRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.blacklistRepository = blacklistRepository;
    }

    @Transactional
    public HqRiskCardPolicy getOrCreate() {
        return riskCardPolicyRepository.findById(1L).orElseGet(() -> {
            HqRiskCardPolicy row = new HqRiskCardPolicy();
            row.setId(1L);
            return riskCardPolicyRepository.save(row);
        });
    }

    public Map<String, Object> toMap(HqRiskCardPolicy s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("enabledYn", yn(s.getEnabledYn()));
        m.put("tier1Hours", intOr(s.getTier1Hours(), 0));
        m.put("tier1Min", intOr(s.getTier1Min(), 5));
        m.put("tier2Hours", intOr(s.getTier2Hours(), 0));
        m.put("tier2Min", intOr(s.getTier2Min(), 10));
        m.put("tier3Hours", intOr(s.getTier3Hours(), 1));
        m.put("tier3Min", intOr(s.getTier3Min(), 0));
        m.put("tier4Hours", intOr(s.getTier4Hours(), 0));
        m.put("tier4Min", intOr(s.getTier4Min(), 0));
        m.put("autoBlacklistTriggerTier", clampTier(s.getAutoBlacklistTriggerTier(), 4));
        m.put("trackPeriodMode", CardRiskTrackPeriod.normalizeMode(s.getTrackPeriodMode()));
        m.put("trackPeriodValue", intOr(s.getTrackPeriodValue(), 0));
        m.put("trackPeriodDisplay", CardRiskTrackPeriod.formatDisplay(s.getTrackPeriodMode(), s.getTrackPeriodValue()));
        m.put("tier1TotalMin", toTotalMinutes(s.getTier1Hours(), s.getTier1Min(), 5));
        m.put("tier2TotalMin", toTotalMinutes(s.getTier2Hours(), s.getTier2Min(), 10));
        m.put("tier3TotalMin", toTotalMinutes(s.getTier3Hours(), s.getTier3Min(), 60));
        m.put("tier4TotalMin", toTotalMinutes(s.getTier4Hours(), s.getTier4Min(), 0));
        m.put("presaleFilterEnabledYn", yn(s.getPresaleFilterEnabledYn()));
        m.put("filterBuyerContactMismatchYn", yn(s.getFilterBuyerContactMismatchYn()));
        m.put("filterHolderNameYn", yn(s.getFilterHolderNameYn()));
        m.put("filterVelocityCardYn", yn(s.getFilterVelocityCardYn()));
        m.put("filterVelocityEmailYn", yn(s.getFilterVelocityEmailYn()));
        m.put("filterVelocityIpYn", yn(s.getFilterVelocityIpYn()));
        /* 채널별 속도제한(권장 기본: 카드10/3, 이메일30/5, IP15/10). legacy 통합 필드는 카드와 동기 */
        int cardWin = intOr(s.getVelocityCardWindowMinutes(), intOr(s.getVelocityWindowMinutes(), 10));
        int cardMax = intOr(s.getVelocityCardMaxAttempts(), intOr(s.getVelocityMaxAttempts(), 3));
        m.put("velocityCardWindowMinutes", cardWin);
        m.put("velocityCardMaxAttempts", cardMax);
        m.put("velocityEmailWindowMinutes", intOr(s.getVelocityEmailWindowMinutes(), 30));
        m.put("velocityEmailMaxAttempts", intOr(s.getVelocityEmailMaxAttempts(), 5));
        m.put("velocityIpWindowMinutes", intOr(s.getVelocityIpWindowMinutes(), 15));
        m.put("velocityIpMaxAttempts", intOr(s.getVelocityIpMaxAttempts(), 10));
        m.put("velocityWindowMinutes", cardWin);
        m.put("velocityMaxAttempts", cardMax);
        m.put("filterPhoneInvalidYn", yn(s.getFilterPhoneInvalidYn()));
        m.put("filterEmailInvalidYn", yn(s.getFilterEmailInvalidYn()));
        m.put("postsaleCooldownJpayHighriskYn", yn(s.getPostsaleCooldownJpayHighriskYn()));
        m.put("postsaleCooldownJpayPy0124Yn", yn(s.getPostsaleCooldownJpayPy0124Yn()));
        return m;
    }

    @Transactional
    public HqRiskCardPolicy save(Map<String, Object> body) {
        HqRiskCardPolicy s = getOrCreate();
        if (body.containsKey("enabledYn")) {
            s.setEnabledYn(parseYn(body.get("enabledYn"), s.getEnabledYn()));
        }
        applyTierFromBody(s, body, 1);
        applyTierFromBody(s, body, 2);
        applyTierFromBody(s, body, 3);
        applyTierFromBody(s, body, 4);
        if (body.containsKey("autoBlacklistTriggerTier")) {
            s.setAutoBlacklistTriggerTier(clampTier(parseInt(body.get("autoBlacklistTriggerTier")), 4));
        }
        if (body.containsKey("trackPeriodMode")) {
            s.setTrackPeriodMode(CardRiskTrackPeriod.normalizeMode(
                    body.get("trackPeriodMode") != null ? body.get("trackPeriodMode").toString() : null));
        }
        if (body.containsKey("trackPeriodValue")) {
            s.setTrackPeriodValue(parseTrackPeriodValue(body.get("trackPeriodValue")));
        }
        applyPresaleFilterFromBody(s, body);
        return riskCardPolicyRepository.save(s);
    }

    private void applyPresaleFilterFromBody(HqRiskCardPolicy s, Map<String, Object> body) {
        if (body.containsKey("presaleFilterEnabledYn")) {
            s.setPresaleFilterEnabledYn(parseYn(body.get("presaleFilterEnabledYn"), s.getPresaleFilterEnabledYn()));
        }
        if (body.containsKey("filterBuyerContactMismatchYn")) {
            s.setFilterBuyerContactMismatchYn(parseYn(body.get("filterBuyerContactMismatchYn"), s.getFilterBuyerContactMismatchYn()));
        }
        if (body.containsKey("filterHolderNameYn")) {
            s.setFilterHolderNameYn(parseYn(body.get("filterHolderNameYn"), s.getFilterHolderNameYn()));
        }
        if (body.containsKey("filterVelocityCardYn")) {
            s.setFilterVelocityCardYn(parseYn(body.get("filterVelocityCardYn"), s.getFilterVelocityCardYn()));
        }
        if (body.containsKey("filterVelocityEmailYn")) {
            s.setFilterVelocityEmailYn(parseYn(body.get("filterVelocityEmailYn"), s.getFilterVelocityEmailYn()));
        }
        if (body.containsKey("filterVelocityIpYn")) {
            s.setFilterVelocityIpYn(parseYn(body.get("filterVelocityIpYn"), s.getFilterVelocityIpYn()));
        }
        applyVelocityChannelFromBody(s, body, "velocityCardWindowMinutes", "velocityCardMaxAttempts",
                true, 10, 3);
        applyVelocityChannelFromBody(s, body, "velocityEmailWindowMinutes", "velocityEmailMaxAttempts",
                false, 30, 5);
        applyVelocityChannelFromBody(s, body, "velocityIpWindowMinutes", "velocityIpMaxAttempts",
                false, 15, 10);
        /* 구 UI 호환: 통합 필드만 오면 카드 채널에 반영 */
        if (!body.containsKey("velocityCardWindowMinutes") && body.containsKey("velocityWindowMinutes")) {
            int w = Math.min(1440, Math.max(1, parseInt(body.get("velocityWindowMinutes"))));
            s.setVelocityCardWindowMinutes(w);
            s.setVelocityWindowMinutes(w);
        }
        if (!body.containsKey("velocityCardMaxAttempts") && body.containsKey("velocityMaxAttempts")) {
            int a = Math.min(99, Math.max(1, parseInt(body.get("velocityMaxAttempts"))));
            s.setVelocityCardMaxAttempts(a);
            s.setVelocityMaxAttempts(a);
        }
        if (body.containsKey("filterPhoneInvalidYn")) {
            s.setFilterPhoneInvalidYn(parseYn(body.get("filterPhoneInvalidYn"), s.getFilterPhoneInvalidYn()));
        }
        if (body.containsKey("filterEmailInvalidYn")) {
            s.setFilterEmailInvalidYn(parseYn(body.get("filterEmailInvalidYn"), s.getFilterEmailInvalidYn()));
        }
        if (body.containsKey("postsaleCooldownJpayHighriskYn")) {
            s.setPostsaleCooldownJpayHighriskYn(parseYn(body.get("postsaleCooldownJpayHighriskYn"),
                    s.getPostsaleCooldownJpayHighriskYn()));
        }
        if (body.containsKey("postsaleCooldownJpayPy0124Yn")) {
            s.setPostsaleCooldownJpayPy0124Yn(parseYn(body.get("postsaleCooldownJpayPy0124Yn"),
                    s.getPostsaleCooldownJpayPy0124Yn()));
        }
    }

    private void applyVelocityChannelFromBody(HqRiskCardPolicy s, Map<String, Object> body,
                                              String winKey, String maxKey, boolean syncLegacy,
                                              int defWin, int defMax) {
        if (body.containsKey(winKey)) {
            int w = Math.min(1440, Math.max(1, parseIntOr(body.get(winKey), defWin)));
            if ("velocityCardWindowMinutes".equals(winKey)) {
                s.setVelocityCardWindowMinutes(w);
                if (syncLegacy) {
                    s.setVelocityWindowMinutes(w);
                }
            } else if ("velocityEmailWindowMinutes".equals(winKey)) {
                s.setVelocityEmailWindowMinutes(w);
            } else if ("velocityIpWindowMinutes".equals(winKey)) {
                s.setVelocityIpWindowMinutes(w);
            }
        }
        if (body.containsKey(maxKey)) {
            int a = Math.min(99, Math.max(1, parseIntOr(body.get(maxKey), defMax)));
            if ("velocityCardMaxAttempts".equals(maxKey)) {
                s.setVelocityCardMaxAttempts(a);
                if (syncLegacy) {
                    s.setVelocityMaxAttempts(a);
                }
            } else if ("velocityEmailMaxAttempts".equals(maxKey)) {
                s.setVelocityEmailMaxAttempts(a);
            } else if ("velocityIpMaxAttempts".equals(maxKey)) {
                s.setVelocityIpMaxAttempts(a);
            }
        }
    }

    private static int parseIntOr(Object o, int def) {
        if (o == null) {
            return def;
        }
        try {
            return Integer.parseInt(o.toString().trim());
        } catch (Exception e) {
            return def;
        }
    }

    public CardRiskPolicyEffective resolveForOrgUnit(Long orgUnitId) {
        if (orgUnitId == null) {
            return resolveHqOnly();
        }
        Optional<MerchantProfile> mpOpt = merchantProfileRepository.findByOrgUnitId(orgUnitId);
        if (mpOpt.isEmpty()) {
            return resolveHqOnly();
        }
        MerchantProfile mp = mpOpt.get();
        String mode = normalizeMode(mp.getCardRiskPolicyMode());
        if (MODE_DISABLED.equals(mode)) {
            return new CardRiskPolicyEffective(false, zeroTiers(), 0, MODE_DISABLED,
                    CardRiskTrackPeriod.MODE_NONE, 0);
        }
        if (MODE_CUSTOM.equals(mode)) {
            int[] tiers = new int[]{
                    toTotalMinutes(mp.getCardRiskTier1Hours(), mp.getCardRiskTier1Min(), 5),
                    toTotalMinutes(mp.getCardRiskTier2Hours(), mp.getCardRiskTier2Min(), 10),
                    toTotalMinutes(mp.getCardRiskTier3Hours(), mp.getCardRiskTier3Min(), 60),
                    toTotalMinutes(mp.getCardRiskTier4Hours(), mp.getCardRiskTier4Min(), 0)
            };
            int trigger = clampTier(mp.getCardRiskAutoBlacklistTier(), 4);
            String trackPolicy = effectiveTrackPolicy(mp);
            String trackMode;
            int trackValue;
            if (TRACK_POLICY_FOLLOW_HQ.equals(trackPolicy)) {
                HqRiskCardPolicy hq = getOrCreate();
                trackMode = CardRiskTrackPeriod.normalizeMode(hq.getTrackPeriodMode());
                trackValue = hq.getTrackPeriodValue() != null ? hq.getTrackPeriodValue() : 0;
            } else if (TRACK_POLICY_CUSTOM.equals(trackPolicy)) {
                trackMode = CardRiskTrackPeriod.normalizeMode(mp.getCardRiskTrackPeriodMode());
                trackValue = mp.getCardRiskTrackPeriodValue() != null ? mp.getCardRiskTrackPeriodValue() : 0;
            } else {
                trackMode = CardRiskTrackPeriod.MODE_NONE;
                trackValue = 0;
            }
            return new CardRiskPolicyEffective(true, tiers, trigger, MODE_CUSTOM, trackMode, trackValue);
        }
        return resolveHqOnly();
    }

    public CardRiskPolicyEffective resolveHqOnly() {
        HqRiskCardPolicy hq = getOrCreate();
        if (!"Y".equalsIgnoreCase(yn(hq.getEnabledYn()))) {
            return new CardRiskPolicyEffective(false, zeroTiers(), 0, MODE_FOLLOW_HQ,
                    CardRiskTrackPeriod.MODE_NONE, 0);
        }
        int[] tiers = new int[]{
                toTotalMinutes(hq.getTier1Hours(), hq.getTier1Min(), 5),
                toTotalMinutes(hq.getTier2Hours(), hq.getTier2Min(), 10),
                toTotalMinutes(hq.getTier3Hours(), hq.getTier3Min(), 60),
                toTotalMinutes(hq.getTier4Hours(), hq.getTier4Min(), 0)
        };
        String trackMode = CardRiskTrackPeriod.normalizeMode(hq.getTrackPeriodMode());
        int trackValue = hq.getTrackPeriodValue() != null ? hq.getTrackPeriodValue() : 0;
        return new CardRiskPolicyEffective(true, tiers, clampTier(hq.getAutoBlacklistTriggerTier(), 4),
                MODE_FOLLOW_HQ, trackMode, trackValue);
    }

    public List<Map<String, Object>> listActiveMerchantRows() {
        List<OrgUnit> merchants = orgUnitRepository.findByOrgLevelOrderByCodeAsc(OrgLevel.MERCHANT);
        List<Map<String, Object>> out = new ArrayList<>();
        for (OrgUnit ou : merchants) {
            Optional<MerchantProfile> mpOpt = merchantProfileRepository.findByOrgUnitId(ou.getId());
            if (mpOpt.isEmpty()) {
                continue;
            }
            MerchantProfile mp = mpOpt.get();
            String mode = normalizeMode(mp.getCardRiskPolicyMode());
            CardRiskPolicyEffective eff = resolveForOrgUnit(ou.getId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("orgUnitId", ou.getId());
            row.put("compId", ou.getCode());
            row.put("compNm", ou.getName());
            row.put("policyMode", mode);
            if (MODE_DISABLED.equals(mode) || !eff.enabled()) {
                row.put("tier1Display", "—");
                row.put("tier2Display", "—");
                row.put("tier3Display", "—");
                row.put("tier4Display", "—");
                row.put("autoBlacklistTier", 0);
                row.put("autoBlacklistTierLabel", "—");
                row.put("trackPeriodDisplay", "—");
            } else {
                row.put("tier1Display", formatDuration(eff.tierMinutes(1)));
                row.put("tier2Display", formatDuration(eff.tierMinutes(2)));
                row.put("tier3Display", formatDuration(eff.tierMinutes(3)));
                row.put("tier4Display", formatDuration(eff.tierMinutes(4)));
                row.put("autoBlacklistTier", eff.autoBlacklistTriggerTier());
                row.put("autoBlacklistTierLabel", tierLabel(eff.autoBlacklistTriggerTier()));
                row.put("trackPeriodDisplay", CardRiskTrackPeriod.formatDisplay(
                        eff.trackPeriodMode(), eff.trackPeriodValue()));
            }
            long manualCnt = blacklistRepository.countByRegisteredOrgUnitIdAndActiveYnAndSource(ou.getId(), "Y", "MANUAL");
            long autoCnt = blacklistRepository.countByRegisteredOrgUnitIdAndActiveYnAndSource(ou.getId(), "Y", "AUTO");
            row.put("registeredCardCountManual", manualCnt);
            row.put("registeredCardCountAuto", autoCnt);
            row.put("latestRegisteredAt", blacklistRepository.findLatestCreatedAtByOrg(ou.getId())
                    .map(Object::toString).orElse(""));
            row.put("latestChannel", blacklistRepository.findLatestSourceByOrg(ou.getId()).orElse(""));
            out.add(row);
        }
        return out;
    }

    public void applyMerchantCardRiskFromRequest(MerchantProfile mp, Map<String, String> fields) {
        if (mp == null || fields == null) {
            return;
        }
        if (fields.containsKey("cardRiskPolicyMode")) {
            mp.setCardRiskPolicyMode(normalizeMode(fields.get("cardRiskPolicyMode")));
        }
        applyMerchantTier(mp, fields, 1);
        applyMerchantTier(mp, fields, 2);
        applyMerchantTier(mp, fields, 3);
        applyMerchantTier(mp, fields, 4);
        if (fields.containsKey("cardRiskAutoBlacklistTier")) {
            mp.setCardRiskAutoBlacklistTier(clampTier(parseInt(fields.get("cardRiskAutoBlacklistTier")), 4));
        }
        if (fields.containsKey("cardRiskTrackPeriodPolicy")) {
            mp.setCardRiskTrackPeriodPolicy(normalizeTrackPolicyForSave(fields.get("cardRiskTrackPeriodPolicy")));
        }
        if (fields.containsKey("cardRiskTrackPeriodMode")) {
            mp.setCardRiskTrackPeriodMode(CardRiskTrackPeriod.normalizeMode(fields.get("cardRiskTrackPeriodMode")));
        }
        if (fields.containsKey("cardRiskTrackPeriodValue")) {
            mp.setCardRiskTrackPeriodValue(parseTrackPeriodValue(fields.get("cardRiskTrackPeriodValue")));
        }
    }

    public void putMerchantCardRiskOnMap(Map<String, Object> m, MerchantProfile mp) {
        if (m == null || mp == null) {
            return;
        }
        m.put("cardRiskPolicyMode", normalizeMode(mp.getCardRiskPolicyMode()));
        m.put("cardRiskTier1Hours", mp.getCardRiskTier1Hours());
        m.put("cardRiskTier1Min", mp.getCardRiskTier1Min());
        m.put("cardRiskTier2Hours", mp.getCardRiskTier2Hours());
        m.put("cardRiskTier2Min", mp.getCardRiskTier2Min());
        m.put("cardRiskTier3Hours", mp.getCardRiskTier3Hours());
        m.put("cardRiskTier3Min", mp.getCardRiskTier3Min());
        m.put("cardRiskTier4Hours", mp.getCardRiskTier4Hours());
        m.put("cardRiskTier4Min", mp.getCardRiskTier4Min());
        m.put("cardRiskAutoBlacklistTier", mp.getCardRiskAutoBlacklistTier());
        m.put("cardRiskTrackPeriodPolicy", effectiveTrackPolicy(mp));
        m.put("cardRiskTrackPeriodMode", CardRiskTrackPeriod.normalizeMode(mp.getCardRiskTrackPeriodMode()));
        m.put("cardRiskTrackPeriodValue", mp.getCardRiskTrackPeriodValue());
    }

    private void applyMerchantTier(MerchantProfile mp, Map<String, String> fields, int tier) {
        String hKey = "cardRiskTier" + tier + "Hours";
        String mKey = "cardRiskTier" + tier + "Min";
        if (fields.containsKey(hKey)) {
            Integer h = parseIntObj(fields.get(hKey));
            switch (tier) {
                case 1 -> mp.setCardRiskTier1Hours(h);
                case 2 -> mp.setCardRiskTier2Hours(h);
                case 3 -> mp.setCardRiskTier3Hours(h);
                case 4 -> mp.setCardRiskTier4Hours(h);
                default -> { }
            }
        }
        if (fields.containsKey(mKey)) {
            Integer min = parseIntObj(fields.get(mKey));
            switch (tier) {
                case 1 -> mp.setCardRiskTier1Min(min);
                case 2 -> mp.setCardRiskTier2Min(min);
                case 3 -> mp.setCardRiskTier3Min(min);
                case 4 -> mp.setCardRiskTier4Min(min);
                default -> { }
            }
        }
    }

    private static void applyTierFromBody(HqRiskCardPolicy s, Map<String, Object> body, int tier) {
        String hKey = "tier" + tier + "Hours";
        String mKey = "tier" + tier + "Min";
        if (body.containsKey(hKey)) {
            int h = clampHour(parseInt(body.get(hKey)));
            switch (tier) {
                case 1 -> s.setTier1Hours(h);
                case 2 -> s.setTier2Hours(h);
                case 3 -> s.setTier3Hours(h);
                case 4 -> s.setTier4Hours(h);
                default -> { }
            }
        }
        if (body.containsKey(mKey)) {
            int min = clampMin(parseInt(body.get(mKey)));
            switch (tier) {
                case 1 -> s.setTier1Min(min);
                case 2 -> s.setTier2Min(min);
                case 3 -> s.setTier3Min(min);
                case 4 -> s.setTier4Min(min);
                default -> { }
            }
        }
    }

    public static String formatDuration(int totalMinutes) {
        if (totalMinutes <= 0) {
            return "0분";
        }
        int h = totalMinutes / 60;
        int m = totalMinutes % 60;
        if (h > 0 && m > 0) {
            return h + "시간 " + m + "분";
        }
        if (h > 0) {
            return h + "시간";
        }
        return m + "분";
    }

    public static String tierLabel(int tier) {
        return switch (clampTier(tier, 4)) {
            case 1 -> "1차";
            case 2 -> "2차";
            case 3 -> "3차";
            default -> "4차";
        };
    }

    private static int[] zeroTiers() {
        return new int[]{0, 0, 0, 0};
    }

    private static int toTotalMinutes(Integer hours, Integer minutes, int defaultMin) {
        int h = hours != null ? Math.max(0, Math.min(hours, 168)) : 0;
        int m = minutes != null ? Math.max(0, Math.min(minutes, 59)) : defaultMin;
        if (hours == null && minutes == null) {
            return defaultMin;
        }
        return h * 60 + m;
    }

    private static String normalizeMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return MODE_DISABLED;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (MODE_CUSTOM.equals(u) || MODE_DISABLED.equals(u)) {
            return u;
        }
        return MODE_FOLLOW_HQ;
    }

    private static int clampTier(Integer v, int def) {
        int n = v != null ? v : def;
        if (n < 1) {
            return 1;
        }
        return Math.min(n, 4);
    }

    private static int clampHour(int v) {
        return Math.max(0, Math.min(v, 168));
    }

    private static int clampMin(int v) {
        return Math.max(0, Math.min(v, 59));
    }

    private static int parseInt(Object o) {
        if (o == null) {
            return 0;
        }
        try {
            return Integer.parseInt(o.toString().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static Integer parseIntObj(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int intOr(Integer v, int def) {
        return v != null ? v : def;
    }

    /** 가맹점 추적기간 기간정책: 저장값이 없으면(레거시) 추적기간 mode로 추론 */
    private String effectiveTrackPolicy(MerchantProfile mp) {
        String norm = normalizeTrackPolicy(mp.getCardRiskTrackPeriodPolicy());
        if (norm != null) {
            return norm;
        }
        String mode = CardRiskTrackPeriod.normalizeMode(mp.getCardRiskTrackPeriodMode());
        return CardRiskTrackPeriod.MODE_NONE.equals(mode) ? TRACK_POLICY_NONE : TRACK_POLICY_CUSTOM;
    }

    private static String normalizeTrackPolicy(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (TRACK_POLICY_FOLLOW_HQ.equals(u) || TRACK_POLICY_CUSTOM.equals(u)) {
            return u;
        }
        return TRACK_POLICY_NONE;
    }

    private static String normalizeTrackPolicyForSave(String raw) {
        String n = normalizeTrackPolicy(raw);
        return n != null ? n : TRACK_POLICY_NONE;
    }

    private static int parseTrackPeriodValue(Object o) {
        if (o == null || o.toString().isBlank()) {
            return 0;
        }
        try {
            return Math.max(0, Math.min(Integer.parseInt(o.toString().trim()), 9999));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String yn(String v) {
        return v != null && "N".equalsIgnoreCase(v.trim()) ? "N" : "Y";
    }

    private static String parseYn(Object o, String def) {
        if (o == null) {
            return def;
        }
        return "N".equalsIgnoreCase(o.toString().trim()) ? "N" : "Y";
    }
}
