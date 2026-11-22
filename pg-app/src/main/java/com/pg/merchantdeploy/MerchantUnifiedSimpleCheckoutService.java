package com.pg.merchantdeploy;

import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.service.ChillPayService;
import com.pg.service.MerchantChatbotProductService;
import com.pg.service.UrlPayDisplayFxService;
import com.pg.urlpay.NeutralCheckoutRoute;
import com.pg.urlpay.SimplePayCheckoutCopyService;
import com.pg.urlpay.SimplePayBrandResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 간편결제(QR URL) — 기존 통합 REDIRECT prepare 를 재사용하고 payUrl 만 확인·QR 페이지로 교체.
 * 기존 redirect/inline prepare 경로는 수정하지 않는다.
 */
@Service
public class MerchantUnifiedSimpleCheckoutService {

    private final MerchantUnifiedRedirectCheckoutService redirectCheckoutService;
    private final MerchantApiIntegrationChannelService integrationChannelService;
    private final MerchantChatbotProductService productService;
    private final MerchantInlineCheckoutTokenService tokenService;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final ChillPayService chillPayService;
    private final UrlPayDisplayFxService urlPayDisplayFxService;
    private final SimplePayCheckoutCopyService simplePayCheckoutCopyService;
    private final SimplePayBrandResolver simplePayBrandResolver;

    public MerchantUnifiedSimpleCheckoutService(MerchantUnifiedRedirectCheckoutService redirectCheckoutService,
                                                MerchantApiIntegrationChannelService integrationChannelService,
                                                MerchantChatbotProductService productService,
                                                MerchantInlineCheckoutTokenService tokenService,
                                                PgTrnsctnRepository pgTrnsctnRepository,
                                                OrgUnitRepository orgUnitRepository,
                                                MerchantProfileRepository merchantProfileRepository,
                                                ChillPayService chillPayService,
                                                UrlPayDisplayFxService urlPayDisplayFxService,
                                                SimplePayCheckoutCopyService simplePayCheckoutCopyService,
                                                SimplePayBrandResolver simplePayBrandResolver) {
        this.redirectCheckoutService = redirectCheckoutService;
        this.integrationChannelService = integrationChannelService;
        this.productService = productService;
        this.tokenService = tokenService;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.chillPayService = chillPayService;
        this.urlPayDisplayFxService = urlPayDisplayFxService;
        this.simplePayCheckoutCopyService = simplePayCheckoutCopyService;
        this.simplePayBrandResolver = simplePayBrandResolver;
    }

    public Map<String, Object> prepare(Long orgUnitId, Map<String, Object> body, HttpServletRequest request) {
        Optional<String> deny = integrationChannelService.denyMessage(orgUnitId,
                MerchantApiIntegrationChannelService.Channel.API_BROKER_SIMPLE_QR);
        if (deny.isPresent()) {
            return fail(deny.get(), MerchantApiIntegrationChannelService.CODE_INTEGRATION_CHANNEL_DISABLED);
        }
        Map<String, Object> result = redirectCheckoutService.prepare(orgUnitId, body, request);
        patchSimplePrepareResponse(result, request);
        enrichConfirmUi(result);
        return result;
    }

    public Map<String, Object> readSession(String token) {
        Map<String, Object> result = redirectCheckoutService.readSession(token);
        enrichConfirmUi(result);
        return result;
    }

    public Map<String, Object> orderStatus(Long orgUnitId, String orderNo) {
        return redirectCheckoutService.orderStatus(orgUnitId, orderNo);
    }

    /**
     * PC QR 대기용 — 세션 토큰만으로 주문 승인 여부 조회(브로커 시크릿 불필요).
     */
    public Map<String, Object> publicStatusBySession(String token) {
        Optional<MerchantInlineCheckoutTokenService.SessionPayload> opt = tokenService.parseValid(token);
        if (opt.isEmpty()) {
            return fail("세션이 유효하지 않거나 만료되었습니다.", "INVALID_SESSION");
        }
        MerchantInlineCheckoutTokenService.SessionPayload s = opt.get();
        String orderNo = s.orderNo() != null ? s.orderNo().trim() : "";
        String compId = s.compId() != null ? s.compId().trim() : "";
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderNo", orderNo);
        data.put("compId", compId);
        data.put("status", "READY");
        data.put("paid", false);
        if (!orderNo.isBlank()) {
            List<PgTrnsctn> list = pgTrnsctnRepository.findByOrderNoOrderByCreatedAtDesc(orderNo);
            for (PgTrnsctn t : list) {
                if (t == null) {
                    continue;
                }
                if (!compId.isBlank() && t.getMerchantId() != null
                        && !compId.equalsIgnoreCase(t.getMerchantId().trim())) {
                    continue;
                }
                String st = t.getStatus() != null ? t.getStatus().trim().toUpperCase(Locale.ROOT) : "";
                if (st.contains("PAID") || st.contains("SUCCESS") || "0000".equals(st) || "SO".equals(st)
                        || st.contains("APPROV")) {
                    data.put("status", "PAID");
                    data.put("paid", true);
                    break;
                }
                if (!st.isBlank()) {
                    data.put("status", st);
                }
                break;
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("data", data);
        return out;
    }

    @SuppressWarnings("unchecked")
    private void patchSimplePrepareResponse(Map<String, Object> result, HttpServletRequest request) {
        Object ok = result.get("success");
        if (!(ok instanceof Boolean) || !(Boolean) ok) {
            return;
        }
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        if (data == null) {
            return;
        }
        String base = trimSlash(productService.resolvePublicCustomerSiteBase(request));
        String compId = str(data.get("compId"));
        String sessionToken = str(data.get("sessionToken"));
        String lang = str(data.get("langCode"));
        if (lang.isBlank()) {
            lang = str(data.get("lang"));
        }
        if (!compId.isBlank() && !sessionToken.isBlank()) {
            String simpleUrl = NeutralCheckoutRoute.buildSimplePayUrl(base, compId, sessionToken, lang);
            data.put("payUrl", simpleUrl);
            data.put("checkoutUrl", simpleUrl);
            data.put("qrPayloadUrl", simpleUrl);
            /* 실제 카드·HPP 진입은 기존 중립 checkout (세션 잠금 유지) */
            data.put("chargePayUrl", NeutralCheckoutRoute.buildPayUrl(base, compId, sessionToken, lang, false));
        }
        data.put("simpleCheckoutPrepareUrl",
                base + "/api/middleware/v1/merchant/checkout/simple/prepare");
        data.put("pgVendor", MerchantApiResponseMapper.MERCHANT_FACING_BRAND);
        data.put("integrationMode", "SIMPLE_QR");
        data.put("redirectUsageHint",
                "ICOPAY 간편결제(QR URL): buyer·amount·orderNo 필수. payUrl 은 확인·QR 페이지. "
                        + "결제는 기존 URL/RE 결제·NOTI 경로. returnUrl body 금지.");
    }

    /**
     * 확인·QR UI용: 필드 노출 플래그 + DP 이면 표시금액·청구금액.
     */
    @SuppressWarnings("unchecked")
    private void enrichConfirmUi(Map<String, Object> result) {
        Object ok = result.get("success");
        if (!(ok instanceof Boolean) || !(Boolean) ok) {
            return;
        }
        Map<String, Object> data = (Map<String, Object>) result.get("data");
        if (data == null) {
            return;
        }
        String compId = str(data.get("compId"));
        if (compId.isBlank()) {
            return;
        }
        Optional<OrgUnit> ouOpt = orgUnitRepository.findByCode(compId);
        if (ouOpt.isEmpty()) {
            return;
        }
        Long orgUnitId = ouOpt.get().getId();
        MerchantProfile mp = merchantProfileRepository.findByOrgUnitId(orgUnitId).orElse(null);
        data.put("simpleQrShowOrderNoYn", yn(mp != null ? mp.getSimpleQrShowOrderNoYn() : "Y"));
        data.put("simpleQrShowProductYn", yn(mp != null ? mp.getSimpleQrShowProductYn() : "Y"));
        data.put("simpleQrShowNameYn", yn(mp != null ? mp.getSimpleQrShowNameYn() : "Y"));
        data.put("simpleQrShowEmailYn", yn(mp != null ? mp.getSimpleQrShowEmailYn() : "Y"));
        data.put("simpleQrShowPhoneYn", yn(mp != null ? mp.getSimpleQrShowPhoneYn() : "Y"));
        /* RE 상·하단 안내와 동일 설정 — /pay-simple 에서도 노출·문구 공유 */
        simplePayCheckoutCopyService.putIntoCheckoutMap(data, orgUnitId);
        simplePayBrandResolver.putIntoCheckoutMap(data, orgUnitId);

        boolean dp = chillPayService.merchantAllowsDisplayFx(orgUnitId);
        if (!dp) {
            if (data.get("urlPayPricingMode") == null) {
                data.put("urlPayPricingMode", "CHECKOUT_CURRENCY");
            }
            return;
        }
        String dispCur = str(data.get("displayCurrency"));
        if (dispCur.isBlank()) {
            dispCur = str(data.get("currency"));
        }
        BigDecimal dispAmt = parseAmount(data.get("amount"));
        if (dispCur.isBlank() || dispAmt == null || dispAmt.compareTo(BigDecimal.ZERO) <= 0) {
            data.put("urlPayPricingMode", UrlPayDisplayFxService.MODE_DISPLAY_FX_THB);
            return;
        }
        String opPg = chillPayService.resolveUrlPayDisplayFxQuotePgCd(orgUnitId);
        if (opPg == null || opPg.isBlank()) {
            opPg = chillPayService.resolveUrlPayOperationalPgCd(orgUnitId);
        }
        try {
            Optional<UrlPayDisplayFxService.QuoteResult> q =
                    urlPayDisplayFxService.buildQuote(compId, dispCur, opPg);
            if (q.isEmpty()) {
                data.put("urlPayPricingMode", UrlPayDisplayFxService.MODE_DISPLAY_FX_THB);
                String settleHint = urlPayDisplayFxService.settlementCurrencyForPg(opPg);
                if (settleHint != null && !settleHint.isBlank()) {
                    data.put("settlementCurrencyHint", settleHint);
                    data.put("displayCurrency", dispCur.toUpperCase(Locale.ROOT));
                }
                return;
            }
            UrlPayDisplayFxService.QuoteResult qr = q.get();
            UrlPayDisplayFxService.FxComputedSettlement fx = urlPayDisplayFxService.computeSettlementFromQuote(
                    compId, dispCur, dispAmt, qr.quoteToken(), opPg);
            data.put("urlPayPricingMode", UrlPayDisplayFxService.MODE_DISPLAY_FX_THB);
            data.put("displayCurrency", dispCur.toUpperCase(Locale.ROOT));
            data.put("currency", dispCur.toUpperCase(Locale.ROOT));
            data.put("settlementCurrency", fx.settlementCurrency());
            data.put("settlementCurrencyHint", fx.settlementCurrency());
            data.put("settlementAmount", fx.amount().stripTrailingZeros().toPlainString());
            data.put("fxQuoteToken", qr.quoteToken());
        } catch (Exception ignore) {
            data.put("urlPayPricingMode", UrlPayDisplayFxService.MODE_DISPLAY_FX_THB);
            String settleHint = urlPayDisplayFxService.settlementCurrencyForPg(opPg);
            if (settleHint != null && !settleHint.isBlank()) {
                data.put("settlementCurrencyHint", settleHint);
                data.put("displayCurrency", dispCur.toUpperCase(Locale.ROOT));
            }
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
        return o == null ? "" : o.toString().trim();
    }

    private static String yn(String v) {
        return v != null && "Y".equalsIgnoreCase(v.trim()) ? "Y" : "N";
    }

    private static BigDecimal parseAmount(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            String s = raw.toString().replace(",", "").trim();
            if (s.isEmpty()) {
                return null;
            }
            return new BigDecimal(s);
        } catch (Exception e) {
            return null;
        }
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
