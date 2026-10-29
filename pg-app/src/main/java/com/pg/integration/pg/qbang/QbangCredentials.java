package com.pg.integration.pg.qbang;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.PgAgency;

/**
 * QBANG(큐뱅) 자격증명 — posId + agencyCode.
 * <ul>
 *   <li>agency {@code merchant_mid} 또는 binding {@code mid} → posId</li>
 *   <li>extra {@code agencyCode} / agency {@code api_key} 보조 → agencyCode</li>
 *   <li>{@code endpoint_api} → command URL (기본 live/test)</li>
 * </ul>
 */
public final class QbangCredentials {

    public static final String LIVE_COMMAND_URL = "https://api.qr-road.com/endpoints/pos/command";
    public static final String TEST_COMMAND_URL = "https://api-test.qr-road.com/endpoints/pos/command";

    private static final ObjectMapper OM = new ObjectMapper();

    private final String posId;
    private final String agencyCode;
    private final boolean sandbox;
    private final String commandUrl;

    public QbangCredentials(String posId, String agencyCode, boolean sandbox, String commandUrl) {
        this.posId = nz(posId);
        this.agencyCode = nz(agencyCode);
        this.sandbox = sandbox;
        String u = nz(commandUrl);
        if (u.isBlank()) {
            u = sandbox ? TEST_COMMAND_URL : LIVE_COMMAND_URL;
        }
        this.commandUrl = u;
    }

    public static QbangCredentials fromAgency(PgAgency agency) {
        if (agency == null) {
            return new QbangCredentials("", "", true, TEST_COMMAND_URL);
        }
        boolean sandbox = !"N".equalsIgnoreCase(nz(agency.getSandboxYn()));
        String posId = nz(agency.getMerchantMid());
        String agencyCode = "";
        String extra = agency.getCredentialsExtraJson();
        if (extra != null && !extra.isBlank()) {
            try {
                JsonNode n = OM.readTree(extra);
                if (posId.isBlank()) {
                    posId = text(n, "posId", "pos_id");
                }
                agencyCode = text(n, "agencyCode", "agency_code");
            } catch (Exception ignored) {
                /* fall through */
            }
        }
        if (agencyCode.isBlank()) {
            agencyCode = nz(agency.getApiKey());
        }
        return new QbangCredentials(posId, agencyCode, sandbox, nz(agency.getEndpointApi()));
    }

    public static QbangCredentials fromAgencyAndBinding(PgAgency agency, MerchantPgBinding binding) {
        QbangCredentials base = fromAgency(agency);
        if (binding == null) {
            return base;
        }
        String posId = nz(binding.getMid());
        if (posId.isBlank()) {
            posId = base.posId();
        }
        String agencyCode = base.agencyCode();
        String extra = binding.getApiKey();
        /* binding.apiKey 에 agencyCode 를 두는 경우도 허용 */
        if (agencyCode.isBlank() && extra != null && !extra.isBlank() && !extra.contains("{")) {
            agencyCode = extra.trim();
        }
        return new QbangCredentials(posId, agencyCode, base.sandbox(), base.commandUrl());
    }

    public boolean isUsable() {
        return !posId.isBlank();
    }

    public String posId() {
        return posId;
    }

    public String agencyCode() {
        return agencyCode;
    }

    public boolean sandbox() {
        return sandbox;
    }

    public String commandUrl() {
        return commandUrl;
    }

    private static String text(JsonNode n, String... keys) {
        if (n == null || keys == null) {
            return "";
        }
        for (String k : keys) {
            if (n.has(k) && !n.get(k).isNull()) {
                String v = n.get(k).asText("").trim();
                if (!v.isBlank()) {
                    return v;
                }
            }
        }
        return "";
    }

    private static String nz(String s) {
        return s != null ? s.trim() : "";
    }
}
