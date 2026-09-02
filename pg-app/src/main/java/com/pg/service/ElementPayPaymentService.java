package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.MerchantNotifyUrl;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgAgency;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.integration.pg.elementpay.ElementPayCredentials;
import com.pg.middleware.notify.PgNotifyIngressPaths;
import com.pg.noti.NotiProvisionClient;
import com.pg.repository.MerchantNotifyUrlRepository;
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.receipt.TransactionReceiptEmailService;
import com.pg.splitpay.SplitPayPaymentHookService;
import com.pg.urlpay.CheckoutFailI18n;
import com.pg.urlpay.IcipayBuyerContactUtil;
import com.pg.urlpay.PayerContextCapture;
import com.pg.util.CountryCallingCode;
import com.pg.util.ElementPayAdditionalParameters;
import com.pg.util.ElementPayCardServiceAlias;
import com.pg.util.ElementPayHashUtil;
import com.pg.util.ElementPayInlineStatusUtil;
import com.pg.util.ElementPayPaymentIdUtil;
import com.pg.util.ElementPayRefundRejectMapper;
import com.pg.util.PayPresaleRiskFilterCodes;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * ElementPay Payment API — THB 전용 initPayment·getStatus.
 * <p><b>노티미들웨어(외부 NOTI) — JPAY 와 동일 토폴로지:</b>
 * ElementPay Cabinet Webhook 은 NOTI {@code /noti/elementpay}(고정 1개)에 등록하고, NOTI 가
 * {@link com.pg.middleware.notify.PgNotifyIngressPaths#MIDDLEWARE_PREFIX}{@code …/ELEMENTPAY} 로 ICOPAY 에 전달합니다.
 * <p><b>브라우저 RESULT:</b> {@code _successUrl}/{@code _rejectUrl}/{@code _waitingUrl} 은
 * NOTI {@code /noti/result/elementpay?order=&compId=} 경유(가맹 resultUrl·Dealmai 릴레이). JPAY callback 슬롯과 동일 역할.
 * <p><b>가맹 통보:</b> 웹훅 적재 또는 getStatus 동기 승인 후 {@link MerchantOutboundNotifyService} 로
 * Background/Result/MIDDLEWARE(Dealmai 등)에 JPAY와 동일 JSON POST.
 * getStatus 만으로 승인된 URL 결제는 {@link ElementPayNotiMiddlewareMirrorService} 가
 * NOTI {@code /noti/elementpay} 에 pay 미러를 보내 노티로그·가맹 릴레이가 쌓이게 합니다.
 */
@Service
public class ElementPayPaymentService {

    private static final Logger log = LoggerFactory.getLogger(ElementPayPaymentService.class);

    private static final String SANDBOX_BASE = "https://api-sbox.elementpay.io";
    private static final String LIVE_BASE = "https://api.elementpay.io";
    private static final String THB = "THB";
    private static final String USER_AGENT = "Mozilla/5.0 (ICOPAY; compatible)";

    private final PgAgencyRepository pgAgencyRepository;
    private final MerchantPgBindingRepository merchantPgBindingRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final MerchantNotifyUrlRepository merchantNotifyUrlRepository;
    private final HqNotifyEnvService hqNotifyEnvService;
    private final ElementPaySaleRecordService elementPaySaleRecordService;
    private final PayPresaleRiskFilterService payPresaleRiskFilterService;
    private final MerchantChatbotProductService productService;
    private final MerchantOutboundNotifyService merchantOutboundNotifyService;
    private final SettlementCalcService settlementCalcService;
    private final TransactionReceiptEmailService transactionReceiptEmailService;
    private final SplitPayPaymentHookService splitPayPaymentHookService;
    private final ElementPayNotiMiddlewareMirrorService elementPayNotiMiddlewareMirrorService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public ElementPayPaymentService(PgAgencyRepository pgAgencyRepository,
                                      MerchantPgBindingRepository merchantPgBindingRepository,
                                      OrgUnitRepository orgUnitRepository,
                                      MerchantNotifyUrlRepository merchantNotifyUrlRepository,
                                      HqNotifyEnvService hqNotifyEnvService,
                                      ElementPaySaleRecordService elementPaySaleRecordService,
                                      PayPresaleRiskFilterService payPresaleRiskFilterService,
                                      MerchantChatbotProductService productService,
                                      MerchantOutboundNotifyService merchantOutboundNotifyService,
                                      SettlementCalcService settlementCalcService,
                                      TransactionReceiptEmailService transactionReceiptEmailService,
                                      SplitPayPaymentHookService splitPayPaymentHookService,
                                      ElementPayNotiMiddlewareMirrorService elementPayNotiMiddlewareMirrorService) {
        this.pgAgencyRepository = pgAgencyRepository;
        this.merchantPgBindingRepository = merchantPgBindingRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.merchantNotifyUrlRepository = merchantNotifyUrlRepository;
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.elementPaySaleRecordService = elementPaySaleRecordService;
        this.payPresaleRiskFilterService = payPresaleRiskFilterService;
        this.productService = productService;
        this.merchantOutboundNotifyService = merchantOutboundNotifyService;
        this.settlementCalcService = settlementCalcService;
        this.transactionReceiptEmailService = transactionReceiptEmailService;
        this.splitPayPaymentHookService = splitPayPaymentHookService;
        this.elementPayNotiMiddlewareMirrorService = elementPayNotiMiddlewareMirrorService;
    }

    public boolean hasOperationalWebBinding(Long orgUnitId) {
        return findOperationalElementPayBinding(orgUnitId).isPresent();
    }

    /**
     * URL 결제 승인(INLINE) — initPayment 후 Bangkok Bank card/keys 로 카드 승인 폼·3DS URL 을 구성합니다.
     * ElementPay Light 카테고리/팝업({@code /merchant/light/#/services/...}) 은 사용하지 않습니다.
     */
    public Map<String, Object> executeInitPayment(Long orgUnitId, Map<String, Object> body,
                                                  HttpServletRequest req, String clientIp) {
        Optional<MerchantPgBinding> bindOpt = findOperationalElementPayBinding(orgUnitId);
        if (bindOpt.isEmpty()) {
            return fail("ElementPay 운영 바인딩이 없습니다.", "ELEMENTPAY_PG_MISSING");
        }
        MerchantPgBinding binding = bindOpt.get();
        Optional<PgAgency> agOpt = pgAgencyRepository.findByPgCd(trim(binding.getPgCd()));
        if (agOpt.isEmpty()) {
            return fail("ElementPay 결제대행사(PG) 설정을 찾을 수 없습니다.", "ELEMENTPAY_AGENCY_MISSING");
        }
        PgAgency agency = agOpt.get();
        ElementPayCredentials cred = ElementPayCredentials.from(agency);
        if (!cred.isConfigured()) {
            return fail("ElementPay Merchant Key·Secret Key 가 설정되지 않았습니다.", "ELEMENTPAY_CREDENTIALS_MISSING");
        }

        PayerContextCapture.enrichSaleBody(body, req, clientIp);
        Optional<OrgUnit> ou = orgUnitRepository.findById(orgUnitId);
        String compCode = ou.map(OrgUnit::getCode).orElse("");

        String orderNo = str(body.get("orderNo"));
        if (orderNo.isBlank()) {
            return fail("orderNo가 필요합니다.", "INVALID_ORDER_NO");
        }
        BigDecimal amount = parseAmount(body.get("amount"));
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return fail("유효한 amount가 필요합니다.", "INVALID_AMOUNT");
        }
        String currency = str(body.get("currency"));
        if (currency.isBlank()) {
            currency = THB;
        }
        if (!THB.equalsIgnoreCase(currency)) {
            return fail("ElementPay는 THB(태국 바트)만 지원합니다.", "ELEMENTPAY_THB_ONLY");
        }

        Map<String, Object> buyerFail = IcipayBuyerContactUtil.failMapIfSaleContactMissing(body);
        if (buyerFail != null) {
            return CheckoutFailI18n.checkoutContactRequired();
        }

        String paymentMethod = str(body.get("paymentMethod"));
        if (paymentMethod.isBlank()) {
            paymentMethod = "CARD";
        }
        String pmU = paymentMethod.trim().toUpperCase(Locale.ROOT);
        if ("PROMPTPAY".equals(pmU) || "QR".equals(pmU) || "PP".equals(pmU)) {
            return fail("ElementPay URL 결제는 신용카드만 지원합니다.", "ELEMENTPAY_CARD_ONLY");
        }
        paymentMethod = "CARD";
        String pan = digitsOnly(str(body.get("payCardno")));
        if (pan.isBlank()) {
            return fail("카드번호가 필요합니다. INLINE 결제는 카드 정보가 필수입니다.", "ELEMENTPAY_CARD_REQUIRED");
        }
        String serviceAlias = ElementPayCardServiceAlias.resolveConfigured(
                cred.serviceAliasForMethod(paymentMethod), cred.sandbox());
        CardMethodCatalog catalog = loadCardMethodCatalog(agency, cred, serviceAlias);
        serviceAlias = catalog.serviceAlias();
        List<String> methodKeys = catalog.attrKeys();

        Optional<PayPresaleRiskFilterService.PresaleRiskBlock> presaleRisk =
                payPresaleRiskFilterService.evaluate(orgUnitId, compCode, PgVendor.ELEMENTPAY, body);
        if (presaleRisk.isPresent()) {
            PayPresaleRiskFilterService.PresaleRiskBlock block = presaleRisk.get();
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("success", false);
            out.put("message", block.message());
            out.put("errorCode", PayPresaleRiskFilterCodes.ERROR_CODE);
            out.put("messageKey", block.filterCode());
            out.put("messages", block.messages());
            return out;
        }

        String publicBase = trimSlash(productService.resolvePublicCustomerSiteBase(req));
        String ingressToken = hqNotifyEnvService.getOrCreate().getIngressToken();
        String notifyBase = PgNotifyIngressPaths.buildIngressBase(publicBase, ingressToken) + "/ELEMENTPAY";

        String amountPlain = formatAmount(amount);
        /* JPAY callback 슬롯과 동일: 브라우저는 NOTI Result 입구 경유 → 가맹 resultUrl·Dealmai */
        String browserReturn = resolveElementPayBrowserReturnUrl(orgUnitId, orderNo, compCode);
        if (browserReturn == null || browserReturn.isBlank()) {
            browserReturn = resolveIcopayCheckoutReturnUrl(publicBase, compCode, orderNo, "success");
        }
        String successReturn = browserReturn;
        String rejectReturn = browserReturn;
        String waitingReturn = browserReturn;

        Map<String, String> canonical = ElementPayAdditionalParameters.build(body, clientIp, combineBuyerPhone(body));
        Map<String, String> extra = extrasForInit(canonical, methodKeys);
        log.info("ElementPay init attrs service={} methodKeys={} extraKeys={} ipHint={}",
                serviceAlias, methodKeys, extra.keySet(), extra.get("ip"));

        JsonNode resp = initPaymentTryingModes(agency, cred, serviceAlias, amountPlain, orderNo,
                successReturn, rejectReturn, waitingReturn, extra);
        if (resp == null) {
            return fail("ElementPay 결제 초기화에 실패했습니다.", "ELEMENTPAY_INIT_FAILED");
        }

        if (resp.has("error")) {
            String msg = extractEpErrorMessage(resp);
            if (isMissingBuyerAttribute(msg)) {
                log.warn("ElementPay missed required after attrs keys={} msg={}", extra.keySet(), msg);
                return CheckoutFailI18n.cardPathFailed();
            }
            String code = resp.path("error").isObject()
                    ? resp.path("error").path("code").asText("ERROR")
                    : "ERROR";
            if (msg != null && msg.toLowerCase(Locale.ROOT).contains("wrong signature")) {
                return fail(
                        "ElementPay 서명 검증 실패(Wrong signature). Secret Key·Merchant Key·샌드박스 여부 및 Result URL 서명 인코딩을 확인하세요.",
                        "ELEMENTPAY_WRONG_SIGNATURE");
            }
            if (isMerchantCommissionUnset(msg)) {
                log.warn("ElementPay init commission-missing service={} msg={}", serviceAlias, msg);
                if (!"kCards".equalsIgnoreCase(serviceAlias)) {
                    String retryAlias = "kCards";
                    extra = extrasForInit(canonical, fetchMethodAttributeKeys(agency, cred, retryAlias));
                    log.info("ElementPay retry init with catalog card alias={}", retryAlias);
                    resp = initPaymentTryingModes(agency, cred, retryAlias, amountPlain, orderNo,
                            successReturn, rejectReturn, waitingReturn, extra);
                    if (resp != null && !resp.has("error")) {
                        serviceAlias = retryAlias;
                    } else {
                        String msgRetry = extractEpErrorMessage(resp);
                        log.warn("ElementPay kCards retry still failed msg={}", msgRetry);
                        return CheckoutFailI18n.cardPathFailed();
                    }
                } else {
                    return CheckoutFailI18n.cardPathFailed();
                }
            }
            if (resp != null && resp.has("error")
                    && msg != null && msg.toLowerCase(Locale.ROOT).contains("payment method is disabled")) {
                Map<String, Object> methodsOut = listPaymentMethods(agency.getId());
                String suggested = str(methodsOut.get("suggestedCardServiceAlias"));
                if (!suggested.isBlank() && !suggested.equalsIgnoreCase(serviceAlias)) {
                    log.info("ElementPay service_id={} disabled → retry with suggested={}", serviceAlias, suggested);
                    serviceAlias = suggested;
                    extra = extrasForInit(canonical, fetchMethodAttributeKeys(agency, cred, serviceAlias));
                    resp = initPaymentTryingModes(agency, cred, serviceAlias, amountPlain, orderNo,
                            successReturn, rejectReturn, waitingReturn, extra);
                    if (resp == null) {
                        return fail("ElementPay 결제 초기화에 실패했습니다.", "ELEMENTPAY_INIT_FAILED");
                    }
                    if (!resp.has("error")) {
                        /* 재시도 성공 — 아래로 진행 */
                    } else {
                        String msg2 = extractEpErrorMessage(resp);
                        if (isMissingBuyerAttribute(msg2)) {
                            return CheckoutFailI18n.cardPathFailed();
                        }
                        return fail(
                                "ElementPay 결제수단이 맞지 않습니다. 본사 PG cardServiceAlias를 맞추세요(예: kCards). 현재 시도="
                                        + serviceAlias + " / EP=" + msg2,
                                "ELEMENTPAY_METHOD_DISABLED");
                    }
                } else {
                    return fail(
                            "ElementPay 결제수단이 맞지 않습니다. 본사 PG에서 [결제수단 조회] 후 cardServiceAlias를 맞추세요(예: kCards).",
                            "ELEMENTPAY_METHOD_DISABLED");
                }
            } else if (resp == null || resp.has("error")) {
                if (isMerchantCommissionUnset(msg)) {
                    return CheckoutFailI18n.cardPathFailed();
                }
                return fail(msg, "ELEMENTPAY_" + code);
            }
        }

        JsonNode response = resp.path("response");
        if (response.isMissingNode()) {
            return fail("ElementPay 응답 형식이 올바르지 않습니다.", "ELEMENTPAY_INIT_INVALID");
        }

        String paymentId = response.path("id").asText("");
        int status = response.path("status").asInt(0);

        BigDecimal shopperDispAmt = parseAmount(body.get("shopperDisplayAmount"));
        if (shopperDispAmt == null) {
            shopperDispAmt = parseAmount(body.get("displayAmount"));
        }
        String shopperDispCur = str(body.get("shopperDisplayCurrency"));
        if (shopperDispCur.isBlank()) {
            shopperDispCur = str(body.get("displayCurrency"));
        }

        elementPaySaleRecordService.recordOrTouchPending(
                orgUnitId, orderNo, amount, THB, binding.getSortOrder(),
                str(body.get("item")), resolveTxnOrigin(body),
                str(body.get("customerNm")), str(body.get("payEmailAddress")),
                paymentMethod, shopperDispAmt, shopperDispCur.isBlank() ? null : shopperDispCur,
                false, paymentId, body);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("orderNo", orderNo);
        out.put("paymentId", paymentId);
        out.put("elementPayStatus", status);
        out.put("paymentMethod", paymentMethod);
        out.put("serviceId", serviceAlias);
        out.put("currency", THB);
        out.put("amount", amountPlain);
        out.put("notifyUrl", notifyBase);

        List<Map<String, String>> attrs = parseAttributes(response.path("attributes"));
        out.put("attributes", attrs);
        String qr = findAttribute(attrs, "qrcode");
        if (qr != null && !qr.isBlank()) {
            out.put("qrCodeDataUri", qr);
        }

        out.put("integrationMode", "INLINE");
        out.put("paymentUiMode", "INLINE");
        out.put("resultReturnUrl", successReturn);
        out.put("resultRejectUrl", rejectReturn);

        /*
         * 라이브 카드: getMethods 에 있는 수단(보통 kCards). 호스티드 폼이면 init redirectUrl.
         * 샌드박스 kCards: /k/cards/form → KTC 자동 POST. BKB card/keys 의 ICOPAY waiting 은 무시.
         */
        String epPayUrl = firstUsablePayUrl(attrs, publicBase);
        log.info("ElementPay init ok paymentId={} service={} attrKeys={} payUrl={}",
                paymentId, serviceAlias, attrKeys(attrs), urlHostPath(epPayUrl));
        if (epPayUrl != null && !epPayUrl.isBlank() && isElementPayCardFormUrl(epPayUrl)) {
            Map<String, String> ktcForm = fetchAndBuildKtcInlineForm(epPayUrl, body, pan, clientIp);
            if (ktcForm != null && ktcForm.get("actionUrl") != null && !ktcForm.get("actionUrl").isBlank()) {
                out.put("bkbInlineCheckout", ktcForm);
                out.put("needsBkbForm", true);
                out.put("cardFormSource", "K_CARDS_FORM");
                out.put("epCardFormUrl", epPayUrl);
                out.put("needs3dsWindow", true);
                return out;
            }
            log.warn("ElementPay card-form 파싱 실패 order={} url={}", orderNo, urlHostPath(epPayUrl));
        }
        if (isUsablePayUrl(epPayUrl, publicBase) && !cred.sandbox()) {
            out.put("redirectUrl", epPayUrl);
            out.put("epCardFormUrl", epPayUrl);
            out.put("needs3ds", true);
            out.put("inlineAcs", true);
            out.put("cardFormSource", "INIT_REDIRECT");
            return out;
        }

        JsonNode bkb = fetchBangkokBankCardKeys(agency, cred, paymentId);
        if (bkb != null && !bkb.has("error")) {
            Map<String, String> form = buildBkbInlineForm(
                    bkb, body, pan, paymentId, amountPlain,
                    successReturn, rejectReturn, rejectReturn);
            if (form != null && form.get("actionUrl") != null && !form.get("actionUrl").isBlank()
                    && !isOurCheckoutReturnUrl(form.get("actionUrl"), publicBase)) {
                out.put("bkbInlineCheckout", form);
                out.put("needsBkbForm", true);
                out.put("cardFormSource", "BKB_KEYS");
                return out;
            }
            String acs = firstNonBlank(
                    bkb.path("redirect_url").asText(""),
                    bkb.path("redirectUrl").asText(""));
            if (isUsablePayUrl(acs, publicBase) && isExternalAcsUrl(acs)
                    && !isOurCheckoutReturnUrl(acs, publicBase)) {
                out.put("redirectUrl", acs);
                out.put("needs3ds", true);
                out.put("inlineAcs", true);
                return out;
            }
            log.warn("ElementPay BKB keys 에 사용 가능한 폼/ACS 없음 order={} payUrl={}",
                    orderNo, urlHostPath(epPayUrl));
        } else {
            String errMsg = bkb != null ? bkb.path("error").path("message").asText("") : "";
            log.warn("ElementPay bangkokbank/card/keys 실패 order={} paymentId={} err={}",
                    orderNo, paymentId, errMsg);
        }

        if (isUsablePayUrl(epPayUrl, publicBase)) {
            out.put("redirectUrl", epPayUrl);
            out.put("epCardFormUrl", epPayUrl);
            out.put("needs3ds", true);
            out.put("inlineAcs", true);
            out.put("cardFormSource", "REDIRECT_FALLBACK");
        return out;
        }

        log.warn("INLINE card path missing order={} paymentId={} attrKeys={} payUrl={}",
                orderNo, paymentId, attrKeys(attrs), urlHostPath(epPayUrl));
        return CheckoutFailI18n.cardPathFailed();
    }

    /**
     * EP {@code /k/cards/form} HTML 을 읽어 KTC paygate 자동 POST 폼을 구성합니다.
     * <p>호스티드 폼(JS)과 같이 <b>동일 Cookie 세션</b>에서 form GET → {@code /k/cards/check} POST 를 수행하고,
     * {@code threeDSCustomerIP} 는 구매자 브라우저 IP 를 우선합니다(샌드박스도 챌린지 UI 없이 frictionless 로 끝날 수 있음).
     */
    private Map<String, String> fetchAndBuildKtcInlineForm(String formUrl, Map<String, Object> body,
                                                           String pan, String clientIp) {
        if (formUrl == null || formUrl.isBlank()) {
            return null;
        }
        String formUrlTrim = formUrl.trim();
        String buyerIp = clientIp != null ? clientIp.trim() : "";
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        HttpClient http = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        String html;
        try {
            HttpRequest getReq = HttpRequest.newBuilder(URI.create(formUrlTrim))
                    .timeout(Duration.ofSeconds(25))
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .GET()
                    .build();
            HttpResponse<String> getResp = http.send(getReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            html = getResp.body() != null ? getResp.body() : "";
            if (getResp.statusCode() < 200 || getResp.statusCode() >= 300) {
                log.warn("ElementPay k/cards/form HTTP {} url={}", getResp.statusCode(), formUrlTrim);
            }
        } catch (Exception e) {
            log.warn("ElementPay k/cards/form HTTP 실패: {}", e.getMessage());
            return null;
        }
        if (html.isBlank()) {
            return null;
        }
        String action = extractHtmlFormAction(html);
        if (action == null || action.isBlank()) {
            return null;
        }
        action = resolveFormAction(formUrlTrim, action);
        if (!isExternalAcsUrl(action) || isOurCheckoutReturnUrl(action, "") || isElementPayCardFormUrl(action)) {
            log.warn("ElementPay k/cards/form action 이 은행 승인 URL이 아님: {}", action);
            return null;
        }
        Map<String, String> hidden = extractHtmlHiddenInputs(html);
        String month = str(body.get("payCardmonth")).replaceAll("\\D", "");
        if (month.length() == 1) {
            month = "0" + month;
        }
        String yearRaw = str(body.get("payCardyear")).replaceAll("\\D", "");
        /* EP 호스티드: UI 는 YY, hidden epYear 는 2000+YY (4자리) */
        String epYear;
        if (yearRaw.length() == 2) {
            try {
                epYear = String.valueOf(2000 + Integer.parseInt(yearRaw));
            } catch (NumberFormatException e) {
                epYear = yearRaw;
            }
        } else if (yearRaw.length() >= 4) {
            epYear = yearRaw.substring(0, 4);
        } else {
            epYear = yearRaw;
        }
        String holder = (str(body.get("payFirstname")) + " " + str(body.get("payLastname"))).trim();
        if (holder.isBlank()) {
            holder = str(body.get("customerNm"));
        }
        String brand = str(body.get("payCardBrand"));
        if (brand.isBlank() || "AUTO".equalsIgnoreCase(brand)) {
            brand = detectBkbCardBrand(pan);
        } else {
            brand = mapBrandToBkb(brand);
        }
        Map<String, String> form = new LinkedHashMap<>();
        /* EP 폼 hidden(3DS 세션 등)을 누락하면 ACS가 안 뜨고 waiting 만 복귀할 수 있음 */
        if (hidden != null) {
            form.putAll(hidden);
        }
        form.put("actionUrl", action);
        form.put("merchantId", firstNonBlank(form.get("merchantId"), hidden.get("merchantId"), ""));
        form.put("amount", firstNonBlank(form.get("amount"), hidden.get("amount"), ""));
        form.put("orderRef", firstNonBlank(form.get("orderRef"), hidden.get("orderRef"), ""));
        form.put("currCode", firstNonBlank(form.get("currCode"), hidden.get("currCode"), "764"));
        form.put("pMethod", brand);
        form.put("payType", firstNonBlank(form.get("payType"), hidden.get("payType"), "N"));
        form.put("TxType", firstNonBlank(form.get("TxType"), hidden.get("TxType"), "Retail"));
        form.put("successUrl", firstNonBlank(form.get("successUrl"), hidden.get("successUrl"), ""));
        form.put("failUrl", firstNonBlank(form.get("failUrl"), hidden.get("failUrl"), ""));
        form.put("errorUrl", firstNonBlank(form.get("errorUrl"), hidden.get("errorUrl"), hidden.get("failUrl"), ""));
        form.put("lang", firstNonBlank(form.get("lang"), hidden.get("lang"), "E"));
        form.put("remark", firstNonBlank(form.get("remark"), hidden.get("remark"), "-"));
        String email = str(body.get("payEmailAddress"));
        String checkedIp = callElementPayCardsCheck(http, formUrlTrim, email, buyerIp);
        if (checkedIp == null) {
            log.warn("ElementPay k/cards/check 거부/실패 url={}", formUrlTrim);
            return null;
        }
        /* 호스티드 폼은 check 응답 customerIp(=브라우저 IP)를 넣음. 서버 check 는 egress IP 가 되므로 구매자 IP 우선. */
        String ip = !buyerIp.isBlank() ? buyerIp : checkedIp.trim();
        form.put("threeDSCustomerIP", ip);
        form.put("threeDSCustomerEmail", email);
        form.put("cardNo", pan);
        form.put("epMonth", month);
        form.put("epYear", epYear);
        form.put("securityCode", str(body.get("payCardcvv")).replaceAll("\\D", ""));
        form.put("cardHolder", holder);
        form.put("orderRef1", firstNonBlank(hidden.get("orderRef1"), ""));
        return form;
    }

    /**
     * EP {@code /k/cards/check} — form 과 동일 HttpClient(쿠키)로 호출. 성공 시 customerIp, 실패 시 null.
     */
    private String callElementPayCardsCheck(HttpClient http, String formUrl, String customerEmail, String buyerIp) {
        if (formUrl == null || formUrl.isBlank() || http == null) {
            return null;
        }
        String checkUrl = formUrl.replace("/k/cards/form", "/k/cards/check");
        if (checkUrl.equals(formUrl)) {
            checkUrl = formUrl.replace("/cards/form", "/cards/check");
        }
        if (checkUrl.equals(formUrl) || !checkUrl.contains("/check")) {
            log.warn("ElementPay cards check URL 변환 실패: {}", formUrl);
            return null;
        }
        try {
            String formBody = "customerEmail=" + URLEncoder.encode(
                    customerEmail != null ? customerEmail : "", StandardCharsets.UTF_8);
            HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(checkUrl))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Referer", formUrl)
                    .header("Origin", originOf(formUrl))
                    .POST(HttpRequest.BodyPublishers.ofString(formBody));
            if (buyerIp != null && !buyerIp.isBlank()) {
                req.header("X-Forwarded-For", buyerIp.trim());
                req.header("X-Real-IP", buyerIp.trim());
            }
            HttpResponse<String> entity = http.send(req.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode resp = objectMapper.readTree(entity.body() != null ? entity.body() : "{}");
            boolean can = resp.path("isCanBeProcessed").asBoolean(false);
            boolean needRedirect = resp.path("isNeedRedirect").asBoolean(false);
            if (!can) {
                log.warn("ElementPay k/cards/check 불가 can={} redirect={} body={}",
                        can, needRedirect, resp);
                return null;
            }
            String ip = resp.path("customerIp").asText("");
            log.info("ElementPay k/cards/check ok buyerIp={} customerIp={}",
                    buyerIp != null ? buyerIp : "", ip != null ? ip : "");
            return ip != null ? ip.trim() : "";
        } catch (Exception e) {
            log.warn("ElementPay k/cards/check HTTP 실패: {}", e.getMessage());
            return null;
        }
    }

    private static String originOf(String url) {
        try {
            URI u = URI.create(url);
            String scheme = u.getScheme() != null ? u.getScheme() : "https";
            String host = u.getHost() != null ? u.getHost() : "api-sbox.elementpay.io";
            int port = u.getPort();
            if (port > 0 && port != 80 && port != 443) {
                return scheme + "://" + host + ":" + port;
            }
            return scheme + "://" + host;
        } catch (Exception e) {
            return "https://api-sbox.elementpay.io";
        }
    }

    private static String attrKeys(List<Map<String, String>> attrs) {
        if (attrs == null || attrs.isEmpty()) {
            return "";
        }
        List<String> keys = new ArrayList<>();
        for (Map<String, String> a : attrs) {
            if (a == null) {
                continue;
            }
            String k = a.get("key");
            if (k != null && !k.isBlank()) {
                keys.add(k);
            }
        }
        return String.join(",", keys);
    }

    private static boolean isElementPayCardFormUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String u = url.toLowerCase(Locale.ROOT);
        return u.contains("elementpay.io") && (u.contains("/k/cards/form") || u.contains("/cards/form")
                || u.contains("/th/cards") || u.contains("/card/form"));
    }

    private static String firstUsablePayUrl(List<Map<String, String>> attrs, String publicBase) {
        String preferred = firstAttr(attrs, "redirectUrl", "redirect_url", "redirect", "RedirectUrl",
                "paymentUrl", "payment_url", "formUrl", "hostedUrl", "checkoutUrl", "url");
        if (isUsablePayUrl(preferred, publicBase)) {
            return preferred;
        }
        String https = firstUsableHttpsAttribute(attrs, publicBase);
        return https != null && !https.isBlank() ? https : "";
    }

    private static boolean isUsablePayUrl(String url, String publicBase) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String t = url.trim();
        if (!(t.startsWith("https://") || t.startsWith("http://"))) {
            return false;
        }
        if (isOurCheckoutReturnUrl(t, publicBase) || isElementPayLightCategoryUrl(t)) {
            return false;
        }
        return true;
    }

    private static String firstUsableHttpsAttribute(List<Map<String, String>> attrs, String publicBase) {
        if (attrs == null) {
            return "";
        }
        for (Map<String, String> a : attrs) {
            if (a == null) {
                continue;
            }
            String v = a.get("value");
            if (isUsablePayUrl(v, publicBase)) {
                return v.trim();
            }
        }
        return "";
    }

    private static String urlHostPath(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        try {
            URI u = URI.create(url.trim());
            String host = u.getHost() != null ? u.getHost() : "";
            String path = u.getPath() != null ? u.getPath() : "";
            return host + path;
        } catch (Exception e) {
            return "(unparseable)";
        }
    }

    private static boolean isOurCheckoutReturnUrl(String url, String publicBase) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String u = url.toLowerCase(Locale.ROOT);
        if (u.contains("elementpayreturn=") || u.contains("elementpayreturn-")) {
            return true;
        }
        String base = publicBase != null ? publicBase.trim().toLowerCase(Locale.ROOT) : "";
        if (!base.isEmpty() && u.startsWith(base) && u.contains("/checkout/")) {
            return true;
        }
        return u.contains("api.icopay.co.kr/checkout/") || u.contains("/checkout/") && u.contains("elementpay");
    }

    private static boolean isExternalAcsUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String u = url.toLowerCase(Locale.ROOT);
        if (!(u.startsWith("https://") || u.startsWith("http://"))) {
            return false;
        }
        /* 우리 waiting/success 복귀 URL 은 ACS 가 아님 */
        if (isOurCheckoutReturnUrl(url, "")) {
            return false;
        }
        return true;
    }

    private static String extractHtmlFormAction(String html) {
        if (html == null) {
            return null;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?is)<form[^>]*\\baction\\s*=\\s*[\"']([^\"']+)[\"']")
                .matcher(html);
        if (m.find()) {
            return decodeHtmlEntities(m.group(1).trim());
        }
        return null;
    }

    private static String resolveFormAction(String formUrl, String action) {
        if (action == null || action.isBlank()) {
            return action;
        }
        String a = action.trim();
        if (a.startsWith("https://") || a.startsWith("http://")) {
            return a;
        }
        try {
            return URI.create(formUrl).resolve(a).toString();
        } catch (Exception e) {
            return a;
        }
    }

    private static Map<String, String> extractHtmlHiddenInputs(String html) {
        Map<String, String> map = new LinkedHashMap<>();
        if (html == null || html.isBlank()) {
            return map;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?is)<input([^>]+)>")
                .matcher(html);
        while (m.find()) {
            String attrs = m.group(1);
            String type = attrValue(attrs, "type");
            if (type != null && !type.equalsIgnoreCase("hidden") && !type.isBlank()) {
                /* visible fields also have names we may need as defaults — skip non-hidden empty */
                if (!"hidden".equalsIgnoreCase(type)) {
                    continue;
                }
            }
            String name = attrValue(attrs, "name");
            if (name == null || name.isBlank()) {
                continue;
            }
            String value = attrValue(attrs, "value");
            map.put(name, value != null ? decodeHtmlEntities(value) : "");
        }
        return map;
    }

    private static String attrValue(String attrs, String key) {
        if (attrs == null || key == null) {
            return null;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?i)\\b" + java.util.regex.Pattern.quote(key) + "\\s*=\\s*[\"']([^\"']*)[\"']")
                .matcher(attrs);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private static String decodeHtmlEntities(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return s.replace("&#x3D;", "=").replace("&#61;", "=").replace("&amp;", "&")
                .replace("&quot;", "\"").replace("&#39;", "'");
    }

    /**
     * Light UI와 동일 — {@code GET /bangkokbank/card/keys?key=&pid=} (서명 없음).
     */
    private JsonNode fetchBangkokBankCardKeys(PgAgency agency, ElementPayCredentials cred, String paymentId) {
        if (paymentId == null || paymentId.isBlank()) {
            return null;
        }
        try {
            String url = resolveBase(agency) + "/bangkokbank/card/keys?key=" + urlEnc(cred.merchantKey())
                    + "&pid=" + urlEnc(paymentId.trim());
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.ALL));
            ResponseEntity<String> entity = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            return objectMapper.readTree(entity.getBody() != null ? entity.getBody() : "{}");
        } catch (Exception e) {
            log.warn("ElementPay bangkokbank/card/keys HTTP 실패: {}", e.getMessage());
            return null;
        }
    }

    private static Map<String, String> buildBkbInlineForm(JsonNode bkb, Map<String, Object> body,
                                                          String pan, String paymentId, String amountPlain,
                                                          String successUrl, String failUrl, String errorUrl) {
        String action = firstNonBlank(
                bkb.path("bkbApiUrl").asText(""),
                bkb.path("apiUrl").asText(""));
        if (action == null || action.isBlank()) {
            return null;
        }
        String month = str(body.get("payCardmonth")).replaceAll("\\D", "");
        if (month.length() == 1) {
            month = "0" + month;
        }
        String yearRaw = str(body.get("payCardyear")).replaceAll("\\D", "");
        String epYear;
        if (yearRaw.length() == 2) {
            try {
                epYear = String.valueOf(2000 + Integer.parseInt(yearRaw));
            } catch (NumberFormatException e) {
                epYear = yearRaw;
            }
        } else if (yearRaw.length() == 4) {
            epYear = yearRaw;
        } else {
            epYear = yearRaw;
        }
        String holder = (str(body.get("payFirstname")) + " " + str(body.get("payLastname"))).trim();
        if (holder.isBlank()) {
            holder = str(body.get("customerNm"));
        }
        String brand = str(body.get("payCardBrand"));
        if (brand.isBlank() || "AUTO".equalsIgnoreCase(brand)) {
            brand = detectBkbCardBrand(pan);
        } else {
            brand = mapBrandToBkb(brand);
        }
        /* 복귀 URL은 반드시 ICOPAY 결제 결과 페이지(가맹·Light 호스티드 결과 화면 금지) */
        String okUrl = firstNonBlank(successUrl, bkb.path("redirectUrls").path("successUrl").asText(""));
        String ngUrl = firstNonBlank(failUrl, bkb.path("redirectUrls").path("failUrl").asText(""));
        String erUrl = firstNonBlank(errorUrl, bkb.path("redirectUrls").path("errorUrl").asText(""), ngUrl);
        Map<String, String> form = new LinkedHashMap<>();
        form.put("actionUrl", action);
        form.put("merchantId", bkb.path("merchantId").asText(""));
        form.put("amount", firstNonBlank(bkb.path("amount").asText(""), amountPlain));
        form.put("orderRef", paymentId);
        form.put("currCode", bkb.path("currCode").asText("764"));
        form.put("pMethod", brand);
        form.put("payType", "N");
        form.put("successUrl", okUrl);
        form.put("failUrl", ngUrl);
        form.put("errorUrl", erUrl);
        form.put("lang", bkb.path("lang").asText("E"));
        form.put("remark", "-");
        form.put("cardNo", pan);
        form.put("epMonth", month);
        form.put("epYear", epYear);
        form.put("securityCode", str(body.get("payCardcvv")).replaceAll("\\D", ""));
        form.put("cardHolder", holder);
        form.put("orderRef1", bkb.path("ref1").asText(""));
        return form;
    }

    private static boolean isElementPayLightCategoryUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String u = url.toLowerCase(Locale.ROOT);
        return u.contains("/merchant/light")
                && (u.contains("/services/") || u.contains("/categories") || u.contains("#/services"));
    }

    private static String detectBkbCardBrand(String pan) {
        String n = pan != null ? pan : "";
        if (n.matches("^(?:2131|1800|35).*") && n.length() >= 15) {
            return "JCB";
        }
        if (n.startsWith("4")) {
            return "VISA";
        }
        if (n.matches("^(5[1-5]|222[1-9]|22[3-9]|2[3-6]|27[01]|2720).*")) {
            return "Master";
        }
        if (n.matches("^(62|81).*")) {
            return "UnionPay";
        }
        return "VISA";
    }

    private static String mapBrandToBkb(String brand) {
        String b = brand.trim().toUpperCase(Locale.ROOT);
        if (b.contains("MASTER")) {
            return "Master";
        }
        if (b.contains("JCB")) {
            return "JCB";
        }
        if (b.contains("UNION")) {
            return "UnionPay";
        }
        if (b.contains("VISA")) {
            return "VISA";
        }
        return brand;
    }

    private static String firstNonBlank(String... vals) {
        if (vals == null) {
            return "";
        }
        for (String v : vals) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return "";
    }

    private static String digitsOnly(String s) {
        if (s == null || s.isBlank()) {
            return "";
        }
        return s.replaceAll("\\D", "");
    }

    /**
     * INLINE 결제 후 폴링용 — getStatus + 로컬 거래 상태를 paid/failed/pending 으로 요약합니다.
     * {@code finalizeReject=true} 일 때만 getStatus 204를 로컬 실패로 확정합니다.
     * (은행 204 직후 EP {@code pay} 웹훅이 올 수 있어, 폴링 중에는 대기를 유지합니다.)
     */
    public Map<String, Object> queryInlineStatus(Long orgUnitId, String paymentId, String orderNo) {
        return queryInlineStatus(orgUnitId, paymentId, orderNo, false);
    }

    public Map<String, Object> queryInlineStatus(Long orgUnitId, String paymentId, String orderNo,
                                                 boolean finalizeReject) {
        /* 로컬 승인은 웹훅(pay) 반영분 — getStatus 보다 우선. 99(선제 실패)는 회복 가능하므로 단축하지 않음. */
        if (orderNo != null && !orderNo.isBlank()) {
            Optional<com.pg.entity.PgTrnsctn> local = elementPaySaleRecordService.findAnyByOrder(orderNo.trim());
            if (local.isPresent()) {
                String stLocal = local.get().getStatus() != null ? local.get().getStatus().trim() : "";
                if (ElementPayInlineStatusUtil.isLocalPaid(stLocal)) {
                    Map<String, Object> out = new LinkedHashMap<>();
                    out.put("success", true);
                    out.put("paymentStatus", "PAID");
                    out.put("paid", true);
                    out.put("orderNo", orderNo);
                    out.put("paymentId", paymentId);
                    out.put("source", "LOCAL");
                    return out;
                }
                if (ElementPayInlineStatusUtil.isLocalHardFail(stLocal)) {
                    Map<String, Object> out = new LinkedHashMap<>();
                    out.put("success", true);
                    out.put("paymentStatus", "FAILED");
                    out.put("paid", false);
                    out.put("orderNo", orderNo);
                    out.put("paymentId", paymentId);
                    out.put("source", "LOCAL");
                    String localMsg = firstNonBlank(
                            local.get().getOutcomeReason(),
                            local.get().getChillPaymentStatus());
                    String mk = checkoutMessageKey(localMsg);
                    if (mk != null) {
                        out.put("messageKey", mk);
                    }
                    if (localMsg != null && !localMsg.isBlank()
                            && !localMsg.toUpperCase(Locale.ROOT).startsWith("ELEMENTPAY_URL")) {
                        out.put("statusMessage", localMsg.trim());
                    }
                    return out;
                }
                if (ElementPayInlineStatusUtil.isLocalRefundOrChargeback(stLocal)) {
                    Map<String, Object> out = new LinkedHashMap<>();
                    out.put("success", true);
                    boolean chargeback = "31".equals(stLocal);
                    out.put("paymentStatus", chargeback ? "REVERSED" : "REFUNDED");
                    out.put("paid", false);
                    out.put("refunded", !chargeback);
                    out.put("reversed", chargeback);
                    out.put("orderNo", orderNo);
                    out.put("paymentId", paymentId);
                    out.put("source", "LOCAL");
                    out.put("messageKey", chargeback
                            ? "ELEMENTPAY_PAYMENT_REVERSED" : "ELEMENTPAY_PAYMENT_REFUNDED");
                    return out;
                }
            }
        }

        Map<String, Object> raw = queryStatus(orgUnitId, paymentId, orderNo);
        if (!Boolean.TRUE.equals(raw.get("success"))) {
            return raw;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> tree = (Map<String, Object>) raw.get("raw");
        Object responseObj = tree != null ? tree.get("response") : null;
        int st = 0;
        String statusMessage = "";
        if (responseObj instanceof Map<?, ?> rm) {
            Object sv = rm.get("status");
            if (sv != null) {
                try {
                    st = Integer.parseInt(String.valueOf(sv).trim());
                } catch (NumberFormatException ignored) {
                    st = 0;
                }
            }
            Object sm = rm.get("status_message");
            if (sm == null) {
                sm = rm.get("message");
            }
            if (sm != null) {
                statusMessage = String.valueOf(sm).trim();
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("elementPayStatus", st);
        out.put("paymentId", paymentId);
        out.put("orderNo", orderNo);
        out.put("source", "ELEMENTPAY");
        if (!statusMessage.isBlank()) {
            out.put("statusMessage", statusMessage);
        }
        ElementPayInlineStatusUtil.Mapped mapped =
                ElementPayInlineStatusUtil.fromGetStatus(st, finalizeReject);
        out.put("paymentStatus", mapped.paymentStatus());
        out.put("paid", mapped.paid());
        if (mapped.kind() == ElementPayInlineStatusUtil.Kind.REFUNDED) {
            out.put("refunded", true);
        }
        if (mapped.provisionalReject()) {
            out.put("provisionalReject", true);
        }
        String mk = checkoutMessageKey(statusMessage);
        if (mk == null) {
            mk = mapped.defaultMessageKey();
        }
        if (mk != null) {
            out.put("messageKey", mk);
        }
        if (mapped.persistPaid()) {
            syncLocalOutcomeFromStatus(orderNo, paymentId, true, statusMessage);
        } else if (mapped.persistFail()) {
            String failMsg = !statusMessage.isBlank() ? statusMessage
                    : (mapped.kind() == ElementPayInlineStatusUtil.Kind.DISPUTED
                    ? "ELEMENTPAY_DISPUTED" : "ElementPay rejected");
            syncLocalOutcomeFromStatus(orderNo, paymentId, false, failMsg);
        } else if (mapped.persistRefund()) {
            syncLocalRefundFromStatus(orderNo, paymentId,
                    !statusMessage.isBlank() ? statusMessage : "Payment is refunded");
        }
        return out;
    }

    /** getStatus 가 최종 승인/거절이면 웹훅 누락 시에도 로컬 대기를 맞춘다. */
    private void syncLocalOutcomeFromStatus(String orderNo, String paymentId, boolean paid, String msg) {
        if (orderNo == null || orderNo.isBlank()) {
            return;
        }
        try {
            elementPaySaleRecordService.findAnyByOrder(orderNo.trim()).ifPresent(t -> {
                String st = t.getStatus() != null ? t.getStatus().trim() : "";
                if (paid) {
                    if (ElementPayInlineStatusUtil.skipSyncWhenPaid(st)) {
                        return;
                    }
                    /* 99(선제 실패)여도 getStatus 203/205 또는 pay 웹훅이 있으면 승인으로 회복 */
                } else if (ElementPayInlineStatusUtil.skipSyncWhenUnpaid(st)) {
                    return;
                }
                if (t.getMerchantId() == null || t.getMerchantId().isBlank()) {
                    return;
                }
                Optional<PgTrnsctn> updated = elementPaySaleRecordService.applyOutcome(
                        t.getMerchantId(), orderNo.trim(), paid, paymentId, msg);
                if (updated.isEmpty()) {
                    return;
                }
                PgTrnsctn u = updated.get();
                if (paid && ElementPayInlineStatusUtil.isLocalPaid(u.getStatus())) {
                    afterStatusSyncPaid(u);
                } else {
                    try {
                        merchantOutboundNotifyService.scheduleAfterTxnCommit(u, null, "STATUS_SYNC");
                    } catch (Exception e) {
                        log.warn("ElementPay status-sync outbound 실패: {}", e.getMessage());
                    }
                    try {
                        elementPayNotiMiddlewareMirrorService.scheduleUrlPayMirrorAfterCommit(u, false);
                    } catch (Exception e) {
                        log.warn("ElementPay NOTI mirror(fail) 예약 실패: {}", e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            log.debug("ElementPay 로컬 상태 동기화 생략: {}", e.getMessage());
        }
    }

    /** getStatus 승인 반영 후 — 웹훅 Callback 경로와 동일(정산·영수증·가맹/Dealmai 통보) + NOTI 미러. */
    private void afterStatusSyncPaid(PgTrnsctn t) {
        try {
            splitPayPaymentHookService.onTxnStatusChange(t.getOrderNo(), t.getStatus(), t.getTrnId());
        } catch (Exception ignored) {
        }
        try {
            transactionReceiptEmailService.scheduleIfDue(t);
        } catch (Exception e) {
            log.warn("ElementPay status-sync 영수증 메일 실패 trnId={}: {}", t.getTrnId(), e.getMessage());
        }
        try {
            if (t.getMerchantId() != null && !t.getMerchantId().isBlank()) {
                settlementCalcService.triggerRealtimeAutoSettlementIfDue(t.getMerchantId().trim(), t);
            }
        } catch (Exception e) {
            log.warn("ElementPay status-sync 정산 트리거 실패: {}", e.getMessage());
        }
        try {
            merchantOutboundNotifyService.scheduleAfterTxnCommit(t, null, "STATUS_SYNC");
        } catch (Exception e) {
            log.warn("ElementPay status-sync outbound 실패: {}", e.getMessage());
        }
        try {
            elementPayNotiMiddlewareMirrorService.scheduleUrlPayMirrorAfterCommit(t, true);
        } catch (Exception e) {
            log.warn("ElementPay NOTI mirror 예약 실패: {}", e.getMessage());
        }
    }

    /** getStatus 207(refunded) — 로컬을 자동환불(42)로 맞춤. */
    private void syncLocalRefundFromStatus(String orderNo, String paymentId, String msg) {
        if (orderNo == null || orderNo.isBlank()) {
            return;
        }
        try {
            elementPaySaleRecordService.findAnyByOrder(orderNo.trim()).ifPresent(t -> {
                String st = t.getStatus() != null ? t.getStatus().trim() : "";
                if ("42".equals(st) || "31".equals(st) || "30".equals(st)) {
                    return;
                }
                elementPaySaleRecordService.applyRefundStatus(t, "42", paymentId, msg);
            });
        } catch (Exception e) {
            log.debug("ElementPay 환불 상태 동기화 생략: {}", e.getMessage());
        }
    }

    /**
     * Merchant API initPayment (문서 1.19.2).
     * Light 위젯의 {@code light=true} 는 Merchant Key HMAC 이라 API Secret 서명과 섞지 않는다.
     * 서명 실패·필수속성 누락이면 다음 전송 형태로 재시도한다.
     */
    private enum InitAttrMode {
        YAML_JSON_AND_TOP,
        USER_PARAMETER,
        PHP_NESTED,
        TOP_LEVEL_ONLY
    }

    private JsonNode initPaymentTryingModes(PgAgency agency, ElementPayCredentials cred, String serviceAlias,
                                            String amountPlain, String orderNo,
                                            String successUrl, String rejectUrl, String waitingUrl,
                                            Map<String, String> extra) {
        JsonNode last = null;
        for (InitAttrMode mode : InitAttrMode.values()) {
            last = postInitPayment(agency, cred, serviceAlias, amountPlain, orderNo,
                    successUrl, rejectUrl, waitingUrl, extra, mode);
            if (last == null) {
                continue;
            }
            if (!last.has("error")) {
                log.info("ElementPay init ok mode={} service={}", mode, serviceAlias);
                return last;
            }
            String msg = extractEpErrorMessage(last);
            log.warn("ElementPay init miss mode={} service={} msg={}", mode, serviceAlias, msg);
            if (!isRetryableInitError(msg)) {
                return last;
            }
        }
        return last;
    }

    private JsonNode postInitPayment(PgAgency agency, ElementPayCredentials cred, String serviceAlias,
                                     String amountPlain, String orderNo,
                                     String successUrl, String rejectUrl, String waitingUrl,
                                     Map<String, String> payerAttrs,
                                     InitAttrMode mode) {
        long ts = Instant.now().getEpochSecond();
        Map<String, String> signParams = new LinkedHashMap<>();
        signParams.put("service_id", serviceAlias);
        signParams.put("amount", amountPlain);
        signParams.put("order", orderNo);
        signParams.put("currency", THB);
        Map<String, String> clean = new LinkedHashMap<>();
        if (payerAttrs != null) {
            for (Map.Entry<String, String> e : payerAttrs.entrySet()) {
                if (e.getKey() == null || e.getValue() == null || e.getValue().isBlank()) {
                    continue;
                }
                if (ElementPayAdditionalParameters.isReservedInitKey(e.getKey())) {
                    continue;
                }
                clean.put(e.getKey(), e.getValue());
            }
        }
        try {
            if (mode == InitAttrMode.YAML_JSON_AND_TOP && !clean.isEmpty()) {
                signParams.put("additional_parameters", objectMapper.writeValueAsString(clean));
            }
            if (mode == InitAttrMode.USER_PARAMETER && !clean.isEmpty()) {
                signParams.put("user_parameter", objectMapper.writeValueAsString(clean));
            }
        } catch (Exception e) {
            log.warn("ElementPay attr JSON 실패 mode={}: {}", mode, e.getMessage());
        }
        if (mode == InitAttrMode.PHP_NESTED) {
            for (Map.Entry<String, String> e : clean.entrySet()) {
                signParams.put("additional_parameters[" + e.getKey() + "]", e.getValue());
                signParams.put("user_parameter[" + e.getKey() + "]", e.getValue());
            }
        } else {
            signParams.putAll(clean);
        }
        signParams.put("_successUrl", successUrl != null ? successUrl : "");
        signParams.put("_waitingUrl", waitingUrl != null ? waitingUrl : "");
        signParams.put("_rejectUrl", rejectUrl != null ? rejectUrl : "");
        signParams.put("key", cred.merchantKey());
        signParams.put("timestamp", String.valueOf(ts));
        log.info("ElementPay initPayment POST service={} mode={} extraKeys={}",
                serviceAlias, mode, clean.keySet());
        String query = ElementPayHashUtil.buildApiQueryString(signParams);
        String hash = ElementPayHashUtil.signApiRequest(cred.apiSecretKey(), "initPayment", signParams);
        String formBody = query + "&hash=" + hash;
        String url = resolveBase(agency) + "/merchant/initPayment";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
            ResponseEntity<String> entity = restTemplate.postForEntity(url, new HttpEntity<>(formBody, headers), String.class);
            return parseEpApiJson("initPayment", cred, entity.getBody());
        } catch (Exception e) {
            log.warn("ElementPay initPayment HTTP 실패 service_id={} mode={}: {}", serviceAlias, mode, e.getMessage());
            return null;
        }
    }

    private static Map<String, String> extrasForInit(Map<String, String> canonical, List<String> methodKeys) {
        List<String> keysToSend = (methodKeys == null || methodKeys.isEmpty())
                ? ElementPayAdditionalParameters.FALLBACK_ATTR_KEYS
                : methodKeys;
        Map<String, String> extra = new LinkedHashMap<>(
                ElementPayAdditionalParameters.onlyKeys(canonical, keysToSend));
        extra.putAll(ElementPayAdditionalParameters.onlyKeys(canonical,
                ElementPayAdditionalParameters.FALLBACK_ATTR_KEYS));
        return ElementPayAdditionalParameters.withAliases(extra);
    }

    public Map<String, Object> queryStatus(Long orgUnitId, String paymentId, String orderNo) {
        Optional<MerchantPgBinding> bindOpt = findOperationalElementPayBinding(orgUnitId);
        if (bindOpt.isEmpty()) {
            return fail("ElementPay 운영 바인딩이 없습니다.", "ELEMENTPAY_PG_MISSING");
        }
        Optional<PgAgency> agOpt = pgAgencyRepository.findByPgCd(trim(bindOpt.get().getPgCd()));
        if (agOpt.isEmpty()) {
            return fail("ElementPay PG 설정 없음", "ELEMENTPAY_AGENCY_MISSING");
        }
        ElementPayCredentials cred = ElementPayCredentials.from(agOpt.get());
        long ts = Instant.now().getEpochSecond();
        Map<String, String> signParams = new LinkedHashMap<>();
        if (paymentId != null && !paymentId.isBlank()) {
            signParams.put("payment_id", paymentId.trim());
        } else if (orderNo != null && !orderNo.isBlank()) {
            signParams.put("order", orderNo.trim());
        } else {
            return fail("paymentId 또는 orderNo가 필요합니다.", "INVALID_QUERY");
        }
        signParams.put("key", cred.merchantKey());
        signParams.put("timestamp", String.valueOf(ts));
        String query = ElementPayHashUtil.buildApiQueryString(signParams);
        String hash = ElementPayHashUtil.signApiRequest(cred.apiSecretKey(), "getStatus", signParams);
        String formBody = query + "&hash=" + hash;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
            String url = resolveBase(agOpt.get()) + "/merchant/getStatus";
            ResponseEntity<String> entity = restTemplate.postForEntity(url, new HttpEntity<>(formBody, headers), String.class);
            JsonNode resp = parseEpApiJson("getStatus", cred, entity.getBody());
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("success", !resp.has("error"));
            out.put("raw", objectMapper.convertValue(resp, Map.class));
            return out;
        } catch (Exception e) {
            return fail("ElementPay 상태 조회 실패", "ELEMENTPAY_STATUS_FAILED");
        }
    }

    /**
     * 결제내역 자동/강제환불 — EP {@code /merchant/initRefund}.
     * {@code amountOpt} 비어 있으면 전액 환불. 성공 시 EP 응답 요약을 반환하고, 실패 시 IllegalStateException.
     */
    public String requestRefund(PgTrnsctn t, String amountOpt, String reason) {
        if (t == null || !PgVendor.isElementPayFamily(t.getVan())) {
            throw new IllegalStateException("ElementPay 거래만 환불 API를 호출할 수 있습니다.");
        }
        String paymentId = ElementPayPaymentIdUtil.fromTxn(t);
        if (paymentId.isBlank()) {
            paymentId = lookupPaymentIdByOrder(t);
        }
        if (paymentId == null || paymentId.isBlank()) {
            throw new IllegalStateException("결제망 payment_id가 없어 환불 API를 호출할 수 없습니다. 주문번호로 상태 조회에 실패했습니다.");
        }
        if (t.getChillTransactionId() == null || t.getChillTransactionId().isBlank()) {
            t.setChillTransactionId(paymentId.length() > 64 ? paymentId.substring(0, 64) : paymentId);
        }
        PgAgency agency = resolveAgencyForTxn(t);
        ElementPayCredentials cred = ElementPayCredentials.from(agency);
        if (!cred.isConfigured()) {
            throw new IllegalStateException("ElementPay Merchant Key·Secret Key 가 설정되지 않았습니다.");
        }
        long ts = Instant.now().getEpochSecond();
        Map<String, String> signParams = new LinkedHashMap<>();
        signParams.put("payment_id", paymentId.trim());
        if (t.getOrderNo() != null && !t.getOrderNo().isBlank()) {
            signParams.put("order", t.getOrderNo().trim());
        }
        if (reason != null && !reason.isBlank()) {
            String r = reason.trim();
            if (r.length() > 255) {
                r = r.substring(0, 255);
            }
            signParams.put("reason", r);
        }
        if (amountOpt != null && !amountOpt.isBlank()) {
            signParams.put("amount", amountOpt.trim());
        }
        signParams.put("key", cred.merchantKey());
        signParams.put("timestamp", String.valueOf(ts));
        String query = ElementPayHashUtil.buildApiQueryString(signParams);
        String hash = ElementPayHashUtil.signApiRequest(cred.apiSecretKey(), "initRefund", signParams);
        String formBody = query + "&hash=" + hash;
        String url = resolveBase(agency) + "/merchant/initRefund";
        JsonNode resp;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
            ResponseEntity<String> entity = restTemplate.postForEntity(
                    url, new HttpEntity<>(formBody, headers), String.class);
            resp = parseEpApiJson("initRefund", cred, entity.getBody());
        } catch (HttpStatusCodeException e) {
            String body = e.getResponseBodyAsString();
            log.warn("ElementPay initRefund HTTP {} paymentId={} body={}",
                    e.getStatusCode().value(), paymentId, body);
            resp = parseEpApiJsonQuiet("initRefund", cred, body);
            if (resp == null || !resp.has("error")) {
                throw new IllegalStateException(ElementPayRefundRejectMapper.operatorMessage(
                        String.valueOf(e.getStatusCode().value()),
                        body != null && !body.isBlank() ? body : e.getStatusText()));
            }
        } catch (Exception e) {
            log.warn("ElementPay initRefund HTTP 실패 paymentId={}: {}", paymentId, e.getMessage());
            throw new IllegalStateException("결제망 환불 요청에 실패했습니다: " + e.getMessage());
        }
        return interpretInitRefundResponse(resp, t, paymentId);
    }

    private String interpretInitRefundResponse(JsonNode resp, PgTrnsctn t, String paymentId) {
        if (resp != null && resp.has("error")) {
            String code = extractEpErrorCode(resp);
            String msg = extractEpErrorMessage(resp);
            String low = msg.toLowerCase(Locale.ROOT);
            if ("479".equals(code) || low.contains("already refunded")) {
                log.info("ElementPay initRefund already refunded orderNo={} paymentId={}", t.getOrderNo(), paymentId);
                return "already refunded";
            }
            if ("470".equals(code) || low.contains("already has refund")) {
                log.info("ElementPay initRefund already in progress orderNo={} paymentId={}", t.getOrderNo(), paymentId);
                return "refund in progress";
            }
            throw new IllegalStateException(ElementPayRefundRejectMapper.operatorMessage(code, msg));
        }
        String status = resp != null ? resp.path("response").path("status").asText("") : "";
        String id = resp != null ? resp.path("response").path("id").asText(paymentId) : paymentId;
        String summary = "ElementPay initRefund OK id=" + id
                + (!status.isBlank() ? " status=" + status : "");
        log.info("ElementPay refund OK orderNo={} paymentId={} status={}",
                t.getOrderNo(), paymentId, status);
        return summary;
    }

    private String lookupPaymentIdByOrder(PgTrnsctn t) {
        if (t == null || t.getOrderNo() == null || t.getOrderNo().isBlank()) {
            return "";
        }
        try {
            long ouId = resolveMerchantOrgUnitIdForRefund(t);
            Map<String, Object> st = queryStatus(ouId, null, t.getOrderNo().trim());
            if (!Boolean.TRUE.equals(st.get("success"))) {
                return "";
            }
            Object raw = st.get("raw");
            if (!(raw instanceof Map<?, ?> tree)) {
                return "";
            }
            Object responseObj = tree.get("response");
            if (!(responseObj instanceof Map<?, ?> rm)) {
                return "";
            }
            Object id = rm.get("id");
            return id != null ? String.valueOf(id).trim() : "";
        } catch (Exception e) {
            log.warn("ElementPay getStatus(order) for refund lookup failed: {}", e.getMessage());
            return "";
        }
    }

    private PgAgency resolveAgencyForTxn(PgTrnsctn t) {
        String van = t.getVan() != null ? t.getVan().trim() : "";
        if (!van.isBlank()) {
            Optional<PgAgency> byCd = pgAgencyRepository.findByPgCd(van);
            if (byCd.isPresent()) {
                return byCd.get();
            }
        }
        long ouId = resolveMerchantOrgUnitIdForRefund(t);
        return findOperationalElementPayBinding(ouId)
                .flatMap(b -> pgAgencyRepository.findByPgCd(trim(b.getPgCd())))
                .orElseThrow(() -> new IllegalStateException(
                        "ElementPay PG 설정을 찾을 수 없습니다. van=" + van));
    }

    private long resolveMerchantOrgUnitIdForRefund(PgTrnsctn t) {
        String code = t.getMerchantId();
        if (code == null || code.isBlank()) {
            throw new IllegalStateException("거래에 가맹점 코드가 없습니다.");
        }
        return orgUnitRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new IllegalStateException("가맹점(조직)을 찾을 수 없습니다: " + code.trim()))
                .getId();
    }

    /**
     * HQ 진단 — ElementPay {@code getMethods} 로 사용 가능 결제수단(id·alias·name) 목록.
     * Cabinet 에서 비활성인 수단은 목록에 없거나 initPayment 시 disabled 오류가 납니다.
     */
    public Map<String, Object> listPaymentMethods(Long agencyId) {
        if (agencyId == null) {
            return fail("PG agency id가 필요합니다.", "INVALID_ID");
        }
        Optional<PgAgency> agOpt = pgAgencyRepository.findById(agencyId);
        if (agOpt.isEmpty()) {
            return fail("ElementPay PG 설정을 찾을 수 없습니다.", "ELEMENTPAY_AGENCY_MISSING");
        }
        PgAgency agency = agOpt.get();
        if (!PgVendor.isElementPayFamily(agency.getPgCd())) {
            return fail("ElementPay PG만 조회할 수 있습니다.", "ELEMENTPAY_NOT_APPLICABLE");
        }
        ElementPayCredentials cred = ElementPayCredentials.from(agency);
        if (!cred.isConfigured()) {
            return fail("ElementPay Merchant Key·Secret Key 가 설정되지 않았습니다.", "ELEMENTPAY_CREDENTIALS_MISSING");
        }
        long ts = Instant.now().getEpochSecond();
        Map<String, String> signParams = new LinkedHashMap<>();
        signParams.put("key", cred.merchantKey());
        signParams.put("timestamp", String.valueOf(ts));
        String query = ElementPayHashUtil.buildApiQueryString(signParams);
        String hash = ElementPayHashUtil.signApiRequest(cred.apiSecretKey(), "getMethods", signParams);
        String formBody = query + "&hash=" + hash;
        JsonNode resp;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
            String url = resolveBase(agency) + "/merchant/getMethods";
            ResponseEntity<String> entity = restTemplate.postForEntity(url, new HttpEntity<>(formBody, headers), String.class);
            resp = parseEpApiJson("getMethods", cred, entity.getBody());
        } catch (Exception e) {
            log.warn("ElementPay getMethods HTTP 실패: {}", e.getMessage());
            return fail("ElementPay 결제수단 조회에 실패했습니다.", "ELEMENTPAY_METHODS_FAILED");
        }
        if (resp.has("error")) {
            String msg = resp.path("error").path("message").asText("ElementPay error");
            return fail(msg, "ELEMENTPAY_" + resp.path("error").path("code").asText("ERROR"));
        }
        List<Map<String, Object>> methods = new ArrayList<>();
        JsonNode arr = resp.path("response").path("methods");
        if (arr.isArray()) {
            for (JsonNode m : arr) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", m.path("id").asText(""));
                row.put("alias", m.path("alias").asText(""));
                row.put("name", m.path("name").asText(""));
                row.put("type", m.path("type").asText(""));
                row.put("currency", m.path("currency").asText(""));
                row.put("min", m.path("min").asText(""));
                row.put("max", m.path("max").asText(""));
                JsonNode attrs = m.get("attributes");
                if (attrs != null && attrs.isArray() && !attrs.isEmpty()) {
                    List<String> attrNames = new ArrayList<>();
                    for (JsonNode a : attrs) {
                        String an = firstNonBlank(a.path("alias").asText(""), a.path("name").asText(""),
                                a.path("key").asText(""), a.asText(""));
                        if (an != null && !an.isBlank()) {
                            attrNames.add(an);
                        }
                    }
                    if (!attrNames.isEmpty()) {
                        row.put("attributes", attrNames);
                    }
                }
                methods.add(row);
            }
        }
        String suggestedCard = ElementPayCardServiceAlias.resolveAgainstCatalog(
                ElementPayCardServiceAlias.resolveConfigured(cred.cardServiceAlias(), cred.sandbox()),
                methods);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("sandbox", cred.sandbox());
        out.put("currentCardServiceAlias", cred.cardServiceAlias());
        out.put("suggestedCardServiceAlias", suggestedCard);
        out.put("methods", methods);
        out.put("hint", "목록의 alias(또는 id)를 cardServiceAlias 에 넣고 저장하세요. getMethods에 있는 카드 수단만 사용합니다.");
        out.put("webhookUrl", "https://noti.icopay.net/noti/elementpay");
        out.put("callbackSourceIps", List.of("3.0.36.253", "3.1.29.20"));
        out.put("liveChecklist", List.of(
                "라이브 Webhook URL: https://noti.icopay.net/noti/elementpay",
                "Cabinet Payments Verifier 켜기 (check 콜백)",
                "Events: payment.rejected, payment.reversed, payment.refunded, payment.wrong_payer, refund.created, refund.paid, refund.canceled",
                "Signing Secret은 API Secret과 다른 키 (해당 Webhook 엔드포인트)",
                "콜백 송신 IP(NOTI 화이트리스트): 3.0.36.253(sandbox), 3.1.29.20(live)"
        ));
        return out;
    }

    private static final class CardMethodCatalog {
        private final String serviceAlias;
        private final List<String> attrKeys;

        private CardMethodCatalog(String serviceAlias, List<String> attrKeys) {
            this.serviceAlias = serviceAlias;
            this.attrKeys = attrKeys != null ? attrKeys : List.of();
        }

        private String serviceAlias() {
            return serviceAlias;
        }

        private List<String> attrKeys() {
            return attrKeys;
        }
    }

    private CardMethodCatalog loadCardMethodCatalog(PgAgency agency, ElementPayCredentials cred, String wantedAlias) {
        List<Map<String, Object>> methods = fetchMethodsRows(agency, cred);
        String resolved = ElementPayCardServiceAlias.resolveAgainstCatalog(wantedAlias, methods);
        if (wantedAlias != null && !wantedAlias.equalsIgnoreCase(resolved)) {
            log.warn("ElementPay service_id={} not in getMethods, using {}", wantedAlias, resolved);
        }
        List<String> keys = attrKeysForAlias(methods, resolved);
        List<String> catalog = new ArrayList<>();
        for (Map<String, Object> m : methods) {
            catalog.add(str(m.get("alias")) + "/" + str(m.get("id")) + ":" + m.get("attributes"));
        }
        log.info("ElementPay getMethods catalog want={} resolved={} matchedKeys={} list={}",
                wantedAlias, resolved, keys, catalog);
        return new CardMethodCatalog(resolved, keys);
    }

    private static List<String> attrKeysForAlias(List<Map<String, Object>> methods, String alias) {
        List<String> keys = new ArrayList<>();
        if (methods == null || alias == null) {
            return keys;
        }
        for (Map<String, Object> m : methods) {
            if (m == null) {
                continue;
            }
            String a = str(m.get("alias"));
            String id = str(m.get("id"));
            if (!alias.equalsIgnoreCase(a) && !alias.equals(id)) {
                continue;
            }
            Object attrs = m.get("attributes");
            if (attrs instanceof List<?> list) {
                for (Object o : list) {
                    String k = o == null ? "" : String.valueOf(o).trim();
                    if (!k.isBlank() && !keys.contains(k)) {
                        keys.add(k);
                    }
                }
            }
        }
        return keys;
    }

    private List<Map<String, Object>> fetchMethodsRows(PgAgency agency, ElementPayCredentials cred) {
        List<Map<String, Object>> methods = new ArrayList<>();
        if (agency == null || cred == null || !cred.isConfigured()) {
            return methods;
        }
        try {
            long ts = Instant.now().getEpochSecond();
            Map<String, String> signParams = new LinkedHashMap<>();
            signParams.put("key", cred.merchantKey());
            signParams.put("timestamp", String.valueOf(ts));
            String query = ElementPayHashUtil.buildApiQueryString(signParams);
            String hash = ElementPayHashUtil.signApiRequest(cred.apiSecretKey(), "getMethods", signParams);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
            String url = resolveBase(agency) + "/merchant/getMethods";
            ResponseEntity<String> entity = restTemplate.postForEntity(
                    url, new HttpEntity<>(query + "&hash=" + hash, headers), String.class);
            JsonNode resp = parseEpApiJson("getMethods", cred, entity.getBody());
            JsonNode arr = resp.path("response").path("methods");
            if (!arr.isArray()) {
                return methods;
            }
            for (JsonNode m : arr) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", m.path("id").asText(""));
                row.put("alias", m.path("alias").asText(""));
                row.put("name", m.path("name").asText(""));
                row.put("type", m.path("type").asText(""));
                row.put("currency", m.path("currency").asText(""));
                row.put("min", m.path("min").asText(""));
                row.put("max", m.path("max").asText(""));
                List<String> methodKeys = extractMethodAttrKeys(m.get("attributes"));
                if (!methodKeys.isEmpty()) {
                    row.put("attributes", methodKeys);
                }
                methods.add(row);
            }
        } catch (Exception e) {
            log.warn("ElementPay getMethods 생략: {}", e.getMessage());
        }
        return methods;
    }

    /** getMethods 는 필터 없이 전체 목록을 받은 뒤 alias 로 고른다. */
    private List<String> fetchMethodAttributeKeys(PgAgency agency, ElementPayCredentials cred, String serviceAlias) {
        return loadCardMethodCatalog(agency, cred, serviceAlias).attrKeys();
    }

    public Optional<PgAgency> resolveAgencyByMerchantKey(String merchantKey) {
        if (merchantKey == null || merchantKey.isBlank()) {
            return Optional.empty();
        }
        return pgAgencyRepository.findByMerchantMidOrderByIdAsc(merchantKey.trim()).stream()
                .filter(a -> PgVendor.isElementPayFamily(a.getPgCd()))
                .findFirst();
    }

    /** 주문번호로 로컬 거래 → 가맹 → ElementPay 바인딩 PG 행. */
    public Optional<PgAgency> resolveAgencyByOrderNo(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            return Optional.empty();
        }
        Optional<PgTrnsctn> txn = elementPaySaleRecordService.findAnyByOrder(orderNo.trim());
        if (txn.isEmpty() || txn.get().getMerchantId() == null || txn.get().getMerchantId().isBlank()) {
            return Optional.empty();
        }
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(txn.get().getMerchantId().trim());
        if (ou.isEmpty()) {
            return Optional.empty();
        }
        return findOperationalElementPayBinding(ou.get().getId())
                .flatMap(b -> pgAgencyRepository.findByPgCd(trim(b.getPgCd())))
                .filter(a -> PgVendor.isElementPayFamily(a.getPgCd()));
    }

    public List<PgAgency> listElementPayAgencies() {
        return pgAgencyRepository.findAllByOrderByPgCdAsc().stream()
                .filter(a -> PgVendor.isElementPayFamily(a.getPgCd()))
                .filter(a -> a.getUseYn() == null || "Y".equalsIgnoreCase(a.getUseYn().trim()))
                .toList();
    }

    /**
     * NOTI Result 미가용 시 폴백 — ICOPAY 중립 checkout 결과 화면.
     * (가맹 쇼핑몰 URL 은 EP 에 넣지 않음.)
     */
    private static String resolveIcopayCheckoutReturnUrl(String publicBase, String compId,
                                                         String orderNo, String result) {
        String base = trimSlash(publicBase);
        String cid = compId != null ? compId.trim() : "";
        if (base.isBlank() || cid.isBlank()) {
            return "";
        }
        String r = result != null ? result.trim().toLowerCase(Locale.ROOT) : "success";
        if (!r.equals("success") && !r.equals("reject") && !r.equals("waiting")) {
            r = "success";
        }
        StringBuilder u = new StringBuilder();
        u.append(base).append("/checkout/").append(urlEnc(cid));
        u.append("?elementpayReturn=").append(r);
        String order = orderNo != null ? orderNo.trim() : "";
        if (!order.isEmpty()) {
            u.append("&orderNo=").append(urlEnc(order));
        }
        return u.toString();
    }

    /**
     * 브라우저 INLINE 복귀 — NOTI {@code /noti/result/elementpay}(JPAY Result 슬롯과 동일 역할).
     */
    private String resolveElementPayBrowserReturnUrl(Long orgUnitId, String orderNo, String compId) {
        String configured = resolveMerchantNotiResultUrl(orgUnitId);
        String base;
        if (configured != null && !configured.isBlank()) {
            base = configured.trim();
        } else {
            base = defaultElementPayNotiResultUrl();
        }
        return appendResultMatchQuery(base, orderNo, compId);
    }

    private String defaultElementPayNotiResultUrl() {
        String notiBase = NotiProvisionClient.defaultBaseUrlIfBlank(
                hqNotifyEnvService.getOrCreate().getNotiProvisionBaseUrl());
        return notiBase + "/noti/result/elementpay";
    }

    /**
     * NOTI {@code /noti/result/elementpay} 가맹 매칭용 쿼리.
     * 우선순위(NOTI): compId/merchantId → (선택) order lookup → webhook 로그 order.
     */
    private static String appendResultMatchQuery(String url, String orderNo, String compId) {
        String u = url != null ? url.trim() : "";
        if (u.isEmpty()) {
            return u;
        }
        String order = orderNo != null ? orderNo.trim() : "";
        String cid = compId != null ? compId.trim() : "";
        StringBuilder q = new StringBuilder();
        if (!order.isEmpty() && !queryHasKey(u, "order") && !queryHasKey(u, "orderNo")) {
            q.append("order=").append(java.net.URLEncoder.encode(order, StandardCharsets.UTF_8));
        }
        if (!cid.isEmpty()) {
            if (!queryHasKey(u, "compId") && !queryHasKey(u, "CompId")) {
                if (q.length() > 0) {
                    q.append('&');
                }
                q.append("compId=").append(java.net.URLEncoder.encode(cid, StandardCharsets.UTF_8));
            }
            if (!queryHasKey(u, "merchantId") && !queryHasKey(u, "MerchantId")) {
                if (q.length() > 0) {
                    q.append('&');
                }
                q.append("merchantId=").append(java.net.URLEncoder.encode(cid, StandardCharsets.UTF_8));
            }
        }
        if (q.length() == 0) {
            return u;
        }
        return u + (u.contains("?") ? "&" : "?") + q;
    }

    private static boolean queryHasKey(String url, String key) {
        if (url == null || key == null || key.isBlank()) {
            return false;
        }
        String lower = url.toLowerCase(Locale.ROOT);
        String k = key.toLowerCase(Locale.ROOT);
        return lower.contains("?" + k + "=") || lower.contains("&" + k + "=");
    }

    /**
     * 업체 「수신통보 URL」 Result(NOTI MW) — {@link MerchantNotifyUrl#URL_TYPE_JPAY_CALLBACK}.
     */
    private String resolveMerchantNotiResultUrl(Long orgUnitId) {
        if (orgUnitId == null) {
            return "";
        }
        Optional<MerchantNotifyUrl> row = merchantNotifyUrlRepository
                .findByOrgUnitIdAndUrlType(orgUnitId, MerchantNotifyUrl.URL_TYPE_JPAY_CALLBACK);
        if (row.isEmpty()) {
            return "";
        }
        MerchantNotifyUrl n = row.get();
        if (n.getUseYn() != null && !"Y".equalsIgnoreCase(n.getUseYn().trim())) {
            return "";
        }
        String u = n.getNotiUrl() != null ? n.getNotiUrl().trim() : "";
        return u;
    }

    public Optional<MerchantPgBinding> findOperationalElementPayBinding(Long orgUnitId) {
        if (orgUnitId == null) {
            return Optional.empty();
        }
        List<MerchantPgBinding> list = merchantPgBindingRepository.findByOrgUnitIdOrderBySortOrderAsc(orgUnitId);
        return list.stream()
                .filter(b -> PgVendor.isElementPayFamily(b.getPgCd()))
                .filter(b -> !"N".equalsIgnoreCase(trim(b.getActivationYn())))
                .filter(b -> "Y".equalsIgnoreCase(trim(b.getOperationalYn())))
                .filter(b -> {
                    String pm = trim(b.getPayMethod());
                    return pm.isEmpty() || "WEB".equalsIgnoreCase(pm) || "APM".equalsIgnoreCase(pm);
                })
                .findFirst();
    }

    private static List<Map<String, String>> parseAttributes(JsonNode attrs) {
        List<Map<String, String>> list = new ArrayList<>();
        if (attrs == null || !attrs.isArray()) {
            return list;
        }
        for (JsonNode n : attrs) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("key", firstNonBlank(n.path("key").asText(""), n.path("alias").asText("")));
            m.put("value", n.path("value").asText(""));
            m.put("name", n.path("name").asText(""));
            list.add(m);
        }
        return list;
    }

    private static String findAttribute(List<Map<String, String>> attrs, String key) {
        if (attrs == null || key == null) {
            return null;
        }
        for (Map<String, String> a : attrs) {
            if (key.equalsIgnoreCase(a.get("key"))) {
                return a.get("value");
            }
        }
        return null;
    }

    private static String firstAttr(List<Map<String, String>> attrs, String... keys) {
        if (keys == null) {
            return null;
        }
        for (String k : keys) {
            String v = findAttribute(attrs, k);
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    private static String resolveTxnOrigin(Map<String, Object> body) {
        String entry = str(body.get("entry"));
        if ("merchant_api".equalsIgnoreCase(entry)) {
            return "MERCHANT_API";
        }
        if ("chatbot".equalsIgnoreCase(entry)) {
            return "CHATBOT";
        }
        return "URL";
    }

    private static String resolveBase(PgAgency agency) {
        ElementPayCredentials cred = ElementPayCredentials.from(agency);
        String extraBase = extraApiBase(agency);
        if (!extraBase.isBlank()) {
            return extraBase;
        }
        /* 샌드박스면 운영 API URL이 행에 있어도 api-sbox 고정. (라이브 카드망 → REJECT BY BANK 방지) */
        if (cred.sandbox()) {
            return SANDBOX_BASE;
        }
        if (agency.getEndpointApi() != null && !agency.getEndpointApi().isBlank()) {
            return trimSlash(agency.getEndpointApi());
        }
        return LIVE_BASE;
    }

    private static List<String> extractMethodAttrKeys(JsonNode attrs) {
        List<String> keys = new ArrayList<>();
        if (attrs == null || attrs.isNull() || attrs.isMissingNode()) {
            return keys;
        }
        if (attrs.isObject()) {
            attrs.fieldNames().forEachRemaining(name -> {
                if (name != null && !name.isBlank() && !ElementPayAdditionalParameters.isReservedInitKey(name)
                        && !keys.contains(name)) {
                    keys.add(name);
                }
            });
            return keys;
        }
        if (!attrs.isArray()) {
            return keys;
        }
        for (JsonNode a : attrs) {
            String k = firstNonBlank(a.path("key").asText(""), a.path("alias").asText(""), a.path("name").asText(""));
            if (!k.isBlank() && !k.contains(" ") && !ElementPayAdditionalParameters.isReservedInitKey(k)
                    && !keys.contains(k)) {
                keys.add(k);
            }
        }
        return keys;
    }

    private static String combineBuyerPhone(Map<String, Object> body) {
        if (body == null) {
            return "";
        }
        String tel = digitsOnly(str(body.get("payTelephone")));
        if (tel.isBlank()) {
            tel = digitsOnly(str(body.get("phone")));
        }
        if (tel.isBlank()) {
            return "";
        }
        String iso = str(body.get("payCountryIsoCode2")).toUpperCase(Locale.ROOT);
        if (iso.length() != 2) {
            iso = str(body.get("payContactCountryCode")).toUpperCase(Locale.ROOT);
        }
        if (iso.length() != 2) {
            iso = str(body.get("countryIso2")).toUpperCase(Locale.ROOT);
        }
        String cc = CountryCallingCode.forIso2(iso);
        if (cc.isBlank()) {
            cc = digitsOnly(str(body.get("payContactCountryCode")));
        }
        if (cc.isBlank() && "TH".equals(iso)) {
            cc = "66";
        }
        if (!cc.isBlank() && !tel.startsWith(cc)) {
            if (tel.startsWith("0") && tel.length() > 8) {
                tel = tel.substring(1);
            }
            return cc + tel;
        }
        return tel;
    }

    private static String extraApiBase(PgAgency agency) {
        if (agency == null || agency.getCredentialsExtraJson() == null) {
            return "";
        }
        try {
            JsonNode root = new ObjectMapper().readTree(agency.getCredentialsExtraJson());
            JsonNode v = root.get("elementPayApiBase");
            return v != null && !v.isNull() ? trimSlash(v.asText("")) : "";
        } catch (Exception e) {
            return "";
        }
    }

    private static String formatAmount(BigDecimal amount) {
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static BigDecimal parseAmount(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            return new BigDecimal(raw.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private JsonNode parseEpApiJson(String method, ElementPayCredentials cred, String rawBody) throws Exception {
        JsonNode resp = objectMapper.readTree(rawBody != null && !rawBody.isBlank() ? rawBody : "{}");
        String api = cred != null ? cred.apiSecretKey() : "";
        String wh = cred != null ? cred.webhookSecretKey() : "";
        if (!ElementPayHashUtil.verifyMerchantApiResponse(api, wh, resp)) {
            log.error("ElementPay {} response hash mismatch", method);
        }
        return resp;
    }

    private JsonNode parseEpApiJsonQuiet(String method, ElementPayCredentials cred, String rawBody) {
        try {
            return parseEpApiJson(method, cred, rawBody);
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractEpErrorCode(JsonNode resp) {
        if (resp == null || resp.isMissingNode() || resp.isNull()) {
            return "";
        }
        JsonNode err = resp.get("error");
        if (err == null || err.isNull() || err.isMissingNode() || err.isTextual() || err.isNumber()) {
            return "";
        }
        JsonNode code = err.get("code");
        if (code == null || code.isNull() || code.isMissingNode()) {
            return "";
        }
        return code.asText("").trim();
    }

    private static boolean isRetryableInitError(String raw) {
        if (isMissingBuyerAttribute(raw)) {
            return true;
        }
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String l = raw.toLowerCase(Locale.ROOT);
        return l.contains("wrong signature") || l.contains("invalid signature") || l.contains("invalid hash");
    }

    private static boolean isMissingBuyerAttribute(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        String key = checkoutMessageKey(raw);
        if ("BUYER_REQUIRED".equals(key) || "ELEMENTPAY_MISSING_ATTRIBUTE".equals(key)) {
            return true;
        }
        String l = raw.toLowerCase(Locale.ROOT);
        return l.contains("missed required") || l.contains("required attribute");
    }

    private static boolean isMerchantCommissionUnset(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        return raw.toLowerCase(Locale.ROOT).contains("no commission has been set");
    }

    /** EP error 가 객체·문자열 어느 쪽이든 메시지 추출 */
    private static String extractEpErrorMessage(JsonNode resp) {
        if (resp == null || resp.isMissingNode() || resp.isNull()) {
            return "";
        }
        JsonNode err = resp.get("error");
        if (err != null && !err.isNull() && !err.isMissingNode()) {
            if (err.isTextual() || err.isNumber()) {
                return err.asText("").trim();
            }
            String m = firstNonBlank(
                    err.path("message").asText(""),
                    err.path("msg").asText(""),
                    err.path("error").asText(""),
                    err.path("description").asText(""));
            if (!m.isBlank()) {
                return m.trim();
            }
            return err.toString();
        }
        return firstNonBlank(resp.path("message").asText(""), resp.path("msg").asText("")).trim();
    }

    private static String checkoutMessageKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (u.startsWith("ELEMENTPAY_") || u.startsWith("BUYER_")) {
            return raw.trim();
        }
        String l = raw.toLowerCase(Locale.ROOT);
        if (l.contains("missed required") || l.contains("required attribute")) {
            return "BUYER_REQUIRED";
        }
        if (l.contains("no commission has been set")) {
            return "CHECKOUT_CARD_PATH_FAILED";
        }
        if (l.contains("rejected by bank") || l.contains("bank reject")) {
            return "ELEMENTPAY_REJECTED_BY_BANK";
        }
        if (l.contains("wrong_payer") || l.contains("wrong payer")) {
            return "ELEMENTPAY_WRONG_PAYER";
        }
        if (l.contains("reversed")) {
            return "ELEMENTPAY_PAYMENT_REVERSED";
        }
        if (l.contains("refund")) {
            return "ELEMENTPAY_PAYMENT_REFUNDED";
        }
        if (l.contains("reject")) {
            return "ELEMENTPAY_PAYMENT_REJECTED";
        }
        return null;
    }

    private static Map<String, Object> fail(String message, String code) {
        if (isMissingBuyerAttribute(message) || isMissingBuyerAttribute(code)) {
            return CheckoutFailI18n.cardPathFailed();
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", false);
        out.put("message", message);
        out.put("errorCode", code);
        return out;
    }

    private static String str(Object o) {
        return o != null ? o.toString().trim() : "";
    }

    private static String trim(String s) {
        return s != null ? s.trim() : "";
    }

    private static String trimSlash(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().replaceAll("/+$", "");
    }

    private static String urlEnc(String s) {
        try {
            return java.net.URLEncoder.encode(s != null ? s : "", StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s != null ? s : "";
        }
    }
}
