package com.pg.integration.pg.qxipay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.PgAgency;

/**
 * QXIPAY 게이트웨이 자격증명 — HTTP Basic {@code accountId:secretKey} / publishableKey.
 * <ul>
 *   <li>{@code merchant_mid} → accountId</li>
 *   <li>{@code api_key} → secretKey (sk_…)</li>
 *   <li>{@code md5_secret_key} 또는 extra JSON {@code publishableKey} → pk_…</li>
 *   <li>extra JSON {@code webhookSecret} → GW-Signature 검증</li>
 *   <li>{@code endpoint_api} → API base (미입력 시 샌드박스 기본)</li>
 * </ul>
 */
public final class QxipayCredentials {

    /** OpenAPI servers.url — Payment Gateway API Sandbox */
    public static final String DEFAULT_SANDBOX_BASE = "http://68.183.182.9/api/sandbox";
    public static final String DEFAULT_LIVE_BASE = "https://api.qxipay.com/v1";

    private static final ObjectMapper OM = new ObjectMapper();

    private final String accountId;
    private final String secretKey;
    private final String publishableKey;
    private final String webhookSecret;
    private final boolean sandbox;
    private final String apiBase;

    public QxipayCredentials(String accountId, String secretKey, String publishableKey,
                             String webhookSecret, boolean sandbox, String apiBase) {
        this.accountId = nz(accountId);
        this.secretKey = nz(secretKey);
        this.publishableKey = nz(publishableKey);
        this.webhookSecret = nz(webhookSecret);
        this.sandbox = sandbox;
        String base = nz(apiBase);
        if (base.isBlank()) {
            base = sandbox ? DEFAULT_SANDBOX_BASE : DEFAULT_LIVE_BASE;
        }
        this.apiBase = base.replaceAll("/+$", "");
    }

    public static QxipayCredentials fromAgency(PgAgency agency) {
        if (agency == null) {
            return new QxipayCredentials("", "", "", "", true, DEFAULT_SANDBOX_BASE);
        }
        boolean sandbox = !"N".equalsIgnoreCase(nz(agency.getSandboxYn()));
        String base = nz(agency.getEndpointApi());
        String accountId = nz(agency.getMerchantMid());
        String secret = nz(agency.getApiKey());
        String pk = nz(agency.getMd5SecretKey());
        String webhook = "";
        String extra = agency.getCredentialsExtraJson();
        if (extra != null && !extra.isBlank()) {
            try {
                JsonNode n = OM.readTree(extra);
                if (pk.isBlank()) {
                    pk = text(n, "publishableKey", "pk", "publicKey");
                }
                webhook = text(n, "webhookSecret", "webhook_secret", "signingSecret");
                if (accountId.isBlank()) {
                    accountId = text(n, "accountId", "account_id");
                }
            } catch (Exception ignored) {
                /* keep column fallbacks */
            }
        }
        return new QxipayCredentials(accountId, secret, pk, webhook, sandbox, base);
    }

    public boolean isUsable() {
        return !accountId.isBlank() && !secretKey.isBlank();
    }

    public boolean canTokenize() {
        return isUsable() && !publishableKey.isBlank();
    }

    public String accountId() {
        return accountId;
    }

    public String secretKey() {
        return secretKey;
    }

    public String publishableKey() {
        return publishableKey;
    }

    public String webhookSecret() {
        return webhookSecret;
    }

    public boolean sandbox() {
        return sandbox;
    }

    public String apiBase() {
        return apiBase;
    }

    private static String text(JsonNode n, String... keys) {
        if (n == null || keys == null) {
            return "";
        }
        for (String k : keys) {
            if (k == null) {
                continue;
            }
            JsonNode v = n.get(k);
            if (v != null && !v.isNull() && v.isValueNode()) {
                String s = v.asText("");
                if (s != null && !s.isBlank()) {
                    return s.trim();
                }
            }
        }
        return "";
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }
}
