package com.pg.integration.pg.ilk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.PgAgency;

/**
 * {@code tb_pg_agency} 행에서 ILK(아이엘케이) 자격·시드를 해석합니다.
 * <ul>
 *   <li>{@code merchant_mid} — ILK merchantId</li>
 *   <li>{@code credentials_extra_json} — merchantSiteId, seedKey, seedIv</li>
 *   <li>extra 비어 있으면 {@code md5_secret_key}=seedKey, {@code api_key}=seedIv 폴백</li>
 *   <li>{@code endpoint_api} — 없으면 sandbox/live 기본 도메인</li>
 * </ul>
 */
public record IlkCredentials(
        String merchantId,
        String merchantSiteId,
        String seedKey,
        String seedIv,
        boolean sandbox,
        String endpointApi
) {

    public static final String SANDBOX_BASE = "https://testocp.ilkrhub.com";
    public static final String LIVE_BASE = "https://ocp.ilkrhub.com";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static IlkCredentials from(PgAgency agency) {
        if (agency == null) {
            return new IlkCredentials("", "", "", "", true, SANDBOX_BASE);
        }
        String siteId = "";
        String seedKey = "";
        String seedIv = "";
        String extra = agency.getCredentialsExtraJson();
        if (extra != null && !extra.isBlank()) {
            try {
                JsonNode root = MAPPER.readTree(extra);
                siteId = text(root, "merchantSiteId");
                seedKey = text(root, "seedKey");
                seedIv = text(root, "seedIv");
            } catch (Exception ignored) {
            }
        }
        if (seedKey.isBlank()) {
            seedKey = nz(agency.getMd5SecretKey());
        }
        if (seedIv.isBlank()) {
            seedIv = nz(agency.getApiKey());
        }
        if (siteId.isBlank()) {
            // 일부 배포에서 siteId 를 api_key 에만 둔 경우 대비(시드와 충돌 시 extra 우선)
            siteId = "";
        }
        boolean sandbox = !"N".equalsIgnoreCase(nz(agency.getSandboxYn()));
        String endpoint = nz(agency.getEndpointApi());
        if (endpoint.isBlank()) {
            endpoint = sandbox ? SANDBOX_BASE : LIVE_BASE;
        }
        return new IlkCredentials(
                nz(agency.getMerchantMid()),
                siteId,
                seedKey,
                seedIv,
                sandbox,
                trimSlash(endpoint)
        );
    }

    public boolean isConfigured() {
        return !merchantId.isBlank() && !merchantSiteId.isBlank()
                && !seedKey.isBlank() && !seedIv.isBlank();
    }

    public String resolveBaseUrl() {
        String ep = endpointApi != null ? endpointApi.trim() : "";
        if (!ep.isBlank()) {
            return trimSlash(ep);
        }
        return sandbox ? SANDBOX_BASE : LIVE_BASE;
    }

    private static String text(JsonNode root, String field) {
        if (root == null || field == null) {
            return "";
        }
        JsonNode n = root.get(field);
        if (n == null || n.isNull()) {
            return "";
        }
        return n.asText("").trim();
    }

    private static String nz(String s) {
        return s != null ? s.trim() : "";
    }

    private static String trimSlash(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().replaceAll("/+$", "");
    }
}
