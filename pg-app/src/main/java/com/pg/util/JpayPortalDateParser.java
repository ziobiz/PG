package com.pg.util;

import org.apache.poi.ss.usermodel.DateUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** JPAY 포털 Export 엑셀 — 거래일·시각 파싱 */
public final class JpayPortalDateParser {

    private static final Pattern ISO_DATE_PREFIX = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})");
    private static final Pattern SLASH_YMD = Pattern.compile("(\\d{4})/(\\d{1,2})/(\\d{1,2})");
    private static final Pattern SLASH_MDY = Pattern.compile("(\\d{1,2})/(\\d{1,2})/(\\d{4})");

    private JpayPortalDateParser() {
    }

    public static Optional<LocalDate> parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String t = raw.trim().replace('\u00a0', ' ');

        Optional<LocalDate> excel = parseExcelSerial(t);
        if (excel.isPresent()) {
            return excel;
        }

        String norm = t.replace('/', '-').replace('T', ' ');
        if (norm.length() >= 10) {
            try {
                return Optional.of(LocalDate.parse(norm.substring(0, 10)));
            } catch (DateTimeParseException ignored) {
                /* try patterns below */
            }
        }

        Matcher ymd = SLASH_YMD.matcher(t);
        if (ymd.find()) {
            try {
                int y = Integer.parseInt(ymd.group(1));
                int mo = Integer.parseInt(ymd.group(2));
                int d = Integer.parseInt(ymd.group(3));
                return Optional.of(LocalDate.of(y, mo, d));
            } catch (Exception ignored) {
                /* fall through */
            }
        }

        Matcher mdy = SLASH_MDY.matcher(t);
        if (mdy.find()) {
            try {
                int mo = Integer.parseInt(mdy.group(1));
                int d = Integer.parseInt(mdy.group(2));
                int y = Integer.parseInt(mdy.group(3));
                return Optional.of(LocalDate.of(y, mo, d));
            } catch (Exception ignored) {
                /* fall through */
            }
        }

        Matcher iso = ISO_DATE_PREFIX.matcher(t);
        if (iso.find()) {
            try {
                return Optional.of(LocalDate.parse(iso.group(1) + "-" + iso.group(2) + "-" + iso.group(3)));
            } catch (Exception ignored) {
                return Optional.empty();
            }
        }

        for (DateTimeFormatter fmt : new DateTimeFormatter[]{
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
        }) {
            try {
                if (t.length() > 10) {
                    return Optional.of(LocalDateTime.parse(t, fmt).toLocalDate());
                }
            } catch (Exception ignored) {
                /* next */
            }
        }

        return Optional.empty();
    }

    private static Optional<LocalDate> parseExcelSerial(String t) {
        String num = t.replace(",", "").trim();
        if (!num.matches("\\d+(\\.\\d+)?")) {
            return Optional.empty();
        }
        try {
            double serial = Double.parseDouble(num);
            if (serial < 30000 || serial > 80000) {
                return Optional.empty();
            }
            java.util.Date jd = DateUtil.getJavaDate(serial, false);
            return Optional.of(jd.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    /** {@code trnDate}·{@code trnTime} 을 행 맵에 채움 */
    public static void applyDateTimeFields(String raw, java.util.Map<String, Object> row) {
        if (raw == null || raw.isBlank() || row == null) {
            return;
        }
        String t = raw.trim().replace('\u00a0', ' ');
        parseDate(t).ifPresent(ld -> row.put("trnDate", ld.toString()));

        String timePart = "";
        String norm = t.replace('/', '-').replace('T', ' ');
        if (norm.length() > 11) {
            timePart = norm.substring(11).trim();
        }
        if (!timePart.isBlank()) {
            row.put("trnTime", timePart.length() > 8 ? timePart.substring(0, 8) : timePart);
        }
    }

    public static Optional<LocalDate> rowTrnDate(java.util.Map<String, Object> row) {
        if (row == null) {
            return Optional.empty();
        }
        for (String key : new String[]{"trnDate", "transactionDate"}) {
            Object v = row.get(key);
            if (v == null) {
                continue;
            }
            Optional<LocalDate> d = parseDate(String.valueOf(v));
            if (d.isPresent()) {
                return d;
            }
        }
        return Optional.empty();
    }

    public static String normalizeRaw(String raw) {
        return raw == null ? "" : raw.trim();
    }
}
