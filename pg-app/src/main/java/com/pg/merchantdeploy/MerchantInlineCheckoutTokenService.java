package com.pg.merchantdeploy;

import com.pg.entity.HqApiConfig;
import com.pg.repository.HqApiConfigRepository;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 가맹점 인라인 결제·구독 세션 토큰(HMAC). DB 없이 서명·만료만 검증합니다.
 */
@Service
public class MerchantInlineCheckoutTokenService {

    public static final String CHECKOUT_ONE_TIME = "ONE_TIME";
    public static final String CHECKOUT_SUBSCRIPTION = "SUBSCRIPTION";

    private static final int DEFAULT_TTL_SECONDS = 1800;

    private final HqApiConfigRepository hqApiConfigRepository;

    public MerchantInlineCheckoutTokenService(HqApiConfigRepository hqApiConfigRepository) {
        this.hqApiConfigRepository = hqApiConfigRepository;
    }

    public record SessionPayload(
            String sessionId,
            String compId,
            String orderNo,
            String amountPlain,
            String currency,
            String productName,
            String pgVendor,
            String checkoutKind,
            String subscriptionPlanJson,
            String buyerPrefillJson,
            long expiresEpochSec
    ) {
        Map<String, Object> toPublicMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("sessionId", sessionId);
            m.put("compId", compId);
            m.put("orderNo", orderNo);
            m.put("amount", amountPlain);
            if (currency != null && !currency.isBlank()) {
                m.put("currency", currency);
            }
            if (productName != null && !productName.isBlank()) {
                m.put("productName", productName);
            }
            if (pgVendor != null && !pgVendor.isBlank()) {
                m.put("pgVendor", pgVendor);
            }
            if (checkoutKind != null && !checkoutKind.isBlank()) {
                m.put("checkoutKind", checkoutKind);
            }
            if (subscriptionPlanJson != null && !subscriptionPlanJson.isBlank()) {
                m.put("subscriptionPlanJson", subscriptionPlanJson);
            }
            if (buyerPrefillJson != null && !buyerPrefillJson.isBlank()) {
                m.put("buyerPrefill", com.pg.urlpay.JpayBuyerPrefillUtil.parsePublicMap(buyerPrefillJson));
            }
            m.put("expiresAt", Instant.ofEpochSecond(expiresEpochSec).toString());
            return m;
        }

        boolean isSubscription() {
            return CHECKOUT_SUBSCRIPTION.equalsIgnoreCase(checkoutKind != null ? checkoutKind.trim() : "");
        }
    }

    public String issue(String pgVendor, String compId, String orderNo, String amountPlain,
                        String currency, String productName) {
        return issueInternal(pgVendor, compId, orderNo, amountPlain, currency, productName,
                CHECKOUT_ONE_TIME, "", "");
    }

    public String issueWithBuyerPrefill(String pgVendor, String compId, String orderNo, String amountPlain,
                                        String currency, String productName, String buyerPrefillJson) {
        return issueInternal(pgVendor, compId, orderNo, amountPlain, currency, productName,
                CHECKOUT_ONE_TIME, "", buyerPrefillJson != null ? buyerPrefillJson : "");
    }

    public String issueSubscription(String pgVendor, String compId, String orderNo, String amountPlain,
                                    String currency, String productName, String subscriptionPlanJson) {
        return issueInternal(pgVendor, compId, orderNo, amountPlain, currency, productName,
                CHECKOUT_SUBSCRIPTION, subscriptionPlanJson != null ? subscriptionPlanJson : "", "");
    }

    private String issueInternal(String pgVendor, String compId, String orderNo, String amountPlain,
                                 String currency, String productName, String checkoutKind,
                                 String subscriptionPlanJson, String buyerPrefillJson) {
        long exp = Instant.now().getEpochSecond() + DEFAULT_TTL_SECONDS;
        String sid = UUID.randomUUID().toString().replace("-", "");
        String vendor = normalizeVendor(pgVendor);
        String kind = normalizeCheckoutKind(checkoutKind);
        String planEnc = encodePlan(subscriptionPlanJson);
        String prefillEnc = encodePlan(buyerPrefillJson);
        String payload = joinPayload(sid, compId, orderNo, amountPlain, currency, productName, vendor, kind, planEnc, prefillEnc, exp);
        String sig = sign(payload);
        return base64Url(payload) + "." + base64Url(sig);
    }

    public Optional<SessionPayload> parseValid(String token) {
        return parseValid(token, null, null);
    }

    public Optional<SessionPayload> parseValid(String token, String expectedVendor) {
        return parseValid(token, expectedVendor, null);
    }

    public Optional<SessionPayload> parseValid(String token, String expectedVendor, String expectedCheckoutKind) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String[] parts = token.trim().split("\\.", 2);
        if (parts.length != 2) {
            return Optional.empty();
        }
        String payload;
        String sigPresent;
        try {
            payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            sigPresent = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        if (!constantTimeEquals(sign(payload), sigPresent)) {
            return Optional.empty();
        }
        String[] fields = payload.split("\\|", -1);
        String vendor;
        String checkoutKind = CHECKOUT_ONE_TIME;
        String planEnc = "";
        String prefillEnc = "";
        long exp;
        if (fields.length >= 11) {
            vendor = normalizeVendor(fields[6]);
            checkoutKind = normalizeCheckoutKind(fields[7]);
            planEnc = fields[8];
            prefillEnc = fields[9];
            try {
                exp = Long.parseLong(fields[10]);
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        } else if (fields.length >= 10) {
            vendor = normalizeVendor(fields[6]);
            checkoutKind = normalizeCheckoutKind(fields[7]);
            planEnc = fields[8];
            try {
                exp = Long.parseLong(fields[9]);
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        } else if (fields.length >= 8) {
            vendor = normalizeVendor(fields[6]);
            try {
                exp = Long.parseLong(fields[7]);
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        } else if (fields.length == 7) {
            vendor = MerchantPgBrokerVendor.CHILLPAY;
            try {
                exp = Long.parseLong(fields[6]);
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
        if (Instant.now().getEpochSecond() > exp) {
            return Optional.empty();
        }
        if (expectedVendor != null && !expectedVendor.isBlank()
                && !normalizeVendor(expectedVendor).equals(vendor)) {
            return Optional.empty();
        }
        if (expectedCheckoutKind != null && !expectedCheckoutKind.isBlank()
                && !normalizeCheckoutKind(expectedCheckoutKind).equals(checkoutKind)) {
            return Optional.empty();
        }
        return Optional.of(new SessionPayload(
                fields[0],
                fields[1],
                fields[2],
                fields[3],
                emptyToNull(fields[4]),
                emptyToNull(fields[5]),
                vendor,
                checkoutKind,
                decodePlan(planEnc),
                decodePlan(prefillEnc),
                exp
        ));
    }

    private static String joinPayload(String sessionId, String compId, String orderNo, String amountPlain,
                                      String currency, String productName, String pgVendor, String checkoutKind,
                                      String planEnc, String prefillEnc, long exp) {
        return String.join("|",
                nz(sessionId),
                nz(compId),
                nz(orderNo),
                nz(amountPlain),
                nz(currency),
                nz(productName),
                normalizeVendor(pgVendor),
                normalizeCheckoutKind(checkoutKind),
                nz(planEnc),
                nz(prefillEnc),
                Long.toString(exp));
    }

    private static String normalizeCheckoutKind(String raw) {
        if (raw == null || raw.isBlank()) {
            return CHECKOUT_ONE_TIME;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        return CHECKOUT_SUBSCRIPTION.equals(u) ? CHECKOUT_SUBSCRIPTION : CHECKOUT_ONE_TIME;
    }

    private static String encodePlan(String planJson) {
        if (planJson == null || planJson.isBlank()) {
            return "";
        }
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(planJson.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodePlan(String enc) {
        if (enc == null || enc.isBlank()) {
            return null;
        }
        try {
            return new String(Base64.getUrlDecoder().decode(enc), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String normalizeVendor(String raw) {
        if (raw == null || raw.isBlank()) {
            return MerchantPgBrokerVendor.CHILLPAY;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (MerchantPgBrokerVendor.JPAY.equals(u) || u.startsWith(MerchantPgBrokerVendor.JPAY + "_")) {
            return MerchantPgBrokerVendor.JPAY;
        }
        return MerchantPgBrokerVendor.CHILLPAY;
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(resolveSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("inline checkout HMAC failed", e);
        }
    }

    private String resolveSecret() {
        return hqApiConfigRepository.findAll().stream()
                .findFirst()
                .map(HqApiConfig::getChillpayMd5Key)
                .filter(s -> s != null && !s.isBlank())
                .orElse("pg-merchant-inline-checkout-fallback");
    }

    private static String base64Url(String s) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String emptyToNull(String s) {
        return s == null || s.isEmpty() ? null : s;
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    public static String normalizeCurrency(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }
}
