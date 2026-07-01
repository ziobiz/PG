package com.pg.urlpay;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * JPAY 포털 Export {@code Customer IP} — {@code 103.5.140.132|日本-千葉縣} 형식.
 */
public final class JpayCustomerIpFieldParser {

    private static final Pattern IPV4 = Pattern.compile(
            "^(?:25[0-5]|2[0-4]\\d|1?\\d{1,2})(?:\\.(?:25[0-5]|2[0-4]\\d|1?\\d{1,2})){3}$");

    private JpayCustomerIpFieldParser() {
    }

    public record ParsedCustomerIp(String ip, String locationSuffix) {
    }

    public static Optional<ParsedCustomerIp> parse(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String v = raw.trim();
        if (v.isEmpty()) {
            return Optional.empty();
        }
        int pipe = v.indexOf('|');
        if (pipe < 0) {
            return looksLikeIp(v) ? Optional.of(new ParsedCustomerIp(v, "")) : Optional.empty();
        }
        String ip = v.substring(0, pipe).trim();
        String suffix = v.substring(pipe + 1).trim();
        if (!looksLikeIp(ip)) {
            return Optional.empty();
        }
        return Optional.of(new ParsedCustomerIp(ip, suffix));
    }

    static boolean looksLikeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        String v = ip.trim();
        if (IPV4.matcher(v).matches()) {
            return true;
        }
        return v.contains(":") && v.chars().filter(ch -> ch == ':').count() <= 8;
    }
}
