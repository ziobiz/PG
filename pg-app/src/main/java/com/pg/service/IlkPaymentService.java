package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgAgency;
import com.pg.integration.pg.PgVendor;
import com.pg.integration.pg.ilk.IlkCredentials;
import com.pg.integration.pg.ilk.IlkCryptoUtil;
import com.pg.middleware.notify.PgNotifyIngressPaths;
import com.pg.repository.MerchantIlkSubscriptionRepository;
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.entity.MerchantIlkSubscription;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.urlpay.CardAuthModeService;
import com.pg.urlpay.CardAuthModeUtil;
import com.pg.urlpay.CheckoutFailI18n;
import com.pg.urlpay.PayerContextCapture;
import com.pg.util.PayPresaleRiskFilterCodes;
import com.pg.util.PgOutboundUrlPolicy;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * ILK OpenAPI — RequestAuth / Payment / Status / Cancel / Refund.
 * <p>3DS: {@code authType=16} (인증+승인, Front/Back Noti). NONE3D: {@code Payment} + {@code noCertApproval=1}.
 * <p>구독(매뉴얼 기반): 초회 {@code cof=08}+{@code cofType=CIT}+RequestAuth(브라우저 3DS);
 * 회차 {@code Payment}+{@code cofType=MIT}+{@code deviceChannel=03(3RI)} (고객창 없음).
 * {@code RePayment} 는 원거래 취소 후 재승인 전용 — 구독 월청구에 사용하지 않음.
 * 가맹 MID·사이트명은 PG 로 보내지 않으며 ICOPAY 집계 descriptor 만 사용합니다.
 */
@Service
public class IlkPaymentService {

    private static final Logger log = LoggerFactory.getLogger(IlkPaymentService.class);
    private static final String DESC_NAME = "ICOPAY";
    private static final String DESC_PHONE = "0000000000";
    private static final String DESC_COUNTRY = "410";

    private final PgAgencyRepository pgAgencyRepository;
    private final MerchantPgBindingRepository merchantPgBindingRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final HqNotifyEnvService hqNotifyEnvService;
    private final IlkSaleRecordService ilkSaleRecordService;
    private final PayPresaleRiskFilterService payPresaleRiskFilterService;
    private final MerchantChatbotProductService productService;
    private final CardAuthModeService cardAuthModeService;
    private final MerchantIlkSubscriptionRepository ilkSubscriptionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public IlkPaymentService(PgAgencyRepository pgAgencyRepository,
                             MerchantPgBindingRepository merchantPgBindingRepository,
                             OrgUnitRepository orgUnitRepository,
                             HqNotifyEnvService hqNotifyEnvService,
                             IlkSaleRecordService ilkSaleRecordService,
                             PayPresaleRiskFilterService payPresaleRiskFilterService,
                             MerchantChatbotProductService productService,
                             CardAuthModeService cardAuthModeService,
                             MerchantIlkSubscriptionRepository ilkSubscriptionRepository) {
        this.pgAgencyRepository = pgAgencyRepository;
        this.merchantPgBindingRepository = merchantPgBindingRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.ilkSaleRecordService = ilkSaleRecordService;
        this.payPresaleRiskFilterService = payPresaleRiskFilterService;
        this.productService = productService;
        this.cardAuthModeService = cardAuthModeService;
        this.ilkSubscriptionRepository = ilkSubscriptionRepository;
    }

    public boolean hasOperationalWebBinding(Long orgUnitId) {
        return findOperationalIlkBinding(orgUnitId).isPresent();
    }

    public boolean hasOperationalSubscriptionBinding(Long orgUnitId) {
        return findOperationalIlkSubscriptionBinding(orgUnitId).isPresent();
    }

    public Optional<MerchantPgBinding> findOperationalIlkBinding(Long orgUnitId) {
        if (orgUnitId == null) {
            return Optional.empty();
        }
        return merchantPgBindingRepository.findByOrgUnitIdOrderBySortOrderAsc(orgUnitId).stream()
                .filter(b -> PgVendor.isIlkFamily(b.getPgCd()))
                .filter(b -> b.getActivationYn() == null || "Y".equalsIgnoreCase(trim(b.getActivationYn())))
                .filter(b -> "Y".equalsIgnoreCase(trim(b.getOperationalYn())))
                .filter(b -> {
                    String pm = b.getPayMethod();
                    return pm == null || pm.isBlank() || "WEB".equalsIgnoreCase(pm.trim());
                })
                .findFirst();
    }

    public Optional<MerchantPgBinding> findOperationalIlkSubscriptionBinding(Long orgUnitId) {
        Optional<MerchantPgBinding> web = findOperationalIlkBinding(orgUnitId);
        if (web.isEmpty()) {
            return Optional.empty();
        }
        String pgCd = trim(web.get().getPgCd());
        Optional<PgAgency> ag = pgAgencyRepository.findByPgCd(pgCd);
        if (ag.isEmpty() || !"Y".equalsIgnoreCase(trim(ag.get().getIntegApiSubscriptionYn()))) {
            return Optional.empty();
        }
        return web;
    }

    /**
     * URL/인라인 카드 승인 — 실효 cardAuthMode 에 따라 NONE3D Payment 또는 3DS RequestAuth(authType 16).
     */
    public Map<String, Object> executeSale(Long orgUnitId, Map<String, Object> body,
                                           HttpServletRequest req, String clientIp) {
        return executeSaleInternal(orgUnitId, body, req, clientIp, false, false);
    }

    /** 구독 초회 CIT+3DS+cof=08 */
    public Map<String, Object> executeSubscriptionFirstCharge(Long orgUnitId, Map<String, Object> body,
                                                              HttpServletRequest req, String clientIp) {
        return executeSaleInternal(orgUnitId, body, req, clientIp, true, false);
    }

    /** 구독 회차 MIT (서버 스케줄) */
    public Map<String, Object> executeSubscriptionMitCharge(Long orgUnitId, Map<String, Object> body) {
        return executeSaleInternal(orgUnitId, body, null, str(body.get("clientIp")), true, true);
    }

    private Map<String, Object> executeSaleInternal(Long orgUnitId, Map<String, Object> body,
                                                    HttpServletRequest req, String clientIp,
                                                    boolean subscription, boolean mit) {
        Optional<MerchantPgBinding> bindOpt = subscription
                ? findOperationalIlkSubscriptionBinding(orgUnitId)
                : findOperationalIlkBinding(orgUnitId);
        if (bindOpt.isEmpty()) {
            return subscription ? CheckoutFailI18n.subscriptionPgMissing() : CheckoutFailI18n.urlPayPgMissing();
        }
        MerchantPgBinding binding = bindOpt.get();
        Optional<PgAgency> agOpt = pgAgencyRepository.findByPgCd(trim(binding.getPgCd()));
        if (agOpt.isEmpty()) {
            return CheckoutFailI18n.pgConfigMissing();
        }
        PgAgency agency = agOpt.get();
        IlkCredentials cred = IlkCredentials.from(agency);
        if (!cred.isConfigured()) {
            return CheckoutFailI18n.pgCredentialsMissing();
        }

        if (req != null) {
            PayerContextCapture.enrichSaleBody(body, req, clientIp);
        }
        Optional<OrgUnit> ou = orgUnitRepository.findById(orgUnitId);
        String compCode = ou.map(OrgUnit::getCode).orElse("");

        String orderNo = str(body.get("orderNo"));
        if (orderNo.isBlank()) {
            return CheckoutFailI18n.invalidOrderNo();
        }
        BigDecimal amount = parseAmount(body.get("amount"));
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return CheckoutFailI18n.invalidAmount();
        }
        String currency = str(body.get("currency"));
        if (currency.isBlank()) {
            currency = "KRW";
        }
        currency = currency.toUpperCase(Locale.ROOT);

        Optional<PayPresaleRiskFilterService.PresaleRiskBlock> presaleRisk =
                payPresaleRiskFilterService.evaluate(orgUnitId, compCode, PgVendor.ILK, body);
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

        String cardNo = digitsOnly(str(body.get("cardNo"), body.get("payCardno"), body.get("cardNumber")));
        String expMonth = pad2(digitsOnly(str(body.get("cardMonth"), body.get("payCardmonth"), body.get("expirationMonth"))));
        String expYear = normalizeYear(str(body.get("cardYear"), body.get("payCardyear"), body.get("expirationYear")));
        String cvv = digitsOnly(str(body.get("cardCvv"), body.get("payCardcvv"), body.get("cvv")));
        if (cardNo.isBlank() || expMonth.isBlank() || expYear.isBlank()) {
            return CheckoutFailI18n.cardRequired();
        }

        String publicBase = "";
        try {
            publicBase = trimSlash(productService.resolvePublicCustomerSiteBase(req));
        } catch (Exception ignored) {
            publicBase = "";
        }
        if (publicBase.isBlank()) {
            publicBase = "https://icopay.co.kr";
        }
        String ingressToken = hqNotifyEnvService.getOrCreate().getIngressToken();
        String notifyUrl = PgOutboundUrlPolicy.enforceOwnDomain(
                PgNotifyIngressPaths.buildIngressBase(publicBase, ingressToken) + "/ILK",
                publicBase, publicBase);
        String returnUrl = PgOutboundUrlPolicy.enforceOwnDomain(
                publicBase + "/api/pay/ilk/front-notify", publicBase, publicBase);
        String descriptorUrl = publicBase.isBlank() ? "https://icopay.co.kr" : publicBase;

        String productName = str(body.get("productName"), body.get("item"));
        if (productName.isBlank()) {
            productName = "Payment";
        }
        String buyerName = str(body.get("buyerName"), body.get("customerName"), body.get("name"));
        if (buyerName.isBlank()) {
            buyerName = "GUEST";
        }
        String buyerEmail = str(body.get("buyerEmail"), body.get("email"), body.get("payEmailAddress"));
        if (buyerEmail.isBlank()) {
            buyerEmail = "noreply@icopay.co.kr";
        }
        String buyerPhone = digitsOnly(str(body.get("buyerPhone"), body.get("mobile"), body.get("payTelephone")));
        if (buyerPhone.isBlank()) {
            buyerPhone = "01000000000";
        }
        String buyerAddr = str(body.get("buyerAddress"), body.get("homeAddress"));
        if (buyerAddr.isBlank()) {
            buyerAddr = "N/A";
        }
        String ip = clientIp != null && !clientIp.isBlank() ? clientIp.trim() : "0.0.0.0";
        if (ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        if (ip.length() > 15) {
            ip = ip.substring(0, 15);
        }

        String amountPlain = formatAmount(amount, currency);
        String exponent = currencyExponent(currency);
        String brand = resolveBrand(str(body.get("cardBrand"), body.get("payCardBrand")), cardNo);

        Integer routeNo = agency.getRouteNo();
        BigDecimal shopperAmt = parseAmount(body.get("shopperDisplayAmount"));
        String shopperCur = str(body.get("shopperDisplayCurrency"));
        String origin = str(body.get("txnOrigin"));
        if (origin.isBlank()) {
            origin = subscription ? "SUB" : "URL";
        }

        ilkSaleRecordService.recordOrTouchPending(orgUnitId, orderNo, amount, currency, routeNo,
                productName, origin, buyerName, buyerEmail, shopperAmt, shopperCur, subscription, null);

        if (subscription && !mit) {
            persistSubscriptionCardSeed(compCode, orderNo, cred, cardNo, expMonth, expYear, brand, amount, currency);
        }

        String authMode = cardAuthModeService.resolveEffective(orgUnitId);
        boolean forceThreeDs = subscription && !mit;
        boolean none3d = !forceThreeDs && !mit && CardAuthModeUtil.isNone3d(authMode);

        Map<String, Object> payload = new LinkedHashMap<>();
        putCommonMerchant(payload, cred);
        payload.put("clientReferenceInformation.code", clamp(orderNo, 32));
        payload.put("orderInformation.amountDetails.currency", currency);
        payload.put("orderInformation.amountDetails.exponent", exponent);
        payload.put("orderInformation.amountDetails.totalAmount", amountPlain);
        List<Map<String, String>> lineItems = new ArrayList<>();
        Map<String, String> line = new LinkedHashMap<>();
        line.put("productName", clamp(productName, 192));
        line.put("unitPrice", amountPlain);
        line.put("quantity", "1");
        lineItems.add(line);
        payload.put("orderInformation.lineItems", lineItems);
        payload.put("merchantInformation.merchantDescriptor.name", DESC_NAME);
        payload.put("merchantInformation.merchantDescriptor.phone", DESC_PHONE);
        payload.put("merchantInformation.merchantDescriptor.url", clamp(descriptorUrl, 128));
        payload.put("merchantInformation.merchantDescriptor.country", DESC_COUNTRY);
        payload.put("paymentInformation.brand", brand);
        payload.put("paymentInformation.number", IlkCryptoUtil.encryptAesBase64(cardNo, cred.seedKey(), cred.seedIv()));
        payload.put("paymentInformation.expirationMonth",
                IlkCryptoUtil.encryptAesBase64(expMonth, cred.seedKey(), cred.seedIv()));
        payload.put("paymentInformation.expirationYear",
                IlkCryptoUtil.encryptAesBase64(expYear, cred.seedKey(), cred.seedIv()));
        if (!cvv.isBlank()) {
            payload.put("paymentInformation.cvv",
                    IlkCryptoUtil.encryptAesBase64(cvv, cred.seedKey(), cred.seedIv()));
        }
        payload.put("orderInformation.billTo.name", clamp(buyerName, 32));
        payload.put("orderInformation.billTo.homeAddress", clamp(buyerAddr, 128));
        payload.put("orderInformation.billTo.mobileNumber", clamp(buyerPhone, 20));
        payload.put("orderInformation.billTo.email", clamp(buyerEmail, 128));
        payload.put("deviceInformation.ipAddress", ip);

        if (subscription) {
            payload.put("paymentInformation.cof", "08");
            payload.put("processingInformation.cofType", mit ? "MIT" : "CIT");
        }

        if (mit) {
            // MIT: Payment + 3RI (고객창 없음). RePayment 아님. noCertApproval 미사용.
            payload.put("consumerAuthenticationInformation.deviceChannel", "03");
            payload.put("consumerAuthenticationInformation.threeRIInd", "01");
            payload.put("consumerAuthenticationInformation.threeDSRequestorAuthenticationInd", "02");
            payload.put("consumerAuthenticationInformation.transType", "01");
            payload.put("consumerAuthenticationInformation.messageCategory", "01");
            payload.put("processingInformation.notifyUrl", notifyUrl);
            // 매뉴얼 Payment 필수는 카드 재전송(암호화)·신규 code·cof/cofType.
            // orgCode/원승인 id 체인 필드는 Payment MIT에 명시되지 않음(RePayment 전용 orgCode).
            return postPayment(cred, payload, orgUnitId, orderNo, amount, currency);
        }

        if (none3d) {
            // 일반결제 2DS만. 구독 MIT/CIT 경로에서는 도달하지 않음(forceThreeDs).
            payload.put("processingInformation.noCertApproval", "1");
            payload.put("processingInformation.notifyUrl", notifyUrl);
            return postPayment(cred, payload, orgUnitId, orderNo, amount, currency);
        }

        // 3DS path — authType 16 (auth+approve with notify). 구독 초회=CIT+브라우저(deviceChannel 02).
        payload.put("processingInformation.authType", "16");
        payload.put("processingInformation.noCertApproval", "0");
        payload.put("processingInformation.returnUrl", returnUrl);
        payload.put("processingInformation.notifyUrl", notifyUrl);
        putBrowserFields(payload, body, req);
        if (subscription) {
            payload.put("consumerAuthenticationInformation.threeDSRequestorAuthenticationInd", "02");
        }
        return postRequestAuth(cred, payload, orgUnitId, orderNo);
    }

    public Map<String, Object> queryStatus(Long orgUnitId, String orderNo) {
        Optional<AgencyCred> ac = resolveAgencyCred(orgUnitId);
        if (ac.isEmpty()) {
            return CheckoutFailI18n.pgConfigMissing();
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        putCommonMerchant(payload, ac.get().cred());
        payload.put("clientReferenceInformation.code", clamp(orderNo, 32));
        return postJson(ac.get().cred(), "/api/Status.json", payload);
    }

    public Map<String, Object> cancel(Long orgUnitId, String orderNo, String amount, String currency) {
        Optional<AgencyCred> ac = resolveAgencyCred(orgUnitId);
        if (ac.isEmpty()) {
            return CheckoutFailI18n.pgConfigMissing();
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        putCommonMerchant(payload, ac.get().cred());
        payload.put("clientReferenceInformation.code", clamp(orderNo, 32));
        if (amount != null && !amount.isBlank()) {
            payload.put("orderInformation.amountDetails.cancelAmount", amount.trim());
        }
        if (currency != null && !currency.isBlank()) {
            payload.put("orderInformation.amountDetails.currency", currency.trim().toUpperCase(Locale.ROOT));
        }
        Map<String, Object> resp = postJson(ac.get().cred(), "/api/Cancel.json", payload);
        if (Boolean.TRUE.equals(resp.get("success"))) {
            Optional<OrgUnit> ou = orgUnitRepository.findById(orgUnitId);
            ou.ifPresent(o -> ilkSaleRecordService.applyCancel(
                    o.getCode(), orderNo, str(resp.get("id")), "CANCEL"));
        }
        return resp;
    }

    public Map<String, Object> refund(Long orgUnitId, String orderNo, String amount, String currency) {
        Optional<AgencyCred> ac = resolveAgencyCred(orgUnitId);
        if (ac.isEmpty()) {
            return CheckoutFailI18n.pgConfigMissing();
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        putCommonMerchant(payload, ac.get().cred());
        payload.put("clientReferenceInformation.code", clamp(orderNo, 32));
        if (amount != null && !amount.isBlank()) {
            payload.put("orderInformation.amountDetails.refundAmount", amount.trim());
        }
        if (currency != null && !currency.isBlank()) {
            payload.put("orderInformation.amountDetails.currency", currency.trim().toUpperCase(Locale.ROOT));
        }
        return postJson(ac.get().cred(), "/api/Refund.json", payload);
    }

    public Optional<IlkCredentials> resolveCredentials(Long orgUnitId) {
        return resolveAgencyCred(orgUnitId).map(AgencyCred::cred);
    }

    private Map<String, Object> postRequestAuth(IlkCredentials cred, Map<String, Object> payload,
                                                Long orgUnitId, String orderNo) {
        Map<String, Object> resp = postJson(cred, "/api/RequestAuth.json", payload);
        if (!Boolean.TRUE.equals(resp.get("success"))) {
            String msg = str(resp.get("message"));
            Optional<OrgUnit> ou = orgUnitRepository.findById(orgUnitId);
            ou.ifPresent(o -> ilkSaleRecordService.applyOutcome(o.getCode(), orderNo, false, null, msg));
            return resp;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("needs3ds", true);
        out.put("authType", "16");
        out.put("status", resp.get("status"));
        out.put("orderNo", orderNo);
        out.put("acsUrl", resp.get("acsUrl"));
        out.put("sizeX", resp.get("sizeX"));
        out.put("sizeY", resp.get("sizeY"));
        out.put("xid", resp.get("xid"));
        out.put("paReq", resp.get("paReq"));
        out.put("termUrl", resp.get("termUrl"));
        out.put("creq", resp.get("creq"));
        out.put("threeDSSessionData", resp.get("threeDSSessionData"));
        out.put("enrolled", resp.get("enrolled"));
        out.put("pgVendor", PgVendor.ILK);
        return out;
    }

    private Map<String, Object> postPayment(IlkCredentials cred, Map<String, Object> payload,
                                            Long orgUnitId, String orderNo,
                                            BigDecimal amount, String currency) {
        Map<String, Object> resp = postJson(cred, "/api/Payment.json", payload);
        boolean paid = Boolean.TRUE.equals(resp.get("success"))
                && "SUCCESS".equalsIgnoreCase(str(resp.get("status")));
        String id = str(resp.get("id"));
        String msg = paid ? "SUCCESS" : str(resp.get("message"));
        Optional<OrgUnit> ou = orgUnitRepository.findById(orgUnitId);
        ou.ifPresent(o -> ilkSaleRecordService.applyOutcome(o.getCode(), orderNo, paid, id, msg));
        Map<String, Object> out = new LinkedHashMap<>(resp);
        out.put("orderNo", orderNo);
        out.put("amount", amount != null ? amount.toPlainString() : null);
        out.put("currency", currency);
        out.put("needs3ds", false);
        out.put("pgVendor", PgVendor.ILK);
        return out;
    }

    private Map<String, Object> postJson(IlkCredentials cred, String path, Map<String, Object> payload) {
        try {
            Map<String, Object> body = new LinkedHashMap<>(payload);
            body.remove("sign");
            String sign = IlkCryptoUtil.signCompactJson(body, cred.seedKey());
            body.put("sign", sign);
            String url = cred.resolveBaseUrl() + path;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            String json = objectMapper.writeValueAsString(body);
            ResponseEntity<String> entity = restTemplate.postForEntity(url, new HttpEntity<>(json, headers), String.class);
            JsonNode root = objectMapper.readTree(entity.getBody() != null ? entity.getBody() : "{}");
            return mapIlkResponse(root);
        } catch (Exception e) {
            log.warn("ILK {} 실패: {}", path, e.getMessage());
            return CheckoutFailI18n.pgHttpFailed();
        }
    }

    private Map<String, Object> mapIlkResponse(JsonNode root) {
        Map<String, Object> out = new LinkedHashMap<>();
        String status = text(root, "status");
        boolean ok = "SUCCESS".equalsIgnoreCase(status);
        out.put("success", ok);
        out.put("status", status);
        out.put("id", text(root, "id"));
        out.put("message", firstNonBlank(
                text(root, "errorInformation.message"),
                text(root, "errorInformation.reason"),
                status));
        out.put("acsUrl", text(root, "consumerAuthenticationInformation.acsURL",
                "consumerAuthenticationInformation.acsUrl"));
        out.put("sizeX", text(root, "consumerAuthenticationInformation.sizeX"));
        out.put("sizeY", text(root, "consumerAuthenticationInformation.sizeY"));
        out.put("xid", text(root, "consumerAuthenticationInformation.xid"));
        out.put("paReq", text(root, "consumerAuthenticationInformation.paReq"));
        out.put("termUrl", text(root, "consumerAuthenticationInformation.termUrl"));
        out.put("creq", text(root, "consumerAuthenticationInformation.creq"));
        out.put("threeDSSessionData", text(root, "consumerAuthenticationInformation.threeDSSessionData"));
        out.put("enrolled", text(root, "consumerAuthenticationInformation.enrolled"));
        out.put("eciRaw", text(root, "consumerAuthenticationInformation.eciRaw"));
        out.put("raw", root.toString());
        if (!ok && (status == null || status.isBlank())) {
            out.put("success", false);
            out.put("errorCode", "ILK_DECLINED");
        }
        return out;
    }

    private void putCommonMerchant(Map<String, Object> payload, IlkCredentials cred) {
        payload.put("merchantInformation.merchantId", cred.merchantId());
        payload.put("merchantInformation.merchantSiteId", cred.merchantSiteId());
    }

    private void putBrowserFields(Map<String, Object> payload, Map<String, Object> body, HttpServletRequest req) {
        payload.put("consumerAuthenticationInformation.browserScreenWidth",
                firstNonBlank(str(body.get("browserScreenWidth")), "1920"));
        payload.put("consumerAuthenticationInformation.browserScreenHeight",
                firstNonBlank(str(body.get("browserScreenHeight")), "1080"));
        payload.put("consumerAuthenticationInformation.browserColorDepth",
                firstNonBlank(str(body.get("browserColorDepth")), "24"));
        payload.put("consumerAuthenticationInformation.browserTZ",
                firstNonBlank(str(body.get("browserTZ")), "540"));
        String accept = req != null ? str(req.getHeader("Accept")) : "";
        payload.put("consumerAuthenticationInformation.browserAcceptHeader",
                firstNonBlank(str(body.get("browserAcceptHeader")), accept, "text/html"));
        payload.put("consumerAuthenticationInformation.browserLanguage",
                firstNonBlank(str(body.get("browserLanguage")), "en-US"));
        String ua = req != null ? str(req.getHeader("User-Agent")) : "";
        payload.put("consumerAuthenticationInformation.browserUserAgent",
                firstNonBlank(str(body.get("browserUserAgent")), ua, "Mozilla/5.0"));
        payload.put("consumerAuthenticationInformation.deviceChannel", "02");
        payload.put("consumerAuthenticationInformation.transType", "01");
        payload.put("consumerAuthenticationInformation.messageCategory", "01");
        payload.put("consumerAuthenticationInformation.challengeWindowSize", "02");
    }

    private void persistSubscriptionCardSeed(String compCode, String orderNo, IlkCredentials cred,
                                             String cardNo, String expMonth, String expYear,
                                             String brand, BigDecimal amount, String currency) {
        if (compCode == null || orderNo == null || cred == null || !cred.isConfigured()) {
            return;
        }
        try {
            Optional<MerchantIlkSubscription> ex =
                    ilkSubscriptionRepository.findByCompIdAndSubscriptionNo(compCode, orderNo);
            MerchantIlkSubscription s = ex.orElseGet(MerchantIlkSubscription::new);
            if (s.getId() == null) {
                orgUnitRepository.findByCode(compCode).ifPresent(ou -> s.setOrgUnitId(ou.getId()));
                s.setCompId(compCode);
                s.setSubscriptionNo(orderNo);
                s.setFirstOrderNo(orderNo);
                s.setStatus(MerchantIlkSubscription.STATUS_PENDING);
                s.setChargeCount(0);
            }
            s.setAmount(amount);
            s.setCurrency(currency);
            s.setCardBrand(brand);
            if (cardNo.length() >= 4) {
                s.setCardLast4(cardNo.substring(cardNo.length() - 4));
            }
            s.setCardTokenEnc(IlkCryptoUtil.encryptAesBase64(cardNo, cred.seedKey(), cred.seedIv()));
            s.setCardExpMonthEnc(IlkCryptoUtil.encryptAesBase64(expMonth, cred.seedKey(), cred.seedIv()));
            s.setCardExpYearEnc(IlkCryptoUtil.encryptAesBase64(expYear, cred.seedKey(), cred.seedIv()));
            ilkSubscriptionRepository.save(s);
        } catch (Exception e) {
            log.warn("ILK 구독 카드 시드 저장 실패: {}", e.getMessage());
        }
    }

    private Optional<AgencyCred> resolveAgencyCred(Long orgUnitId) {
        Optional<MerchantPgBinding> bind = findOperationalIlkBinding(orgUnitId);
        if (bind.isEmpty()) {
            return Optional.empty();
        }
        return pgAgencyRepository.findByPgCd(trim(bind.get().getPgCd()))
                .map(a -> new AgencyCred(a, IlkCredentials.from(a)));
    }

    private record AgencyCred(PgAgency agency, IlkCredentials cred) {
    }

    private static String resolveBrand(String raw, String pan) {
        String b = raw != null ? raw.trim().toUpperCase(Locale.ROOT) : "";
        if (!b.isBlank() && !"AUTO".equals(b)) {
            if (b.startsWith("MASTER")) {
                return "Master";
            }
            if (b.startsWith("VISA")) {
                return "Visa";
            }
            if (b.startsWith("JCB")) {
                return "JCB";
            }
            if (b.startsWith("AMEX") || b.startsWith("AMERICAN")) {
                return "AMEX";
            }
            if (b.startsWith("UNION")) {
                return "UnionPay";
            }
            return b;
        }
        if (pan != null && pan.startsWith("4")) {
            return "Visa";
        }
        if (pan != null && (pan.startsWith("5") || pan.startsWith("2"))) {
            return "Master";
        }
        if (pan != null && pan.startsWith("35")) {
            return "JCB";
        }
        if (pan != null && pan.startsWith("62")) {
            return "UnionPay";
        }
        return "Visa";
    }

    private static String currencyExponent(String currency) {
        String c = currency != null ? currency.toUpperCase(Locale.ROOT) : "KRW";
        return switch (c) {
            case "KRW", "JPY", "VND" -> "0";
            default -> "2";
        };
    }

    private static String formatAmount(BigDecimal amount, String currency) {
        int scale = "0".equals(currencyExponent(currency)) ? 0 : 2;
        return amount.setScale(scale, RoundingMode.HALF_UP).toPlainString();
    }

    private static String normalizeYear(String raw) {
        String d = digitsOnly(raw);
        if (d.length() == 4) {
            return d.substring(2);
        }
        return pad2(d);
    }

    private static String pad2(String s) {
        if (s == null || s.isBlank()) {
            return "";
        }
        String d = digitsOnly(s);
        if (d.length() == 1) {
            return "0" + d;
        }
        return d.length() > 2 ? d.substring(d.length() - 2) : d;
    }

    private static String digitsOnly(String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("\\D+", "");
    }

    private static String text(JsonNode root, String... keys) {
        if (root == null || keys == null) {
            return "";
        }
        for (String key : keys) {
            JsonNode n = root.get(key);
            if (n != null && !n.isNull() && !n.asText("").isBlank()) {
                return n.asText("").trim();
            }
            // nested path fallback
            if (key != null && key.contains(".")) {
                JsonNode cur = root;
                for (String part : key.split("\\.")) {
                    if (cur == null) {
                        break;
                    }
                    cur = cur.get(part);
                }
                if (cur != null && !cur.isNull() && !cur.asText("").isBlank()) {
                    return cur.asText("").trim();
                }
            }
        }
        return "";
    }

    private static Map<String, Object> fail(String message, String code) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", false);
        out.put("message", message);
        out.put("errorCode", code);
        return out;
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

    private static String str(Object... vals) {
        if (vals == null) {
            return "";
        }
        for (Object o : vals) {
            if (o != null && !o.toString().isBlank()) {
                return o.toString().trim();
            }
        }
        return "";
    }

    private static String firstNonBlank(String... vals) {
        return str((Object[]) vals);
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

    private static String clamp(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
