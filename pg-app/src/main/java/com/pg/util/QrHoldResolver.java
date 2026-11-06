package com.pg.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.CommissionPolicy;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.CommissionService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * 가맹 QR 보증금(롤링 보류율%·보류일수).
 * 본사정책 따름(Y) → QR 수수료정책({@code DEFAULT_QR} 등)의 rollingPct/Days (기본 10%·180일).
 * 직접입력(N) → regionalSettings {@code qrHoldRate}/{@code qrHoldDays}.
 * FIAT 보류율·CRYPTO 보증금과 분리.
 */
@Component
public class QrHoldResolver {

    public static final BigDecimal DEFAULT_PCT = new BigDecimal("10");
    public static final int DEFAULT_DAYS = 180;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final CommissionService commissionService;

    public QrHoldResolver(OrgUnitRepository orgUnitRepository,
                          MerchantProfileRepository merchantProfileRepository,
                          CommissionService commissionService) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.commissionService = commissionService;
    }

    public Effective resolve(String merchantCode) {
        String mid = merchantCode != null ? merchantCode.trim() : "";
        if (mid.isEmpty()) {
            return new Effective(true, DEFAULT_PCT, DEFAULT_DAYS);
        }
        Optional<OrgUnit> ouOpt = orgUnitRepository.findByCode(mid)
                .or(() -> orgUnitRepository.findByCodeIgnoreCase(mid));
        if (ouOpt.isEmpty() || ouOpt.get().getOrgLevel() != OrgLevel.MERCHANT) {
            return fromQrPolicy(mid);
        }
        Optional<MerchantProfile> mpOpt = merchantProfileRepository.findByOrgUnitId(ouOpt.get().getId());
        if (mpOpt.isEmpty()) {
            return fromQrPolicy(mid);
        }
        Map<String, Object> rs = parseRs(mpOpt.get().getRegionalSettings());
        String follow = rs.get("commissionQrHoldFollowHq") != null
                ? String.valueOf(rs.get("commissionQrHoldFollowHq")).trim() : "Y";
        if ("N".equalsIgnoreCase(follow)) {
            BigDecimal pct = parsePct(rs.get("qrHoldRate"));
            int days = parseDays(rs.get("qrHoldDays"));
            return new Effective(false, pct, days);
        }
        return fromQrPolicy(mid);
    }

    private Effective fromQrPolicy(String merchantCode) {
        CommissionPolicy pol = null;
        try {
            pol = commissionService.resolveCommissionPolicyForSettlement(merchantCode, CommissionFeeChannel.QR);
        } catch (Exception ignored) {
            pol = null;
        }
        BigDecimal pct = DEFAULT_PCT;
        int days = DEFAULT_DAYS;
        if (pol != null) {
            if (pol.getRollingPct() != null) {
                pct = pol.getRollingPct();
            }
            if (pol.getRollingDays() != null) {
                days = Math.max(0, pol.getRollingDays());
            }
        }
        if (pct.compareTo(BigDecimal.ZERO) < 0) {
            pct = BigDecimal.ZERO;
        }
        if (pct.compareTo(new BigDecimal("100")) > 0) {
            pct = new BigDecimal("100");
        }
        return new Effective(true, pct, days);
    }

    private static Map<String, Object> parseRs(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static BigDecimal parsePct(Object raw) {
        if (raw == null) {
            return DEFAULT_PCT;
        }
        String s = String.valueOf(raw).trim();
        if (s.isEmpty()) {
            return DEFAULT_PCT;
        }
        try {
            BigDecimal p = PercentDecimalHelper.parsePercentOneDecimal(s);
            if (p.compareTo(BigDecimal.ZERO) < 0) {
                return BigDecimal.ZERO;
            }
            if (p.compareTo(new BigDecimal("100")) > 0) {
                return new BigDecimal("100");
            }
            return p;
        } catch (Exception e) {
            return DEFAULT_PCT;
        }
    }

    private static int parseDays(Object raw) {
        if (raw == null) {
            return DEFAULT_DAYS;
        }
        String s = String.valueOf(raw).trim();
        if (s.isEmpty()) {
            return DEFAULT_DAYS;
        }
        try {
            return Math.max(0, Integer.parseInt(s));
        } catch (Exception e) {
            return DEFAULT_DAYS;
        }
    }

    public record Effective(boolean followHq, BigDecimal rollingPct, int rollingDays) {
        public String pctPlain() {
            return PercentDecimalHelper.toPlainOneDecimal(rollingPct != null ? rollingPct : BigDecimal.ZERO);
        }
    }
}
