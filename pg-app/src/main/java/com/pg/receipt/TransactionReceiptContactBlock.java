package com.pg.receipt;

/** 거래명세서 연락처 블록 — 이름 / (전화 · 이메일) */
public record TransactionReceiptContactBlock(String displayLine) {

    /**
     * 이름·전화·이메일을 줄바꿈으로 연결. 비어 있는 항목은 생략.
     */
    public static TransactionReceiptContactBlock of(String name, String tel, String email) {
        StringBuilder sb = new StringBuilder();
        appendLine(sb, name);
        appendLine(sb, joinTelEmail(tel, email));
        return new TransactionReceiptContactBlock(sb.toString().trim());
    }

    /**
     * 가맹점 블록: 상호명 다음 줄에 「국가번호·전화 / 이메일」.
     */
    public static TransactionReceiptContactBlock ofMerchant(String name, String tel, String email) {
        return of(name, tel, email);
    }

    static String joinTelEmail(String tel, String email) {
        String t = tel != null ? tel.trim() : "";
        String e = email != null ? email.trim() : "";
        if (!t.isEmpty() && !e.isEmpty()) {
            return t + " / " + e;
        }
        if (!t.isEmpty()) {
            return t;
        }
        return e;
    }

    private static void appendLine(StringBuilder sb, String part) {
        if (part == null || part.isBlank()) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append('\n');
        }
        sb.append(part.trim());
    }

    public boolean isEmpty() {
        return displayLine == null || displayLine.isBlank();
    }
}
