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
 *   <li>{@code endpoint_api} → command URL (비우면 sandbox→DEV, 아니면 LIVE)</li>
 * </ul>
 * 환경(큐뱅 POS 가이드 v1.9): <strong>DEV/개발</strong>={@link #DEV_COMMAND_URL},
 * <strong>운영</strong>={@link #LIVE_COMMAND_URL}.
 */
public final class QbangCredentials {

    public static final String LIVE_COMMAND_URL = "https://api.qr-road.com/endpoints/pos/command";
    /** 큐뱅 개발(DEV) = api-test.qr-road.com */
    public static final String DEV_COMMAND_URL = "https://api-test.qr-road.com/endpoints/pos/command";
    /** 이름만 레거시 — {@link #DEV_COMMAND_URL} 과 동일 */
    @Deprecated
    public static final String TEST_COMMAND_URL = DEV_COMMAND_URL;

    private static final ObjectMapper OM = new ObjectMapper();

    /** 큐뱅 POS 가이드 간편결제사 — 제품은 Alipay 전용. */
    public static final String DEFAULT_PAYMENT_COMPANY_ID = "Alipay";

    private final String posId;
    private final String agencyCode;
    private final String paymentCompanyId;
    private final boolean sandbox;
    private final String commandUrl;

    public QbangCredentials(String posId, String agencyCode, boolean sandbox, String commandUrl) {
        this(posId, agencyCode, DEFAULT_PAYMENT_COMPANY_ID, sandbox, commandUrl);
    }

    public QbangCredentials(String posId, String agencyCode, String paymentCompanyId,
                            boolean sandbox, String commandUrl) {
        this.posId = nz(posId);
        this.agencyCode = nz(agencyCode);
        String pc = nz(paymentCompanyId);
        this.paymentCompanyId = pc.isBlank() ? DEFAULT_PAYMENT_COMPANY_ID : pc;
        this.sandbox = sandbox;
        String u = nz(commandUrl);
        if (u.isBlank()) {
            u = sandbox ? TEST_COMMAND_URL : LIVE_COMMAND_URL;
        }
        this.commandUrl = u;
    }

    public static QbangCredentials fromAgency(PgAgency agency) {
        if (agency == null) {
            return new QbangCredentials("", "", DEFAULT_PAYMENT_COMPANY_ID, true, TEST_COMMAND_URL);
        }
        boolean sandbox = !"N".equalsIgnoreCase(nz(agency.getSandboxYn()));
        String posId = nz(agency.getMerchantMid());
        String agencyCode = "";
        String paymentCompanyId = DEFAULT_PAYMENT_COMPANY_ID;
        String extra = agency.getCredentialsExtraJson();
        if (extra != null && !extra.isBlank()) {
            try {
                JsonNode n = OM.readTree(extra);
                if (posId.isBlank()) {
                    posId = text(n, "posId", "pos_id");
                }
                agencyCode = text(n, "agencyCode", "agency_code");
                String pc = text(n, "paymentCompanyId", "payment_company_id", "paymentBrand");
                if (!pc.isBlank()) {
                    paymentCompanyId = pc;
                }
            } catch (Exception ignored) {
                /* fall through */
            }
        }
        if (agencyCode.isBlank()) {
            agencyCode = nz(agency.getApiKey());
        }
        return new QbangCredentials(posId, agencyCode, paymentCompanyId, sandbox, nz(agency.getEndpointApi()));
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
        return new QbangCredentials(posId, agencyCode, base.paymentCompanyId(),
                base.sandbox(), base.commandUrl());
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

    public String paymentCompanyId() {
        return paymentCompanyId;
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
