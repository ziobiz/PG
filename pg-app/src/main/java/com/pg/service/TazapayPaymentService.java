package com.pg.service;

import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.integration.pg.tazapay.TazapayApiClient;
import com.pg.integration.pg.tazapay.TazapayCredentials;
import com.pg.middleware.notify.PgNotifyIngressPaths;
import com.pg.noti.NotiProvisionClient;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.urlpay.PayerContextCapture;
import com.pg.urlpay.UrlPayPublicCheckoutService;
import com.pg.util.IcopayPgOrderRef;
import com.pg.util.PayPresaleRiskFilterCodes;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Tazapay — Create Payin(client_token) + 카드 embed / 선택적 Hosted Checkout.
 */
@Service
public class TazapayPaymentService {

    private static final Logger log = LoggerFactory.getLogger(TazapayPaymentService.class);
    public static final String NOTI_WEBHOOK_PATH = "/noti/tazapay";
    public static final String NOTI_RESULT_PATH = "/noti/result/tazapay";

    private final OrgUnitRepository orgUnitRepository;
    private final PgAgencyRepository pgAgencyRepository;
    private final ChillPayService chillPayService;
    private final UrlPayChargeResolutionService urlPayChargeResolutionService;
    private final TazapayApiClient apiClient;
    private final TazapaySaleRecordService tazapaySaleRecordService;
    private final HqNotifyEnvService hqNotifyEnvService;
    private final HostedPayReturnTokenService hostedPayReturnTokenService;
    private final PayPresaleRiskFilterService payPresaleRiskFilterService;
    private final UrlPayPublicCheckoutService urlPayPublicCheckoutService;

    @Value("${pg.public-base-url:https://api.icopay.co.kr}")
    private String publicBaseUrl;

    public TazapayPaymentService(OrgUnitRepository orgUnitRepository,
                                 PgAgencyRepository pgAgencyRepository,
                                 ChillPayService chillPayService,
                                 UrlPayChargeResolutionService urlPayChargeResolutionService,
                                 TazapayApiClient apiClient,
                                 TazapaySaleRecordService tazapaySaleRecordService,
                                 HqNotifyEnvService hqNotifyEnvService,
                                 HostedPayReturnTokenService hostedPayReturnTokenService,
                                 PayPresaleRiskFilterService payPresaleRiskFilterService,
                                 @Lazy UrlPayPublicCheckoutService urlPayPublicCheckoutService) {
        this.orgUnitRepository = orgUnitRepository;
        this.pgAgencyRepository = pgAgencyRepository;
        this.chillPayService = chillPayService;
        this.urlPayChargeResolutionService = urlPayChargeResolutionService;
        this.apiClient = apiClient;
        this.tazapaySaleRecordService = tazapaySaleRecordService;
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.hostedPayReturnTokenService = hostedPayReturnTokenService;
        this.payPresaleRiskFilterService = payPresaleRiskFilterService;
        this.urlPayPublicCheckoutService = urlPayPublicCheckoutService;
    }

    public Map<String, String> publicNotiIngressUrls() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("pgVendor", PgVendor.TAZAPAY);
        m.put("notiWebhookPath", NOTI_WEBHOOK_PATH);
        String notiBase = NotiProvisionClient.defaultBaseUrlIfBlank(
                hqNotifyEnvService.getOrCreate().getNotiProvisionBaseUrl());
        m.put("tazapayResultUrl", notiBase + NOTI_RESULT_PATH);
        String ingressToken = hqNotifyEnvService.getOrCreate().getIngressToken();
        m.put("icopayIngressBase",
                PgNotifyIngressPaths.buildIngressBase(publicBaseUrl, ingressToken) + "/" + PgVendor.TAZAPAY);
        String base = publicBaseUrl != null ? publicBaseUrl.trim().replaceAll("/+$", "") : "https://api.icopay.co.kr";
        m.put("icopayOrderLookupUrl", base + "/api/pay/tazapay/order-lookup?order={order}");
        return m;
    }

    public Optional<TazapayCredentials> resolveCredentials(Long orgUnitId) {
        String opPg = chillPayService.resolveUrlPayOperationalPgCdPreferringFamily(
                orgUnitId, PgVendor::isTazapayFamily);
        if (opPg == null || !PgVendor.isTazapayFamily(opPg)) {
            return Optional.empty();
        }
        return pgAgencyRepository.findByPgCd(opPg).map(TazapayCredentials::fromAgency);
    }

    public Map<String, Object> checkoutContext(Long orgUnitId, HttpServletRequest request) {
        Map<String, Object> data = urlPayPublicCheckoutService.buildCheckoutContext(orgUnitId, request, false);
        String opPg = chillPayService.resolveUrlPayOperationalPgCdPreferringFamily(
                orgUnitId, PgVendor::isTazapayFamily);
        if (opPg == null || opPg.isBlank()) {
            throw new IllegalStateException("TAZAPAY_URL_NOT_CONFIGURED");
        }
        urlPayPublicCheckoutService.rebindOperationalPg(data, orgUnitId, opPg, request);
        resolveCredentials(orgUnitId).ifPresent(c -> data.put("tazapaySandbox", c.sandbox()));
        return data;
    }

  /**
     * URL / 인라인 embed — {@code /api/pay/url/sale} 진입점. client_token·publicKey 반환.
     */
    public Map<String, Object> executeSale(Long orgUnitId, Map<String, Object> body,
                                           HttpServletRequest req, String clientIp) {
        if (orgUnitId == null) {
            return fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND");
        }
        Optional<OrgUnit> ou = orgUnitRepository.findById(orgUnitId);
        if (ou.isEmpty()) {
            return fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND");
        }
        Optional<TazapayCredentials> credOpt = resolveCredentials(orgUnitId);
        if (credOpt.isEmpty() || !credOpt.get().isUsable()) {
            return fail("Tazapay API·publicKey가 없습니다. 결제대행사 설정에서 등록하세요.", "TAZAPAY_CREDENTIALS_MISSING");
        }
        TazapayCredentials creds = credOpt.get();

        PayerContextCapture.enrichSaleBody(body, req, clientIp);
        String compCode = ou.get().getCode();
        String opPg = chillPayService.resolveUrlPayOperationalPgCdPreferringFamily(
                orgUnitId, PgVendor::isTazapayFamily);

        UrlPayChargeResolutionService.ResolvedCharge charge;
        try {
            charge = urlPayChargeResolutionService.resolve(orgUnitId, body != null ? body : Map.of(), opPg);
        } catch (IllegalArgumentException e) {
            return fail(UrlPayChargeResolutionService.failMessageForCode(e.getMessage()), e.getMessage());
        }

        String orderNo = str(body.get("orderNo"));
        if (orderNo.isBlank()) {
            return fail("orderNo가 필요합니다.", "INVALID_ORDER_NO");
        }
        String buyerName = firstNonBlank(str(body.get("customerNm")), str(body.get("buyerName")),
                joinName(str(body.get("payFirstname")), str(body.get("payLastname"))));
        String buyerEmail = firstNonBlank(str(body.get("payEmailAddress")), str(body.get("buyerEmail")),
                str(body.get("email")));
        String buyerTel = firstNonBlank(str(body.get("payTelephone")), str(body.get("phone")));
        String origin = firstNonBlank(str(body.get("txnOrigin")), str(body.get("origin")), "URL");

        if (!creds.sandbox()) {
            Optional<PayPresaleRiskFilterService.PresaleRiskBlock> presale =
                    payPresaleRiskFilterService.evaluate(orgUnitId, compCode, PgVendor.TAZAPAY, body);
            if (presale.isPresent()) {
                PayPresaleRiskFilterService.PresaleRiskBlock block = presale.get();
                payPresaleRiskFilterService.recordEvent(orgUnitId, compCode, orderNo, null, PgVendor.TAZAPAY, block);
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("success", false);
                out.put("errorCode", PayPresaleRiskFilterCodes.ERROR_CODE);
                out.put("filterCode", block.filterCode());
                out.put("message", block.message());
                out.put("messages", block.messages());
                out.put("icopayPresaleBlock", true);
                return out;
            }
        }

        final String pgOrderRef = IcopayPgOrderRef.standard();
        IcopayPgOrderRef.putInSaleBody(body, pgOrderRef);

        tazapaySaleRecordService.recordOrTouchPending(orgUnitId, orderNo, charge.pgAmount(),
                charge.settlementCurrency(), null, origin,
                charge.shopperDisplayAmount(), charge.shopperDisplayCurrency(),
                buyerName, buyerEmail, buyerTel, body);

        String publicBase = resolvePublicApiBase(req);
        boolean hosted = "1".equals(str(body.get("hostedCheckout")))
                || "HOSTED".equalsIgnoreCase(str(body.get("integrationMode")));
        String returnUrl = hostedPayReturnTokenService.buildOpaqueReturnUrl(
                publicBase, "/tazapay-pay.html", PgVendor.TAZAPAY,
                compCode, orderNo, "MERCHANT_API".equalsIgnoreCase(origin),
                "tazapayReturn=1&done=1");
        String cancelUrl = hostedPayReturnTokenService.buildOpaqueReturnUrl(
                publicBase, "/tazapay-pay.html", PgVendor.TAZAPAY,
                compCode, orderNo, "MERCHANT_API".equalsIgnoreCase(origin),
                "tazapayCancel=1");
        String webhookUrl = NotiProvisionClient.defaultBaseUrlIfBlank(
                hqNotifyEnvService.getOrCreate().getNotiProvisionBaseUrl()) + NOTI_WEBHOOK_PATH;

        Map<String, Object> api;
        if (hosted) {
            api = apiClient.createCheckout(creds, pgOrderRef, charge.pgAmount(), charge.settlementCurrency(),
                    buyerEmail, returnUrl, cancelUrl, webhookUrl);
        } else {
            String buyerCountry = firstNonBlank(
                    str(body.get("payCountry")), str(body.get("country")),
                    str(body.get("countryCode")), str(body.get("billingCountry")));
            api = apiClient.createPayin(creds, pgOrderRef, charge.pgAmount(), charge.settlementCurrency(),
                    buyerEmail, buyerName, buyerCountry, returnUrl, cancelUrl, webhookUrl);
        }
        if (!Boolean.TRUE.equals(api.get("ok"))) {
            String msg = str(api.get("message"));
            if (msg.isBlank()) {
                msg = "Tazapay 결제 생성에 실패했습니다.";
            }
            String code = str(api.get("errorCode"));
            if (code.isBlank()) {
                code = "TAZAPAY_CREATE";
            }
            return fail(msg, code);
        }
        String payinId = str(api.get("payinId"));
        if (!payinId.isBlank()) {
            tazapaySaleRecordService.recordOrTouchPending(orgUnitId, orderNo, charge.pgAmount(),
                    charge.settlementCurrency(), payinId, origin,
                    charge.shopperDisplayAmount(), charge.shopperDisplayCurrency(),
                    buyerName, buyerEmail, buyerTel, body);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("clientToken", str(api.get("clientToken")));
        data.put("payinId", payinId);
        data.put("publicKey", creds.publicKey());
        data.put("tazapayJsUrl", creds.jsSdkUrl());
        data.put("sandbox", creds.sandbox());
        data.put("referenceId", pgOrderRef);
        data.put("orderNo", orderNo);
        data.put("compId", compCode);
        data.put("amount", charge.pgAmount().stripTrailingZeros().toPlainString());
        data.put("currency", charge.settlementCurrency());
        data.put("successUrl", returnUrl);
        data.put("cancelUrl", cancelUrl);
        String redirectUrl = str(api.get("redirectUrl"));
        if (hosted && !redirectUrl.isBlank()) {
            data.put("redirectUrl", redirectUrl);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("pgVendor", "ICOPAY");
        out.put("data", data);
        log.info("TAZAPAY payin created mid={} order={} ref={} payin={} hosted={}", compCode, orderNo, pgOrderRef, payinId, hosted);
        return out;
    }

    public Map<String, Object> buyerResultStatus(Long orgUnitId, String orderNo) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pgVendor", "ICOPAY");
        if (orgUnitId == null || orderNo == null || orderNo.isBlank()) {
            return fail("orderNo가 필요합니다.", "INVALID_ORDER_NO");
        }
        Optional<OrgUnit> ou = orgUnitRepository.findById(orgUnitId);
        if (ou.isEmpty()) {
            return fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND");
        }
        Optional<PgTrnsctn> txn = tazapaySaleRecordService.findByMerchantAndOrder(ou.get().getCode(), orderNo);
        if (txn.isEmpty()) {
            data.put("found", false);
            data.put("paymentStatus", "NOT_FOUND");
        } else {
            PgTrnsctn t = txn.get();
            data.put("found", true);
            data.put("paymentStatus", mapPaymentStatus(t.getStatus()));
            data.put("transactionId", t.getTrnId());
            data.put("amount", t.getAmtKrw());
            data.put("currency", t.getCurType());
            if (t.getChillTransactionId() != null && !t.getChillTransactionId().isBlank()) {
                data.put("pgTxnId", t.getChillTransactionId().trim());
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("data", data);
        return out;
    }

    public Optional<PgTrnsctn> confirmPaidViaGetPayin(String compId, String orderNo) {
        if (compId == null || compId.isBlank() || orderNo == null || orderNo.isBlank()) {
            return Optional.empty();
        }
        Optional<PgTrnsctn> txn = tazapaySaleRecordService.findByMerchantAndOrder(compId, orderNo);
        if (txn.isEmpty()) {
            return Optional.empty();
        }
        String payinId = txn.get().getChillTransactionId();
        if (payinId == null || payinId.isBlank()) {
            return Optional.empty();
        }
        Long orgId = orgUnitRepository.findByCode(compId).map(OrgUnit::getId).orElse(null);
        if (orgId == null) {
            return Optional.empty();
        }
        Optional<TazapayCredentials> credOpt = resolveCredentials(orgId);
        if (credOpt.isEmpty()) {
            return Optional.empty();
        }
        Map<String, Object> payin = apiClient.getPayin(credOpt.get(), payinId);
        if (!Boolean.TRUE.equals(payin.get("ok"))) {
            return Optional.empty();
        }
        Object raw = payin.get("raw");
        boolean paid = raw instanceof com.fasterxml.jackson.databind.JsonNode
                && TazapayApiClient.isPaidStatus((com.fasterxml.jackson.databind.JsonNode) raw);
        if (!paid) {
            return Optional.empty();
        }
        String ref = txn.get().getPayNo() != null ? txn.get().getPayNo() : "";
        return tazapaySaleRecordService.applyPaidOutcome(orderNo, compId, payinId, ref);
    }

    private static String mapPaymentStatus(String status) {
        if (status == null || status.isBlank()) {
            return "UNKNOWN";
        }
        return switch (status.trim()) {
            case "10", "00" -> "PAID";
            case "08" -> "PENDING";
            case "99", "F0" -> "FAILED";
            default -> status.trim();
        };
    }

    private String resolvePublicApiBase(HttpServletRequest req) {
        String base = publicBaseUrl != null ? publicBaseUrl.trim().replaceAll("/+$", "") : "";
        if (!base.isBlank()) {
            return base;
        }
        if (req != null && req.getScheme() != null && req.getServerName() != null) {
            int port = req.getServerPort();
            boolean def = ("http".equalsIgnoreCase(req.getScheme()) && port == 80)
                    || ("https".equalsIgnoreCase(req.getScheme()) && port == 443);
            return req.getScheme() + "://" + req.getServerName() + (def ? "" : ":" + port);
        }
        return "https://api.icopay.co.kr";
    }

    private static String str(Object v) {
        return v == null ? "" : String.valueOf(v).trim();
    }

    private static String firstNonBlank(String... vals) {
        if (vals == null) {
            return "";
        }
        for (String v : vals) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
    }

    private static String joinName(String first, String last) {
        return (nz(first) + " " + nz(last)).trim();
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    private static Map<String, Object> fail(String message, String code) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", false);
        out.put("message", message);
        out.put("errorCode", code);
        return out;
    }
}