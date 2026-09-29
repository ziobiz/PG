package com.pg.integration.pg.ox;

import com.pg.entity.PgAgency;

/**
 * ox(OxPay Financial) 자격증명 — Merchant Portal API Key / Sign Key.
 * Create Payment(Merchant Hosted) 상세 엔드포인트는 OxPay 제공 API 문서로 확정.
 */
public final class OxCredentials {

    public static final String DEFAULT_LIVE_BASE = "https://api.oxpayfinancial.com";
    public static final String DEFAULT_SANDBOX_BASE = "https://api.oxpayfinancial.com";

    private final String apiKey;
    private final String signKey;
    private final String mid;
    private final boolean sandbox;
    private final String apiBase;

    public OxCredentials(String apiKey, String signKey, String mid, boolean sandbox, String apiBase) {
        this.apiKey = apiKey != null ? apiKey : "";
        this.signKey = signKey != null ? signKey : "";
        this.mid = mid != null ? mid : "";
        this.sandbox = sandbox;
        this.apiBase = apiBase != null && !apiBase.isBlank()
                ? apiBase.trim().replaceAll("/+$", "")
                : DEFAULT_LIVE_BASE;
    }

    public static OxCredentials fromAgency(PgAgency agency) {
        if (agency == null) {
            return new OxCredentials("", "", "", true, DEFAULT_SANDBOX_BASE);
        }
        boolean sandbox = !"N".equalsIgnoreCase(nz(agency.getSandboxYn()));
        String base = nz(agency.getEndpointApi());
        if (base.isBlank()) {
            base = sandbox ? DEFAULT_SANDBOX_BASE : DEFAULT_LIVE_BASE;
        }
        String apiKey = nz(agency.getApiKey());
        if (apiKey.isBlank()) {
            apiKey = nz(agency.getMerchantMid());
        }
        String sign = nz(agency.getMd5SecretKey());
        return new OxCredentials(apiKey, sign, nz(agency.getMerchantMid()), sandbox, base);
    }

    public boolean isUsable() {
        return !apiKey.isBlank();
    }

    public String apiKey() {
        return apiKey;
    }

    public String signKey() {
        return signKey;
    }

    public String mid() {
        return mid;
    }

    public boolean sandbox() {
        return sandbox;
    }

    public String apiBase() {
        return apiBase;
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }
}
