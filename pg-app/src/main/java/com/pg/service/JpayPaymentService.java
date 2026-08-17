package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.HqApiConfig;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgAgency;
import com.pg.integration.pg.PgVendor;
import com.pg.middleware.notify.PgNotifyIngressPaths;
import com.pg.entity.MerchantNotifyUrl;
import com.pg.entity.MerchantJpaySubscription;
import com.pg.repository.MerchantJpaySubscriptionRepository;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.MerchantNotifyUrlRepository;
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.entity.PgTrnsctn;
import com.pg.util.JpayCheckoutMinAmountUtil;
import com.pg.util.JpayOrderDuplicateUtil;
import com.pg.util.JpayPayIndexResponseParser;
import com.pg.util.JpaySignatureUtil;
import com.pg.util.MerchantPgCredentialUtil;
import com.pg.util.NotifyToTxnStatusMerge;
import com.pg.util.PayPresaleRiskFilterCodes;
import com.pg.util.PgNotifyInternalStatusMapper;
import com.pg.util.PgOutboundUrlPolicy;
import com.pg.util.PgTrnsctnOrderLookup;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * JPAY 샌드박스·운영 {@code pay_index} 직접 호출(서버 사이드).
 * {@code pay_notifyurl}·{@code pay_callbackurl} 은 가맹 {@code tb_merchant_notify_url}(JPAY_NOTIFY/JPAY_CALLBACK)
 * — 노티미들웨어 등 외부 주소 포함 — 을 그대로 사용하고, 없으면 ICOPAY 노티 ingress(cbJpay/rsJpay) 기본값입니다.
 */
@Service
public class JpayPaymentService {

    private static final Logger log = LoggerFactory.getLogger(JpayPaymentService.class);
    private static final ObjectMapper OM = new ObjectMapper();
    private static final DateTimeFormatter APPLY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DEFAULT_SANDBOX_PAY_INDEX = "https://sandbox.j-pay.net/pay_index";
    /** 운영 API 호스트 — {@code www.j-pay.net/pay_index} 는 404(마케팅 사이트)이므로 {@code api.j-pay.net} 사용 */
    private static final String DEFAULT_LIVE_PAY_INDEX = "https://api.j-pay.net/pay_index";
    private static final String DEFAULT_BANK_CODE = "901";

    private final MerchantPgBindingRepository merchantPgBindingRepository;
    private final PgAgencyRepository pgAgencyRepository;
    private final OrgServiceUseService orgServiceUseService;
    private final HqNotifyEnvService hqNotifyEnvService;
    private final HqApiConfigRepository hqApiConfigRepository;
    private final JpaySaleRecordService jpaySaleRecordService;
    private final OrgUnitRepository orgUnitRepository;
    private final MerchantNotifyUrlRepository merchantNotifyUrlRepository;
    private final UrlPayCheckoutCurrencyService urlPayCheckoutCurrencyService;
    private final JpaySubscriptionConfigService jpaySubscriptionConfigService;
    private final MerchantJpaySubscriptionRepository merchantJpaySubscriptionRepository;
    private final PayCardPolicyService payCardPolicyService;
    private final PayPresaleRiskFilterService payPresaleRiskFilterService;
    private final JpayTradeApiService jpayTradeApiService;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final RestTemplate restTemplate = createJpayRestTemplate();

    public JpayPaymentService(MerchantPgBindingRepository merchantPgBindingRepository,
                              PgAgencyRepository pgAgencyRepository,
                              OrgServiceUseService orgServiceUseService,
                              HqNotifyEnvService hqNotifyEnvService,
                              HqApiConfigRepository hqApiConfigRepository,
                              JpaySaleRecordService jpaySaleRecordService,
                              OrgUnitRepository orgUnitRepository,
                              MerchantNotifyUrlRepository merchantNotifyUrlRepository,
                              UrlPayCheckoutCurrencyService urlPayCheckoutCurrencyService,
                              JpaySubscriptionConfigService jpaySubscriptionConfigService,
                              MerchantJpaySubscriptionRepository merchantJpaySubscriptionRepository,
                              PayCardPolicyService payCardPolicyService,
                              PayPresaleRiskFilterService payPresaleRiskFilterService,
                              JpayTradeApiService jpayTradeApiService,
                              PgTrnsctnRepository pgTrnsctnRepository) {
        this.merchantPgBindingRepository = merchantPgBindingRepository;
        this.pgAgencyRepository = pgAgencyRepository;
        this.orgServiceUseService = orgServiceUseService;
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.jpaySaleRecordService = jpaySaleRecordService;
        this.orgUnitRepository = orgUnitRepository;
        this.merchantNotifyUrlRepository = merchantNotifyUrlRepository;
        this.urlPayCheckoutCurrencyService = urlPayCheckoutCurrencyService;
        this.jpaySubscriptionConfigService = jpaySubscriptionConfigService;
        this.merchantJpaySubscriptionRepository = merchantJpaySubscriptionRepository;
        this.payCardPolicyService = payCardPolicyService;
        this.payPresaleRiskFilterService = payPresaleRiskFilterService;
        this.jpayTradeApiService = jpayTradeApiService;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
    }

    /**
     * @return success, status(0/1/2), msg, redirectUrl?, orderNo, memberId(마스킹), rawResponse
     */
    public Map<String, Object> executeDirectSale(Long orgUnitId,
                                                 Map<String, Object> body,
                                                 HttpServletRequest req,
                                                 String clientIp) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (orgUnitId == null) {
            return failOut("가맹점을 찾을 수 없습니다.", "NOT_FOUND");
        }
        if (!orgServiceUseService.isOrgServiceActive(orgUnitId)) {
            return failOut(OrgServiceUseService.MSG_ORG_SERVICE_DISABLED, "ORG_DISABLED");
        }
        Optional<MerchantPgBinding> bindOpt = findOperationalJpayWebBinding(orgUnitId);
        if (bindOpt.isEmpty()) {
            return failOut("JPAY URL 결제(운영) 바인딩이 없습니다. 결제대행사에 JPAY·URL결제를 등록하세요.", "URL_PAYMENT_PG_MISSING");
        }
        MerchantPgBinding binding = bindOpt.get();
        Optional<PgAgency> agOpt = pgAgencyRepository.findByPgCd(binding.getPgCd() != null ? binding.getPgCd().trim() : "");
        if (agOpt.isEmpty()) {
            return failOut("PG사 연동(tb_pg_agency) 행을 찾을 수 없습니다.", "PG_AGENCY_MISSING");
        }
        PgAgency agency = agOpt.get();
        MerchantPgCredentialUtil.Resolved cred = MerchantPgCredentialUtil.resolve(binding, agency);
        String mid = cred.mid();
        String apiKey = cred.apiKey();
        if (mid.isEmpty() || apiKey.isEmpty()) {
            return failOut("JPAY MID·API Key를 설정하세요. (가맹 MID+Key 쌍 또는 본사 PG 연동)", "JPAY_CREDENTIALS_MISSING");
        }
        int routeNo = parseRouteNo(binding.getRootNo());

        String orderNo = str(body.get("orderNo"));
        if (orderNo.isBlank()) {
            return failOut("orderNo가 필요합니다.", "INVALID_ORDER_NO");
        }
        if (orderNo.length() > 64) {
            orderNo = orderNo.substring(0, 64);
        }
        String countryIso2 = com.pg.urlpay.JpayBuyerPrefillUtil.canonicalCountryIso2(str(body.get("payCountryIsoCode2")));
        if (countryIso2.length() != 2) {
            return failOut("payCountryIsoCode2(국가코드 ISO2)가 필요합니다.", "JPAY_COUNTRY_CODE_REQUIRED");
        }
        body.put("payCountryIsoCode2", countryIso2);
        String localPhone = normalizeLocalTelephone(str(body.get("payTelephone")));
        if (localPhone.isBlank()) {
            return failOut("payTelephone(전화번호)가 필요합니다.", "JPAY_PHONE_REQUIRED");
        }
        body.put("payTelephone", localPhone);
        // 이메일은 규격상 필수(M). 프런트에서도 강제하지만, 직접 API 호출 시 JPAY 원시 실패 대신 명확히 거부한다.
        String payEmail = str(body.get("payEmailAddress"));
        if (payEmail.isBlank()) {
            return failOut("payEmailAddress(이메일)가 필요합니다.", "JPAY_EMAIL_REQUIRED");
        }
        BigDecimal amountBd = parseAmount(body.get("amount"));
        if (amountBd == null || amountBd.compareTo(BigDecimal.ZERO) <= 0) {
            return failOut("amount는 0보다 커야 합니다.", "INVALID_AMOUNT");
        }
        String currency = urlPayCheckoutCurrencyService.resolveCheckoutCurrency(orgUnitId, str(body.get("currency")));
        Optional<Map<String, Object>> minDeny = JpayCheckoutMinAmountUtil.validate(amountBd, currency);
        if (minDeny.isPresent()) {
            return minDeny.get();
        }
        com.pg.urlpay.PayerContextCapture.enrichSaleBody(body, req, clientIp);
        String txnOrigin = resolveTxnOrigin(str(body.get("txnOrigin")));
        BigDecimal shopperDisplayAmt = parseAmount(body.get("shopperDisplayAmount"));
        String shopperDisplayCur = str(body.get("shopperDisplayCurrency"));
        Optional<OrgUnit> ouForRecord = orgUnitRepository.findById(orgUnitId);
        String merchantCode = ouForRecord.map(OrgUnit::getCode).orElse("");
        Map<String, Object> cardVal = validateCardPolicyForDirectSale(orgUnitId, body);
        if (!Boolean.TRUE.equals(cardVal.get("valid"))) {
            return cardPolicyBlockOut(cardVal, merchantCode, orderNo, txnOrigin, orgUnitId, amountBd, currency,
                    routeNo, body, shopperDisplayAmt, shopperDisplayCur);
        }

        Optional<PayPresaleRiskFilterService.PresaleRiskBlock> presaleRisk =
                payPresaleRiskFilterService.evaluate(orgUnitId, merchantCode, PgVendor.JPAY, body);
        if (presaleRisk.isPresent()) {
            return presaleRiskBlockOut(presaleRisk.get(), orgUnitId, merchantCode, orderNo, txnOrigin, amountBd,
                    currency, routeNo, body, shopperDisplayAmt, shopperDisplayCur);
        }

        Optional<Map<String, Object>> preSaleGuard = guardBeforePayIndex(orgUnitId, merchantCode, orderNo);
        if (preSaleGuard.isPresent()) {
            return preSaleGuard.get();
        }

        jpaySaleRecordService.recordOrTouchPending(orgUnitId, orderNo, amountBd, currency, routeNo,
                str(body.get("payEmailAddress")),
                str(body.get("item")),
                txnOrigin,
                shopperDisplayAmt,
                shopperDisplayCur,
                body);

        String payIndexUrl = resolvePayIndexUrl(agency);
        String bankCode = resolveBankCode(agency);
        String notifyTarget = resolveExtraStr(agency, "jpayNotifyTarget", "cbJpay");
        String resultTarget = resolveExtraStr(agency, "jpayResultTarget", "rsJpay");
        String publicBase = resolvePublicApiBase(req);
        if (publicBase.isBlank()) {
            return failOut("공개 API 베이스 URL이 없습니다. 배포설정에 publicApiBaseUrl 또는 노티 publicBaseUrl을 넣으세요.", "PUBLIC_API_BASE_MISSING");
        }
        String token = hqNotifyEnvService.getOrCreate().getIngressToken();
        String notifyPathPrefix = resolveJpayNotifyPathPrefix(agency);
        String defaultNotifyUrl = publicBase + notifyPathPrefix + token + "/" + notifyTarget;
        String defaultCallbackUrl = publicBase + notifyPathPrefix + token + "/" + resultTarget;
        String notifyUrl = resolveMerchantJpayNotifyUrl(orgUnitId, defaultNotifyUrl);
        String callbackUrl = resolveMerchantJpayCallbackUrl(orgUnitId, defaultCallbackUrl);

        // 가맹 개인정보 보호: PG(pay_url)에는 가맹 몰 도메인을 절대 넣지 않는다. 우리 도메인이 아니면 publicBase 로 강제.
        String siteUrl = PgOutboundUrlPolicy.enforceOwnDomain(str(body.get("payUrl")), publicBase, publicBase);

        String compCode = str(body.get("compId"));
        String attach = compCode.isBlank() ? "" : "icopayCompId=" + compCode.trim();

        String applyDate = LocalDateTime.now().format(APPLY_FMT);

        TreeMap<String, String> signParams = new TreeMap<>();
        signParams.put("pay_memberid", mid);
        signParams.put("pay_orderid", orderNo);
        signParams.put("pay_applydate", applyDate);
        signParams.put("pay_bankcode", bankCode);
        signParams.put("pay_notifyurl", notifyUrl);
        signParams.put("pay_callbackurl", callbackUrl);
        signParams.put("pay_amount", amountBd.stripTrailingZeros().toPlainString());

        String md5sign = JpaySignatureUtil.signRequestParams(signParams, apiKey);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        for (Map.Entry<String, String> e : signParams.entrySet()) {
            form.add(e.getKey(), e.getValue());
        }
        form.add("pay_md5sign", md5sign);
        form.add("pay_currency", currency);
        form.add("pay_url", siteUrl);
        if (!attach.isBlank()) {
            form.add("attach", attach);
        }

        addIfPresent(form, body, "pay_cardno", "payCardno");
        addIfPresent(form, body, "pay_cardmonth", "payCardmonth");
        addIfPresent(form, body, "pay_cardyear", "payCardyear");
        addIfPresent(form, body, "pay_cardcvv", "payCardcvv");
        addIfPresent(form, body, "pay_firstname", "payFirstname");
        addIfPresent(form, body, "pay_lastname", "payLastname");
        addIfPresent(form, body, "pay_street_address1", "payStreetAddress1");
        addIfPresent(form, body, "pay_street_address2", "payStreetAddress2");
        addIfPresent(form, body, "pay_city", "payCity");
        addIfPresent(form, body, "pay_postcode", "payPostcode");
        addIfPresent(form, body, "pay_state", "payState");
        addIfPresent(form, body, "pay_country_iso_code_2", "payCountryIsoCode2");
        addIfPresent(form, body, "pay_email_address", "payEmailAddress");
        addTelephoneIfPresent(form, body);
        addIfPresent(form, body, "pay_language", "payLanguage");
        addIfPresent(form, body, "system", "system");

        addIfPresent(form, body, "shipping_firstname", "shippingFirstname");
        addIfPresent(form, body, "shipping_lastname", "shippingLastname");
        addIfPresent(form, body, "shipping_street_address1", "shippingStreetAddress1");
        addIfPresent(form, body, "shipping_street_address2", "shippingStreetAddress2");
        addIfPresent(form, body, "shipping_city", "shippingCity");
        addIfPresent(form, body, "shipping_state", "shippingState");
        addIfPresent(form, body, "shipping_postcode", "shippingPostcode");
        addIfPresent(form, body, "shipping_country_iso_code_2", "shippingCountryIsoCode2");
        addIfPresent(form, body, "shipping_telephone", "shippingTelephone");

        applyJpaySaleDefaults(form, body);

        String productJson = str(body.get("payProductname"));
        if (productJson.isBlank()) {
            String item = str(body.get("item"));
            if (item.isBlank()) {
                item = "Order " + orderNo;
            }
            productJson = defaultProductJson(item, amountBd.stripTrailingZeros().toPlainString());
        }
        form.add("pay_productname", productJson);

        String ip = clientIp != null && !clientIp.isBlank() ? clientIp : "127.0.0.1";
        form.add("pay_ip", ip);
        String ua = req != null ? req.getHeader("User-Agent") : "";
        if (ua != null && !ua.isBlank()) {
            form.add("pay_useragent", ua.length() > 512 ? ua.substring(0, 512) : ua);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
        String raw;
        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(payIndexUrl, entity, String.class);
            raw = resp.getBody() != null ? resp.getBody() : "";
        } catch (Exception e) {
            log.warn("JPAY pay_index 호출 실패: {}", e.getMessage());
            out.put("success", false);
            out.put("message", "JPAY 연동 호출 실패: " + e.getMessage());
            return out;
        }

        JpayPayIndexResponseParser.Outcome parsed;
        int status = -1;
        String msg = "";
        String url3ds = "";
        try {
            parsed = JpayPayIndexResponseParser.parse(raw);
            status = parsed.status();
            msg = parsed.msg();
            url3ds = parsed.url3ds();
            if (status == 2 && msg.contains("empty response")) {
                log.warn("JPAY pay_index 빈 응답 orderNo={} payIndexUrl={}", orderNo, payIndexUrl);
            } else if (status == 2 && parsed.rawJsonUsed() != null && parsed.rawJsonUsed().contains("status")) {
                log.warn("JPAY pay_index 실패 orderNo={} status={} msg={}", orderNo, status, msg);
            } else if (status == 1) {
                log.info("JPAY pay_index 3DS orderNo={} txnId={} url={}", orderNo, parsed.transactionId(), url3ds);
            }
        } catch (Exception e) {
            log.warn("JPAY pay_index 응답 파싱 실패 orderNo={} payIndexUrl={} err={} raw={}",
                    orderNo, payIndexUrl, e.getMessage(), truncateRaw(raw));
            out.put("success", false);
            out.put("message", "JPAY 응답 파싱 실패: " + truncateRaw(raw));
            out.put("rawResponse", raw);
            return out;
        }

        /* compId for sync outcome — resolve from org */
        String midCode = resolveMerchantCode(orgUnitId);

        String jpayTxnId = parsed.transactionId();
        if (jpayTxnId != null && !jpayTxnId.isBlank()) {
            jpaySaleRecordService.applyJpayTransactionId(midCode, orderNo, jpayTxnId,
                    resolveTxnOrigin(str(body.get("txnOrigin"))));
        }
        if (status == 0 || status == 2) {
            jpaySaleRecordService.applySyncApiOutcome(midCode, orderNo, status, msg,
                    resolveTxnOrigin(str(body.get("txnOrigin"))), jpayTxnId,
                    str(body.get("payCardno")));
        }
        if (status == 2 && JpayOrderDuplicateUtil.isDuplicateOrderMessage(msg)) {
            return JpayOrderDuplicateUtil.orderDupFailPayload(orderNo);
        }

        out.put("success", true);
        out.put("status", status);
        out.put("msg", msg);
        if (status == 1 && url3ds != null && !url3ds.isBlank()) {
            out.put("redirectUrl", url3ds);
        }
        putJpayPayIndexExtras(out, parsed);
        out.put("orderNo", orderNo);
        out.put("payIndexUrl", payIndexUrl);
        out.put("rawResponse", raw);
        return out;
    }

    /** 가맹점 API JPAY 구독(운영) 바인딩 존재 여부 */
    public boolean hasOperationalSubscriptionBinding(Long orgUnitId) {
        return jpaySubscriptionConfigService.findOperationalSubscriptionBinding(orgUnitId).isPresent();
    }

    /**
     * JPAY 구독 Sale — {@code pay_type=Subscription}, {@code subscription_plan} JSON.
     */
    public Map<String, Object> executeSubscriptionSale(Long orgUnitId,
                                                       Map<String, Object> body,
                                                       HttpServletRequest req,
                                                       String clientIp,
                                                       String subscriptionPlanJson) {
        if (subscriptionPlanJson == null || subscriptionPlanJson.isBlank()) {
            return failOut("subscriptionPlan이 필요합니다.", "SUBSCRIPTION_PLAN_REQUIRED");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        if (orgUnitId == null) {
            return failOut("가맹점을 찾을 수 없습니다.", "NOT_FOUND");
        }
        if (!orgServiceUseService.isOrgServiceActive(orgUnitId)) {
            return failOut(OrgServiceUseService.MSG_ORG_SERVICE_DISABLED, "ORG_DISABLED");
        }
        Optional<MerchantPgBinding> bindOpt = jpaySubscriptionConfigService.findOperationalSubscriptionBinding(orgUnitId);
        if (bindOpt.isEmpty()) {
            return failOut("JPAY API 구독(운영) 바인딩이 없습니다.", "SUBSCRIPTION_PG_MISSING");
        }
        MerchantPgBinding binding = bindOpt.get();
        Optional<PgAgency> agOpt = pgAgencyRepository.findByPgCd(binding.getPgCd() != null ? binding.getPgCd().trim() : "");
        if (agOpt.isEmpty()) {
            return failOut("PG사 연동(tb_pg_agency) 행을 찾을 수 없습니다.", "PG_AGENCY_MISSING");
        }
        PgAgency agency = agOpt.get();
        MerchantPgCredentialUtil.Resolved cred = MerchantPgCredentialUtil.resolve(binding, agency);
        String mid = cred.mid();
        String apiKey = cred.apiKey();
        if (mid.isEmpty() || apiKey.isEmpty()) {
            return failOut("JPAY MID·API Key를 설정하세요. (가맹 MID+Key 쌍 또는 본사 PG 연동)", "JPAY_CREDENTIALS_MISSING");
        }
        int routeNo = parseRouteNo(binding.getRootNo());

        String orderNo = str(body.get("orderNo"));
        if (orderNo.isBlank()) {
            return failOut("orderNo가 필요합니다.", "INVALID_ORDER_NO");
        }
        if (orderNo.length() > 64) {
            orderNo = orderNo.substring(0, 64);
        }
        BigDecimal amountBd = parseAmount(body.get("amount"));
        if (amountBd == null || amountBd.compareTo(BigDecimal.ZERO) <= 0) {
            return failOut("amount는 0보다 커야 합니다.", "INVALID_AMOUNT");
        }
        String currency = urlPayCheckoutCurrencyService.resolveCheckoutCurrency(orgUnitId, str(body.get("currency")));
        Map<String, Object> cardVal = validateCardPolicyForDirectSale(orgUnitId, body);
        if (!Boolean.TRUE.equals(cardVal.get("valid"))) {
            String msg = cardVal.get("message") != null ? cardVal.get("message").toString() : "카드번호를 확인해 주세요.";
            String code = cardVal.get("errorCode") != null ? cardVal.get("errorCode").toString() : "CARD_POLICY";
            return cardPolicyFailOut(cardVal, msg, code);
        }

        String payIndexUrl = resolvePayIndexUrl(agency);
        String bankCode = resolveBankCode(agency);
        String notifyTarget = resolveExtraStr(agency, "jpayNotifyTarget", "cbJpay");
        String resultTarget = resolveExtraStr(agency, "jpayResultTarget", "rsJpay");
        String publicBase = resolvePublicApiBase(req);
        if (publicBase.isBlank()) {
            return failOut("공개 API 베이스 URL이 없습니다.", "PUBLIC_API_BASE_MISSING");
        }
        String token = hqNotifyEnvService.getOrCreate().getIngressToken();
        String notifyPathPrefix = resolveJpayNotifyPathPrefix(agency);
        String defaultNotifyUrl = publicBase + notifyPathPrefix + token + "/" + notifyTarget;
        String defaultCallbackUrl = publicBase + notifyPathPrefix + token + "/" + resultTarget;
        String notifyUrl = resolveMerchantJpayNotifyUrl(orgUnitId, defaultNotifyUrl);
        String callbackUrl = resolveMerchantJpayCallbackUrl(orgUnitId, defaultCallbackUrl);

        // 가맹 개인정보 보호: PG(pay_url)에는 가맹 몰 도메인을 절대 넣지 않는다. 우리 도메인이 아니면 publicBase 로 강제.
        String siteUrl = PgOutboundUrlPolicy.enforceOwnDomain(str(body.get("payUrl")), publicBase, publicBase);
        String compCode = str(body.get("compId"));
        String attach = compCode.isBlank() ? "" : "icopayCompId=" + compCode.trim();

        String applyDate = LocalDateTime.now().format(APPLY_FMT);
        TreeMap<String, String> signParams = new TreeMap<>();
        signParams.put("pay_memberid", mid);
        signParams.put("pay_orderid", orderNo);
        signParams.put("pay_applydate", applyDate);
        signParams.put("pay_bankcode", bankCode);
        signParams.put("pay_notifyurl", notifyUrl);
        signParams.put("pay_callbackurl", callbackUrl);
        signParams.put("pay_amount", amountBd.stripTrailingZeros().toPlainString());

        String md5sign = JpaySignatureUtil.signRequestParams(signParams, apiKey);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        for (Map.Entry<String, String> e : signParams.entrySet()) {
            form.add(e.getKey(), e.getValue());
        }
        form.add("pay_md5sign", md5sign);
        form.add("pay_currency", currency);
        form.add("pay_url", siteUrl);
        form.add("pay_type", "Subscription");
        form.add("subscription_plan", subscriptionPlanJson);
        if (!attach.isBlank()) {
            form.add("attach", attach);
        }

        addIfPresent(form, body, "pay_cardno", "payCardno");
        addIfPresent(form, body, "pay_cardmonth", "payCardmonth");
        addIfPresent(form, body, "pay_cardyear", "payCardyear");
        addIfPresent(form, body, "pay_cardcvv", "payCardcvv");
        addIfPresent(form, body, "pay_firstname", "payFirstname");
        addIfPresent(form, body, "pay_lastname", "payLastname");
        addIfPresent(form, body, "pay_street_address1", "payStreetAddress1");
        addIfPresent(form, body, "pay_street_address2", "payStreetAddress2");
        addIfPresent(form, body, "pay_city", "payCity");
        addIfPresent(form, body, "pay_postcode", "payPostcode");
        addIfPresent(form, body, "pay_state", "payState");
        addIfPresent(form, body, "pay_country_iso_code_2", "payCountryIsoCode2");
        addIfPresent(form, body, "pay_email_address", "payEmailAddress");
        addTelephoneIfPresent(form, body);
        addIfPresent(form, body, "pay_language", "payLanguage");
        addIfPresent(form, body, "system", "system");
        applyJpaySaleDefaults(form, body);

        String productJson = str(body.get("payProductname"));
        if (productJson.isBlank()) {
            String item = str(body.get("item"));
            if (item.isBlank()) {
                item = "Subscription " + orderNo;
            }
            productJson = defaultProductJson(item, amountBd.stripTrailingZeros().toPlainString());
        }
        form.add("pay_productname", productJson);

        String ip = clientIp != null && !clientIp.isBlank() ? clientIp : "127.0.0.1";
        form.add("pay_ip", ip);
        String ua = req != null ? req.getHeader("User-Agent") : "";
        if (ua != null && !ua.isBlank()) {
            form.add("pay_useragent", ua.length() > 512 ? ua.substring(0, 512) : ua);
        }

        com.pg.urlpay.PayerContextCapture.enrichSaleBody(body, req, clientIp);
        String merchantCodeSub = resolveMerchantCode(orgUnitId);
        Optional<PayPresaleRiskFilterService.PresaleRiskBlock> presaleRiskSub =
                payPresaleRiskFilterService.evaluate(orgUnitId, merchantCodeSub, PgVendor.JPAY, body);
        if (presaleRiskSub.isPresent()) {
            return presaleRiskBlockOut(presaleRiskSub.get(), orgUnitId, merchantCodeSub, orderNo, "SUBSCRIPTION",
                    amountBd, currency, routeNo, body, null, null);
        }
        jpaySaleRecordService.recordOrTouchPending(orgUnitId, orderNo, amountBd, currency, routeNo,
                str(body.get("payEmailAddress")),
                str(body.get("item")),
                "SUBSCRIPTION",
                null,
                null,
                body);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
        String raw;
        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(payIndexUrl, entity, String.class);
            raw = resp.getBody() != null ? resp.getBody() : "";
        } catch (Exception e) {
            log.warn("JPAY subscription pay_index 호출 실패: {}", e.getMessage());
            out.put("success", false);
            out.put("message", "JPAY 구독 연동 호출 실패: " + e.getMessage());
            return out;
        }

        JpayPayIndexResponseParser.Outcome parsed;
        int status = -1;
        String msg = "";
        String url3ds = "";
        try {
            parsed = JpayPayIndexResponseParser.parse(raw);
            status = parsed.status();
            msg = parsed.msg();
            url3ds = parsed.url3ds();
        } catch (Exception e) {
            log.warn("JPAY subscription pay_index 응답 파싱 실패 orderNo={} err={}", orderNo, e.getMessage());
            out.put("success", false);
            out.put("message", "JPAY 응답 파싱 실패: " + truncateRaw(raw));
            out.put("rawResponse", raw);
            return out;
        }

        String midCode = resolveMerchantCode(orgUnitId);
        String jpaySubTxnId = parsed.transactionId();
        if (jpaySubTxnId != null && !jpaySubTxnId.isBlank()) {
            jpaySaleRecordService.applyJpayTransactionId(midCode, orderNo, jpaySubTxnId, "SUBSCRIPTION");
        }
        if (status == 0 || status == 2) {
            jpaySaleRecordService.applySyncApiOutcome(midCode, orderNo, status, msg, "SUBSCRIPTION", jpaySubTxnId,
                    str(body.get("payCardno")));
        }

        out.put("success", true);
        out.put("status", status);
        out.put("msg", msg);
        if (status == 1 && url3ds != null && !url3ds.isBlank()) {
            out.put("redirectUrl", url3ds);
        }
        putJpayPayIndexExtras(out, parsed);
        out.put("orderNo", orderNo);
        out.put("checkoutKind", "SUBSCRIPTION");
        out.put("payIndexUrl", payIndexUrl);
        out.put("rawResponse", raw);
        return out;
    }

    /** JPAY 구독 해지 — 최초 {@code pay_orderid} 기준. */
    public Map<String, Object> executeSubscriptionCancel(Long orgUnitId, String orderNo, HttpServletRequest req) {
        if (orgUnitId == null || orderNo == null || orderNo.isBlank()) {
            return failOut("orderNo가 필요합니다.", "INVALID_ORDER_NO");
        }
        Optional<MerchantPgBinding> bindOpt = jpaySubscriptionConfigService.findOperationalSubscriptionBinding(orgUnitId);
        if (bindOpt.isEmpty()) {
            return failOut("JPAY API 구독(운영) 바인딩이 없습니다.", "SUBSCRIPTION_PG_MISSING");
        }
        MerchantPgBinding binding = bindOpt.get();
        Optional<PgAgency> agOpt = pgAgencyRepository.findByPgCd(binding.getPgCd() != null ? binding.getPgCd().trim() : "");
        if (agOpt.isEmpty()) {
            return failOut("PG사 연동 행을 찾을 수 없습니다.", "PG_AGENCY_MISSING");
        }
        PgAgency agency = agOpt.get();
        MerchantPgCredentialUtil.Resolved cred = MerchantPgCredentialUtil.resolve(binding, agency);
        String mid = cred.mid();
        String apiKey = cred.apiKey();
        if (mid.isEmpty() || apiKey.isEmpty()) {
            return failOut("JPAY MID·API Key를 설정하세요. (가맹 MID+Key 쌍 또는 본사 PG 연동)", "JPAY_CREDENTIALS_MISSING");
        }
        String on = orderNo.trim();
        if (on.length() > 64) {
            on = on.substring(0, 64);
        }
        TreeMap<String, String> signParams = new TreeMap<>();
        signParams.put("pay_memberid", mid);
        signParams.put("pay_orderid", on);
        String md5sign = JpaySignatureUtil.signRequestParams(signParams, apiKey);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("pay_memberid", mid);
        form.add("pay_orderid", on);
        form.add("pay_md5sign", md5sign);

        String cancelUrl = resolveSubscriptionCancelUrl(agency);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
        String raw;
        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(cancelUrl, entity, String.class);
            raw = resp.getBody() != null ? resp.getBody() : "";
        } catch (Exception e) {
            log.warn("JPAY subscription cancel 호출 실패: {}", e.getMessage());
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("success", false);
            out.put("message", "JPAY 구독 해지 호출 실패: " + e.getMessage());
            return out;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("rawResponse", raw);
        try {
            JsonNode n = OM.readTree(raw.trim().startsWith("{") ? raw : "{}");
            out.put("status", n.path("status").asText(""));
            out.put("msg", n.path("msg").asText(""));
            out.put("paymentTransactionId", n.path("payment_transaction_id").asText(""));
        } catch (Exception ignored) {
            /* keep raw */
        }
        out.put("orderNo", on);
        out.put("cancelUrl", cancelUrl);
        return out;
    }

    private String resolveSubscriptionCancelUrl(PgAgency agency) {
        String fromJson = resolveExtraStr(agency, "jpaySubscriptionCancelUrl", "");
        if (!fromJson.isBlank()) {
            return fromJson.trim();
        }
        String payIndex = resolvePayIndexUrl(agency);
        if (payIndex.contains("/pay_index")) {
            return payIndex.replace("/pay_index", "/pay_index/subscriptioncancel");
        }
        return payIndex + "/subscriptioncancel";
    }

    /** 가맹점 API 인라인 결제(jpay-pay.html) 등 — JPAY URL 결제 운영 바인딩 존재 여부 */
    public boolean hasOperationalWebBinding(Long orgUnitId) {
        return findOperationalJpayWebBinding(orgUnitId).isPresent();
    }

    /** 운영 JPAY URL 바인딩의 {@code pg_cd} (결제통화 스케일·표시 연동용). */
    public Optional<String> resolveOperationalPgCd(Long orgUnitId) {
        return findOperationalJpayWebBinding(orgUnitId)
                .map(b -> b.getPgCd() != null ? b.getPgCd().trim() : "")
                .filter(s -> !s.isEmpty());
    }

    private static Map<String, Object> failOut(String message, String errorCode) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", false);
        out.put("message", message);
        out.put("errorCode", errorCode);
        return out;
    }

    private Optional<Map<String, Object>> guardBeforePayIndex(Long orgUnitId, String merchantCode, String orderNo) {
        if (merchantCode != null && !merchantCode.isBlank()) {
            Optional<PgTrnsctn> local = PgTrnsctnOrderLookup.findPreferredByMerchantAndOrder(
                    pgTrnsctnRepository, merchantCode, orderNo);
            if (local.isPresent()) {
                String st = local.get().getStatus() != null ? local.get().getStatus().trim() : "";
                if ("10".equals(st) || "00".equals(st)) {
                    Map<String, Object> paid = new LinkedHashMap<>();
                    paid.put("success", true);
                    paid.put("status", 0);
                    paid.put("msg", "transaction success");
                    paid.put("orderNo", orderNo);
                    paid.put("idempotent", true);
                    return Optional.of(paid);
                }
                if ("08".equals(st)) {
                    return Optional.of(JpayOrderDuplicateUtil.orderPendingFailPayload(orderNo));
                }
                if (NotifyToTxnStatusMerge.isTerminalOutcome(st)) {
                    return Optional.of(JpayOrderDuplicateUtil.orderDupFailPayload(orderNo));
                }
            }
        }
        Optional<JpayTradeApiService.TradeQuerySnapshot> snap =
                jpayTradeApiService.tryQueryTradeForOrgUnit(orgUnitId, orderNo);
        if (snap.isEmpty()) {
            return Optional.empty();
        }
        JpayTradeApiService.TradeQuerySnapshot q = snap.get();
        String mapped = q.mappedInternalStatus();
        if (PgNotifyInternalStatusMapper.ST_PAID.equals(mapped)) {
            Map<String, Object> paid = new LinkedHashMap<>();
            paid.put("success", true);
            paid.put("status", 0);
            paid.put("msg", "transaction success");
            paid.put("orderNo", orderNo);
            paid.put("transactionId", q.transactionId());
            paid.put("idempotent", true);
            return Optional.of(paid);
        }
        if (PgNotifyInternalStatusMapper.ST_FAIL.equals(mapped)) {
            return Optional.of(JpayOrderDuplicateUtil.orderDupFailPayload(orderNo));
        }
        String ts = q.tradeState() != null ? q.tradeState().trim().toUpperCase(Locale.ROOT) : "";
        if ("UNPAID".equals(ts) || PgNotifyInternalStatusMapper.ST_CANCEL.equals(mapped)) {
            return Optional.of(JpayOrderDuplicateUtil.orderPendingFailPayload(orderNo));
        }
        return Optional.empty();
    }

    private String resolveMerchantCode(Long orgUnitId) {
        if (orgUnitId == null) {
            return "";
        }
        return orgUnitRepository.findById(orgUnitId)
                .map(o -> o.getCode() != null ? o.getCode().trim() : "")
                .orElse("");
    }

    private static String resolveTxnOrigin(String raw) {
        if (raw == null || raw.isBlank()) {
            return "URL";
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if ("MERCHANT_API".equals(u)) {
            return "MERCHANT_API";
        }
        if ("CHATBOT".equals(u)) {
            return "CHATBOT";
        }
        if ("SUBSCRIPTION".equals(u)) {
            return "SUBSCRIPTION";
        }
        return "URL";
    }

    private Optional<MerchantPgBinding> findOperationalJpayWebBinding(Long orgUnitId) {
        List<MerchantPgBinding> list = merchantPgBindingRepository.findByOrgUnitIdOrderBySortOrderAsc(orgUnitId);
        return list.stream()
                .filter(b -> b.getOperationalYn() != null && "Y".equalsIgnoreCase(b.getOperationalYn().trim()))
                .filter(b -> b.getActivationYn() == null || "Y".equalsIgnoreCase(b.getActivationYn().trim()))
                .filter(b -> b.getPgCd() != null && PgVendor.isJpayFamily(b.getPgCd()))
                .filter(b -> isAgencyUrlPayIntegration(b.getPgCd()))
                .filter(b -> {
                    String pm = b.getPayMethod();
                    return pm == null || pm.isBlank() || "WEB".equalsIgnoreCase(pm.trim());
                })
                .min(Comparator.comparingInt(b -> b.getSortOrder() != null ? b.getSortOrder() : Integer.MAX_VALUE));
    }

    /** 연동용도 URL결제({@code integ_url_pay_yn=Y}) PG만 URL·웹결제·인라인 후보. API 전용 발급은 제외. */
    private boolean isAgencyUrlPayIntegration(String pgCd) {
        if (pgCd == null || pgCd.isBlank()) {
            return false;
        }
        return pgAgencyRepository.findByPgCd(pgCd.trim())
                .filter(a -> a.getUseYn() != null && "Y".equalsIgnoreCase(a.getUseYn().trim()))
                .map(a -> "Y".equalsIgnoreCase(a.getIntegUrlPayYn() != null ? a.getIntegUrlPayYn().trim() : ""))
                .orElse(false);
    }

    private String resolvePayIndexUrl(PgAgency agency) {
        String fromJson = resolveExtraStr(agency, "jpayPayIndexUrl", "");
        if (!fromJson.isBlank()) {
            return normalizeJpayPayIndexUrl(fromJson.trim(), agency);
        }
        String epPay = agency.getEndpointUrlPay();
        if (epPay != null && !epPay.isBlank()) {
            return normalizeJpayPayIndexUrl(epPay.trim(), agency);
        }
        String legacyEp = agency.getApiEndpoint();
        if (legacyEp != null && !legacyEp.isBlank()) {
            return normalizeJpayPayIndexUrl(legacyEp.trim(), agency);
        }
        boolean sand = agency.getSandboxYn() == null || "Y".equalsIgnoreCase(agency.getSandboxYn().trim());
        return sand ? DEFAULT_SANDBOX_PAY_INDEX : DEFAULT_LIVE_PAY_INDEX;
    }

    /**
     * 구 {@code www.j-pay.net}(404)·{@code http://} 등 잘못된 운영 URL을 {@link #DEFAULT_LIVE_PAY_INDEX} 로 보정.
     * 샌드박스 URL({@code sandbox.j-pay.net})은 그대로 둡니다.
     */
    private static String normalizeJpayPayIndexUrl(String url, PgAgency agency) {
        if (url == null || url.isBlank()) {
            boolean sand = agency == null || agency.getSandboxYn() == null
                    || "Y".equalsIgnoreCase(agency.getSandboxYn().trim());
            return sand ? DEFAULT_SANDBOX_PAY_INDEX : DEFAULT_LIVE_PAY_INDEX;
        }
        String u = url.trim();
        String lower = u.toLowerCase(Locale.ROOT);
        if (lower.contains("sandbox.j-pay.net")) {
            if (lower.startsWith("http://")) {
                return "https://" + u.substring(7);
            }
            return u;
        }
        if (lower.matches("https?://(www\\.)?j-pay\\.net/pay_index/?")) {
            return DEFAULT_LIVE_PAY_INDEX;
        }
        if (lower.startsWith("http://") && lower.contains("j-pay.net")) {
            return "https://" + u.substring(7);
        }
        return u;
    }

    private String resolveBankCode(PgAgency agency) {
        String c = resolveExtraStr(agency, "jpayBankCode", "");
        return c.isBlank() ? DEFAULT_BANK_CODE : c.trim();
    }

    /**
     * {@code tb_pg_agency.credentials_extra_json} 의 {@code jpayNotifyIngressStyle}.
     * {@code OPEN} 이면 레거시 open 경로, 그 외(비우거나 {@code MIDDLEWARE})는 권장 미들웨어 경로.
     */
    private static String resolveJpayNotifyPathPrefix(PgAgency agency) {
        String style = resolveExtraStr(agency, "jpayNotifyIngressStyle", "").trim().toUpperCase(Locale.ROOT);
        if ("OPEN".equals(style)) {
            return PgNotifyIngressPaths.OPEN_PREFIX;
        }
        return PgNotifyIngressPaths.MIDDLEWARE_PREFIX;
    }

    /** 가맹 JPAY 수신통보 URL — {@link MerchantNotifyUrl#URL_TYPE_JPAY_NOTIFY} 우선, 없으면 ingress 기본값 */
    private String resolveMerchantJpayNotifyUrl(Long orgUnitId, String defaultIngressUrl) {
        return resolveMerchantConfiguredNotifyUrl(orgUnitId, MerchantNotifyUrl.URL_TYPE_JPAY_NOTIFY, defaultIngressUrl);
    }

    /** 가맹 JPAY 콜백 URL — {@link MerchantNotifyUrl#URL_TYPE_JPAY_CALLBACK} 우선, 없으면 ingress 기본값 */
    private String resolveMerchantJpayCallbackUrl(Long orgUnitId, String defaultIngressUrl) {
        return resolveMerchantConfiguredNotifyUrl(orgUnitId, MerchantNotifyUrl.URL_TYPE_JPAY_CALLBACK, defaultIngressUrl);
    }

    /**
     * 가맹 JPAY 수신통보(Notify/Callback) — 노티미들웨어 등 외부 URL을 그대로 {@code pay_notifyurl}/{@code pay_callbackurl} 에 사용합니다.
     * (2026-07-04 아웃바운드 도메인 강제 치환 이전·7/3 정상 연동 방식. 미들웨어 주소의 PG 노출은 설계상 허용.)
     * 미등록·미사용·공백일 때만 ICOPAY ingress({@code cbJpay}/{@code rsJpay}) 기본값.
     */
    private String resolveMerchantConfiguredNotifyUrl(Long orgUnitId, String urlType, String defaultIngressUrl) {
        if (orgUnitId == null || urlType == null || urlType.isBlank()) {
            return defaultIngressUrl != null ? defaultIngressUrl : "";
        }
        Optional<MerchantNotifyUrl> row = merchantNotifyUrlRepository.findByOrgUnitIdAndUrlType(orgUnitId, urlType.trim());
        if (row.isEmpty()) {
            return defaultIngressUrl != null ? defaultIngressUrl : "";
        }
        MerchantNotifyUrl n = row.get();
        if (n.getUseYn() != null && !"Y".equalsIgnoreCase(n.getUseYn().trim())) {
            return defaultIngressUrl != null ? defaultIngressUrl : "";
        }
        String u = n.getNotiUrl() != null ? n.getNotiUrl().trim() : "";
        if (u.isBlank()) {
            return defaultIngressUrl != null ? defaultIngressUrl : "";
        }
        return u;
    }

    private static String resolveExtraStr(PgAgency agency, String key, String def) {
        if (agency == null || agency.getCredentialsExtraJson() == null || agency.getCredentialsExtraJson().isBlank()) {
            return def != null ? def : "";
        }
        try {
            JsonNode n = OM.readTree(agency.getCredentialsExtraJson());
            String v = n.path(key).asText("");
            return v.isBlank() && def != null ? def : v;
        } catch (Exception e) {
            return def != null ? def : "";
        }
    }

    private String resolvePublicApiBase(HttpServletRequest req) {
        Optional<HqApiConfig> cfg = hqApiConfigRepository.findAll().stream().findFirst();
        if (cfg.isPresent() && cfg.get().getPublicApiBaseUrl() != null && !cfg.get().getPublicApiBaseUrl().isBlank()) {
            return trimSlash(cfg.get().getPublicApiBaseUrl().trim());
        }
        String pub = hqNotifyEnvService.getOrCreate().getPublicBaseUrl();
        if (pub != null && !pub.isBlank()) {
            return trimSlash(pub.trim());
        }
        if (req != null) {
            String scheme = req.getHeader("X-Forwarded-Proto");
            if (scheme == null || scheme.isBlank()) {
                scheme = req.getScheme();
            }
            String host = req.getHeader("X-Forwarded-Host");
            if (host == null || host.isBlank()) {
                host = req.getServerName();
                int port = req.getServerPort();
                if (("http".equalsIgnoreCase(scheme) && port != 80)
                        || ("https".equalsIgnoreCase(scheme) && port != 443)) {
                    host = host + ":" + port;
                }
            }
            return trimSlash(scheme + "://" + host);
        }
        return "";
    }

    private static String trimSlash(String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("/+$", "");
    }

    private static void addIfPresent(MultiValueMap<String, String> form, Map<String, Object> body, String formKey, String bodyKey) {
        String v = str(body.get(bodyKey));
        if (!v.isBlank()) {
            form.add(formKey, v);
        }
    }

    /** JPAY — 전화번호는 국가코드(+82 등) 없이 로컬 번호만 전달. */
    private static void addTelephoneIfPresent(MultiValueMap<String, String> form, Map<String, Object> body) {
        String v = normalizeLocalTelephone(str(body.get("payTelephone")));
        if (!v.isBlank()) {
            form.add("pay_telephone", v);
        }
    }

    private static String normalizeLocalTelephone(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String t = raw.trim();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^\\+\\d{1,4}[\\s\\-]*(.*)$").matcher(t);
        if (m.matches()) {
            return m.group(1).trim();
        }
        return t;
    }

    /**
     * J-Pay Sale API 필수에 가까운 필드 기본값 — {@code system}, {@code pay_language}, 배송=청구 복제.
     * @see <a href="https://docs.j-pay.net/docs/api/sale">J-Pay Sale</a>
     */
    private static void applyJpaySaleDefaults(MultiValueMap<String, String> form, Map<String, Object> body) {
        if (firstFormVal(form, "system").isBlank()) {
            form.add("system", "icopay");
        }
        if (firstFormVal(form, "pay_language").isBlank()) {
            String lang = resolveJpayPayLanguage(form, body);
            form.add("pay_language", lang);
        }
        copyShippingFromBillingIfAbsent(form);
    }

    private static String resolveJpayPayLanguage(MultiValueMap<String, String> form, Map<String, Object> body) {
        for (String key : new String[]{"payLanguage", "langCode", "lang"}) {
            String mapped = mapIcopayLangToJpay(str(body.get(key)));
            if (!mapped.isBlank()) {
                return mapped;
            }
        }
        String fromForm = mapIcopayLangToJpay(firstFormVal(form, "pay_language"));
        if (!fromForm.isBlank()) {
            return fromForm;
        }
        return "en";
    }

    /** ICOPAY 결제창 코드(KOR/ENG/…) 또는 ISO 태그 → J-Pay demo 형식( en, ko, zh …). */
    private static String mapIcopayLangToJpay(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        return switch (u) {
            case "KOR", "KO", "KR" -> "ko";
            case "ENG", "EN" -> "en";
            case "JPN", "JA", "JP" -> "ja";
            case "CHN", "ZH", "CN" -> "zh";
            case "THA", "TH" -> "th";
            default -> {
                if (u.length() == 2) {
                    yield u.toLowerCase(Locale.ROOT);
                }
                String lower = raw.trim().toLowerCase(Locale.ROOT);
                if (lower.length() >= 2 && lower.charAt(0) >= 'a' && lower.charAt(0) <= 'z') {
                    yield lower.length() > 2 ? lower.substring(0, 2) : lower;
                }
                yield "";
            }
        };
    }

    private static RestTemplate createJpayRestTemplate() {
        RestTemplate rt = new RestTemplate();
        rt.getMessageConverters().removeIf(c -> c instanceof StringHttpMessageConverter);
        StringHttpMessageConverter utf8 = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        utf8.setWriteAcceptCharset(false);
        rt.getMessageConverters().add(0, utf8);
        return rt;
    }

    private static String truncateRaw(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.trim();
        return t.length() > 400 ? t.substring(0, 400) + "…" : t;
    }

    private static void putJpayPayIndexExtras(Map<String, Object> out, JpayPayIndexResponseParser.Outcome parsed) {
        if (out == null || parsed == null) {
            return;
        }
        String txnId = parsed.transactionId();
        if (txnId != null && !txnId.isBlank()) {
            out.put("jpayTransactionId", txnId);
        }
    }

    private static void copyShippingFromBillingIfAbsent(MultiValueMap<String, String> form) {
        copyFormIfAbsent(form, "shipping_firstname", "pay_firstname");
        copyFormIfAbsent(form, "shipping_lastname", "pay_lastname");
        copyFormIfAbsent(form, "shipping_street_address1", "pay_street_address1");
        copyFormIfAbsent(form, "shipping_street_address2", "pay_street_address2");
        copyFormIfAbsent(form, "shipping_city", "pay_city");
        copyFormIfAbsent(form, "shipping_state", "pay_state");
        copyFormIfAbsent(form, "shipping_postcode", "pay_postcode");
        copyFormIfAbsent(form, "shipping_country_iso_code_2", "pay_country_iso_code_2");
        copyFormIfAbsent(form, "shipping_telephone", "pay_telephone");
    }

    private static void copyFormIfAbsent(MultiValueMap<String, String> form, String targetKey, String sourceKey) {
        if (!firstFormVal(form, targetKey).isBlank()) {
            return;
        }
        String src = firstFormVal(form, sourceKey);
        if (!src.isBlank()) {
            form.add(targetKey, src);
        }
    }

    private static String firstFormVal(MultiValueMap<String, String> form, String key) {
        if (form == null || key == null) {
            return "";
        }
        List<String> vals = form.get(key);
        if (vals == null || vals.isEmpty()) {
            return "";
        }
        return vals.get(0) != null ? vals.get(0).trim() : "";
    }

    private static int parseRouteNo(String rootNo) {
        if (rootNo == null || rootNo.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(rootNo.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    private static String joinPayerName(String first, String last) {
        String f = first != null ? first.trim() : "";
        String l = last != null ? last.trim() : "";
        if (f.isEmpty()) {
            return l;
        }
        if (l.isEmpty()) {
            return f;
        }
        return f + " " + l;
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
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> validateCardPolicyForDirectSale(Long orgUnitId, Map<String, Object> body) {
        return payCardPolicyService.validateForSale(
                PgVendor.JPAY,
                str(body.get("payCardno")),
                str(body.get("payCardBrand")),
                str(body.get("payLanguage")),
                orgUnitId,
                joinPayerName(str(body.get("payFirstname")), str(body.get("payLastname"))));
    }

    private Map<String, Object> cardPolicyBlockOut(Map<String, Object> cardVal,
                                                   String merchantCode,
                                                   String orderNo,
                                                   String txnOrigin,
                                                   Long orgUnitId,
                                                   BigDecimal amountBd,
                                                   String currency,
                                                   int routeNo,
                                                   Map<String, Object> body,
                                                   BigDecimal shopperDisplayAmt,
                                                   String shopperDisplayCur) {
        String msg = cardVal.get("message") != null ? cardVal.get("message").toString() : "카드번호를 확인해 주세요.";
        String code = cardVal.get("errorCode") != null ? cardVal.get("errorCode").toString() : "CARD_POLICY";
        if (!PayCardPolicyService.suppressesPaymentListRecording(cardVal)) {
            if (orgUnitId != null) {
                jpaySaleRecordService.recordOrTouchPending(orgUnitId, orderNo, amountBd, currency, routeNo,
                        str(body.get("payEmailAddress")),
                        str(body.get("item")),
                        txnOrigin,
                        shopperDisplayAmt,
                        shopperDisplayCur,
                        body);
            }
            if (!merchantCode.isBlank()) {
                jpaySaleRecordService.applyIcopayPreSaleFail(merchantCode, orderNo, txnOrigin, msg, code);
            }
        }
        return cardPolicyFailOut(cardVal, msg, code);
    }

    private Map<String, Object> presaleRiskBlockOut(PayPresaleRiskFilterService.PresaleRiskBlock block,
                                                   Long orgUnitId,
                                                   String merchantCode,
                                                   String orderNo,
                                                   String txnOrigin,
                                                   BigDecimal amountBd,
                                                   String currency,
                                                   int routeNo,
                                                   Map<String, Object> body,
                                                   BigDecimal shopperDisplayAmt,
                                                   String shopperDisplayCur) {
        jpaySaleRecordService.recordOrTouchPending(orgUnitId, orderNo, amountBd, currency, routeNo,
                str(body.get("payEmailAddress")),
                str(body.get("item")),
                txnOrigin,
                shopperDisplayAmt,
                shopperDisplayCur,
                body);
        String trnId = jpaySaleRecordService.applyIcopayPresaleRiskCancel(
                merchantCode, orderNo, txnOrigin, block);
        payPresaleRiskFilterService.recordEvent(orgUnitId, merchantCode, orderNo, trnId, PgVendor.JPAY, block);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", false);
        out.put("errorCode", PayPresaleRiskFilterCodes.ERROR_CODE);
        out.put("filterCode", block.filterCode());
        out.put("message", block.message());
        out.put("messages", block.messages());
        out.put("icopayPresaleBlock", true);
        out.put("txnStatus", "20");
        return out;
    }

    private Map<String, Object> cardPolicyFailOut(Map<String, Object> cardVal, String msg, String code) {
        Map<String, Object> out = failOut(msg, code);
        if (cardVal.get("messageKey") != null) {
            out.put("messageKey", cardVal.get("messageKey"));
        }
        if (cardVal.get("messages") != null) {
            out.put("messages", cardVal.get("messages"));
        }
        if (cardVal.get("remainingMinutes") != null) {
            out.put("remainingMinutes", cardVal.get("remainingMinutes"));
        }
        out.put("icopayPresaleBlock", true);
        return out;
    }

    private static String defaultProductJson(String productName, String price) {
        try {
            List<Map<String, String>> one = List.of(Map.of(
                    "sku", "SKU1",
                    "productName", productName,
                    "productImage", "https://example.com/p.png",
                    "attributes", "",
                    "price", price,
                    "quantity", "1"));
            return OM.writeValueAsString(one);
        } catch (Exception e) {
            return "[{\"sku\":\"SKU1\",\"productName\":\"item\",\"productImage\":\"\",\"attributes\":\"\",\"price\":\"0.01\",\"quantity\":\"1\"}]";
        }
    }
}
