package com.pg.integration.pg.qbang;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.PgAgency;

/**
 * QBANG(큐뱅) 자격증명 — ICOPAY 집계 POS 1개({@code posId}+{@code agencyCode}).
 * <p>
 * 통합 원칙: 가맹 MID·상호는 큐뱅으로 보내지 않는다. 여러 가맹은 동일 agency posId 로
 * TK/QR/PL/OKAY 하고, ICOPAY 주문번호·transactionNo 로 가맹을 구분한다.
 * </p>
 * <ul>
 *   <li>agency {@code merchant_mid} / extra {@code posId} → posId (ICOPAY 집계)</li>
 *   <li>extra {@code agencyCode} / agency {@code api_key} → agencyCode</li>
 *   <li>{@code endpoint_api} → command URL (비우면 sandbox→DEV, 아니면 LIVE)</li>
 *   <li>가맹 binding {@code mid} 는 posId 로 쓰지 않음(가맹 식별 유출·가맹별 POS 오해 방지)</li>
 * </ul>
 */
public final class QbangCredentials {

    public static final String LIVE_COMMAND_URL = "https://api.qr-road.com/endpoints/pos/command";
    /** 큐뱅 개발(DEV) = api-test.qr-road.com */
    public static final String DEV_COMMAND_URL = "https://api-test.qr-road.com/endpoints/pos/command";
    /** 이름만 레거시 — {@link #DEV_COMMAND_URL} 과 동일 */
    @Deprecated
    public static final String TEST_COMMAND_URL = DEV_COMMAND_URL;

    private static final ObjectMapper OM = new ObjectMapper();

    private final String posId;
    private final String agencyCode;
    /** 비어 있으면 QR payload에 paymentCompanyId 미전송(권장). */
    private final String paymentCompanyId;
    private final boolean sandbox;
    private final String commandUrl;

    public QbangCredentials(String posId, String agencyCode, boolean sandbox, String commandUrl) {
        this(posId, agencyCode, "", sandbox, commandUrl);
    }

    public QbangCredentials(String posId, String agencyCode, String paymentCompanyId,
                            boolean sandbox, String commandUrl) {
        this.posId = nz(posId);
        this.agencyCode = nz(agencyCode);
        this.paymentCompanyId = nz(paymentCompanyId);
        this.sandbox = sandbox;
        String u = nz(commandUrl);
        if (u.isBlank()) {
            u = sandbox ? TEST_COMMAND_URL : LIVE_COMMAND_URL;
        }
        this.commandUrl = u;
    }

    public static QbangCredentials fromAgency(PgAgency agency) {
        if (agency == null) {
            return new QbangCredentials("", "", "", true, TEST_COMMAND_URL);
        }
        boolean sandbox = !"N".equalsIgnoreCase(nz(agency.getSandboxYn()));
        String posId = nz(agency.getMerchantMid());
        String agencyCode = "";
        String paymentCompanyId = "";
        String extra = agency.getCredentialsExtraJson();
        if (extra != null && !extra.isBlank()) {
            try {
                JsonNode n = OM.readTree(extra);
                if (posId.isBlank()) {
                    posId = text(n, "posId", "pos_id");
                }
                agencyCode = text(n, "agencyCode", "agency_code");
                paymentCompanyId = text(n, "paymentCompanyId", "payment_company_id");
            } catch (Exception ignored) {
                /* fall through */
            }
        }
        if (agencyCode.isBlank()) {
            agencyCode = nz(agency.getApiKey());
        }
        return new QbangCredentials(posId, agencyCode, paymentCompanyId, sandbox, nz(agency.getEndpointApi()));
    }

    /**
     * 가맹 바인딩이 있어도 큐뱅 posId 는 agency(집계)만 사용.
     * binding 은 QR 사용 여부·라우팅 확인용이며, mid 를 posId 로 덮지 않는다.
     */
    public static QbangCredentials fromAgencyAndBinding(PgAgency agency, MerchantPgBinding binding) {
        return fromAgency(agency);
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
