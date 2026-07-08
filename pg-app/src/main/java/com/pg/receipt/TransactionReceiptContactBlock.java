package com.pg.receipt;

/** 거래명세서 연락처 블록 — 입력된 필드만 표시 */
public record TransactionReceiptContactBlock(String displayLine) {

    public static TransactionReceiptContactBlock of(String name, String tel, String email) {
        StringBuilder sb = new StringBuilder();
        appendPart(sb, name);
        appendPart(sb, tel);
        appendPart(sb, email);
        return new TransactionReceiptContactBlock(sb.toString().trim());
    }

    private static void appendPart(StringBuilder sb, String part) {
        if (part == null || part.isBlank()) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append(" · ");
        }
        sb.append(part.trim());
    }

    public boolean isEmpty() {
        return displayLine == null || displayLine.isBlank();
    }
}
