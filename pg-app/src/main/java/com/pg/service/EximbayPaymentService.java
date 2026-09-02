package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgAgency;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.middleware.notify.PgNotifyIngressPaths;
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.urlpay.CheckoutFailI18n;
import com.pg.urlpay.PayerContextCapture;
import com.pg.util.MerchantPgCredentialUtil;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Eximbay(엑심베이) 결제 서비스 — {@code /v1/payments/ready} 로 FGKey 를 생성해 JS SDK 결제창을 호출한다.
 *
 * <p>플로우: 프론트가 주문/결제수단을 보내면 → ready(FGKey) → SDK 결제창(카드·PayPay·UnionPay·WeChat·
 * Alipay·GrabPay·LinePay·ApplePay 등) → status_url(webhook) 로 최종 결과 수신 →
 * {@link EximbayNotifyToTrnsctnService} 가 거래를 확정한다.
 *
 * <p><b>가맹 정보 보호:</b> Eximbay 로 나가는 {@code return_url}·{@code status_url} 은
 * {@link PgOutboundUrlPolicy}·전사 ingress 로 <b>항상 우리(ICOPAY) 도메인</b> 만 전송한다. 가맹점 도메인은 전달하지 않는다.
 */
@Service
public class EximbayPaymentService {

    private static final Logger log = LoggerFactory.getLogger(EximbayPaymentService.class);

    private static final String SANDBOX_BASE = "https://api-test.eximbay.com";
    private static final String LIVE_BASE = "https://api.eximbay.com";
    private static final String READY_PATH = "/v1/payments/ready";
    private static final String PAYMENTS_PATH = "/v1/payments";
    private static final String VERIFY_PATH = "/v1/payments/verify";
    private static final String RETRIEVE_PATH = "/v1/payments/retrieve";
    private static final String CANCEL_PATH_PREFIX = "/v1/payments/";
    private static final DateTimeFormatter ORDER_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final PgAgencyRepository pgAgencyRepository;
    private final MerchantPgBindingRepository merchantPgBindingRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final HqNotifyEnvService hqNotifyEnvService;
    private final EximbaySaleRecordService eximbaySaleRecordService;
    private final PayPresaleRiskFilterService payPresaleRiskFilterService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public EximbayPaymentService(PgAgencyRepository pgAgencyRepository,
                                 MerchantPgBindingRepository merchantPgBindingRepository,
                                 OrgUnitRepository orgUnitRepository,
                                 HqNotifyEnvService hqNotifyEnvService,
                                 EximbaySaleRecordService eximbaySaleRecordService,
                                 PayPresaleRiskFilterService payPresaleRiskFilterService) {
        this.pgAgencyRepository = pgAgencyRepository;
        this.merchantPgBindingRepository = merchantPgBindingRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.eximbaySaleRecordService = eximbaySaleRecordService;
        this.payPresaleRiskFilterService = payPresaleRiskFilterService;
    }

    /** 운영 Eximbay WEB 바인딩 존재 여부 — checkout-context 노출·구독 게이트에 사용. */
    public boolean hasOperationalWebBinding(Long orgUnitId) {
        return findOperationalEximbayBinding(orgUnitId).isPresent();
    }

    /**
     * URL 결제 승인 채널({@link com.pg.urlpay.UrlPaySaleChannel#EXIMBAY_READY_SALE}) 진입점.
     * ready 호출로 FGKey 를 만들고, 프론트가 SDK 결제창을 열도록 파라미터를 반환한다.
     */
    public Map<String, Object> executeReady(Long orgUnitId, Map<String, Object> body,
                                            HttpServletRequest req, String clientIp) {
        return doReady(orgUnitId, body, req, clientIp, false, null);
    }

    /** 구독(정기결제) 등록 — token_creation=Y + recurring. 최초 결제창에서 토큰을 만들고 1차 결제를 수행한다. */
    public Map<String, Object> executeSubscriptionReady(Long orgUnitId, Map<String, Object> body,
                                                        HttpServletRequest req, String clientIp,
                                                        Map<String, Object> subscriptionPlan) {
        return doReady(orgUnitId, body, req, clientIp, true, subscriptionPlan);
    }

    private Map<String, Object> doReady(Long orgUnitId, Map<String, Object> body,
                                        HttpServletRequest req, String clientIp,
                                        boolean subscription, Map<String, Object> plan) {
        Optional<MerchantPgBinding> bindOpt = findOperationalEximbayBinding(orgUnitId);
        if (bindOpt.isEmpty()) {
            return fail("Eximbay 운영 바인딩이 없습니다.", "EXIMBAY_PG_MISSING");
        }
        MerchantPgBinding binding = bindOpt.get();
        Optional<PgAgency> agOpt = pgAgencyRepository.findByPgCd(binding.getPgCd() != null ? binding.getPgCd().trim() : "");
        if (agOpt.isEmpty()) {
            return fail("Eximbay 결제대행사(PG) 설정을 찾을 수 없습니다.", "EXIMBAY_AGENCY_MISSING");
        }
        PgAgency agency = agOpt.get();
        MerchantPgCredentialUtil.Resolved cred = MerchantPgCredentialUtil.resolve(binding, agency);
        String secretKey = cred.apiKey();
        String mid = cred.mid();
        if (secretKey.isEmpty() || mid.isEmpty()) {
            return fail("Eximbay MID/Secret Key 가 설정되지 않았습니다. (가맹 MID+Key 쌍 또는 본사 PG 연동)", "EXIMBAY_CREDENTIALS_MISSING");
        }

        // 결제 전 고객 컨텍스트(IP·UA 등) 적재 — 사전 리스크 필터의 IP 속도 판정에도 사용된다.
        PayerContextCapture.enrichSaleBody(body, req, clientIp);

        Optional<OrgUnit> ou = orgUnitRepository.findById(orgUnitId);
        String compCode = ou.map(OrgUnit::getCode).orElse("");

        String orderNo = str(body.get("orderNo"));
        if (orderNo.isBlank()) {
            orderNo = "EXB" + LocalDateTime.now().format(ORDER_FMT);
        }
        BigDecimal amountBd = parseAmount(body.get("amount"));
        if (amountBd == null || amountBd.compareTo(BigDecimal.ZERO) <= 0) {
            return fail("결제 금액이 올바르지 않습니다.", "INVALID_AMOUNT");
        }
        String currency = str(body.get("currency"));
        if (currency.isBlank()) {
            currency = "USD";
        }
        currency = currency.trim().toUpperCase(Locale.ROOT);
        String lang = eximbayLang(str(body.get("langCode")));
        String buyerName = firstNonBlank(str(body.get("buyerName")),
                joinName(str(body.get("payFirstname")), str(body.get("payLastname"))), "Customer");
        String buyerEmail = firstNonBlank(str(body.get("payEmailAddress")), str(body.get("email")));
        String buyerPhone = firstNonBlank(str(body.get("payTelephone")), str(body.get("phone")));
        String productName = firstNonBlank(str(body.get("item")), str(body.get("productName")), "Order");

        Map<String, String> methodOverrides = readMethodOverrides(agency);
        String methodKey = EximbayPaymentMethodCatalog.normalizeKey(str(body.get("paymentMethod")));
        String methodCode = EximbayPaymentMethodCatalog.resolveCode(methodKey, methodOverrides);
        boolean paypay = EximbayPaymentMethodCatalog.KEY_PAYPAY.equals(methodKey);
        boolean jpBank = EximbayPaymentMethodCatalog.KEY_JPCONVBANK.equals(methodKey);
        boolean unionPay = EximbayPaymentMethodCatalog.KEY_UNIONPAY.equals(methodKey);
        boolean jpWallet = paypay || jpBank;
        boolean cardHosted = methodKey.isEmpty() || EximbayPaymentMethodCatalog.KEY_CARD.equals(methodKey);
        boolean hostedWallet = paypay || jpBank || unionPay || cardHosted;
        boolean sandbox = "Y".equalsIgnoreCase(trimToEmpty(agency.getSandboxYn()));
        if (methodCode.isBlank()) {
            if (paypay) {
                methodCode = "P201";
            } else if (jpBank) {
                methodCode = "P006";
            } else if (unionPay) {
                methodCode = "P002";
            } else if (cardHosted) {
                methodCode = "P000";
            }
        }
        if (jpWallet) {
            if (sandbox) {
                if (!"JPY".equals(currency)) {
                    currency = "JPY";
                }
                amountBd = amountBd.setScale(0, RoundingMode.DOWN);
                if (amountBd.compareTo(BigDecimal.ZERO) <= 0) {
                    return CheckoutFailI18n.fail("EXIMBAY_JPY_WHOLE_YEN",
                            "JPY 결제는 소수점 없이 엔 단위로 입력하세요.",
                            "JPY amounts must be whole yen (no decimals).",
                            "JPYは小数なしの円単位で入力してください。",
                            "日元金额须为整数（不可有小数）。",
                            "ยอด JPY ต้องเป็นจำนวนเต็มเยน");
                }
            } else {
                if (!"JPY".equals(currency)) {
                    return CheckoutFailI18n.fail("EXIMBAY_PAYPAY_JPY_REQUIRED",
                            "PayPay·일본 편의점·은행 결제는 JPY 금액으로만 결제할 수 있습니다.",
                            "PayPay and Japan convenience-store/bank payments require JPY.",
                            "PayPay・コンビニ・銀行決済はJPYのみです。",
                            "PayPay/日本便利店/银行支付仅支持日元(JPY)。",
                            "PayPay และการชำระร้านสะดวกซื้อ/ธนาคารญี่ปุ่นใช้ได้เฉพาะ JPY");
                }
                if (amountBd.stripTrailingZeros().scale() > 0) {
                    return CheckoutFailI18n.fail("EXIMBAY_JPY_WHOLE_YEN",
                            "JPY 결제는 소수점 없이 엔 단위로 입력하세요.",
                            "JPY amounts must be whole yen (no decimals).",
                            "JPYは小数なしの円単位で入力してください。",
                            "日元金额须为整数（不可有小数）。",
                            "ยอด JPY ต้องเป็นจำนวนเต็มเยน");
                }
            }
        }
        if (buyerEmail.isBlank()) {
            return CheckoutFailI18n.fail("EXIMBAY_EMAIL_REQUIRED",
                    "이메일은 필수입니다.",
                    "Email is required.",
                    "メールアドレスは必須です。",
                    "邮箱为必填。",
                    "ต้องระบุอีเมล");
        }
        String displayType = firstNonBlank(str(body.get("displayType")), resolveExtra(agency, "eximbayDisplayType"),
                hostedWallet ? "P" : "R");
        displayType = "P".equalsIgnoreCase(displayType) ? "P" : "R";
        if (hostedWallet) {
            displayType = "P";
        }

        // 사전 리스크 필터 — 샌드박스는 테스트 카드·반복 이메일로 막히지 않게 생략.
        String txnOrigin = subscription ? "SUBSCRIPTION" : str(body.get("txnOrigin"));
        if (!sandbox) {
        Optional<PayPresaleRiskFilterService.PresaleRiskBlock> presaleRisk =
                payPresaleRiskFilterService.evaluate(orgUnitId, compCode, PgVendor.EXIMBAY, body);
        if (presaleRisk.isPresent()) {
            return presaleRiskBlockOut(presaleRisk.get(), orgUnitId, compCode, orderNo, txnOrigin,
                    amountBd, currency, binding.getSortOrder(), productName, buyerName, buyerEmail,
                    methodKey.isBlank() ? "EXIMBAY" : methodKey, subscription);
            }
        }

        String publicBase = resolvePublicApiBase(req);
        String ingressToken = hqNotifyEnvService.getOrCreate().getIngressToken();
        // 가맹 도메인 절대 미전송 — 항상 우리 도메인 ingress/result 페이지.
        // 토큰 전용 ingress 경로(대상코드 없음) 를 사용해 별도 노티대상 등록 없이 벤더 스니핑 디스패치로 처리한다.
        String statusUrl = PgNotifyIngressPaths.buildIngressBase(publicBase, ingressToken);
        // 가맹점 식별정보 절대 미전송: 복귀 URL 에도 가맹점 코드(m=)를 넣지 않는다.
        // 복귀 페이지는 Eximbay 가 붙여 돌려주는 결과(order_id·rescode)만으로 결과를 표시하고,
        // 가맹점 매핑·매출 확정은 서버(webhook)가 order_id 로 우리 대기거래를 역추적해 처리한다.
        String returnUrl = PgOutboundUrlPolicy.enforceOwnDomain(
                publicBase + "/eximbay-pay.html?eximbayReturn=1", publicBase, publicBase);

        Map<String, Object> payment = new LinkedHashMap<>();
        // tokenbilling(구독)·일반 결제 모두 PAYMENT 로 승인+매입까지 자동 처리. AUTHORIZE 는 수동 매입 계약이 필요.
        payment.put("transaction_type", "PAYMENT");
        payment.put("order_id", orderNo);
        payment.put("currency", currency);
        payment.put("amount", amountBd.stripTrailingZeros().toPlainString());
        payment.put("lang", lang);
        if (!methodCode.isBlank()) {
            payment.put("payment_method", methodCode);
        }

        Map<String, Object> merchant = new LinkedHashMap<>();
        merchant.put("mid", mid);

        Map<String, Object> buyer = new LinkedHashMap<>();
        buyer.put("name", buyerName);
        if (!buyerEmail.isBlank()) {
            buyer.put("email", buyerEmail);
        }
        if (!buyerPhone.isBlank()) {
            buyer.put("phone_number", buyerPhone);
        }

        Map<String, Object> url = new LinkedHashMap<>();
        url.put("return_url", returnUrl);
        url.put("status_url", statusUrl);

        // other_param(가맹점 코드·플랫폼 표식) 미전송 — Eximbay 에는 "우리 가맹점이 누구인지" 를 절대 알리지 않는다.
        // 가맹점 식별·구독 여부는 우리가 ready 시 적재한 대기거래(order_id 기준)로만 복원한다.

        Map<String, Object> settings = new LinkedHashMap<>();
        settings.put("display_type", displayType);
        settings.put("autoclose", "Y");
        settings.put("ostype", resolveOstype(req));
        settings.put("call_from_app", "N");

        Map<String, Object> reqBody = new LinkedHashMap<>();
        reqBody.put("payment", payment);
        reqBody.put("merchant", merchant);
        reqBody.put("buyer", buyer);
        reqBody.put("url", url);
        reqBody.put("settings", settings);
        reqBody.put("product", buildProductLine(productName, amountBd, publicBase));

        if (subscription) {
            Map<String, Object> tokenbilling = new LinkedHashMap<>();
            tokenbilling.put("token_creation", "Y");
            tokenbilling.put("unique_token_id", "Y");
            reqBody.put("tokenbilling", tokenbilling);
            Map<String, Object> recurring = buildRecurring(plan, amountBd, statusUrl);
            if (!recurring.isEmpty()) {
                reqBody.put("recurring", recurring);
            }
        }

        // 대기 거래 적재
        eximbaySaleRecordService.recordOrTouchPending(orgUnitId, orderNo, amountBd, currency,
                binding.getSortOrder(), productName, txnOrigin,
                buyerName, buyerEmail, methodKey.isBlank() ? "EXIMBAY" : methodKey, null, null, subscription);

        String base = resolveBase(agency);
        JsonNode resp;
        try {
            resp = postJson(base + READY_PATH, secretKey, reqBody);
        } catch (Exception e) {
            log.warn("Eximbay ready 호출 실패 orderNo={} method={}: {}", orderNo, methodCode, e.getMessage());
            return fail("Eximbay 결제준비 호출에 실패했습니다.", "EXIMBAY_READY_FAILED");
        }
        String rescode = text(resp, "rescode");
        String resmsg = text(resp, "resmsg");
        String fgkey = text(resp, "fgkey");
        if (!"0000".equals(rescode) || fgkey.isBlank()) {
            log.warn("Eximbay ready 거부 orderNo={} method={} rescode={} resmsg={}",
                    orderNo, methodCode, rescode, resmsg);
            return fail(resmsg.isBlank() ? "Eximbay 결제준비에 실패했습니다." : resmsg,
                    rescode.isBlank() ? "EXIMBAY_READY_ERROR" : "EXIMBAY_" + rescode);
        }

        // SDK request_pay 는 /ready 요청과 파라미터가 100% 일치해야 fgkey 검증을 통과한다.
        // 따라서 ready 로 보낸 payload 그대로 + fgkey 를 프론트에 돌려주고, 프론트는 그대로 EXIMBAY.request_pay 에 전달한다.
        Map<String, Object> sdkRequest = new LinkedHashMap<>();
        sdkRequest.put("fgkey", fgkey);
        for (Map.Entry<String, Object> e : reqBody.entrySet()) {
            sdkRequest.put(e.getKey(), e.getValue());
        }

        JsonNode payLaunch = null;
        try {
            payLaunch = postJsonNoAuth(base + PAYMENTS_PATH, sdkRequest);
        } catch (Exception e) {
            log.warn("Eximbay payments(no-auth) 실패 orderNo={}: {}", orderNo, e.getMessage());
        }
        if (payLaunch == null || extractHostedPayUrl(payLaunch, base).isBlank()) {
            try {
                payLaunch = postJson(base + PAYMENTS_PATH, secretKey, sdkRequest);
            } catch (Exception e) {
                log.warn("Eximbay payments 호출 실패 orderNo={}: {}", orderNo, e.getMessage());
                if (payLaunch == null) {
                    return fail("결제창을 열지 못했습니다.", "EXIMBAY_PAYMENTS_FAILED");
                }
            }
        }
        if (payLaunch == null) {
            return fail("결제창을 열지 못했습니다.", "EXIMBAY_PAYMENTS_FAILED");
        }
        String payRes = text(payLaunch, "rescode");
        if (!payRes.isBlank() && !"0000".equals(payRes)) {
            String payMsg = text(payLaunch, "resmsg");
            log.warn("Eximbay payments 거부 orderNo={} rescode={} resmsg={}", orderNo, payRes, payMsg);
            return fail(payMsg.isBlank() ? "결제창을 열지 못했습니다." : payMsg, "EXIMBAY_" + payRes);
        }
        String hostedUrl = extractHostedPayUrl(payLaunch, base);
        if (hostedUrl.isBlank() || isOwnCheckoutHost(hostedUrl)) {
            log.warn("Eximbay 결제 URL 비정상 orderNo={} url={} res={}",
                    orderNo, hostedUrl, payLaunch);
            return CheckoutFailI18n.fail("EXIMBAY_HOSTED_URL_INVALID",
                    "결제창 주소를 받지 못했습니다. 잠시 후 다시 시도하세요.",
                    "Could not open the payment window. Please try again.",
                    "決済画面のURLを取得できませんでした。しばらくして再度お試しください。",
                    "未能取得支付窗口地址，请稍后重试。",
                    "ไม่ได้รับที่อยู่หน้าต่างชำระเงิน โปรดลองอีกครั้ง");
        }
        Map<String, String> hostedFields = extractHostedPayFields(payLaunch);
        String hostedDisplay = firstNonBlank(text(payLaunch, "display_type"), displayType);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("pgVendor", "EXIMBAY");
        out.put("fgkey", fgkey);
        out.put("orderNo", orderNo);
        out.put("amount", amountBd.stripTrailingZeros().toPlainString());
        out.put("currency", currency);
        out.put("paymentMethod", methodCode);
        out.put("displayType", hostedDisplay);
        out.put("sdkScriptUrl", resolveSdkScriptUrl(agency));
        out.put("returnUrl", returnUrl);
        out.put("mid", mid);
        out.put("subscription", subscription);
        out.put("sdkRequest", sdkRequest);
        out.put("hostedPayUrl", hostedUrl);
        out.put("hostedPayFields", hostedFields);
        out.put("hostedDisplayType", hostedDisplay);
        return out;
    }

    /**
     * SDK/status_url 결과(쿼리스트링)의 위·변조 검증 — {@code /v1/payments/verify}.
     * 요청 본문은 {@code {"data": "<status_url 로 받은 원본 쿼리스트링>"}} 이며, Eximbay 가 fgkey 로 무결성을 재검증한다.
     */
    public Map<String, Object> verify(String mid, String data) {
        Map<String, Object> out = new LinkedHashMap<>();
        Optional<PgAgency> agOpt = resolveAgencyByMid(mid);
        if (agOpt.isEmpty()) {
            out.put("success", false);
            out.put("errorCode", "EXIMBAY_AGENCY_MISSING");
            return out;
        }
        String secretKey = trimToEmpty(agOpt.get().getApiKey());
        String base = resolveBase(agOpt.get());
        try {
            Map<String, Object> reqBody = new LinkedHashMap<>();
            reqBody.put("data", data);
            JsonNode resp = postJson(base + VERIFY_PATH, secretKey, reqBody);
            String rescode = text(resp, "rescode");
            out.put("success", "0000".equals(rescode));
            out.put("rescode", rescode);
            out.put("resmsg", text(resp, "resmsg"));
            return out;
        } catch (Exception e) {
            log.warn("Eximbay verify 실패 mid={}: {}", mid, e.getMessage());
            out.put("success", false);
            out.put("errorCode", "EXIMBAY_VERIFY_FAILED");
            return out;
        }
    }

    /**
     * 결제내역 자동/강제환불 — Eximbay {@code POST /v1/payments/{transaction_id}/cancel} 전액(F).
     */
    public String requestCancel(PgTrnsctn t, String reason) {
        if (t == null || !PgVendor.isEximbayFamily(t.getVan())) {
            throw new IllegalStateException("해당 거래는 취소 API를 호출할 수 없습니다.");
        }
        String transactionId = t.getChillTransactionId();
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalStateException("거래번호(transaction_id)가 없어 취소 API를 호출할 수 없습니다.");
        }
        PgAgency agency = resolveAgencyForTxn(t);
        Optional<MerchantPgBinding> bind = findOperationalEximbayBinding(resolveMerchantOrgUnitId(t));
        MerchantPgCredentialUtil.Resolved cred = MerchantPgCredentialUtil.resolve(bind.orElse(null), agency);
        String mid = cred.mid();
        String secretKey = cred.apiKey();
        if (mid.isEmpty() || secretKey.isEmpty()) {
            throw new IllegalStateException("결제대행사 MID/Secret Key 가 설정되지 않았습니다.");
        }
        String amount = formatAmount(t.getAmtKrw());
        if (amount.isBlank()) {
            throw new IllegalStateException("취소할 거래 금액이 없습니다.");
        }
        String currency = t.getCurType() != null && !t.getCurType().isBlank()
                ? t.getCurType().trim().toUpperCase(Locale.ROOT) : "USD";
        String orderId = t.getOrderNo() != null ? t.getOrderNo().trim() : "";
        if (orderId.isBlank()) {
            throw new IllegalStateException("주문번호가 없어 취소 API를 호출할 수 없습니다.");
        }
        String refundReason = reason != null && !reason.isBlank() ? reason.trim() : "icopay refund";
        if (refundReason.length() > 255) {
            refundReason = refundReason.substring(0, 255);
        }
        String refundId = "RF" + (t.getTrnId() != null ? t.getTrnId().trim() : "")
                + Long.toString(System.currentTimeMillis() % 1000000000L, 36);
        if (refundId.length() > 40) {
            refundId = refundId.substring(0, 40);
        }
        Map<String, Object> refund = new LinkedHashMap<>();
        refund.put("refund_type", "F");
        refund.put("refund_amount", amount);
        refund.put("refund_id", refundId);
        refund.put("reason", refundReason);
        Map<String, Object> payment = new LinkedHashMap<>();
        payment.put("order_id", orderId);
        payment.put("currency", currency);
        payment.put("amount", amount);
        payment.put("balance", amount);
        payment.put("lang", "EN");
        Map<String, Object> reqBody = new LinkedHashMap<>();
        reqBody.put("mid", mid);
        reqBody.put("refund", refund);
        reqBody.put("payment", payment);
        String url = resolveBase(agency) + CANCEL_PATH_PREFIX + enc(transactionId.trim()) + "/cancel";
        JsonNode resp;
        try {
            resp = postJson(url, secretKey, reqBody);
        } catch (Exception e) {
            log.warn("Eximbay cancel HTTP 실패 txnId={}: {}", transactionId, e.getMessage());
            throw new IllegalStateException("결제 취소 요청에 실패했습니다: " + e.getMessage());
        }
        String rescode = text(resp, "rescode");
        String resmsg = text(resp, "resmsg");
        if (!"0000".equals(rescode)) {
            throw new IllegalStateException("결제 취소가 거부되었습니다"
                    + (!rescode.isBlank() ? " (" + rescode + ")" : "")
                    + (!resmsg.isBlank() ? ": " + resmsg : ""));
        }
        String summary = "Eximbay cancel OK txn=" + transactionId
                + (!rescode.isBlank() ? " rescode=" + rescode : "");
        log.info("Eximbay cancel OK orderNo={} txnId={}", orderId, transactionId);
        return summary;
    }

    private PgAgency resolveAgencyForTxn(PgTrnsctn t) {
        String van = t.getVan() != null ? t.getVan().trim() : "";
        if (!van.isBlank()) {
            Optional<PgAgency> byCd = pgAgencyRepository.findByPgCd(van);
            if (byCd.isPresent()) {
                return byCd.get();
            }
        }
        long ouId = resolveMerchantOrgUnitId(t);
        return findOperationalEximbayBinding(ouId)
                .flatMap(b -> pgAgencyRepository.findByPgCd(trimToEmpty(b.getPgCd())))
                .orElseThrow(() -> new IllegalStateException("결제대행사 설정을 찾을 수 없습니다."));
    }

    private long resolveMerchantOrgUnitId(PgTrnsctn t) {
        String code = t.getMerchantId();
        if (code == null || code.isBlank()) {
            throw new IllegalStateException("거래에 가맹점 코드가 없습니다.");
        }
        return orgUnitRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new IllegalStateException("가맹점(조직)을 찾을 수 없습니다: " + code.trim()))
                .getId();
    }

    private static String formatAmount(BigDecimal amt) {
        if (amt == null) {
            return "";
        }
        return amt.stripTrailingZeros().toPlainString();
    }

    /** 거래 조회 — {@code /v1/payments/retrieve}. */
    public JsonNode retrieve(String mid, String transactionId) {
        Optional<PgAgency> agOpt = resolveAgencyByMid(mid);
        if (agOpt.isEmpty()) {
            return null;
        }
        try {
            Map<String, Object> reqBody = new LinkedHashMap<>();
            reqBody.put("mid", mid);
            reqBody.put("transaction_id", transactionId);
            return postJson(resolveBase(agOpt.get()) + RETRIEVE_PATH, trimToEmpty(agOpt.get().getApiKey()), reqBody);
        } catch (Exception e) {
            log.warn("Eximbay retrieve 실패: {}", e.getMessage());
            return null;
        }
    }

    /** 정기결제 재청구 — {@code /v1/payments/tokenbilling/{token_id}/rebill}. */
    public Map<String, Object> rebill(Long orgUnitId, String tokenId, String orderNo,
                                      BigDecimal amount, String currency) {
        Optional<MerchantPgBinding> bindOpt = findOperationalEximbayBinding(orgUnitId);
        if (bindOpt.isEmpty()) {
            return fail("Eximbay 운영 바인딩이 없습니다.", "EXIMBAY_PG_MISSING");
        }
        Optional<PgAgency> agOpt = pgAgencyRepository.findByPgCd(bindOpt.get().getPgCd().trim());
        if (agOpt.isEmpty()) {
            return fail("Eximbay 결제대행사 설정 없음", "EXIMBAY_AGENCY_MISSING");
        }
        String mid = firstNonBlank(bindOpt.get().getMid(), agOpt.get().getMerchantMid());
        String cur = currency != null && !currency.isBlank() ? currency.trim().toUpperCase(Locale.ROOT) : "USD";
        try {
            Map<String, Object> payment = new LinkedHashMap<>();
            payment.put("transaction_type", "REBILL");
            payment.put("order_id", orderNo);
            payment.put("currency", cur);
            payment.put("amount", amount.stripTrailingZeros().toPlainString());
            Map<String, Object> merchant = new LinkedHashMap<>();
            merchant.put("mid", mid);
            Map<String, Object> reqBody = new LinkedHashMap<>();
            reqBody.put("payment", payment);
            reqBody.put("merchant", merchant);
            String url = resolveBase(agOpt.get()) + "/v1/payments/tokenbilling/" + enc(tokenId) + "/rebill";
            JsonNode resp = postJson(url, trimToEmpty(agOpt.get().getApiKey()), reqBody);
            String rescode = text(resp, "rescode");
            boolean ok = "0000".equals(rescode);
            String txnId = text(resp, "transaction_id");
            if (ou(orgUnitId).isPresent()) {
                eximbaySaleRecordService.recordOrTouchPending(orgUnitId, orderNo, amount, cur,
                        bindOpt.get().getSortOrder(), "REBILL", "SUBSCRIPTION", null, null, "EXIMBAY", null, null, true);
                eximbaySaleRecordService.applyOutcome(ou(orgUnitId).get().getCode(), orderNo, ok, txnId,
                        text(resp, "resmsg"), "SUBSCRIPTION", null);
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("success", ok);
            out.put("rescode", rescode);
            out.put("resmsg", text(resp, "resmsg"));
            out.put("transactionId", txnId);
            return out;
        } catch (Exception e) {
            log.warn("Eximbay rebill 실패 token={}: {}", tokenId, e.getMessage());
            return fail("Eximbay 재청구 실패", "EXIMBAY_REBILL_FAILED");
        }
    }

    /* ================= 내부 헬퍼 ================= */

    /** 사전 리스크 필터 차단 — 대기 거래 적재 후 취소(20) + 이벤트 기록, 프론트에 다국어 사유 반환. */
    private Map<String, Object> presaleRiskBlockOut(PayPresaleRiskFilterService.PresaleRiskBlock block,
                                                    Long orgUnitId, String merchantCode, String orderNo,
                                                    String txnOrigin, BigDecimal amountBd, String currency,
                                                    Integer routeNo, String productName, String buyerName,
                                                    String buyerEmail, String channel, boolean subscription) {
        eximbaySaleRecordService.recordOrTouchPending(orgUnitId, orderNo, amountBd, currency,
                routeNo, productName, txnOrigin, buyerName, buyerEmail, channel, null, null, subscription);
        String trnId = eximbaySaleRecordService.applyIcopayPresaleRiskCancel(
                merchantCode, orderNo, txnOrigin, block.message());
        payPresaleRiskFilterService.recordEvent(orgUnitId, merchantCode, orderNo, trnId, PgVendor.EXIMBAY, block);
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

    private Optional<OrgUnit> ou(Long orgUnitId) {
        return orgUnitId == null ? Optional.empty() : orgUnitRepository.findById(orgUnitId);
    }

    private Optional<MerchantPgBinding> findOperationalEximbayBinding(Long orgUnitId) {
        if (orgUnitId == null) {
            return Optional.empty();
        }
        List<MerchantPgBinding> list = merchantPgBindingRepository.findByOrgUnitIdOrderBySortOrderAsc(orgUnitId);
        return list.stream()
                .filter(b -> PgVendor.isEximbayFamily(b.getPgCd()))
                .filter(b -> !"N".equalsIgnoreCase(trimToEmpty(b.getActivationYn())))
                .filter(b -> "Y".equalsIgnoreCase(trimToEmpty(b.getOperationalYn())))
                .filter(b -> {
                    String pm = trimToEmpty(b.getPayMethod());
                    return pm.isEmpty() || "WEB".equalsIgnoreCase(pm) || "APM".equalsIgnoreCase(pm);
                })
                .findFirst();
    }

    private List<Map<String, String>> buildProductLine(String productName, BigDecimal amount, String publicBase) {
        String name = productName == null || productName.isBlank() ? "Order" : productName.trim();
        if (name.length() > 127) {
            name = name.substring(0, 127);
        }
        String link = publicBase != null && !publicBase.isBlank()
                ? publicBase.replaceAll("/+$", "") + "/"
                : "https://icopay.co.kr/";
        Map<String, String> line = new LinkedHashMap<>();
        line.put("name", name);
        line.put("quantity", "1");
        line.put("unit_price", amount.stripTrailingZeros().toPlainString());
        line.put("link", link);
        List<Map<String, String>> product = new ArrayList<>();
        product.add(line);
        return product;
    }

    private static String resolveOstype(HttpServletRequest req) {
        String ua = req != null ? str(req.getHeader("User-Agent")) : "";
        if (ua.matches("(?i).*(iphone|ipad|ipod|android|mobile|silk|opera mini|webos).*")) {
            return "M";
        }
        return "P";
    }

    private Optional<PgAgency> resolveAgencyByMid(String mid) {
        if (mid == null || mid.isBlank()) {
            return Optional.empty();
        }
        return pgAgencyRepository.findByMerchantMidOrderByIdAsc(mid.trim()).stream()
                .filter(a -> PgVendor.isEximbayFamily(a.getPgCd()))
                .findFirst();
    }

    private Map<String, Object> buildRecurring(Map<String, Object> plan, BigDecimal amount, String notiUrl) {
        Map<String, Object> recurring = new LinkedHashMap<>();
        if (plan == null) {
            return recurring;
        }
        Object amt = plan.get("recurring_amount");
        recurring.put("recurring_amount", amt != null ? amt.toString() : amount.stripTrailingZeros().toPlainString());
        if (plan.get("recurring_start_date") != null) {
            recurring.put("recurring_start_date", plan.get("recurring_start_date").toString());
        }
        if (plan.get("recurring_interval") != null) {
            recurring.put("recurring_interval", plan.get("recurring_interval").toString());
        }
        recurring.put("recurring_noti_url", notiUrl);
        if (plan.get("remind_email_interval") != null) {
            recurring.put("remind_email_interval", plan.get("remind_email_interval").toString());
        }
        return recurring;
    }

    private JsonNode postJson(String url, String secretKey, Map<String, Object> body) throws Exception {
        return postJson(url, secretKey, body, true);
    }

    /** JS SDK 와 동일 — /v1/payments 런칭은 Basic 인증 없이 JSON POST. */
    private JsonNode postJsonNoAuth(String url, Map<String, Object> body) throws Exception {
        return postJson(url, "", body, false);
    }

    private JsonNode postJson(String url, String secretKey, Map<String, Object> body, boolean basicAuth) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (basicAuth && secretKey != null && !secretKey.isBlank()) {
        headers.setBasicAuth(secretKey, "");
        }
        String json = objectMapper.writeValueAsString(body);
        HttpEntity<String> entity = new HttpEntity<>(json, headers);
        ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
        String raw = resp.getBody() != null ? resp.getBody() : "{}";
        return objectMapper.readTree(raw);
    }

    private String resolveBase(PgAgency agency) {
        String extra = resolveExtra(agency, "eximbayApiBase");
        if (!extra.isBlank()) {
            return extra.replaceAll("/+$", "");
        }
        if ("Y".equalsIgnoreCase(trimToEmpty(agency.getSandboxYn()))) {
            return SANDBOX_BASE;
        }
        String ep = firstNonBlank(agency.getEndpointUrlPay(), agency.getEndpointApi(), agency.getApiEndpoint());
        if (!ep.isBlank() && ep.toLowerCase(Locale.ROOT).startsWith("http")) {
            return ep.replaceAll("/+$", "");
        }
        return LIVE_BASE;
    }

    private String extractHostedPayUrl(JsonNode payLaunch, String apiBase) {
        if (payLaunch == null || payLaunch.isNull()) {
            return "";
        }
        String raw = firstNonBlank(
                text(payLaunch, "url"),
                text(payLaunch, "payment_url"),
                text(payLaunch, "redirect_url"));
        JsonNode urlNode = payLaunch.get("url");
        if (raw.isBlank() && urlNode != null && urlNode.isObject()) {
            raw = firstNonBlank(text(urlNode, "payment"), text(urlNode, "redirect"), text(urlNode, "href"));
        }
        return toAbsoluteUrl(apiBase, raw);
    }

    private Map<String, String> extractHostedPayFields(JsonNode payLaunch) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (payLaunch == null || !payLaunch.isObject()) {
            return fields;
        }
        payLaunch.fields().forEachRemaining(e -> {
            String k = e.getKey();
            if (k == null) {
                return;
            }
            if ("url".equals(k) || "display_type".equals(k) || "rescode".equals(k) || "resmsg".equals(k)) {
                return;
            }
            JsonNode v = e.getValue();
            if (v == null || v.isNull() || v.isObject() || v.isArray()) {
                return;
            }
            fields.put(k, v.asText(""));
        });
        fields.putIfAbsent("param3", "OPENAPI");
        return fields;
    }

    private static String toAbsoluteUrl(String apiBase, String raw) {
        String u = trimToEmpty(raw);
        if (u.isEmpty()) {
            return "";
        }
        if (u.startsWith("//")) {
            return "https:" + u;
        }
        if (u.startsWith("http://") || u.startsWith("https://")) {
            return u;
        }
        String b = apiBase != null ? apiBase.replaceAll("/+$", "") : "";
        if (u.startsWith("/")) {
            return b + u;
        }
        return b + "/" + u;
    }

    private static boolean isOwnCheckoutHost(String url) {
        String u = url != null ? url.toLowerCase(Locale.ROOT) : "";
        return u.contains("icopay.co.kr") || u.contains("ontheline");
    }

    private String resolveSdkScriptUrl(PgAgency agency) {
        String extra = resolveExtra(agency, "eximbaySdkUrl");
        if (!extra.isBlank()) {
            return extra.trim();
        }
        String base = resolveBase(agency);
        // Eximbay JavaScript SDK v2 (샌드박스: api-test, 운영: api). EXIMBAY.request_pay 제공.
        return base + "/v2/javascriptSDK.js";
    }

    private Map<String, String> readMethodOverrides(PgAgency agency) {
        Map<String, String> out = new LinkedHashMap<>();
        String raw = trimToEmpty(agency.getCredentialsExtraJson());
        if (raw.isEmpty()) {
            return out;
        }
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode codes = root.get("eximbayMethodCodes");
            if (codes != null && codes.isObject()) {
                codes.fields().forEachRemaining(e ->
                        out.put(EximbayPaymentMethodCatalog.normalizeKey(e.getKey()), e.getValue().asText("")));
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private String resolveExtra(PgAgency agency, String key) {
        String raw = trimToEmpty(agency.getCredentialsExtraJson());
        if (raw.isEmpty()) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode v = root.get(key);
            return v != null && !v.isNull() ? v.asText("") : "";
        } catch (Exception e) {
            return "";
        }
    }

    private String resolvePublicApiBase(HttpServletRequest req) {
        String base = hqNotifyEnvService.getOrCreate().getPublicBaseUrl();
        if (base != null && !base.isBlank()) {
            return base.trim().replaceAll("/+$", "");
        }
        if (req != null) {
            String scheme = firstNonBlank(req.getHeader("X-Forwarded-Proto"), req.getScheme());
            String host = firstNonBlank(req.getHeader("X-Forwarded-Host"), req.getServerName());
            if (req.getHeader("X-Forwarded-Host") == null || req.getHeader("X-Forwarded-Host").isBlank()) {
                int port = req.getServerPort();
                if (("http".equalsIgnoreCase(scheme) && port != 80) || ("https".equalsIgnoreCase(scheme) && port != 443)) {
                    host = host + ":" + port;
                }
            }
            return (scheme + "://" + host).replaceAll("/+$", "");
        }
        return "";
    }

    private static String eximbayLang(String uiLang) {
        String u = uiLang != null ? uiLang.trim().toUpperCase(Locale.ROOT) : "";
        return switch (u) {
            case "KOR", "KO", "KR" -> "KO";
            case "JPN", "JA", "JP" -> "JP";
            case "CHN", "ZH", "CN" -> "ZH";
            case "THA", "TH" -> "TH";
            default -> "EN";
        };
    }

    private static BigDecimal parseAmount(Object v) {
        if (v == null) {
            return null;
        }
        try {
            return new BigDecimal(v.toString().trim().replace(",", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private static String joinName(String first, String last) {
        String f = first != null ? first.trim() : "";
        String l = last != null ? last.trim() : "";
        return (f + " " + l).trim();
    }

    private static String enc(String s) {
        return java.net.URLEncoder.encode(s != null ? s : "", java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String text(JsonNode node, String field) {
        if (node == null) {
            return "";
        }
        JsonNode v = node.get(field);
        return v != null && !v.isNull() ? v.asText("") : "";
    }

    private static String str(Object v) {
        return v != null ? v.toString().trim() : "";
    }

    private static String trimToEmpty(String s) {
        return s != null ? s.trim() : "";
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

    private static Map<String, Object> fail(String message, String code) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", false);
        out.put("message", message);
        out.put("errorCode", code != null ? code : "EXIMBAY_ERROR");
        return out;
    }
}
