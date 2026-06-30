package com.pg.service.ops;

import com.pg.entity.PgAgencyCostPolicy;
import com.pg.integration.pg.PgVendor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 대행수수료 화면 — 거래 van·가맹 결제대행사 설정 PG코드·통화로 {@link PgAgencyCostPolicy} 를 찾는다.
 * 과거 거래도 저장 시점 정책이 아니라 <strong>현재</strong> 대행수수료설정으로 건별 재계산한다.
 */
public final class AgencyCostPolicyResolver {

    private final Map<String, PgAgencyCostPolicy> byKey;
    private final List<PgAgencyCostPolicy> activePolicies;

    private AgencyCostPolicyResolver(Map<String, PgAgencyCostPolicy> byKey, List<PgAgencyCostPolicy> activePolicies) {
        this.byKey = byKey;
        this.activePolicies = activePolicies;
    }

    public static AgencyCostPolicyResolver from(List<PgAgencyCostPolicy> policies) {
        Map<String, PgAgencyCostPolicy> byKey = new HashMap<>();
        List<PgAgencyCostPolicy> active = new ArrayList<>();
        if (policies != null) {
            for (PgAgencyCostPolicy p : policies) {
                if (p == null || p.getPgCd() == null || p.getPgCd().isBlank()) {
                    continue;
                }
                if (p.getUseYn() != null && "N".equalsIgnoreCase(p.getUseYn().trim())) {
                    continue;
                }
                active.add(p);
                String raw = p.getPgCd().trim().toUpperCase(Locale.ROOT);
                byKey.putIfAbsent(raw, p);
                String norm = PgVendor.normalizePgCdKey(p.getPgCd());
                if (!norm.isEmpty()) {
                    byKey.putIfAbsent(norm, p);
                }
            }
        }
        return new AgencyCostPolicyResolver(byKey, active);
    }

    /**
     * @param bindingPgCd 가맹 결제대행사 설정(tb_merchant_pg_binding) PG코드
     * @param vanKey      거래 van
     * @param currency    거래·정책 통화 (JPY 등)
     */
    public PgAgencyCostPolicy resolve(String bindingPgCd, String vanKey, String currency, Map<String, String> pgNmByCd) {
        PgAgencyCostPolicy hit = lookupKey(bindingPgCd);
        if (hit != null) {
            return hit;
        }
        hit = lookupKey(vanKey);
        if (hit != null) {
            return hit;
        }
        String probe = bindingPgCd != null && !bindingPgCd.isBlank() ? bindingPgCd.trim() : vanKey;
        if (probe == null || probe.isBlank()) {
            return null;
        }
        String probeNorm = PgVendor.normalizePgCdKey(probe);
        List<PgAgencyCostPolicy> family = new ArrayList<>();
        for (PgAgencyCostPolicy p : activePolicies) {
            if (cdMatchesFamily(p.getPgCd(), probe, probeNorm)) {
                family.add(p);
            }
        }
        if (family.isEmpty()) {
            return null;
        }
        String cur = currency != null ? currency.trim().toUpperCase(Locale.ROOT) : "";
        if (!cur.isEmpty()) {
            for (PgAgencyCostPolicy p : family) {
                if (policyMatchesCurrency(p, cur, pgNmByCd)) {
                    return p;
                }
            }
        }
        return family.get(0);
    }

    private PgAgencyCostPolicy lookupKey(String cd) {
        if (cd == null || cd.isBlank()) {
            return null;
        }
        PgAgencyCostPolicy p = byKey.get(cd.trim().toUpperCase(Locale.ROOT));
        if (p != null) {
            return p;
        }
        return byKey.get(PgVendor.normalizePgCdKey(cd));
    }

    private static boolean policyMatchesCurrency(PgAgencyCostPolicy p, String cur, Map<String, String> pgNmByCd) {
        String cdU = p.getPgCd().trim().toUpperCase(Locale.ROOT);
        String polCur = p.getCurrencyCode() != null ? p.getCurrencyCode().trim().toUpperCase(Locale.ROOT) : "";
        if (cur.equals(polCur) || cdU.contains(cur)) {
            return true;
        }
        if (pgNmByCd != null) {
            String nm = pgNmByCd.getOrDefault(cdU, "");
            if (nm != null && nm.toUpperCase(Locale.ROOT).contains(cur)) {
                return true;
            }
        }
        return false;
    }

    static boolean cdMatchesFamily(String policyPgCd, String probe, String probeNorm) {
        if (policyPgCd == null || policyPgCd.isBlank() || probe == null || probe.isBlank()) {
            return false;
        }
        String cd = policyPgCd.trim();
        if (cd.equalsIgnoreCase(probe)) {
            return true;
        }
        String cdNorm = PgVendor.normalizePgCdKey(cd);
        if (probeNorm.equals(cdNorm)) {
            return true;
        }
        if (PgVendor.isJpayFamily(probe) && PgVendor.isJpayFamily(cd)) {
            return true;
        }
        if (PgVendor.isChillPayFamily(probe) && PgVendor.isChillPayFamily(cd)) {
            return true;
        }
        return !probeNorm.isEmpty() && (cdNorm.startsWith(probeNorm + "_") || cdNorm.startsWith(probeNorm));
    }
}
