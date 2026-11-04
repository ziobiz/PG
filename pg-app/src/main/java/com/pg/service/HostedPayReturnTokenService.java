package com.pg.service;

import com.pg.entity.HqApiConfig;
import com.pg.repository.HqApiConfigRepository;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

/**
 * OMS/Beam 등 외부 HPP {@code return_url} 용 암호화 토큰.
 * 가맹코드·주문번호·entry 를 PG에 평문으로 넣지 않는다 (상호·주소·도메인 비노출 목적).
 */
@Service
public class HostedPayReturnTokenService {

    private static final int TTL_SECONDS = 3600;
    private static final int GCM_IV_LEN = 12;
    private static final int GCM_TAG_BITS = 128;

    private final HqApiConfigRepository hqApiConfigRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public HostedPayReturnTokenService(HqApiConfigRepository hqApiConfigRepository) {
        this.hqApiConfigRepository = hqApiConfigRepository;
    }

    public record ReturnPayload(String vendor, String compId, String orderNo, boolean merchantApi, long expiresEpochSec) {
    }

    public String issue(String vendor, String compId, String orderNo, boolean merchantApi) {
        long exp = Instant.now().getEpochSecond() + TTL_SECONDS;
        String plain = String.join("|",
                nz(normalizeVendor(vendor)),
                nz(compId),
                nz(orderNo),
                merchantApi ? "1" : "0",
                Long.toString(exp));
        try {
            byte[] iv = new byte[GCM_IV_LEN];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buf = ByteBuffer.allocate(iv.length + ct.length);
            buf.put(iv);
            buf.put(ct);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(buf.array());
        } catch (Exception e) {
            throw new IllegalStateException("hosted return token encrypt failed", e);
        }
    }

    public Optional<ReturnPayload> parseValid(String token, String expectedVendor) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            byte[] all = Base64.getUrlDecoder().decode(token.trim());
            if (all.length <= GCM_IV_LEN + 16) {
                return Optional.empty();
            }
            byte[] iv = Arrays.copyOfRange(all, 0, GCM_IV_LEN);
            byte[] ct = Arrays.copyOfRange(all, GCM_IV_LEN, all.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            String plain = new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
            String[] f = plain.split("\\|", -1);
            if (f.length < 5) {
                return Optional.empty();
            }
            long exp = Long.parseLong(f[4]);
            if (Instant.now().getEpochSecond() > exp) {
                return Optional.empty();
            }
            String vendor = normalizeVendor(f[0]);
            if (expectedVendor != null && !expectedVendor.isBlank()
                    && !normalizeVendor(expectedVendor).equals(vendor)) {
                return Optional.empty();
            }
            boolean merchantApi = "1".equals(f[3]);
            String compId = f[1] != null ? f[1].trim() : "";
            String orderNo = f[2] != null ? f[2].trim() : "";
            if (compId.isBlank() || orderNo.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new ReturnPayload(vendor, compId, orderNo, merchantApi, exp));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * PG에 넘기는 return_url — {@code rt} 만 (가맹코드·주문번호·상호·주소 없음).
     * 해시에도 동일 토큰을 넣어 OMS가 쿼리를 잘라도 복귀 가능.
     */
    public String buildOpaqueReturnUrl(String publicBaseUrl, String pathHtml, String vendor,
                                       String compId, String orderNo, boolean merchantApi,
                                       String returnFlagQuery) {
        String base = publicBaseUrl != null ? publicBaseUrl.trim().replaceAll("/+$", "") : "https://api.icopay.co.kr";
        String path = pathHtml != null && !pathHtml.isBlank() ? pathHtml.trim() : "/omspay-pay.html";
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        String rt = issue(vendor, compId, orderNo, merchantApi);
        String flag = returnFlagQuery != null ? returnFlagQuery.trim() : "omspayReturn=1&done=1";
        String qs = "rt=" + urlEncode(rt) + "&" + flag;
        String hs = "rt=" + urlEncode(rt) + "&" + flag;
        return base + path + "?" + qs + "#" + hs;
    }

    private SecretKeySpec aesKey() {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] key = sha.digest(resolveSecret().getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(key, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("AES key derive failed", e);
        }
    }

    private String resolveSecret() {
        String fromHq = hqApiConfigRepository.findAll().stream()
                .findFirst()
                .map(HqApiConfig::getChillpayMd5Key)
                .filter(s -> s != null && !s.isBlank())
                .orElse("");
        if (!fromHq.isBlank()) {
            return fromHq;
        }
        /* 서명 키 없을 때도 인스턴스별로 안정적인 파생값 — 운영에서는 HQ 키 사용 권장 */
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec("pg-hosted-return-fallback".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal("icopay-return".getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return "pg-hosted-return-fallback-key";
        }
    }

    private static String normalizeVendor(String raw) {
        if (raw == null || raw.isBlank()) {
            return "OMSPAY";
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s != null ? s : "", StandardCharsets.UTF_8);
    }
}
