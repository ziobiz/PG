package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgAgency;
import com.pg.integration.pg.PgVendor;
import com.pg.integration.pg.elementpay.ElementPayCredentials;
import com.pg.middleware.notify.PgNotifyIngressPaths;
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.urlpay.PayerContextCapture;
import com.pg.util.ElementPayHashUtil;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * ElementPay Payment API — THB 전용 initPayment·getStatus.
 * <p><b>노티미들웨어(외부 NOTI) — ChillPay·Eximbay 와 동일:</b>
 * ElementPay 캐비net Webhook 은 NOTI 서버에 등록하고, NOTI 가
 * {@link com.pg.middleware.notify.PgNotifyIngressPaths#MIDDLEWARE_PREFIX} 경로
 * {@code …/ELEMENTPAY} 로 ICOPAY 에 전달합니다. {@code notifyUrl} 은 운영 등록용 안내 값입니다.
 * <p><b>가맹 비식별:</b> ElementPay API·웹훅에는 ICOPAY 집계 Merchant Key 만 사용하며,
 * {@code _merchantData}·{@code icopayCompId=} 등 가맹 업체코드는 PG 로 전송하지 않습니다.
 * 웹훅 {@code order} 와 내부 {@code pg_trnsctn} 으로 가맹을 복원합니다.
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
    private final HqNotifyEnvService hqNotifyEnvService;
    private final ElementPaySaleRecordService elementPaySaleRecordService;
    private final PayPresaleRiskFilterService payPresaleRiskFilterService;
    private final MerchantChatbotProductService productService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public ElementPayPaymentService(PgAgencyRepository pgAgencyRepository,
                                      MerchantPgBindingRepository merchantPgBindingRepository,
                                      OrgUnitRepository orgUnitRepository,
                                      HqNotifyEnvService hqNotifyEnvService,
                                      ElementPaySaleRecordService elementPaySaleRecordService,
                                      PayPresaleRiskFilterService payPresaleRiskFilterService,
                                      MerchantChatbotProductService productService) {
        this.pgAgencyRepository = pgAgencyRepository;
        this.merchantPgBindingRepository = merchantPgBindingRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.elementPaySaleRecordService = elementPaySaleRecordService;
        this.payPresaleRiskFilterService = payPresaleRiskFilterService;
        this.productService = productService;
    }

    public boolean hasOperationalWebBinding(Long orgUnitId) {
        return findOperationalElementPayBinding(orgUnitId).isPresent();
    }

    /**
     * URL 결제 승인 — initPayment 호출 후 QR·리다이렉트·Light iframe URL 을 반환합니다.
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

        String paymentMethod = str(body.get("paymentMethod"));
        if (paymentMethod.isBlank()) {
            paymentMethod = "CARD";
        }
        String serviceAlias = cred.serviceAliasForMethod(paymentMethod);

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
        long ts = Instant.now().getEpochSecond();
        Map<String, String> signParams = new LinkedHashMap<>();
        signParams.put("service_id", serviceAlias);
        signParams.put("amount", amountPlain);
        signParams.put("order", orderNo);
        signParams.put("currency", THB);
        signParams.put("key", cred.merchantKey());
        signParams.put("timestamp", String.valueOf(ts));
        // 가맹 업체코드를 PG 필드에 넣지 않음 — check/pay 웹훅은 order 만으로 내부 대기거래를 복원
        String successUrl = PgOutboundUrlPolicy.enforceOwnDomain(
                publicBase + "/elementpay-pay.html?elementpayReturn=success", publicBase, publicBase);
        String rejectUrl = PgOutboundUrlPolicy.enforceOwnDomain(
                publicBase + "/elementpay-pay.html?elementpayReturn=reject", publicBase, publicBase);
        String waitingUrl = PgOutboundUrlPolicy.enforceOwnDomain(
                publicBase + "/elementpay-pay.html?elementpayReturn=waiting", publicBase, publicBase);
        signParams.put("_successUrl", successUrl);
        signParams.put("_rejectUrl", rejectUrl);
        signParams.put("_waitingUrl", waitingUrl);

        String hash = ElementPayHashUtil.signApiRequest(cred.apiSecretKey(), "initPayment", signParams);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        for (Map.Entry<String, String> e : signParams.entrySet()) {
            form.add(e.getKey(), e.getValue());
        }
        form.add("hash", hash);

        String url = resolveBase(agency) + "/merchant/initPayment";
        JsonNode resp;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
            ResponseEntity<String> entity = restTemplate.postForEntity(url, new HttpEntity<>(form, headers), String.class);
            resp = objectMapper.readTree(entity.getBody() != null ? entity.getBody() : "{}");
        } catch (Exception e) {
            log.warn("ElementPay initPayment HTTP 실패: {}", e.getMessage());
            return fail("ElementPay 결제 초기화에 실패했습니다.", "ELEMENTPAY_INIT_FAILED");
        }

        if (resp.has("error")) {
            String msg = resp.path("error").path("message").asText("ElementPay error");
            return fail(msg, "ELEMENTPAY_" + resp.path("error").path("code").asText("ERROR"));
        }

        JsonNode response = resp.path("response");
        if (response.isMissingNode()) {
            return fail("ElementPay 응답 형식이 올바르지 않습니다.", "ELEMENTPAY_INIT_INVALID");
        }

        String paymentId = response.path("id").asText("");
        int status = response.path("status").asInt(0);

        elementPaySaleRecordService.recordOrTouchPending(
                orgUnitId, orderNo, amount, THB, binding.getSortOrder(),
                str(body.get("item")), resolveTxnOrigin(body),
                str(body.get("customerNm")), str(body.get("payEmailAddress")),
                paymentMethod, null, null, false, paymentId);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("orderNo", orderNo);
        out.put("paymentId", paymentId);
        out.put("elementPayStatus", status);
        out.put("paymentMethod", paymentMethod);
        out.put("currency", THB);
        out.put("amount", amountPlain);
        out.put("notifyUrl", notifyBase);

        List<Map<String, String>> attrs = parseAttributes(response.path("attributes"));
        out.put("attributes", attrs);
        String qr = findAttribute(attrs, "qrcode");
        if (qr != null && !qr.isBlank()) {
            out.put("qrCodeDataUri", qr);
        }
        String redirect = findAttribute(attrs, "redirect_url");
        if (redirect == null || redirect.isBlank()) {
            redirect = findAttribute(attrs, "redirect");
        }
        if (redirect != null && !redirect.isBlank()) {
            out.put("redirectUrl", redirect);
        }

        String env = cred.sandbox() ? "sandbox" : "live";
        if ("CARD".equalsIgnoreCase(paymentMethod) && (redirect == null || redirect.isBlank())) {
            String light = resolveBase(agency) + "/merchant/light/#/services/card?amount=" + urlEnc(amountPlain)
                    + "&key=" + urlEnc(cred.merchantKey()) + "&env=" + env
                    + "&order=" + urlEnc(orderNo);
            out.put("lightIframeUrl", light);
        }

        return out;
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
        String hash = ElementPayHashUtil.signApiRequest(cred.apiSecretKey(), "getStatus", signParams);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        for (Map.Entry<String, String> e : signParams.entrySet()) {
            form.add(e.getKey(), e.getValue());
        }
        form.add("hash", hash);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
            String url = resolveBase(agOpt.get()) + "/merchant/getStatus";
            ResponseEntity<String> entity = restTemplate.postForEntity(url, new HttpEntity<>(form, headers), String.class);
            JsonNode resp = objectMapper.readTree(entity.getBody() != null ? entity.getBody() : "{}");
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("success", !resp.has("error"));
            out.put("raw", objectMapper.convertValue(resp, Map.class));
            return out;
        } catch (Exception e) {
            return fail("ElementPay 상태 조회 실패", "ELEMENTPAY_STATUS_FAILED");
        }
    }

    public Optional<PgAgency> resolveAgencyByMerchantKey(String merchantKey) {
        if (merchantKey == null || merchantKey.isBlank()) {
            return Optional.empty();
        }
        return pgAgencyRepository.findByMerchantMidOrderByIdAsc(merchantKey.trim()).stream()
                .filter(a -> PgVendor.isElementPayFamily(a.getPgCd()))
                .findFirst();
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
            m.put("key", n.path("key").asText(""));
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
        if (agency.getEndpointApi() != null && !agency.getEndpointApi().isBlank()) {
            return trimSlash(agency.getEndpointApi());
        }
        return cred.sandbox() ? SANDBOX_BASE : LIVE_BASE;
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

    private static Map<String, Object> fail(String message, String code) {
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
