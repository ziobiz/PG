package com.pg.integration.pg.omspay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.PgAgency;

/**
 * OMSPAY(Waylar OMS) 자격증명 — ICOPAY↔OMS 1:1.
 * <ul>
 *   <li>{@code api_key} → X-API-Key (wlr_…)</li>
 *   <li>{@code merchant_mid} 또는 extra {@code customerId} → OMS 공용 customer_id</li>
 *   <li>extra {@code webhookSecret} → x-waylar-signature</li>
 *   <li>extra {@code sku} → 가변금액용 SKU (기본 PAYMENT)</li>
 *   <li>{@code endpoint_api} → API base (기본 https://oms.waylar.net/api)</li>
 * </ul>
 */
public final class OmspayCredentials {

    public static final String DEFAULT_API_BASE = "https://oms.waylar.net/api";
    public static final String DEFAULT_SKU = "PAYMENT";

    private static final ObjectMapper OM = new ObjectMapper();

    private final String apiKey;
    private final String customerId;
    private final String webhookSecret;
    private final String sku;
    private final boolean sandbox;
    private final String apiBase;

    public OmspayCredentials(String apiKey, String customerId, String webhookSecret,
                             String sku, boolean sandbox, String apiBase) {
        this.apiKey = nz(apiKey);
        this.customerId = nz(customerId);
        this.webhookSecret = nz(webhookSecret);
        String s = nz(sku);
        this.sku = s.isBlank() ? DEFAULT_SKU : s;
        this.sandbox = sandbox;
        String base = nz(apiBase);
        if (base.isBlank()) {
            base = DEFAULT_API_BASE;
        }
        this.apiBase = base.replaceAll("/+$", "");
    }

    public static OmspayCredentials fromAgency(PgAgency agency) {
        if (agency == null) {
            return new OmspayCredentials("", "", "", DEFAULT_SKU, true, DEFAULT_API_BASE);
        }
        boolean sandbox = !"N".equalsIgnoreCase(nz(agency.getSandboxYn()));
        String base = nz(agency.getEndpointApi());
        String key = nz(agency.getApiKey());
        String customerId = nz(agency.getMerchantMid());
        String webhook = "";
        String sku = DEFAULT_SKU;
        String extra = agency.getCredentialsExtraJson();
        if (extra != null && !extra.isBlank()) {
            try {
                JsonNode n = OM.readTree(extra);
                webhook = text(n, "webhookSecret", "webhook_secret", "signingSecret");
                if (customerId.isBlank()) {
                    customerId = text(n, "customerId", "customer_id");
                }
                String sk = text(n, "sku", "productSku");
                if (!sk.isBlank()) {
                    sku = sk;
                }
            } catch (Exception ignored) {
                /* column fallbacks */
            }
        }
        return new OmspayCredentials(key, customerId, webhook, sku, sandbox, base);
    }

    public boolean isUsable() {
        return !apiKey.isBlank();
    }

    public boolean canCreateOrder() {
        return isUsable() && !customerId.isBlank();
    }

    public String apiKey() {
        return apiKey;
    }

    public String customerId() {
        return customerId;
    }

    public String webhookSecret() {
        return webhookSecret;
    }

    public String sku() {
        return sku;
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
