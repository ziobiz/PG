package com.pg.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Locale;
import java.util.Set;

/**
 * ElementPay Cabinet 웹훅 송신원 IP (Sandbox / Live).
 * EP→NOTI 1차 화이트리스트는 NOTI가 담당. ICOPAY는 NOTI가 넘긴
 * {@code X-ElementPay-Source-Ip} 를 감사·로그용으로만 대조한다.
 */
public final class ElementPayWebhookIpUtil {

    /** NOTI → ICOPAY 전달 헤더 (EP 실제 출구 IP) */
    public static final String SOURCE_IP_HEADER = "X-ElementPay-Source-Ip";

    /** ElementPay 문서·메일 기준 Sandbox 출구 */
    public static final String SANDBOX_IP = "3.0.36.253";
    /** ElementPay 문서·메일 기준 Live 출구 */
    public static final String LIVE_IP = "3.1.29.20";

    private static final Set<String> KNOWN = Set.of(SANDBOX_IP, LIVE_IP);

    private ElementPayWebhookIpUtil() {}

    public static String normalize(String ip) {
        if (ip == null) {
            return "";
        }
        String s = ip.trim().toLowerCase(Locale.ROOT);
        if (s.startsWith("::ffff:")) {
            s = s.substring(7);
        }
        if (s.startsWith("[") && s.endsWith("]") && s.length() > 2) {
            s = s.substring(1, s.length() - 1);
        }
        int colon = s.indexOf(':');
        if (colon > 0 && s.indexOf('.') > 0 && s.chars().filter(ch -> ch == ':').count() == 1) {
            // host:port (IPv4 only)
            s = s.substring(0, colon);
        }
        return s;
    }

    public static boolean isKnownCabinetIp(String ip) {
        String n = normalize(ip);
        return !n.isEmpty() && KNOWN.contains(n);
    }

    /** 요청에서 {@link #SOURCE_IP_HEADER} 값을 정규화해 반환. 없으면 빈 문자열. */
    public static String extractSourceIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        return normalize(request.getHeader(SOURCE_IP_HEADER));
    }

    /**
     * 인바운드 {@code client_ip}(길이 64)용: EP 출구 IP를 앞에 두고 NOTI peer를 뒤에 붙인다.
     * 헤더가 없으면 NOTI peer만 반환.
     */
    public static String composeInboundClientIp(String elementPaySourceIp, String notiPeerIp) {
        String ep = normalize(elementPaySourceIp);
        String peer = normalize(notiPeerIp);
        if (ep.isEmpty()) {
            return truncate(peer, 64);
        }
        if (peer.isEmpty() || peer.equals(ep)) {
            return truncate(ep, 64);
        }
        return truncate(ep + " via " + peer, 64);
    }

    public static AuditResult audit(HttpServletRequest request, String notiPeerIp) {
        String ep = extractSourceIp(request);
        String peer = normalize(notiPeerIp);
        if (ep.isEmpty()) {
            return new AuditResult(ep, peer, false, true);
        }
        return new AuditResult(ep, peer, isKnownCabinetIp(ep), false);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    public record AuditResult(String elementPaySourceIp, String notiPeerIp, boolean knownCabinetIp, boolean headerMissing) {}
}
