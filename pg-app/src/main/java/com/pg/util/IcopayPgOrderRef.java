package com.pg.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.UUID;

/**
 * ICOPAY → 결제대행사(PG) 주문/참조번호.
 * <p>가맹 orderNo는 DB·가맹 통보에만 두고, PG에는 동일 자리수·동일 패턴의 불투명 ref만 전달한다.
 * 웹훅은 {@code pay_no}(이 ref)로 역추적한다.
 */
public final class IcopayPgOrderRef {

    private static final SecureRandom RND = new SecureRandom();
    private static final char[] ALNUM = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private IcopayPgOrderRef() {
    }

    /** PG별 길이·문자셋 프로필 (자리수는 고정폭). */
    public enum Profile {
        /** PAYSO referenceNo */
        DIGITS_12(12, true),
        /** ChillPay OrderNo */
        ALNUM_20(20, false),
        /** ILK clientReferenceInformation.code */
        ALNUM_32(32, false),
        /**
         * JPAY / ElementPay / Eximbay / QXIPAY / OMSPAY / KSNET 공통.
         * DB {@code pay_no} VARCHAR(50) — PG max가 더 커도 50으로 통일.
         */
        ALNUM_50(50, false);

        private final int length;
        private final boolean digitsOnly;

        Profile(int length, boolean digitsOnly) {
            this.length = length;
            this.digitsOnly = digitsOnly;
        }

        public int length() {
            return length;
        }

        public boolean digitsOnly() {
            return digitsOnly;
        }
    }

    /**
     * 결제 시도마다 고유한 PG ref 발급.
     * 가맹 order 내용과 무관 — 동일 패턴만 PG에 노출.
     */
    public static String newRef(Profile profile) {
        if (profile == null) {
            profile = Profile.ALNUM_50;
        }
        String seed = System.currentTimeMillis() + "|" + UUID.randomUUID() + "|" + RND.nextLong();
        return fromSeed(profile, seed);
    }

    /**
     * 결정적 변환(같은 seed → 같은 ref). 재시도·구독 해지 등 seed를 trnId로 고정할 때 사용.
     * 가맹 orderNo를 seed로 쓰지 말 것(패턴 노출·재시도 충돌).
     */
    public static String fromSeed(Profile profile, String seed) {
        if (profile == null) {
            profile = Profile.ALNUM_50;
        }
        String s = seed != null && !seed.isBlank() ? seed.trim() : UUID.randomUUID().toString();
        byte[] dig = sha256("ICOPAY|PGREF|v1|" + profile.name() + "|" + s);
        if (profile.digitsOnly()) {
            return toFixedDigits(dig, profile.length());
        }
        return toFixedAlnum(dig, profile.length());
    }

    /** PAYSO 호환 — {@link Profile#DIGITS_12}. 신규 결제는 {@link #newRef} 권장. */
    public static String paysoDigits12() {
        return newRef(Profile.DIGITS_12);
    }

    public static String chillPayOrderNo() {
        return newRef(Profile.ALNUM_20);
    }

    public static String ilkCode() {
        return newRef(Profile.ALNUM_32);
    }

    /** KSNET orderNumb / 결제내역 승인번호 — PAYSO와 동일 숫자 12자리. */
    public static String ksnetOrderNumb() {
        return newRef(Profile.DIGITS_12);
    }

    /** JPAY·ElementPay·Eximbay·QXIPAY·OMSPAY 표준 (50자, DB pay_no 한도). */
    public static String standard() {
        return newRef(Profile.ALNUM_50);
    }

    /** @deprecated use {@link #standard()} */
    public static String standard64() {
        return standard();
    }

    /** saleBody 키 — {@link #putInSaleBody} / {@link #applyPayNoToTxn}. */
    public static final String SALE_BODY_KEY = "icopayPgOrderRef";

    public static void putInSaleBody(java.util.Map<String, Object> saleBody, String pgRef) {
        if (saleBody == null || pgRef == null || pgRef.isBlank()) {
            return;
        }
        saleBody.put(SALE_BODY_KEY, pgRef.trim());
    }

    public static String fromSaleBody(java.util.Map<String, Object> saleBody) {
        if (saleBody == null) {
            return "";
        }
        Object v = saleBody.get(SALE_BODY_KEY);
        return v != null ? String.valueOf(v).trim() : "";
    }

    /**
     * pending 적재 시 pay_no = ICOPAY PG ref.
     * 기존 행에 이미 ref가 있으면 재사용(재시도·취소 동일 키).
     */
    public static String ensureAndApply(com.pg.entity.PgTrnsctn t, Profile profile,
                                        java.util.Map<String, Object> saleBody) {
        String fromBody = fromSaleBody(saleBody);
        if (!fromBody.isBlank()) {
            if (t != null) {
                String pay = fromBody.length() > 50 ? fromBody.substring(0, 50) : fromBody;
                t.setPayNo(pay);
                if (t.getApprovalNo() == null || t.getApprovalNo().isBlank()) {
                    t.setApprovalNo(pay);
                }
            }
            return fromBody;
        }
        String existing = t != null && t.getPayNo() != null ? t.getPayNo().trim() : "";
        if (looksLikeIcopayRef(existing, profile)) {
            putInSaleBody(saleBody, existing);
            return existing;
        }
        String created = newRef(profile != null ? profile : Profile.ALNUM_50);
        putInSaleBody(saleBody, created);
        if (t != null) {
            applyPayNoRaw(t, created);
        }
        return created;
    }

    public static void applyPayNoToTxn(com.pg.entity.PgTrnsctn t, java.util.Map<String, Object> saleBody) {
        if (t == null) {
            return;
        }
        String ref = fromSaleBody(saleBody);
        if (ref.isBlank()) {
            return;
        }
        applyPayNoRaw(t, ref);
    }

    private static void applyPayNoRaw(com.pg.entity.PgTrnsctn t, String ref) {
        String pay = ref.length() > 50 ? ref.substring(0, 50) : ref;
        t.setPayNo(pay);
        /* approval_no VARCHAR(20) — 짧은 digits ref만 */
        if ((t.getApprovalNo() == null || t.getApprovalNo().isBlank()) && pay.length() <= 20) {
            t.setApprovalNo(pay);
        }
    }

    /** ICOPAY 발급 ref 형태인지(레거시 가맹 order와 구분). */
    public static boolean looksLikeIcopayRef(String value, Profile profile) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String v = value.trim();
        if (profile != null && profile.digitsOnly()) {
            return v.matches("^[0-9]{" + profile.length() + "}$");
        }
        if (v.length() >= 12 && v.charAt(0) == 'I' && v.matches("^[A-Z0-9]+$")) {
            return true;
        }
        return false;
    }

    private static byte[] sha256(String raw) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            byte[] b = new byte[32];
            RND.nextBytes(b);
            return b;
        }
    }

    private static String toFixedDigits(byte[] dig, int len) {
        StringBuilder sb = new StringBuilder(len);
        /* 선행 0 방지: 첫 자리 1–9 */
        int first = (dig[0] & 0xff) % 9 + 1;
        sb.append(first);
        int i = 1;
        while (sb.length() < len) {
            int v = dig[i % dig.length] & 0xff;
            if (i >= dig.length) {
                v ^= (sb.length() * 31 + i);
            }
            sb.append(v % 10);
            i++;
        }
        return sb.substring(0, len);
    }

    private static String toFixedAlnum(byte[] dig, int len) {
        StringBuilder sb = new StringBuilder(len);
        /* 통일 접두 — PG가 동일 사이트 패턴으로 인식 */
        sb.append('I');
        int i = 0;
        while (sb.length() < len) {
            int v = dig[i % dig.length] & 0xff;
            if (i >= dig.length) {
                v ^= (sb.length() * 17 + i);
            }
            sb.append(ALNUM[v % ALNUM.length]);
            i++;
        }
        return sb.substring(0, len).toUpperCase(Locale.ROOT);
    }
}
