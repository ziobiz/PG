package com.pg.util;

import java.util.regex.Pattern;

/**
 * 엑셀 내보내기용: 화면 표시(de-DE·ko-KR 천단위/소수) 문자열을 Excel SUM 가능한 숫자로 파싱.
 */
public final class ExcelExportNumberUtil {

    private static final Pattern DATE_LIKE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern TIME_LIKE = Pattern.compile(":\\d{2}");
    private static final Pattern EN_GROUPED = Pattern.compile("^\\d{1,3}(,\\d{3})+(\\.\\d+)?$");
    private static final Pattern DE_GROUPED = Pattern.compile("^\\d{1,3}(\\.\\d{3})+(,\\d+)?$");

    private ExcelExportNumberUtil() {
    }

    /** 파싱 불가·날짜·문자열이면 null */
    public static Double tryParse(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.isEmpty() || "-".equals(s) || "—".equals(s) || "–".equals(s)) {
            return null;
        }
        if (s.indexOf('%') >= 0) {
            return null;
        }
        if (DATE_LIKE.matcher(s).find() || TIME_LIKE.matcher(s).find()) {
            return null;
        }
        /* 통화 코드 접미(예: 1.234,56 USD) */
        s = s.replaceAll("\\s+[A-Za-z]{3}\\s*$", "").trim();
        s = s.replace(" ", "");
        if (s.isEmpty()) {
            return null;
        }
        /* 순수 숫자(부호·소수점) */
        if (s.matches("^[+-]?\\d+(\\.\\d+)?$")) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        String normalized = normalizeLocaleNumber(s);
        if (normalized == null) {
            return null;
        }
        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Excel 표시 서식: 정수는 #,##0, 소수는 최대 8자리 */
    public static String excelFormatFor(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "@";
        }
        if (value == Math.rint(value) && Math.abs(value) < 1e15) {
            return "#,##0";
        }
        return "#,##0.########";
    }

    private static String normalizeLocaleNumber(String s) {
        if (EN_GROUPED.matcher(s).matches()) {
            return s.replace(",", "");
        }
        if (DE_GROUPED.matcher(s).matches()) {
            int lastComma = s.lastIndexOf(',');
            if (lastComma >= 0) {
                return s.substring(0, lastComma).replace(".", "") + "." + s.substring(lastComma + 1);
            }
            return s.replace(".", "");
        }
        int lastComma = s.lastIndexOf(',');
        int lastDot = s.lastIndexOf('.');
        if (lastComma >= 0 && lastDot >= 0) {
            if (lastComma > lastDot) {
                return s.replace(".", "").replace(',', '.');
            }
            return s.replace(",", "");
        }
        if (lastComma >= 0) {
            String after = s.substring(lastComma + 1);
            if (after.length() == 3 && s.indexOf(',') == lastComma && s.substring(0, lastComma).matches("\\d{1,3}")) {
                return s.replace(",", "");
            }
            return s.replace(",", ".");
        }
        if (lastDot >= 0) {
            long dotCount = s.chars().filter(ch -> ch == '.').count();
            String after = s.substring(lastDot + 1);
            if (dotCount > 1 || (after.length() == 3 && dotCount == 1 && s.substring(0, lastDot).matches("\\d{1,3}"))) {
                return s.replace(".", "");
            }
        }
        if (s.matches("^[+-]?\\d+$")) {
            return s;
        }
        return null;
    }
}
