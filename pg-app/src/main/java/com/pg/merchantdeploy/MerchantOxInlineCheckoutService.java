package com.pg.merchantdeploy;

import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.service.ChillPayService;
import com.pg.service.MerchantChatbotProductService;
import com.pg.service.OrgServiceUseService;
import com.pg.service.UrlPayDisplayFxService;
import com.pg.urlpay.IcipayBuyerContactUtil;
import com.pg.urlpay.NeutralCheckoutRoute;
import com.pg.util.PgTrnsctnOrderLookup;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 가맹 통합 API — ox 인라인·리다이렉트·Woo가 공통으로 쓰는 prepare.
 * 카드 승인(Create Payment)은 OxPay 상세 스펙 전까지 호출하지 않습니다.
 * 응답에는 ICOPAY만 노출합니다.
 */
@Service
public class MerchantOxInlineCheckoutService {

    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final OrgServiceUseService orgServiceUseService;
    private final MerchantChatbotProductService productService;
    private final MerchantInlineCheckoutTokenService tokenService;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final MerchantApiIntegrationChannelService integrationChannelService;
    private final ChillPayService chillPayService;
    private final UrlPayDisplayFxService urlPayDisplayFxService;
    private final MerchantCheckoutPrepareCurrencyService prepareCurrencyService;

    public MerchantOxInlineCheckoutService(OrgUnitRepository orgUnitRepository,
                                           MerchantProfileRepository merchantProfileRepository,
                                           OrgServiceUseService orgServiceUseService,
                                           MerchantChatbotProductService productService,
                                           MerchantInlineCheckoutTokenService tokenService,
                                           PgTrnsctnRepository pgTrnsctnRepository,
                                           MerchantApiIntegrationChannelService integrationChannelService,
                                           ChillPayService chillPayService,
                                           UrlPayDisplayFxService urlPayDisplayFxService,
                                           MerchantCheckoutPrepareCurrencyService prepareCurrencyService) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.orgServiceUseService = orgServiceUseService;
        this.productService = productService;
        this.tokenService = tokenService;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.integrationChannelService = integrationChannelService;
        this.chillPayService = chillPayService;
        this.urlPayDisplayFxService = urlPayDisplayFxService;
        this.prepareCurrencyService = prepareCurrencyService;
    }

    public Map<String, Object> prepare(Long orgUnitId, Map<String, Object> body, HttpServletRequest request) {
        if (orgUnitId == null) {
            return fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND");
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
        if (profOpt.isPresent() && !orgServiceUseService.isWebPaymentActive(orgUnitId)) {
            return fail(OrgServiceUseService.MSG_WEB_PAYMENT_DISABLED, "WEB_PAYMENT_DISABLED");
        }
        String opPg = chillPayService.resolveUrlPayOperationalPgCd(orgUnitId);
        if (opPg == null || !PgVendor.isOxFamily(opPg)) {
            return fail("URL 결제(운영) 바인딩이 없습니다.", "URL_PAYMENT_PG_MISSING");
        }
        Optional<String> inlineDeny = integrationChannelService.denyMessage(orgUnitId,
                MerchantApiIntegrationChannelService.Channel.API_BROKER_INLINE);
        if (inlineDeny.isPresent()) {
            return fail(inlineDeny.get(), MerchantApiIntegrationChannelService.CODE_INTEGRATION_CHANNEL_DISABLED);
        }

        String orderNo = str(body.get("orderNo"));
        if (orderNo.isBlank()) {
            return fail("orderNo가 필요합니다.", "INVALID_ORDER_NO");
        }
        BigDecimal amount = parseAmount(body.get("amount"));
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return fail("유효한 amount가 필요합니다.", "INVALID_AMOUNT");
        }
        String settle = urlPayDisplayFxService.settlementCurrencyForPg(opPg);
        MerchantCheckoutPrepareCurrencyService.Resolved curResolved =
                prepareCurrencyService.resolveWithFixedSettlement(orgUnitId, str(body.get("currency")), settle);
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
                ? tokenService.issueWithBuyerPrefill(MerchantPgBrokerVendor.OX, ou.getCode(), orderNo,
                amountPlain, currency, productName, buyerPrefillJson)
                : tokenService.issue(MerchantPgBrokerVendor.OX, ou.getCode(), orderNo,
                amountPlain, currency, productName);
        if (tokenService.parseValid(sessionToken, MerchantPgBrokerVendor.OX).isEmpty()) {
            return fail("세션 토큰 생성에 실패했습니다.", "SESSION_ERROR");
        }
        String base = trimSlash(productService.resolvePublicCustomerSiteBase(request));
        Map<String, Object> data = new LinkedHashMap<>();
        tokenService.parseValid(sessionToken, MerchantPgBrokerVendor.OX)
                .ifPresent(s -> data.putAll(s.toPublicMap()));
        data.put("sessionToken", sessionToken);
        data.put("payUrl", NeutralCheckoutRoute.buildPayUrl(base, ou.getCode(), sessionToken, langCode, true));
        data.put("embedScriptUrl", (base.isEmpty() ? "" : base) + "/v1/embed-checkout/" + ou.getCode());
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
            return fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND");
        }
        Optional<OrgUnit> ouOpt = orgUnitRepository.findById(orgUnitId);
        if (ouOpt.isEmpty()) {
            return fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND");
        }
        String orderNo = orderNoRaw != null ? orderNoRaw.trim() : "";
        if (orderNo.isBlank()) {
            return fail("orderNo가 필요합니다.", "INVALID_ORDER_NO");
        }
        String mid = ouOpt.get().getCode();
        Optional<PgTrnsctn> txn = PgTrnsctnOrderLookup.findPreferredByMerchantAndOrder(
                pgTrnsctnRepository, mid, orderNo);
        if (txn.isPresent() && !PgVendor.isOxFamily(txn.get().getVan())) {
            txn = Optional.empty();
        }
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
            data.put("amount", t.getAmtKrw());
            data.put("currency", t.getCurType());
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("data", data);
        return out;
    }

    public Map<String, Object> status(Long orgUnitId, Map<String, Object> body) {
        String orderNo = body != null && body.get("orderNo") != null ? String.valueOf(body.get("orderNo")) : "";
        return orderStatus(orgUnitId, orderNo);
    }

    private static String mapPaymentStatus(String status) {
        if (status == null || status.isBlank()) {
            return "UNKNOWN";
        }
        return switch (status.trim()) {
            case "10", "00" -> "PAID";
            case "08" -> "PENDING";
            case "99", "F0" -> "FAILED";
            case "30", "42" -> "REFUNDED";
            default -> status.trim();
        };
    }

    private static BigDecimal parseAmount(Object v) {
        if (v == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(v).replace(",", "").trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static String str(Object v) {
        return v == null ? "" : String.valueOf(v).trim();
    }

    private static String clamp(String s, int max) {
        if (s == null || s.length() <= max) {
            return s == null ? "" : s;
        }
        return s.substring(0, max);
    }

    private static String trimSlash(String s) {
        if (s == null) {
            return "";
        }
        while (s.endsWith("/")) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private static Map<String, Object> fail(String message, String code) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", false);
        out.put("message", message);
        out.put("errorCode", code);
        return out;
    }
}
