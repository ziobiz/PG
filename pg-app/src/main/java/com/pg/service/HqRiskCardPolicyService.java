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

    /**
     * 가맹 위험관리(트리거) 실효값.
     * 업체정보 「리스크 위험관리트리거」또는 본사 「가맹점 리스크 현황」에서 저장한
     * {@code cardRiskPolicyMode=CUSTOM} 은 본사 리스크설정 기본값보다 항상 우선한다.
     * (두 UI는 동일 MerchantProfile 필드를 공유한다.)
     */
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

    /**
     * 가맹 사전 리스크 필터 실효값.
     * 업체정보 「리스크 사전필터트리거」또는 본사 「가맹점 리스크 필터링」에서 저장한
     * {@code cardRiskPresaleMode=CUSTOM} 은 본사 리스크 필터링 기본값보다 항상 우선한다.
     * DISABLED 는 해당 가맹만 OFF. FOLLOW_HQ 는 본사 기본.
     * 단, 본사 사전필터 마스터({@code presaleFilterEnabledYn})가 OFF 이면 CUSTOM 이어도 전체 OFF.
     */
    public PresaleRiskFilterEffective resolvePresaleForOrgUnit(Long orgUnitId) {
        HqRiskCardPolicy hq = getOrCreate();
        if (orgUnitId == null) {
            return resolvePresaleHqOnly(hq);
        }
        Optional<MerchantProfile> mpOpt = merchantProfileRepository.findByOrgUnitId(orgUnitId);
        if (mpOpt.isEmpty()) {
            return resolvePresaleHqOnly(hq);
        }
        MerchantProfile mp = mpOpt.get();
        String mode = normalizePresaleMode(mp.getCardRiskPresaleMode());
        if (MODE_DISABLED.equals(mode)) {
            return PresaleRiskFilterEffective.disabled(MODE_DISABLED);
        }
        if (MODE_CUSTOM.equals(mode)) {
            return buildPresaleCustom(hq, mp);
        }
        return resolvePresaleHqOnly(hq);
    }

    public PresaleRiskFilterEffective resolvePresaleHqOnly() {
        return resolvePresaleHqOnly(getOrCreate());
    }

    private PresaleRiskFilterEffective resolvePresaleHqOnly(HqRiskCardPolicy hq) {
        if (!"Y".equalsIgnoreCase(yn(hq.getPresaleFilterEnabledYn()))) {
            return PresaleRiskFilterEffective.disabled(MODE_FOLLOW_HQ);
        }
        int cardWin = intOr(hq.getVelocityCardWindowMinutes(), intOr(hq.getVelocityWindowMinutes(), 10));
        int cardMax = intOr(hq.getVelocityCardMaxAttempts(), intOr(hq.getVelocityMaxAttempts(), 3));
        return new PresaleRiskFilterEffective(
                true,
                MODE_FOLLOW_HQ,
                yn(hq.getFilterBuyerContactMismatchYn()),
                yn(hq.getFilterHolderNameYn()),
                yn(hq.getFilterPhoneInvalidYn()),
                yn(hq.getFilterEmailInvalidYn()),
                yn(hq.getFilterVelocityCardYn()),
                yn(hq.getFilterVelocityEmailYn()),
                yn(hq.getFilterVelocityIpYn()),
                cardWin,
                cardMax,
                intOr(hq.getVelocityEmailWindowMinutes(), 30),
                intOr(hq.getVelocityEmailMaxAttempts(), 5),
                intOr(hq.getVelocityIpWindowMinutes(), 15),
                intOr(hq.getVelocityIpMaxAttempts(), 10)
        );
    }

    private PresaleRiskFilterEffective buildPresaleCustom(HqRiskCardPolicy hq, MerchantProfile mp) {
        PresaleRiskFilterEffective base = resolvePresaleHqOnly(hq);
        /* CUSTOM 이어도 본사 사전필터 마스터가 OFF 이면 전체 OFF */
        if (!base.enabled()) {
            return PresaleRiskFilterEffective.disabled(MODE_CUSTOM);
        }
        return new PresaleRiskFilterEffective(
                true,
                MODE_CUSTOM,
                ynOr(mp.getCardRiskPresaleBuyerMismatchYn(), base.filterBuyerContactMismatchYn()),
                ynOr(mp.getCardRiskPresaleHolderNameYn(), base.filterHolderNameYn()),
                ynOr(mp.getCardRiskPresalePhoneInvalidYn(), base.filterPhoneInvalidYn()),
                ynOr(mp.getCardRiskPresaleEmailInvalidYn(), base.filterEmailInvalidYn()),
                ynOr(mp.getCardRiskPresaleVelocityCardYn(), base.filterVelocityCardYn()),
                ynOr(mp.getCardRiskPresaleVelocityEmailYn(), base.filterVelocityEmailYn()),
                ynOr(mp.getCardRiskPresaleVelocityIpYn(), base.filterVelocityIpYn()),
                intOr(mp.getCardRiskPresaleVelCardWinMin(), base.velocityCardWindowMinutes()),
                intOr(mp.getCardRiskPresaleVelCardMax(), base.velocityCardMaxAttempts()),
                intOr(mp.getCardRiskPresaleVelEmailWinMin(), base.velocityEmailWindowMinutes()),
                intOr(mp.getCardRiskPresaleVelEmailMax(), base.velocityEmailMaxAttempts()),
                intOr(mp.getCardRiskPresaleVelIpWinMin(), base.velocityIpWindowMinutes()),
                intOr(mp.getCardRiskPresaleVelIpMax(), base.velocityIpMaxAttempts())
        );
    }

    public List<Map<String, Object>> listActiveMerchantRows() {
        List<OrgUnit> merchants = orgUnitRepository.findByOrgLevelOrderByCodeAsc(OrgLevel.MERCHANT);
        List<Map<String, Object>> out = new ArrayList<>();
        HqRiskCardPolicy hq = getOrCreate();
        PresaleRiskFilterEffective hqPresale = resolvePresaleHqOnly(hq);
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
            row.put("cardRiskTier1Hours", mp.getCardRiskTier1Hours());
            row.put("cardRiskTier1Min", mp.getCardRiskTier1Min());
            row.put("cardRiskTier2Hours", mp.getCardRiskTier2Hours());
            row.put("cardRiskTier2Min", mp.getCardRiskTier2Min());
            row.put("cardRiskTier3Hours", mp.getCardRiskTier3Hours());
            row.put("cardRiskTier3Min", mp.getCardRiskTier3Min());
            row.put("cardRiskTier4Hours", mp.getCardRiskTier4Hours());
            row.put("cardRiskTier4Min", mp.getCardRiskTier4Min());
            row.put("cardRiskAutoBlacklistTier", mp.getCardRiskAutoBlacklistTier());
            row.put("cardRiskTrackPeriodPolicy", effectiveTrackPolicy(mp));
            row.put("cardRiskTrackPeriodMode", CardRiskTrackPeriod.normalizeMode(mp.getCardRiskTrackPeriodMode()));
            row.put("cardRiskTrackPeriodValue", mp.getCardRiskTrackPeriodValue());
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

            String presaleMode = normalizePresaleMode(mp.getCardRiskPresaleMode());
            PresaleRiskFilterEffective pEff = resolvePresaleForOrgUnit(ou.getId());
            row.put("presaleMode", presaleMode);
            row.put("presaleEffectiveEnabled", pEff.enabled());
            row.put("presaleEffectiveSource", pEff.policySource());
            row.put("presaleStatusLabel", presaleStatusLabel(presaleMode, pEff, hqPresale));
            putPresaleStoredFields(row, mp, hqPresale);
            putPresaleEffectiveFields(row, pEff);
            out.add(row);
        }
        return out;
    }

    private void putPresaleStoredFields(Map<String, Object> row, MerchantProfile mp, PresaleRiskFilterEffective hq) {
        row.put("cardRiskPresaleMode", normalizePresaleMode(mp.getCardRiskPresaleMode()));
        row.put("cardRiskPresaleBuyerMismatchYn", ynOr(mp.getCardRiskPresaleBuyerMismatchYn(), hq.filterBuyerContactMismatchYn()));
        row.put("cardRiskPresaleHolderNameYn", ynOr(mp.getCardRiskPresaleHolderNameYn(), hq.filterHolderNameYn()));
        row.put("cardRiskPresalePhoneInvalidYn", ynOr(mp.getCardRiskPresalePhoneInvalidYn(), hq.filterPhoneInvalidYn()));
        row.put("cardRiskPresaleEmailInvalidYn", ynOr(mp.getCardRiskPresaleEmailInvalidYn(), hq.filterEmailInvalidYn()));
        row.put("cardRiskPresaleVelocityCardYn", ynOr(mp.getCardRiskPresaleVelocityCardYn(), hq.filterVelocityCardYn()));
        row.put("cardRiskPresaleVelocityEmailYn", ynOr(mp.getCardRiskPresaleVelocityEmailYn(), hq.filterVelocityEmailYn()));
        row.put("cardRiskPresaleVelocityIpYn", ynOr(mp.getCardRiskPresaleVelocityIpYn(), hq.filterVelocityIpYn()));
        row.put("cardRiskPresaleVelCardWinMin", intOr(mp.getCardRiskPresaleVelCardWinMin(), hq.velocityCardWindowMinutes()));
        row.put("cardRiskPresaleVelCardMax", intOr(mp.getCardRiskPresaleVelCardMax(), hq.velocityCardMaxAttempts()));
        row.put("cardRiskPresaleVelEmailWinMin", intOr(mp.getCardRiskPresaleVelEmailWinMin(), hq.velocityEmailWindowMinutes()));
        row.put("cardRiskPresaleVelEmailMax", intOr(mp.getCardRiskPresaleVelEmailMax(), hq.velocityEmailMaxAttempts()));
        row.put("cardRiskPresaleVelIpWinMin", intOr(mp.getCardRiskPresaleVelIpWinMin(), hq.velocityIpWindowMinutes()));
        row.put("cardRiskPresaleVelIpMax", intOr(mp.getCardRiskPresaleVelIpMax(), hq.velocityIpMaxAttempts()));
    }

    private void putPresaleEffectiveFields(Map<String, Object> row, PresaleRiskFilterEffective pEff) {
        row.put("effFilterBuyerContactMismatchYn", pEff.filterBuyerContactMismatchYn());
        row.put("effFilterHolderNameYn", pEff.filterHolderNameYn());
        row.put("effFilterPhoneInvalidYn", pEff.filterPhoneInvalidYn());
        row.put("effFilterEmailInvalidYn", pEff.filterEmailInvalidYn());
        row.put("effFilterVelocityCardYn", pEff.filterVelocityCardYn());
        row.put("effFilterVelocityEmailYn", pEff.filterVelocityEmailYn());
        row.put("effFilterVelocityIpYn", pEff.filterVelocityIpYn());
        row.put("effVelocityCardWindowMinutes", pEff.velocityCardWindowMinutes());
        row.put("effVelocityCardMaxAttempts", pEff.velocityCardMaxAttempts());
        row.put("effVelocityEmailWindowMinutes", pEff.velocityEmailWindowMinutes());
        row.put("effVelocityEmailMaxAttempts", pEff.velocityEmailMaxAttempts());
        row.put("effVelocityIpWindowMinutes", pEff.velocityIpWindowMinutes());
        row.put("effVelocityIpMaxAttempts", pEff.velocityIpMaxAttempts());
    }

    private static String presaleStatusLabel(String mode, PresaleRiskFilterEffective pEff, PresaleRiskFilterEffective hq) {
        if (MODE_DISABLED.equals(mode)) {
            return "미사용";
        }
        if (!pEff.enabled()) {
            return "본사 사전필터 미사용";
        }
        if (MODE_CUSTOM.equals(mode)) {
            return "별도설정";
        }
        return hq.enabled() ? "본사설정" : "본사 사전필터 미사용";
    }

    /**
     * 본사 「가맹점 리스크 현황」에서 가맹별 트리거·사전필터 저장.
     */
    @Transactional
    public Map<String, Object> saveMerchantRiskRow(Map<String, Object> body) {
        if (body == null || body.get("orgUnitId") == null) {
            throw new IllegalArgumentException("orgUnitId required");
        }
        Long orgUnitId;
        try {
            orgUnitId = Long.parseLong(body.get("orgUnitId").toString().trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("orgUnitId invalid");
        }
        MerchantProfile mp = merchantProfileRepository.findByOrgUnitId(orgUnitId)
                .orElseThrow(() -> new IllegalArgumentException("merchant not found"));
        if (body.containsKey("cardRiskPolicyMode") || body.containsKey("policyMode")) {
            Object raw = body.containsKey("cardRiskPolicyMode") ? body.get("cardRiskPolicyMode") : body.get("policyMode");
            mp.setCardRiskPolicyMode(normalizeMode(raw != null ? raw.toString() : null));
        }
        Map<String, String> triggerFields = new LinkedHashMap<>();
        putIfPresent(triggerFields, body, "cardRiskTier1Hours");
        putIfPresent(triggerFields, body, "cardRiskTier1Min");
        putIfPresent(triggerFields, body, "cardRiskTier2Hours");
        putIfPresent(triggerFields, body, "cardRiskTier2Min");
        putIfPresent(triggerFields, body, "cardRiskTier3Hours");
        putIfPresent(triggerFields, body, "cardRiskTier3Min");
        putIfPresent(triggerFields, body, "cardRiskTier4Hours");
        putIfPresent(triggerFields, body, "cardRiskTier4Min");
        putIfPresent(triggerFields, body, "cardRiskAutoBlacklistTier");
        putIfPresent(triggerFields, body, "cardRiskTrackPeriodPolicy");
        putIfPresent(triggerFields, body, "cardRiskTrackPeriodMode");
        putIfPresent(triggerFields, body, "cardRiskTrackPeriodValue");
        if (!triggerFields.isEmpty()) {
            applyMerchantCardRiskFromRequest(mp, triggerFields);
        }
        /* CUSTOM 전환 시 본사 값 시드(미입력 필드) */
        if (MODE_CUSTOM.equals(normalizeMode(mp.getCardRiskPolicyMode()))) {
            seedTriggerFromHqIfBlank(mp);
        }
        if (body.containsKey("cardRiskPresaleMode") || body.containsKey("presaleMode")) {
            Object raw = body.containsKey("cardRiskPresaleMode") ? body.get("cardRiskPresaleMode") : body.get("presaleMode");
            mp.setCardRiskPresaleMode(normalizePresaleMode(raw != null ? raw.toString() : null));
        }
        applyMerchantPresaleFromBody(mp, body);
        if (MODE_CUSTOM.equals(normalizePresaleMode(mp.getCardRiskPresaleMode()))) {
            seedPresaleFromHqIfBlank(mp);
        }
        merchantProfileRepository.save(mp);
        return listActiveMerchantRows().stream()
                .filter(r -> orgUnitId.equals(r.get("orgUnitId")))
                .findFirst()
                .orElseGet(LinkedHashMap::new);
    }

    private void seedTriggerFromHqIfBlank(MerchantProfile mp) {
        HqRiskCardPolicy hq = getOrCreate();
        if (mp.getCardRiskTier1Hours() == null && mp.getCardRiskTier1Min() == null) {
            mp.setCardRiskTier1Hours(intOr(hq.getTier1Hours(), 0));
            mp.setCardRiskTier1Min(intOr(hq.getTier1Min(), 5));
        }
        if (mp.getCardRiskTier2Hours() == null && mp.getCardRiskTier2Min() == null) {
            mp.setCardRiskTier2Hours(intOr(hq.getTier2Hours(), 0));
            mp.setCardRiskTier2Min(intOr(hq.getTier2Min(), 10));
        }
        if (mp.getCardRiskTier3Hours() == null && mp.getCardRiskTier3Min() == null) {
            mp.setCardRiskTier3Hours(intOr(hq.getTier3Hours(), 1));
            mp.setCardRiskTier3Min(intOr(hq.getTier3Min(), 0));
        }
        if (mp.getCardRiskTier4Hours() == null && mp.getCardRiskTier4Min() == null) {
            mp.setCardRiskTier4Hours(intOr(hq.getTier4Hours(), 0));
            mp.setCardRiskTier4Min(intOr(hq.getTier4Min(), 0));
        }
        if (mp.getCardRiskAutoBlacklistTier() == null) {
            mp.setCardRiskAutoBlacklistTier(clampTier(hq.getAutoBlacklistTriggerTier(), 4));
        }
        if (mp.getCardRiskTrackPeriodPolicy() == null || mp.getCardRiskTrackPeriodPolicy().isBlank()) {
            mp.setCardRiskTrackPeriodPolicy(TRACK_POLICY_FOLLOW_HQ);
        }
    }

    private void seedPresaleFromHqIfBlank(MerchantProfile mp) {
        PresaleRiskFilterEffective hq = resolvePresaleHqOnly();
        if (mp.getCardRiskPresaleBuyerMismatchYn() == null) {
            mp.setCardRiskPresaleBuyerMismatchYn(hq.filterBuyerContactMismatchYn());
        }
        if (mp.getCardRiskPresaleHolderNameYn() == null) {
            mp.setCardRiskPresaleHolderNameYn(hq.filterHolderNameYn());
        }
        if (mp.getCardRiskPresalePhoneInvalidYn() == null) {
            mp.setCardRiskPresalePhoneInvalidYn(hq.filterPhoneInvalidYn());
        }
        if (mp.getCardRiskPresaleEmailInvalidYn() == null) {
            mp.setCardRiskPresaleEmailInvalidYn(hq.filterEmailInvalidYn());
        }
        if (mp.getCardRiskPresaleVelocityCardYn() == null) {
            mp.setCardRiskPresaleVelocityCardYn(hq.filterVelocityCardYn());
        }
        if (mp.getCardRiskPresaleVelocityEmailYn() == null) {
            mp.setCardRiskPresaleVelocityEmailYn(hq.filterVelocityEmailYn());
        }
        if (mp.getCardRiskPresaleVelocityIpYn() == null) {
            mp.setCardRiskPresaleVelocityIpYn(hq.filterVelocityIpYn());
        }
        if (mp.getCardRiskPresaleVelCardWinMin() == null) {
            mp.setCardRiskPresaleVelCardWinMin(hq.velocityCardWindowMinutes());
        }
        if (mp.getCardRiskPresaleVelCardMax() == null) {
            mp.setCardRiskPresaleVelCardMax(hq.velocityCardMaxAttempts());
        }
        if (mp.getCardRiskPresaleVelEmailWinMin() == null) {
            mp.setCardRiskPresaleVelEmailWinMin(hq.velocityEmailWindowMinutes());
        }
        if (mp.getCardRiskPresaleVelEmailMax() == null) {
            mp.setCardRiskPresaleVelEmailMax(hq.velocityEmailMaxAttempts());
        }
        if (mp.getCardRiskPresaleVelIpWinMin() == null) {
            mp.setCardRiskPresaleVelIpWinMin(hq.velocityIpWindowMinutes());
        }
        if (mp.getCardRiskPresaleVelIpMax() == null) {
            mp.setCardRiskPresaleVelIpMax(hq.velocityIpMaxAttempts());
        }
    }

    private void applyMerchantPresaleFromBody(MerchantProfile mp, Map<String, Object> body) {
        if (body.containsKey("cardRiskPresaleBuyerMismatchYn") || body.containsKey("filterBuyerContactMismatchYn")) {
            Object v = body.containsKey("cardRiskPresaleBuyerMismatchYn")
                    ? body.get("cardRiskPresaleBuyerMismatchYn") : body.get("filterBuyerContactMismatchYn");
            mp.setCardRiskPresaleBuyerMismatchYn(parseYn(v, "Y"));
        }
        if (body.containsKey("cardRiskPresaleHolderNameYn") || body.containsKey("filterHolderNameYn")) {
            Object v = body.containsKey("cardRiskPresaleHolderNameYn")
                    ? body.get("cardRiskPresaleHolderNameYn") : body.get("filterHolderNameYn");
            mp.setCardRiskPresaleHolderNameYn(parseYn(v, "Y"));
        }
        if (body.containsKey("cardRiskPresalePhoneInvalidYn") || body.containsKey("filterPhoneInvalidYn")) {
            Object v = body.containsKey("cardRiskPresalePhoneInvalidYn")
                    ? body.get("cardRiskPresalePhoneInvalidYn") : body.get("filterPhoneInvalidYn");
            mp.setCardRiskPresalePhoneInvalidYn(parseYn(v, "Y"));
        }
        if (body.containsKey("cardRiskPresaleEmailInvalidYn") || body.containsKey("filterEmailInvalidYn")) {
            Object v = body.containsKey("cardRiskPresaleEmailInvalidYn")
                    ? body.get("cardRiskPresaleEmailInvalidYn") : body.get("filterEmailInvalidYn");
            mp.setCardRiskPresaleEmailInvalidYn(parseYn(v, "Y"));
        }
        if (body.containsKey("cardRiskPresaleVelocityCardYn") || body.containsKey("filterVelocityCardYn")) {
            Object v = body.containsKey("cardRiskPresaleVelocityCardYn")
                    ? body.get("cardRiskPresaleVelocityCardYn") : body.get("filterVelocityCardYn");
            mp.setCardRiskPresaleVelocityCardYn(parseYn(v, "Y"));
        }
        if (body.containsKey("cardRiskPresaleVelocityEmailYn") || body.containsKey("filterVelocityEmailYn")) {
            Object v = body.containsKey("cardRiskPresaleVelocityEmailYn")
                    ? body.get("cardRiskPresaleVelocityEmailYn") : body.get("filterVelocityEmailYn");
            mp.setCardRiskPresaleVelocityEmailYn(parseYn(v, "Y"));
        }
        if (body.containsKey("cardRiskPresaleVelocityIpYn") || body.containsKey("filterVelocityIpYn")) {
            Object v = body.containsKey("cardRiskPresaleVelocityIpYn")
                    ? body.get("cardRiskPresaleVelocityIpYn") : body.get("filterVelocityIpYn");
            mp.setCardRiskPresaleVelocityIpYn(parseYn(v, "Y"));
        }
        if (body.containsKey("cardRiskPresaleVelCardWinMin") || body.containsKey("velocityCardWindowMinutes")) {
            Object v = body.containsKey("cardRiskPresaleVelCardWinMin")
                    ? body.get("cardRiskPresaleVelCardWinMin") : body.get("velocityCardWindowMinutes");
            mp.setCardRiskPresaleVelCardWinMin(Math.min(1440, Math.max(1, parseIntOr(v, 10))));
        }
        if (body.containsKey("cardRiskPresaleVelCardMax") || body.containsKey("velocityCardMaxAttempts")) {
            Object v = body.containsKey("cardRiskPresaleVelCardMax")
                    ? body.get("cardRiskPresaleVelCardMax") : body.get("velocityCardMaxAttempts");
            mp.setCardRiskPresaleVelCardMax(Math.min(99, Math.max(1, parseIntOr(v, 3))));
        }
        if (body.containsKey("cardRiskPresaleVelEmailWinMin") || body.containsKey("velocityEmailWindowMinutes")) {
            Object v = body.containsKey("cardRiskPresaleVelEmailWinMin")
                    ? body.get("cardRiskPresaleVelEmailWinMin") : body.get("velocityEmailWindowMinutes");
            mp.setCardRiskPresaleVelEmailWinMin(Math.min(1440, Math.max(1, parseIntOr(v, 30))));
        }
        if (body.containsKey("cardRiskPresaleVelEmailMax") || body.containsKey("velocityEmailMaxAttempts")) {
            Object v = body.containsKey("cardRiskPresaleVelEmailMax")
                    ? body.get("cardRiskPresaleVelEmailMax") : body.get("velocityEmailMaxAttempts");
            mp.setCardRiskPresaleVelEmailMax(Math.min(99, Math.max(1, parseIntOr(v, 5))));
        }
        if (body.containsKey("cardRiskPresaleVelIpWinMin") || body.containsKey("velocityIpWindowMinutes")) {
            Object v = body.containsKey("cardRiskPresaleVelIpWinMin")
                    ? body.get("cardRiskPresaleVelIpWinMin") : body.get("velocityIpWindowMinutes");
            mp.setCardRiskPresaleVelIpWinMin(Math.min(1440, Math.max(1, parseIntOr(v, 15))));
        }
        if (body.containsKey("cardRiskPresaleVelIpMax") || body.containsKey("velocityIpMaxAttempts")) {
            Object v = body.containsKey("cardRiskPresaleVelIpMax")
                    ? body.get("cardRiskPresaleVelIpMax") : body.get("velocityIpMaxAttempts");
            mp.setCardRiskPresaleVelIpMax(Math.min(99, Math.max(1, parseIntOr(v, 10))));
        }
    }

    private static void putIfPresent(Map<String, String> fields, Map<String, Object> body, String key) {
        if (body.containsKey(key) && body.get(key) != null) {
            fields.put(key, body.get(key).toString());
        }
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
        PresaleRiskFilterEffective hq = resolvePresaleHqOnly();
        String pMode = normalizePresaleMode(mp.getCardRiskPresaleMode());
        m.put("cardRiskPresaleMode", pMode);
        m.put("cardRiskPresaleBuyerMismatchYn", ynOr(mp.getCardRiskPresaleBuyerMismatchYn(), hq.filterBuyerContactMismatchYn()));
        m.put("cardRiskPresaleHolderNameYn", ynOr(mp.getCardRiskPresaleHolderNameYn(), hq.filterHolderNameYn()));
        m.put("cardRiskPresalePhoneInvalidYn", ynOr(mp.getCardRiskPresalePhoneInvalidYn(), hq.filterPhoneInvalidYn()));
        m.put("cardRiskPresaleEmailInvalidYn", ynOr(mp.getCardRiskPresaleEmailInvalidYn(), hq.filterEmailInvalidYn()));
        m.put("cardRiskPresaleVelocityCardYn", ynOr(mp.getCardRiskPresaleVelocityCardYn(), hq.filterVelocityCardYn()));
        m.put("cardRiskPresaleVelocityEmailYn", ynOr(mp.getCardRiskPresaleVelocityEmailYn(), hq.filterVelocityEmailYn()));
        m.put("cardRiskPresaleVelocityIpYn", ynOr(mp.getCardRiskPresaleVelocityIpYn(), hq.filterVelocityIpYn()));
        m.put("cardRiskPresaleVelCardWinMin", intOr(mp.getCardRiskPresaleVelCardWinMin(), hq.velocityCardWindowMinutes()));
        m.put("cardRiskPresaleVelCardMax", intOr(mp.getCardRiskPresaleVelCardMax(), hq.velocityCardMaxAttempts()));
        m.put("cardRiskPresaleVelEmailWinMin", intOr(mp.getCardRiskPresaleVelEmailWinMin(), hq.velocityEmailWindowMinutes()));
        m.put("cardRiskPresaleVelEmailMax", intOr(mp.getCardRiskPresaleVelEmailMax(), hq.velocityEmailMaxAttempts()));
        m.put("cardRiskPresaleVelIpWinMin", intOr(mp.getCardRiskPresaleVelIpWinMin(), hq.velocityIpWindowMinutes()));
        m.put("cardRiskPresaleVelIpMax", intOr(mp.getCardRiskPresaleVelIpMax(), hq.velocityIpMaxAttempts()));
    }

    /** 업체등록·업체정보 저장용 — Map&lt;String,String&gt; */
    public void applyMerchantPresaleRiskFromRequest(MerchantProfile mp, Map<String, String> fields) {
        if (mp == null || fields == null || fields.isEmpty()) {
            return;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        fields.forEach(body::put);
        if (body.containsKey("cardRiskPresaleMode")) {
            mp.setCardRiskPresaleMode(normalizePresaleMode(String.valueOf(body.get("cardRiskPresaleMode"))));
        }
        applyMerchantPresaleFromBody(mp, body);
        if (MODE_CUSTOM.equals(normalizePresaleMode(mp.getCardRiskPresaleMode()))) {
            seedPresaleFromHqIfBlank(mp);
        }
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

    /** 사전필터 기본은 본사 따름(FOLLOW_HQ). 트리거(기본 DISABLED)와 다름. */
    private static String normalizePresaleMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return MODE_FOLLOW_HQ;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (MODE_CUSTOM.equals(u) || MODE_DISABLED.equals(u)) {
            return u;
        }
        return MODE_FOLLOW_HQ;
    }

    private static String ynOr(String v, String def) {
        if (v == null || v.isBlank()) {
            return yn(def);
        }
        return yn(v);
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
