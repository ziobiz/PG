package com.pg.integration.pg.payso;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.PgAgency;

/**
 * Payso(Pay Solutions) 자격증명 — ICOPAY↔Payso 1:1.
 * <ul>
 *   <li>{@code api_key} → apiKey 헤더</li>
 *   <li>{@code secret_key} / credentialsExtra → secretKey 헤더</li>
 *   <li>{@code merchant_mid} → Payso merchantId(8)</li>
 *   <li>extra {@code terminalId} → terminalId</li>
 *   <li>{@code endpoint_api} → API base (기본 https://apis.paysolutions.asia)</li>
 * </ul>
 */
public final class PaysoCredentials {

    public static final String DEFAULT_API_BASE = "https://apis.paysolutions.asia";

    private static final ObjectMapper OM = new ObjectMapper();

    private final String apiKey;
    private final String secretKey;
    private final String merchantId;
    private final String terminalId;
    private final boolean sandbox;
    private final String apiBase;
    private final int keyVersion;

    public PaysoCredentials(String apiKey, String secretKey, String merchantId, String terminalId,
                            boolean sandbox, String apiBase, int keyVersion) {
        this.apiKey = nz(apiKey);
        this.secretKey = nz(secretKey);
        this.merchantId = nz(merchantId);
        this.terminalId = nz(terminalId);
        this.sandbox = sandbox;
        String base = nz(apiBase);
        if (base.isBlank()) {
            base = DEFAULT_API_BASE;
        }
        this.apiBase = base.replaceAll("/+$", "");
        this.keyVersion = keyVersion > 0 ? keyVersion : 1;
    }

    public static PaysoCredentials fromAgency(PgAgency agency) {
        if (agency == null) {
            return new PaysoCredentials("", "", "", "", true, DEFAULT_API_BASE, 1);
        }
        boolean sandbox = !"N".equalsIgnoreCase(nz(agency.getSandboxYn()));
        String base = nz(agency.getEndpointApi());
        String key = nz(agency.getApiKey());
        String secret = nz(agency.getMd5SecretKey());
        String mid = nz(agency.getMerchantMid());
        String terminal = "";
        int keyVer = 1;
        String extra = agency.getCredentialsExtraJson();
        if (extra != null && !extra.isBlank()) {
            try {
                JsonNode n = OM.readTree(extra);
                if (secret.isBlank()) {
                    secret = text(n, "secretKey", "secret_key");
                }
                if (mid.isBlank()) {
                    mid = text(n, "merchantId", "merchant_id");
                }
                terminal = text(n, "terminalId", "terminal_id");
                String kv = text(n, "keyVersion", "key_version");
                if (!kv.isBlank()) {
                    try {
                        keyVer = Integer.parseInt(kv.trim());
                    } catch (NumberFormatException ignored) {
                        keyVer = 1;
                    }
                }
            } catch (Exception ignored) {
                /* ignore */
            }
        }
        return new PaysoCredentials(key, secret, mid, terminal, sandbox, base, keyVer);
    }

    public boolean isUsable() {
        return !apiKey.isBlank() && !secretKey.isBlank() && !merchantId.isBlank() && !terminalId.isBlank();
    }

    public String apiKey() {
        return apiKey;
    }

    public String secretKey() {
        return secretKey;
    }

    public String merchantId() {
        return merchantId;
    }

    public String terminalId() {
        return terminalId;
    }

    public boolean sandbox() {
        return sandbox;
    }

    public String apiBase() {
        return apiBase;
    }

    public int keyVersion() {
        return keyVersion;
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
