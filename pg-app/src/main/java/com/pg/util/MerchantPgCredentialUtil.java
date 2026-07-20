package com.pg.util;

import com.pg.entity.MerchantPgBinding;
import com.pg.entity.PgAgency;

/**
 * 가맹점 PG 바인딩 자격(MID·API Key·IV) 해석.
 * <ul>
 *   <li>MID + API Key <strong>둘 다</strong> 있으면 가맹 값이 본사({@link PgAgency})보다 우선</li>
 *   <li>하나만 있으면 불완전 → 본사 MID·API Key 사용</li>
 *   <li>둘 다 비면 본사 사용</li>
 *   <li>IV(또는 ChillPay MD5)는 선택: 가맹에 있으면 사용, 없으면 본사</li>
 * </ul>
 */
public final class MerchantPgCredentialUtil {

    private MerchantPgCredentialUtil() {}

    /** 결제·서명용으로 해석된 자격 */
    public record Resolved(String mid, String apiKey, String ivKey, boolean merchantMidKeyOverride) {}

    /** DB 저장용으로 정규화된 자격 (apiKey null = 본사 키 사용) */
    public record PersistCreds(String mid, String apiKey, String ivKey) {}

    public static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** API Key·MD5 등 — 원문 대신 앞 3자 + ***** */
    public static String maskSecretPreview(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String t = raw.trim();
        String head = t.length() <= 3 ? t : t.substring(0, 3);
        return head + "*****";
    }

    /** 화면 마스킹값·플레이스홀더인지 (원문으로 저장하면 안 됨) */
    public static boolean isMaskedSecretPreview(String s) {
        if (s == null || s.isBlank()) {
            return false;
        }
        String t = s.trim();
        if ("********".equals(t) || "••••••••••••".equals(t)) {
            return true;
        }
        return t.matches("^.{1,3}\\*{3,}$");
    }

    /**
     * 결제 시점: 바인딩 + 본사 행 → 실제 사용할 MID·API Key·IV.
     * {@code agency} 의 MD5는 IV 폴백으로 쓴다(ChillPay).
     */
    public static Resolved resolve(MerchantPgBinding binding, PgAgency agency) {
        String hqMid = agency != null ? trimToNull(agency.getMerchantMid()) : null;
        String hqKey = agency != null ? trimToNull(agency.getApiKey()) : null;
        String hqIv = agency != null ? trimToNull(agency.getMd5SecretKey()) : null;

        String bMid = binding != null ? trimToNull(binding.getMid()) : null;
        String bKey = binding != null ? trimToNull(binding.getApiKey()) : null;
        String bIv = binding != null ? trimToNull(binding.getIvKey()) : null;
        if (bKey != null && isMaskedSecretPreview(bKey)) {
            bKey = null;
        }

        boolean pair = bMid != null && bKey != null;
        String mid;
        String key;
        if (pair) {
            mid = bMid;
            key = bKey;
        } else {
            mid = hqMid != null ? hqMid : "";
            key = hqKey != null ? hqKey : "";
        }
        String iv = bIv != null ? bIv : (hqIv != null ? hqIv : "");
        return new Resolved(mid != null ? mid : "", key != null ? key : "", iv, pair);
    }

    /**
     * 저장 시점 정규화.
     * <ul>
     *   <li>마스킹값 → 기존 키 유지</li>
     *   <li>공백 → 가맹 키 해제(본사 사용)</li>
     *   <li>새 값 → 갱신</li>
     *   <li>MID·Key 쌍이 불완전하면 본사 MID 저장·apiKey null</li>
     * </ul>
     */
    public static PersistCreds normalizeForPersist(
            String midIncoming,
            String apiKeyIncoming,
            String ivKeyIncoming,
            String previousApiKey,
            String previousIvKey,
            PgAgency agency) {
        String midIn = trimToNull(midIncoming);
        String keyIn = trimToNull(apiKeyIncoming);
        String ivIn = trimToNull(ivKeyIncoming);

        String keyResolved;
        if (keyIn == null) {
            keyResolved = null;
        } else if (isMaskedSecretPreview(keyIn)
                || (previousApiKey != null && keyIn.equals(maskSecretPreview(previousApiKey)))) {
            keyResolved = trimToNull(previousApiKey);
        } else {
            keyResolved = keyIn;
        }

        String ivResolved;
        if (ivIn == null) {
            /* 공백 제출 = IV 해제. 마스킹만 이전 유지하려면 호출측에서 마스킹을 보냄 */
            ivResolved = null;
        } else if (isMaskedSecretPreview(ivIn)
                || (previousIvKey != null && ivIn.equals(maskSecretPreview(previousIvKey)))) {
            ivResolved = trimToNull(previousIvKey);
        } else {
            ivResolved = ivIn;
        }

        boolean pair = midIn != null && keyResolved != null;
        if (pair) {
            return new PersistCreds(midIn, keyResolved, ivResolved);
        }
        String hqMid = agency != null ? trimToNull(agency.getMerchantMid()) : null;
        return new PersistCreds(hqMid, null, ivResolved);
    }
}
