package com.pg.service;

import com.pg.config.PgNotifyIngressProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.security.MessageDigest;

/**
 * ziobiz/NOTI → PG 노티 수신 시 송신원(IP) 및 선택적 HMAC 검증.
 */
@Service
public class PgNotifyIngressGuard {

    private static final Logger log = LoggerFactory.getLogger(PgNotifyIngressGuard.class);

    private final PgNotifyIngressProperties props;

    public PgNotifyIngressGuard(PgNotifyIngressProperties props) {
        this.props = props;
    }

    public void assertAllowed(String clientIp, String rawBody, HttpServletRequest request) {
        List<String> cidrs = parseCidrs(props.getAllowedClientCidrs());
        if (!cidrs.isEmpty()) {
            if (clientIp == null || clientIp.isBlank() || !ipMatchesAny(clientIp.trim(), cidrs)) {
                log.warn("pg-notify ingress denied: ip not in allowlist clientIp={}", clientIp);
                throw new SecurityException("notify ingress ip denied");
            }
        }
        String secret = props.getHmacSecret();
        if (StringUtils.hasText(secret)) {
            String headerName = props.getHmacHeader();
            String presented = request.getHeader(headerName);
            if (!StringUtils.hasText(presented)) {
                log.warn("pg-notify ingress denied: missing HMAC header {}", headerName);
                throw new SecurityException("notify hmac missing");
            }
            String normalized = presented.trim();
            if (normalized.regionMatches(true, 0, "sha256=", 0, 7)) {
                normalized = normalized.substring(7).trim();
            }
            byte[] expected;
            try {
                expected = HexFormat.of().parseHex(normalized.toLowerCase());
            } catch (IllegalArgumentException e) {
                log.warn("pg-notify ingress denied: invalid HMAC hex");
                throw new SecurityException("notify hmac invalid");
            }
            byte[] bodyBytes = rawBody != null ? rawBody.getBytes(StandardCharsets.UTF_8) : new byte[0];
            byte[] computed = hmacSha256(secret.getBytes(StandardCharsets.UTF_8), bodyBytes);
            if (expected.length != computed.length || !MessageDigest.isEqual(expected, computed)) {
                log.warn("pg-notify ingress denied: HMAC mismatch");
                throw new SecurityException("notify hmac mismatch");
            }
        }
    }

    private static List<String> parseCidrs(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String part : raw.split(",")) {
            String p = part.trim();
            if (!p.isEmpty()) {
                out.add(p);
            }
        }
        return out;
    }

    private static boolean ipMatchesAny(String clientIp, List<String> cidrs) {
        for (String cidr : cidrs) {
            try {
                if (new IpAddressMatcher(cidr).matches(clientIp)) {
                    return true;
                }
            } catch (Exception e) {
                log.warn("pg-notify invalid CIDR pattern skipped: {} ({})", cidr, e.getMessage());
            }
        }
        return false;
    }

    private static byte[] hmacSha256(byte[] secret, byte[] message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(message);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 init failed", e);
        }
    }
}
