package com.pg.receipt;

/** 거래명세서 연락처 블록 — 입력된 필드만 표시(이름 / 전화 / 이메일 줄바꿈) */
public record TransactionReceiptContactBlock(String displayLine) {

    /**
     * 이름·전화·이메일을 줄바꿈으로 연결. 비어 있는 항목은 생략.
     */
    public static TransactionReceiptContactBlock of(String name, String tel, String email) {
        StringBuilder sb = new StringBuilder();
        appendLine(sb, name);
        appendLine(sb, tel);
        appendLine(sb, email);
        return new TransactionReceiptContactBlock(sb.toString().trim());
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
