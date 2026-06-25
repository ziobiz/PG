package com.pg.util;

import com.pg.entity.MerchantProfile;
import com.pg.entity.PgTrnsctn;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 결제내역·통합조회 그리드 통화 표시 — 가맹 {@link MerchantProfile#getBaseCurrency()} 와
 * {@link PgTrnsctn#getCurType()} 보강 규칙(약한 KRW/410 기본값 치환).
 */
public final class MerchantDisplayCurrencyResolver {

    private MerchantDisplayCurrencyResolver() {
    }

    public static boolean looksLikeWeakDefaultKrw(String c) {
        return c == null || c.isEmpty() || "KRW".equals(c) || "410".equals(c);
    }

    public static String resolveCurrencyCodeForDisplay(PgTrnsctn t, MerchantProfile mp) {
        if (t == null) {
            return primaryFromMerchantProfile(mp);
        }
        String db = t.getCurType() != null ? t.getCurType().trim().toUpperCase(Locale.ROOT) : "";
        String assembled;
        if (!looksLikeWeakDefaultKrw(db)) {
            assembled = db.isEmpty() ? "" : db;
        } else {
            assembled = assembleFromMerchantProfile(mp, db);
        }
        return normalizeDisplayCurrency(assembled);
    }

    /**
     * JPAY 통합조회 — 1차 포털 Export 통화, 부족 시 업체관리(가맹·총판) 기준통화·결제내역 보강.
     * 약한 KRW(노티 기본값)만으로는 표시하지 않는다.
     */
    public static String resolveJpayRowCurrency(String portalCurrency,
                                                String originalCurrency,
                                                PgTrnsctn txn,
                                                MerchantProfile merchantMp,
                                                MerchantProfile masterDistMp) {
        String portal = normalizeToken(portalCurrency);
        if (portal.isBlank()) {
            portal = normalizeToken(originalCurrency);
        }
        if (!portal.isBlank() && !looksLikeWeakDefaultKrw(portal)) {
            return portal;
        }
        MerchantProfile policyMp = merchantMp != null ? merchantMp : masterDistMp;
        if (txn != null) {
            String fromTxn = resolveCurrencyCodeForDisplay(txn, policyMp);
            if (!fromTxn.isBlank() && !looksLikeWeakDefaultKrw(fromTxn)) {
                return fromTxn;
            }
        }
        String fromMerchant = primaryFromMerchantProfile(merchantMp);
        if (!fromMerchant.isBlank()) {
            return fromMerchant;
        }
        String fromMaster = primaryFromMerchantProfile(masterDistMp);
        if (!fromMaster.isBlank()) {
            return fromMaster;
        }
        if (!portal.isBlank()) {
            return portal;
        }
        if (txn != null) {
            String last = resolveCurrencyCodeForDisplay(txn, policyMp);
            if (!last.isBlank()) {
                return last;
            }
        }
        return "";
    }

    /** 업체관리 목록 baseCurrency 와 동일 토큰(총판 프로필 기준) */
    public static String primaryFromMerchantProfile(MerchantProfile mp) {
        if (mp == null || mp.getBaseCurrency() == null || mp.getBaseCurrency().isBlank()) {
            return "";
        }
        return normalizeDisplayCurrency(assembleFromMerchantProfile(mp, ""));
    }

    private static String assembleFromMerchantProfile(MerchantProfile mp, String weakDbFallback) {
        List<String> bases = parseBaseCurrencyTokens(mp);
        List<String> nonKrwBases = new ArrayList<>();
        for (String b : bases) {
            if (!looksLikeWeakDefaultKrw(b)) {
                nonKrwBases.add(b);
            }
        }
        if (nonKrwBases.size() == 1) {
            return nonKrwBases.get(0);
        }
        if (nonKrwBases.size() >= 2) {
            nonKrwBases.sort(String::compareTo);
            return String.join("/", nonKrwBases);
        }
        if (bases.size() == 1) {
            return bases.get(0);
        }
        return weakDbFallback != null ? weakDbFallback : "";
    }

    private static String normalizeToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return PayListStatusBarBuckets.normalizeCurrency(raw.trim());
    }

    private static String normalizeDisplayCurrency(String assembled) {
        if (assembled == null || assembled.isBlank()) {
            return "";
        }
        if (assembled.contains("/")) {
            List<String> parts = new ArrayList<>();
            for (String p : assembled.split("/")) {
                if (p != null && !p.isBlank()) {
                    parts.add(PayListStatusBarBuckets.normalizeCurrency(p.trim()));
                }
            }
            return parts.isEmpty() ? "" : String.join("/", parts);
        }
        return PayListStatusBarBuckets.normalizeCurrency(assembled);
    }

    private static List<String> parseBaseCurrencyTokens(MerchantProfile mp) {
        if (mp == null || mp.getBaseCurrency() == null || mp.getBaseCurrency().isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String p : mp.getBaseCurrency().split(",")) {
            String s = p != null ? p.trim().toUpperCase(Locale.ROOT) : "";
            if (!s.isEmpty()) {
                out.add(PayListStatusBarBuckets.normalizeCurrency(s));
            }
        }
        return out;
    }
}
