package com.pg.merchantdeploy;

import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.service.IlkPaymentService;
import com.pg.service.MerchantChatbotProductService;
import com.pg.service.OrgServiceUseService;
import com.pg.urlpay.CheckoutFailI18n;
import com.pg.urlpay.IcipayBuyerContactUtil;
import com.pg.urlpay.NeutralCheckoutRoute;
import com.pg.util.PgTrnsctnOrderLookup;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** 가맹점 통합 API — ILK 인라인 카드 결제창 세션 준비. */
@Service
public class MerchantIlkInlineCheckoutService {

    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final OrgServiceUseService orgServiceUseService;
    private final IlkPaymentService ilkPaymentService;
    private final MerchantChatbotProductService productService;
    private final MerchantInlineCheckoutTokenService tokenService;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final MerchantApiIntegrationChannelService integrationChannelService;
    private final MerchantCheckoutPrepareCurrencyService prepareCurrencyService;

    public MerchantIlkInlineCheckoutService(OrgUnitRepository orgUnitRepository,
                                            MerchantProfileRepository merchantProfileRepository,
                                            OrgServiceUseService orgServiceUseService,
                                            IlkPaymentService ilkPaymentService,
                                            MerchantChatbotProductService productService,
                                            MerchantInlineCheckoutTokenService tokenService,
                                            PgTrnsctnRepository pgTrnsctnRepository,
                                            MerchantApiIntegrationChannelService integrationChannelService,
                                            MerchantCheckoutPrepareCurrencyService prepareCurrencyService) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.orgServiceUseService = orgServiceUseService;
        this.ilkPaymentService = ilkPaymentService;
        this.productService = productService;
        this.tokenService = tokenService;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.integrationChannelService = integrationChannelService;
        this.prepareCurrencyService = prepareCurrencyService;
    }

    public Map<String, Object> prepare(Long orgUnitId, Map<String, Object> body, HttpServletRequest request) {
        if (orgUnitId == null) {
            return CheckoutFailI18n.merchantNotFound();
        }
        Optional<OrgUnit> ouOpt = orgUnitRepository.findById(orgUnitId);
        if (ouOpt.isEmpty()) {
            return CheckoutFailI18n.merchantNotFound();
        }
        OrgUnit ou = ouOpt.get();
        if (!orgServiceUseService.isOrgServiceActive(orgUnitId)) {
            return fail(OrgServiceUseService.MSG_ORG_SERVICE_DISABLED, "ORG_DISABLED");
        }
        Optional<MerchantProfile> profOpt = merchantProfileRepository.findByOrgUnitId(orgUnitId);
        if (profOpt.isPresent() && !orgServiceUseService.isWebPaymentActive(orgUnitId)) {
            return fail(OrgServiceUseService.MSG_WEB_PAYMENT_DISABLED, "WEB_PAYMENT_DISABLED");
        }
        if (!ilkPaymentService.hasOperationalWebBinding(orgUnitId)) {
            return CheckoutFailI18n.urlPayPgMissing();
        }
        Optional<String> inlineDeny = integrationChannelService.denyMessage(orgUnitId,
                MerchantApiIntegrationChannelService.Channel.API_BROKER_INLINE);
        if (inlineDeny.isPresent()) {
            return fail(inlineDeny.get(), MerchantApiIntegrationChannelService.CODE_INTEGRATION_CHANNEL_DISABLED);
        }

        String orderNo = normalizeOrderNo(str(body.get("orderNo")));
        if (orderNo.isBlank()) {
            return CheckoutFailI18n.invalidOrderNo();
        }
        BigDecimal amount = parseAmount(body.get("amount"));
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return CheckoutFailI18n.invalidAmount();
        }
        MerchantCheckoutPrepareCurrencyService.Resolved curResolved =
                prepareCurrencyService.resolve(orgUnitId, str(body.get("currency")), () -> {
                    String c = str(body.get("currency"));
                    return c.isBlank() ? "KRW" : c.trim().toUpperCase(java.util.Locale.ROOT);
                });
        if (!curResolved.ok()) {
            return prepareCurrencyService.failMap(curResolved);
        }
        String currency = curResolved.sessionCurrency();
        String amountPlain = amount.stripTrailingZeros().toPlainString();
        String productName = clamp(str(body.get("productName")), 500);
        if (productName.isBlank()) {
            productName = clamp(str(body.get("item")), 500);
        }
        String langCode = MerchantCheckoutLangUtil.fromBody(body);

        String buyerPrefillJson;
        try {
            buyerPrefillJson = IcipayBuyerContactUtil.resolvePrefillJsonFromBodyOptional(body);
        } catch (IllegalArgumentException ex) {
            return fail(ex.getMessage(), "BUYER_PREFILL_INVALID");
        }

        String sessionToken = buyerPrefillJson != null && !buyerPrefillJson.isBlank()
                ? tokenService.issueWithBuyerPrefill(MerchantPgBrokerVendor.ILK, ou.getCode(), orderNo,
                amountPlain, currency, productName, buyerPrefillJson)
                : tokenService.issue(MerchantPgBrokerVendor.ILK, ou.getCode(), orderNo,
                amountPlain, currency, productName);
        Optional<MerchantInlineCheckoutTokenService.SessionPayload> parsed =
                tokenService.parseValid(sessionToken, MerchantPgBrokerVendor.ILK);
        if (parsed.isEmpty()) {
            return CheckoutFailI18n.sessionError();
        }
        MerchantInlineCheckoutTokenService.SessionPayload session = parsed.get();

        String base = trimSlash(productService.resolvePublicCustomerSiteBase(request));
        String payUrl = NeutralCheckoutRoute.buildPayUrl(base, ou.getCode(), sessionToken, langCode, true);
        String embedScriptUrl = (base.isEmpty() ? "" : base) + "/v1/embed-checkout/" + urlEnc(ou.getCode());

        Map<String, Object> data = new LinkedHashMap<>();
        data.putAll(session.toPublicMap());
        data.put("sessionToken", sessionToken);
        data.put("payUrl", payUrl);
        data.put("embedScriptUrl", embedScriptUrl);
        data.put("integrationMode", "INLINE");
        data.put("pgVendor", MerchantApiResponseMapper.MERCHANT_FACING_BRAND);
        prepareCurrencyService.putPublicFields(data, curResolved);
        if (langCode != null && !langCode.isBlank()) {
            data.put("langCode", langCode);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("data", data);
        return out;
    }

    public Map<String, Object> orderStatus(Long orgUnitId, String orderNoRaw) {
        if (orgUnitId == null) {
            return CheckoutFailI18n.merchantNotFound();
        }
        Optional<OrgUnit> ouOpt = orgUnitRepository.findById(orgUnitId);
        if (ouOpt.isEmpty()) {
            return CheckoutFailI18n.merchantNotFound();
        }
        String orderNo = normalizeOrderNo(orderNoRaw);
        if (orderNo.isBlank()) {
            return CheckoutFailI18n.invalidOrderNo();
        }
        String mid = ouOpt.get().getCode();
        Optional<PgTrnsctn> txn = findIlkTxnByOrder(mid, orderNo);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("compId", mid);
        data.put("orderNo", orderNo);
        data.put("pgVendor", MerchantApiResponseMapper.MERCHANT_FACING_BRAND);
        if (txn.isEmpty()) {
            data.put("found", false);
            data.put("paymentStatus", "NOT_FOUND");
        } else {
            PgTrnsctn t = txn.get();
            data.put("found", true);
            data.put("paymentStatus", mapPaymentStatus(t.getStatus()));
            data.put("transactionId", t.getTrnId());
            data.put("approvalNo", t.getApprovalNo());
            data.put("amount", t.getAmtKrw());
            data.put("currency", t.getCurType());
            data.put("paidAt", t.getPaidAt() != null ? t.getPaidAt().toString() : null);
            data.put("origin", t.getOrigin());
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("data", data);
        return out;
    }

    private Optional<PgTrnsctn> findIlkTxnByOrder(String merchantId, String orderNo) {
        Optional<PgTrnsctn> t = PgTrnsctnOrderLookup.findPreferredByMerchantAndOrder(
                pgTrnsctnRepository, merchantId, orderNo);
        if (t.isPresent() && PgVendor.isIlkFamily(t.get().getVan())) {
            return t;
        }
        return Optional.empty();
    }

    private static String mapPaymentStatus(String status) {
        if (status == null) {
            return "UNKNOWN";
        }
        return switch (status.trim()) {
            case "10" -> "PAID";
            case "08" -> "PENDING";
            case "20", "30" -> "CANCELLED";
            case "99" -> "FAILED";
            default -> "UNKNOWN";
        };
    }

    private static Map<String, Object> fail(String message, String code) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", false);
        out.put("message", message);
        out.put("errorCode", code);
        return out;
    }

    private static String normalizeOrderNo(String raw) {
        return raw != null ? raw.trim() : "";
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

    private static String str(Object o) {
        return o != null ? o.toString().trim() : "";
    }

    private static String clamp(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String trimSlash(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().replaceAll("/+$", "");
    }

    private static String urlEnc(String s) {
        try {
            return java.net.URLEncoder.encode(s != null ? s : "", java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s != null ? s : "";
        }
    }
}
