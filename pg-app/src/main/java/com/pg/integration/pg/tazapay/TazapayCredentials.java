package com.pg.integration.pg.tazapay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.PgAgency;

/**
 * Tazapay API 자격증명 — ICOPAY↔Tazapay 1:1 (가맹 MID 미전달).
 * <ul>
 *   <li>{@code api_key} → API Key (Basic auth user)</li>
 *   <li>{@code md5_secret_key} 또는 credentialsExtra {@code apiSecret} → API Secret</li>
 *   <li>credentialsExtra {@code publicKey} → JS SDK pk_…</li>
 *   <li>{@code endpoint_api} → API base (sandbox / live)</li>
 * </ul>
 */
public final class TazapayCredentials {

    public static final String DEFAULT_SANDBOX_API = "https://service-sandbox.tazapay.com";
    public static final String DEFAULT_LIVE_API = "https://service.tazapay.com";
    public static final String JS_SANDBOX = "https://js-sandbox.tazapay.com/v3.js";
    public static final String JS_LIVE = "https://js.tazapay.com/v3.js";

    private static final ObjectMapper OM = new ObjectMapper();

    private final String apiKey;
    private final String apiSecret;
    private final String publicKey;
    private final boolean sandbox;
    private final String apiBase;
    private final String jsSdkUrl;

    public TazapayCredentials(String apiKey, String apiSecret, String publicKey,
                                boolean sandbox, String apiBase, String jsSdkUrl) {
        this.apiKey = nz(apiKey);
        this.apiSecret = nz(apiSecret);
        this.publicKey = nz(publicKey);
        this.sandbox = sandbox;
        String base = nz(apiBase);
        if (base.isBlank()) {
            base = sandbox ? DEFAULT_SANDBOX_API : DEFAULT_LIVE_API;
        }
        this.apiBase = base.replaceAll("/+$", "");
        this.jsSdkUrl = nz(jsSdkUrl).isEmpty()
                ? (sandbox ? JS_SANDBOX : JS_LIVE)
                : nz(jsSdkUrl);
    }

    public static TazapayCredentials fromAgency(PgAgency agency) {
        if (agency == null) {
            return new TazapayCredentials("", "", "", true, DEFAULT_SANDBOX_API, JS_SANDBOX);
        }
        boolean sandbox = !"N".equalsIgnoreCase(nz(agency.getSandboxYn()));
        String base = nz(agency.getEndpointApi());
        String key = nz(agency.getApiKey());
        String secret = nz(agency.getMd5SecretKey());
        String publicKey = "";
        String jsUrl = "";
        String extra = agency.getCredentialsExtraJson();
        if (extra != null && !extra.isBlank()) {
            try {
                JsonNode n = OM.readTree(extra);
                if (secret.isBlank()) {
                    secret = text(n, "apiSecret", "api_secret", "secretKey", "secret_key");
                }
                publicKey = text(n, "publicKey", "public_key", "pk");
                jsUrl = text(n, "jsSdkUrl", "js_sdk_url", "tazapayJsUrl");
            } catch (Exception ignored) {
                /* ignore */
            }
        }
        if (base.isBlank()) {
            base = sandbox ? DEFAULT_SANDBOX_API : DEFAULT_LIVE_API;
        }
        return new TazapayCredentials(key, secret, publicKey, sandbox, base, jsUrl);
    }

    public boolean isUsable() {
        return !apiKey.isBlank() && !apiSecret.isBlank() && !publicKey.isBlank();
    }

    public boolean isApiUsable() {
        return !apiKey.isBlank() && !apiSecret.isBlank();
    }

    public String apiKey() {
        return apiKey;
    }

    public String apiSecret() {
        return apiSecret;
    }

    public String publicKey() {
        return publicKey;
    }

    public boolean sandbox() {
        return sandbox;
    }

    public String apiBase() {
        return apiBase;
    }

    public String jsSdkUrl() {
        return jsSdkUrl;
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