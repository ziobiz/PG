package com.pg.merchantdeploy;

import com.pg.entity.HqApiConfig;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.service.JpayPaymentService;
import com.pg.service.MerchantChatbotProductService;
import com.pg.splitpay.SplitPayCheckoutModeGuard;
import com.pg.service.OrgServiceUseService;
import com.pg.service.UrlPayCheckoutCurrencyService;
import com.pg.urlpay.UrlPayCheckoutModeUtil;
import com.pg.urlpay.NeutralCheckoutRoute;
import com.pg.util.PgTrnsctnOrderLookup;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 가맹점 API — JPAY 인라인 결제 페이지({@code jpay-pay.html}) 세션 준비.
 */
@Service
public class MerchantJpayInlineCheckoutService {

    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final OrgServiceUseService orgServiceUseService;
    private final JpayPaymentService jpayPaymentService;
    private final HqApiConfigRepository hqApiConfigRepository;
    private final MerchantChatbotProductService productService;
    private final MerchantInlineCheckoutTokenService tokenService;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final UrlPayCheckoutCurrencyService urlPayCheckoutCurrencyService;
    private final MerchantApiIntegrationChannelService integrationChannelService;
    private final MerchantOperationalPgGuard operationalPgGuard;
    private final SplitPayCheckoutModeGuard splitPayCheckoutModeGuard;
    private final JpayInlineCheckoutPrepareGuard jpayInlineCheckoutPrepareGuard;
    private final MerchantCheckoutPrepareCurrencyService prepareCurrencyService;

    public MerchantJpayInlineCheckoutService(OrgUnitRepository orgUnitRepository,
                                             MerchantProfileRepository merchantProfileRepository,
                                             OrgServiceUseService orgServiceUseService,
                                             JpayPaymentService jpayPaymentService,
                                             HqApiConfigRepository hqApiConfigRepository,
                                             MerchantChatbotProductService productService,
                                             MerchantInlineCheckoutTokenService tokenService,
                                             PgTrnsctnRepository pgTrnsctnRepository,
                                             UrlPayCheckoutCurrencyService urlPayCheckoutCurrencyService,
                                             MerchantApiIntegrationChannelService integrationChannelService,
                                             MerchantOperationalPgGuard operationalPgGuard,
                                             SplitPayCheckoutModeGuard splitPayCheckoutModeGuard,
                                             JpayInlineCheckoutPrepareGuard jpayInlineCheckoutPrepareGuard,
                                             MerchantCheckoutPrepareCurrencyService prepareCurrencyService) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.orgServiceUseService = orgServiceUseService;
        this.jpayPaymentService = jpayPaymentService;
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.productService = productService;
        this.tokenService = tokenService;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.urlPayCheckoutCurrencyService = urlPayCheckoutCurrencyService;
        this.integrationChannelService = integrationChannelService;
        this.operationalPgGuard = operationalPgGuard;
        this.splitPayCheckoutModeGuard = splitPayCheckoutModeGuard;
        this.jpayInlineCheckoutPrepareGuard = jpayInlineCheckoutPrepareGuard;
        this.prepareCurrencyService = prepareCurrencyService;
    }

    public Map<String, Object> prepare(Long orgUnitId, Map<String, Object> body, HttpServletRequest request) {
        Optional<Map<String, Object>> splitDeny = splitPayCheckoutModeGuard.denyInlineOneShotPrepare(orgUnitId);
        if (splitDeny.isPresent()) {
            return splitDeny.get();
        }
        if (orgUnitId == null) {
            return fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND");
        }
        if (body != null && (body.containsKey("subscriptionPlan") || body.containsKey("subscription_plan")
                || "SUBSCRIPTION".equalsIgnoreCase(str(body.get("checkoutKind"))))) {
            return fail("구독(정기결제) 가입은 /api/middleware/v1/merchant/checkout/subscription/prepare 를 사용하세요.", "SUBSCRIPTION_USE_DEDICATED_API");
        }
        Optional<OrgUnit> ouOpt = orgUnitRepository.findById(orgUnitId);
        if (ouOpt.isEmpty()) {
            return fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND");
        }
        OrgUnit ou = ouOpt.get();
        if (!orgServiceUseService.isOrgServiceActive(orgUnitId)) {
            return fail(OrgServiceUseService.MSG_ORG_SERVICE_DISABLED, "ORG_DISABLED");
        }
        Optional<MerchantProfile> profOpt = merchantProfileRepository.findByOrgUnitId(orgUnitId);
        if (profOpt.isPresent()) {
            if (!orgServiceUseService.isWebPaymentActive(orgUnitId)) {
                return fail(OrgServiceUseService.MSG_WEB_PAYMENT_DISABLED, "WEB_PAYMENT_DISABLED");
            }
            if (UrlPayCheckoutModeUtil.isSplitPay(profOpt.get().getApiUrlPayCheckoutMode())) {
                String base = trimSlash(productService.resolvePublicCustomerSiteBase(request));
                String setup = (base.isEmpty() ? "" : base) + "/split-pay-setup.html?m=" + urlEnc(ou.getCode());
                Map<String, Object> hint = new LinkedHashMap<>();
                hint.put("splitPaySetupUrl", setup);
                hint.put("checkoutKind", "SPLIT_PAY");
                Map<String, Object> out = new LinkedHashMap<>();
                out.put("success", false);
                out.put("message", "API URL 결제 방식이 분할 결제입니다. POST /api/pay/split/contracts 또는 분할 신청 URL을 사용하세요.");
                out.put("errorCode", "SPLIT_PAY_MODE");
                out.put("data", hint);
                return out;
            }
        }
        if (!jpayPaymentService.hasOperationalWebBinding(orgUnitId)) {
            return fail("JPAY URL 결제(운영) 바인딩이 없습니다.", "URL_PAYMENT_PG_MISSING");
        }
        Optional<Map<String, Object>> vendorDeny = operationalPgGuard.denyIfUrlPayVendorMismatch(
                orgUnitId, MerchantPgBrokerVendor.JPAY, false);
        if (vendorDeny.isPresent()) {
            return vendorDeny.get();
        }
        Optional<String> inlineDeny = integrationChannelService.denyMessage(orgUnitId,
                MerchantApiIntegrationChannelService.Channel.API_BROKER_INLINE);
        if (inlineDeny.isPresent()) {
            return fail(inlineDeny.get(), MerchantApiIntegrationChannelService.CODE_INTEGRATION_CHANNEL_DISABLED);
        }
        HqApiConfig hq = hqApiConfigRepository.findAll().stream().findFirst().orElse(null);

        String orderNo = normalizeOrderNo(str(body.get("orderNo")));
        if (orderNo.isBlank()) {
            return fail("orderNo가 필요합니다.", "INVALID_ORDER_NO");
        }
        BigDecimal amount = parseAmount(body.get("amount"));
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return fail("유효한 amount가 필요합니다.", "INVALID_AMOUNT");
        }
        String amountPlain = amount.stripTrailingZeros().toPlainString();
        MerchantCheckoutPrepareCurrencyService.Resolved curResolved =
                prepareCurrencyService.resolveOrgCheckout(orgUnitId, str(body.get("currency")));
        if (!curResolved.ok()) {
            return prepareCurrencyService.failMap(curResolved);
        }
        String currency = curResolved.sessionCurrency();
        Optional<Map<String, Object>> prepareGuard = jpayInlineCheckoutPrepareGuard.validatePrepare(
                orgUnitId, orderNo, amount, currency);
        if (prepareGuard.isPresent()) {
            return prepareGuard.get();
        }
        String productName = clamp(str(body.get("productName")), 500);
        if (productName.isBlank()) {
            productName = clamp(str(body.get("item")), 500);
        }
        String langCode = MerchantCheckoutLangUtil.fromBody(body);

        String fieldMode = com.pg.urlpay.JpayCheckoutFieldModeUtil.resolve(
                profOpt.map(MerchantProfile::getJpayCheckoutFieldMode).orElse(null),
                hq != null ? hq.getJpayCheckoutFieldMode() : null);
        String buyerPrefillJson;
        try {
            buyerPrefillJson = com.pg.urlpay.IcipayBuyerContactUtil.resolvePrefillJsonFromBodyOptional(body);
            if (buyerPrefillJson == null) {
                buyerPrefillJson = com.pg.urlpay.JpayBuyerPrefillUtil.resolvePrefillJsonForPrepare(body, fieldMode);
            }
        } catch (IllegalArgumentException ex) {
            return fail(ex.getMessage(), "BUYER_PREFILL_INVALID");
        }

        String sessionToken = buyerPrefillJson != null && !buyerPrefillJson.isBlank()
                ? tokenService.issueWithBuyerPrefill(MerchantPgBrokerVendor.JPAY, ou.getCode(), orderNo,
                amountPlain, currency, productName, buyerPrefillJson)
                : tokenService.issue(MerchantPgBrokerVendor.JPAY, ou.getCode(), orderNo,
                amountPlain, currency, productName);
        Optional<MerchantInlineCheckoutTokenService.SessionPayload> parsed =
                tokenService.parseValid(sessionToken, MerchantPgBrokerVendor.JPAY);
        if (parsed.isEmpty()) {
            return fail("세션 토큰 생성에 실패했습니다.", "SESSION_ERROR");
        }
        MerchantInlineCheckoutTokenService.SessionPayload session = parsed.get();

        String base = trimSlash(productService.resolvePublicCustomerSiteBase(request));
        String payUrl = NeutralCheckoutRoute.buildPayUrl(base, ou.getCode(), sessionToken, langCode, true);
        String embedScriptUrl = NeutralCheckoutRoute.buildEmbedScriptUrl(base, ou.getCode());

        Map<String, Object> data = new LinkedHashMap<>();
        data.putAll(session.toPublicMap());
        data.put("sessionToken", sessionToken);
        data.put("payUrl", payUrl);
        data.put("embedScriptUrl", embedScriptUrl);
        data.put("inlineCheckoutPrepareUrl",
                trimSlash(productService.resolvePublicCustomerSiteBase(request))
                        + "/api/middleware/v1/merchant/checkout/prepare");
        data.put("integrationMode", "INLINE");
        data.put("pgVendor", MerchantPgBrokerVendor.JPAY);
        prepareCurrencyService.putPublicFields(data, curResolved);
        if (langCode != null && !langCode.isBlank()) {
            data.put("langCode", langCode);
        }
        data.put("embedUsageHint",
                "가맹점 서버에서 prepare 호출 후 sessionToken을 embed 스크립트 data-session-token에 넣거나, payUrl을 iframe src로 사용하세요. "
                        + "결제창 언어: prepare JSON lang(또는 embed data-lang), 없으면 가맹 페이지 html[lang]/브라우저 언어를 자동 감지합니다.");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("data", data);
        return out;
    }

    public Map<String, Object> readSession(String token) {
        Optional<MerchantInlineCheckoutTokenService.SessionPayload> parsed =
                tokenService.parseValid(token, MerchantPgBrokerVendor.JPAY);
        if (parsed.isEmpty()) {
            return fail("세션이 유효하지 않거나 만료되었습니다.", "INVALID_SESSION");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("data", parsed.get().toPublicMap());
        return out;
    }

    public Map<String, Object> orderStatus(Long orgUnitId, String orderNoRaw) {
        if (orgUnitId == null) {
            return fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND");
        }
        Optional<OrgUnit> ouOpt = orgUnitRepository.findById(orgUnitId);
        if (ouOpt.isEmpty()) {
            return fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND");
        }
        String orderNo = normalizeOrderNo(orderNoRaw);
        if (orderNo.isBlank()) {
            return fail("orderNo가 필요합니다.", "INVALID_ORDER_NO");
        }
        String mid = ouOpt.get().getCode();
        Optional<PgTrnsctn> txn = findJpayTxnByOrder(mid, orderNo);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("compId", mid);
        data.put("orderNo", orderNo);
        data.put("pgVendor", MerchantPgBrokerVendor.JPAY);
        if (txn.isEmpty()) {
            data.put("found", false);
            data.put("paymentStatus", "NOT_FOUND");
        } else {
            PgTrnsctn t = txn.get();
            data.put("found", true);
            data.put("paymentStatus", mapJpayPaymentStatus(t.getStatus()));
            data.put("transactionId", t.getTrnId());
            data.put("approvalNo", t.getApprovalNo());
            data.put("amount", t.getAmtKrw());
            data.put("currency", t.getCurType());
            data.put("paidAt", t.getPaidAt() != null ? t.getPaidAt().toString() : null);
            data.put("origin", t.getOrigin());
            data.put("jpayStatus", t.getChillPaymentStatus());
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("data", data);
        return out;
    }

    private Optional<PgTrnsctn> findJpayTxnByOrder(String merchantId, String orderNo) {
        Optional<PgTrnsctn> t = PgTrnsctnOrderLookup.findPreferredByMerchantAndOrder(
                pgTrnsctnRepository, merchantId, orderNo);
        if (t.isPresent() && PgVendor.isJpayFamily(t.get().getVan())) {
            return t;
        }
        return Optional.empty();
    }

    private static String mapJpayPaymentStatus(String statusCode) {
        if (statusCode == null || statusCode.isBlank()) {
            return "UNKNOWN";
        }
        return switch (statusCode.trim()) {
            case "10", "00" -> "PAID";
            case "08" -> "PENDING";
            case "99", "02" -> "FAILED";
            case "01" -> "CANCELLED";
            default -> statusCode;
        };
    }

    private static String buildJpayPayPath(String compCode, String sessionToken, String orderNo,
                                           String amountPlain, String currency, String productName, String langCode) {
        StringBuilder q = new StringBuilder();
        q.append("/jpay-pay/").append(urlEnc(compCode));
        q.append("?entry=merchant_api");
        q.append("&embed=1");
        q.append("&session=").append(urlEnc(sessionToken));
        q.append("&orderNo=").append(urlEnc(orderNo));
        q.append("&amount=").append(urlEnc(amountPlain));
        q.append("&currency=").append(urlEnc(currency));
        if (productName != null && !productName.isBlank()) {
            q.append("&item=").append(urlEnc(productName));
        }
        if (langCode != null && !langCode.isBlank()) {
            q.append("&lang=").append(urlEnc(langCode));
        }
        return q.toString();
    }

    private static String normalizeOrderNo(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        return s.length() > 64 ? s.substring(0, 64) : s;
    }

    private static Map<String, Object> fail(String message, String code) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", false);
        out.put("message", message);
        out.put("errorCode", code);
        return out;
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    private static BigDecimal parseAmount(Object o) {
        if (o == null) {
            return null;
        }
        try {
            String s = o.toString().trim().replace(",", "");
            if (s.isEmpty()) {
                return null;
            }
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String clamp(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static String urlEnc(String s) {
        return URLEncoder.encode(s != null ? s : "", StandardCharsets.UTF_8);
    }

    private static String trimSlash(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        while (t.endsWith("/")) {
            t = t.substring(0, t.length() - 1);
        }
        return t;
    }
}
