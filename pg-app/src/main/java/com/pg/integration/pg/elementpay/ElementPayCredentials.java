package com.pg.integration.pg.elementpay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.PgAgency;

import java.util.Locale;

/**
 * {@code tb_pg_agency} 행에서 ElementPay 자격·서비스 alias 를 해석합니다.
 * <ul>
 *   <li>{@code merchant_mid} — Merchant Key (UUID)</li>
 *   <li>{@code md5_secret_key} — API Secret Key (initPayment/getStatus HMAC)</li>
 *   <li>{@code api_key} — Webhook Signing Secret (check/pay 콜백 HMAC)</li>
 *   <li>{@code credentials_extra_json.cardServiceAlias} — 기본 {@code kCards} (EP THB 카드)</li>
 *   <li>{@code credentials_extra_json.promptPayServiceAlias} — 기본 {@code promptpay}</li>
 * </ul>
 */
public record ElementPayCredentials(
        String merchantKey,
        String apiSecretKey,
        String webhookSecretKey,
        boolean sandbox,
        String cardServiceAlias,
        String promptPayServiceAlias
) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static ElementPayCredentials from(PgAgency agency) {
        if (agency == null) {
            return new ElementPayCredentials("", "", "", true, "card", "promptpay");
        }
        String extra = agency.getCredentialsExtraJson();
        String cardAlias = "kCards";
        String ppAlias = "promptpay";
        if (extra != null && !extra.isBlank()) {
            try {
                JsonNode root = MAPPER.readTree(extra);
                JsonNode c = root.get("cardServiceAlias");
                if (c != null && !c.isNull() && !c.asText("").isBlank()) {
                    cardAlias = c.asText("").trim();
                }
                JsonNode p = root.get("promptPayServiceAlias");
                if (p != null && !p.isNull() && !p.asText("").isBlank()) {
                    ppAlias = p.asText("").trim();
                }
            } catch (Exception ignored) {
            }
        }
        String webhook = nz(agency.getApiKey());
        if (webhook.isEmpty()) {
            webhook = nz(agency.getMd5SecretKey());
        }
        return new ElementPayCredentials(
                nz(agency.getMerchantMid()),
                nz(agency.getMd5SecretKey()),
                webhook,
                !"N".equalsIgnoreCase(nz(agency.getSandboxYn())),
                cardAlias,
                ppAlias
        );
    }

    public boolean isConfigured() {
        return !merchantKey.isBlank() && !apiSecretKey.isBlank();
    }

    public String serviceAliasForMethod(String paymentMethod) {
        if (paymentMethod == null) {
            return cardServiceAlias;
        }
        String u = paymentMethod.trim().toUpperCase(Locale.ROOT);
        if ("PROMPTPAY".equals(u) || "QR".equals(u) || "PP".equals(u)) {
            return promptPayServiceAlias;
        }
        return cardServiceAlias;
    }

    private static String nz(String s) {
        return s != null ? s.trim() : "";
    }
}
