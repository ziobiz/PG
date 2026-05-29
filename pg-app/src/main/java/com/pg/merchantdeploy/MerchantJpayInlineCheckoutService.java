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
import com.pg.service.ChillPayService;
import com.pg.service.JpayPaymentService;
import com.pg.service.MerchantChatbotProductService;
import com.pg.service.OrgServiceUseService;
import com.pg.service.UrlPayCheckoutCurrencyService;
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

    public MerchantJpayInlineCheckoutService(OrgUnitRepository orgUnitRepository,
                                             MerchantProfileRepository merchantProfileRepository,
                                             OrgServiceUseService orgServiceUseService,
                                             JpayPaymentService jpayPaymentService,
                                             HqApiConfigRepository hqApiConfigRepository,
                                             MerchantChatbotProductService productService,
                                             MerchantInlineCheckoutTokenService tokenService,
                                             PgTrnsctnRepository pgTrnsctnRepository,
                                             UrlPayCheckoutCurrencyService urlPayCheckoutCurrencyService) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.orgServiceUseService = orgServiceUseService;
        this.jpayPaymentService = jpayPaymentService;
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.productService = productService;
        this.tokenService = tokenService;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.urlPayCheckoutCurrencyService = urlPayCheckoutCurrencyService;
    }

    public Map<String, Object> prepare(Long orgUnitId, Map<String, Object> body, HttpServletRequest request) {
        if (orgUnitId == null) {
            return fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND");
        }
        if (body != null && (body.containsKey("subscriptionPlan") || body.containsKey("subscription_plan")
                || "SUBSCRIPTION".equalsIgnoreCase(str(body.get("checkoutKind"))))) {
            return fail("JPAY 구독 가입은 /api/middleware/v1/merchant/jpay/subscription/prepare 를 사용하세요.", "SUBSCRIPTION_USE_DEDICATED_API");
        }
        Optional<OrgUnit> ouOpt = orgUnitRepository.findById(orgUnitId);
        if (ouOpt.isEmpty()) {
            return fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND");
        }
        OrgUnit ou = ouOpt.get();
        if (!orgServiceUseService.isOrgServiceActive(orgUnitId)) {
            return fail("서비스가 중지된 업체입니다.", "ORG_DISABLED");
        }
        Optional<MerchantProfile> profOpt = merchantProfileRepository.findByOrgUnitId(orgUnitId);
        if (profOpt.isPresent()) {
            String wpy = profOpt.get().getWebPaymentUseYn();
            if (wpy != null && "N".equalsIgnoreCase(wpy.trim())) {
                return fail("이 가맹점은 웹결제(URL 결제)가 미사용으로 설정되어 있습니다.", "WEB_PAYMENT_DISABLED");
            }
        }
        if (!jpayPaymentService.hasOperationalWebBinding(orgUnitId)) {
            return fail("JPAY URL 결제(운영) 바인딩이 없습니다.", "URL_PAYMENT_PG_MISSING");
        }
        HqApiConfig hq = hqApiConfigRepository.findAll().stream().findFirst().orElse(null);
        if (hq != null) {
            if (!"INLINE".equalsIgnoreCase(ChillPayService.effectiveUrlPayFlow(hq))) {
                return fail("본사 URL 결제 기본 방식이 INLINE이 아닙니다. 결제로직설정을 확인하세요.", "INLINE_NOT_ENABLED");
            }
            if ("N".equalsIgnoreCase(hq.getUrlPayInlineEnabledYn())) {
                return fail("본사 설정에서 URL 결제형 INLINE 제공이 꺼져 있습니다.", "INLINE_NOT_ENABLED");
            }
        }

        String orderNo = normalizeOrderNo(str(body.get("orderNo")));
        if (orderNo.isBlank()) {
            return fail("orderNo가 필요합니다.", "INVALID_ORDER_NO");
        }
        BigDecimal amount = parseAmount(body.get("amount"));
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return fail("유효한 amount가 필요합니다.", "INVALID_AMOUNT");
        }
        String amountPlain = amount.stripTrailingZeros().toPlainString();
        String currency = urlPayCheckoutCurrencyService.resolveCheckoutCurrency(
                orgUnitId, str(body.get("currency")));
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
            buyerPrefillJson = com.pg.urlpay.JpayBuyerPrefillUtil.resolvePrefillJsonForPrepare(body, fieldMode);
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
        String payPath = buildJpayPayPath(ou.getCode(), sessionToken, orderNo, amountPlain, currency, productName, langCode);
        String payUrl = base.isEmpty() ? payPath : base + payPath;
        String embedScriptUrl = base.isEmpty()
                ? "/v1/embed-jpay-pay/" + urlEnc(ou.getCode())
                : base + "/v1/embed-jpay-pay/" + urlEnc(ou.getCode());

        Map<String, Object> data = new LinkedHashMap<>();
        data.putAll(session.toPublicMap());
        data.put("sessionToken", sessionToken);
        data.put("payUrl", payUrl);
        data.put("embedScriptUrl", embedScriptUrl);
        data.put("inlineCheckoutPrepareUrl",
                trimSlash(productService.resolvePublicCustomerSiteBase(request))
                        + "/api/middleware/v1/merchant/jpay/inline-checkout/prepare");
        data.put("integrationMode", "INLINE");
        data.put("pgVendor", MerchantPgBrokerVendor.JPAY);
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
        for (String origin : new String[]{"MERCHANT_API", "URL"}) {
            Optional<PgTrnsctn> t = pgTrnsctnRepository.findFirstByMerchantIdAndOrderNoAndOrigin(
                    merchantId, orderNo, origin);
            if (t.isPresent() && PgVendor.isJpayFamily(t.get().getVan())) {
                return t;
            }
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
