package com.pg.integration.pg.ox;

import com.pg.entity.PgAgency;

/**
 * ox(OxPay Financial) 자격증명 — Merchant Portal API Key / Sign Key.
 * Create Payment(Merchant Hosted) 상세 엔드포인트는 OxPay 제공 API 문서로 확정.
 * <p>OxPay Getting Started: 별도 UAT/샌드박스 API는 없습니다. 테스트도 라이브 주소에서
 * OxPay가 준 테스트 케이스로 합니다. {@code sandboxYn} 이어도 base URL 은 같습니다.
 */
public final class OxCredentials {

    public static final String DEFAULT_LIVE_BASE = "https://api.oxpayfinancial.com";
    /** ox 공개 문서에 분리된 샌드박스 호스트가 없어 라이브와 같은 주소입니다. */
    public static final String DEFAULT_SANDBOX_BASE = DEFAULT_LIVE_BASE;

    /** OxPay 는 별도 샌드박스 API 를 제공하지 않습니다. */
    public static boolean hasDedicatedSandbox() {
        return false;
    }

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
