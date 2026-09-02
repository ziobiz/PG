package com.pg.merchantdeploy;

import com.pg.service.ChillPayService;
import com.pg.service.UrlPayCheckoutCurrencyService;
import com.pg.service.UrlPayDisplayFxService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 가맹 prepare API — 전 PG 공통 표시통화(DP)·실결제 통화 해석.
 * <p>
 * 본사 URL결제설정에서 PG {@code amountMode}=DISPLAY|BLIND 이면 prepare {@code currency}는
 * <strong>표시통화</strong>이고, 실결제 통화는 PG {@code settlementCurrency}(THB·USD·JPY 등)입니다.
 * STANDARD(일반)이면 기존 체크아웃 통화 규칙을 따릅니다.
 */
@Service
public class MerchantCheckoutPrepareCurrencyService {

    public record Resolved(
            String sessionCurrency,
            boolean displayFx,
            String settlementCurrencyHint,
            String failMessage,
            String failCode) {

        public boolean ok() {
            return failCode == null || failCode.isBlank();
        }

        public static Resolved fail(String message, String code) {
            return new Resolved(null, false, null, message, code);
        }

        public static Resolved of(String sessionCurrency, boolean displayFx, String settlementHint) {
            return new Resolved(sessionCurrency, displayFx, settlementHint, null, null);
        }
    }

    private final ChillPayService chillPayService;
    private final UrlPayDisplayFxService urlPayDisplayFxService;
    private final UrlPayCheckoutCurrencyService urlPayCheckoutCurrencyService;

    public MerchantCheckoutPrepareCurrencyService(ChillPayService chillPayService,
                                                    UrlPayDisplayFxService urlPayDisplayFxService,
                                                    UrlPayCheckoutCurrencyService urlPayCheckoutCurrencyService) {
        this.chillPayService = chillPayService;
        this.urlPayDisplayFxService = urlPayDisplayFxService;
        this.urlPayCheckoutCurrencyService = urlPayCheckoutCurrencyService;
    }

    /**
     * DP 가맹이면 표시통화, 아니면 {@code nonDpCurrency} 공급값.
     */
    public Resolved resolve(Long orgUnitId, String bodyCurrency, Supplier<String> nonDpCurrency) {
        if (orgUnitId == null) {
            return Resolved.fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND");
        }
        if (chillPayService.merchantAllowsDisplayFx(orgUnitId)) {
            String fxPg = chillPayService.resolveUrlPayDisplayFxQuotePgCd(orgUnitId);
            String cur = bodyCurrency != null ? bodyCurrency.trim().toUpperCase(Locale.ROOT) : "";
            if (cur.isEmpty()) {
                cur = urlPayDisplayFxService.defaultDisplayCurrencyForPg(fxPg);
            }
            if (!urlPayDisplayFxService.isAllowedDisplayCurrency(cur)) {
                return Resolved.fail("지원하지 않는 표시 통화입니다.", "INVALID_DISPLAY_CURRENCY");
            }
            String settle = urlPayDisplayFxService.settlementCurrencyForPg(fxPg);
            return Resolved.of(cur, true, settle);
        }
        String cur = nonDpCurrency != null ? nonDpCurrency.get() : null;
        if (cur == null || cur.isBlank()) {
            cur = urlPayCheckoutCurrencyService.resolveCheckoutCurrency(orgUnitId, bodyCurrency);
        }
        return Resolved.of(cur.trim().toUpperCase(Locale.ROOT), false, null);
    }

    /**
     * ElementPay 등 PG 고유 실결제 통화가 있는 경우 — 비 DP 시 body는 실결제 통화만 허용.
     */
    public Resolved resolveWithFixedSettlement(Long orgUnitId, String bodyCurrency, String settlementCurrency) {
        String settleRaw = settlementCurrency != null ? settlementCurrency.trim().toUpperCase(Locale.ROOT) : "";
        final String settle = (settleRaw.isEmpty() || !urlPayDisplayFxService.isAllowedSettlementCurrency(settleRaw))
                ? "THB"
                : settleRaw;
        if (chillPayService.merchantAllowsDisplayFx(orgUnitId)) {
            return resolve(orgUnitId, bodyCurrency, () -> settle);
        }
        String body = bodyCurrency != null ? bodyCurrency.trim().toUpperCase(Locale.ROOT) : "";
        if (!body.isEmpty() && !settle.equals(body)) {
            return Resolved.fail(
                    "실결제 통화는 " + settle + " 입니다. DISPLAY/BLIND(DP) 설정 시 표시통화(JPY 등)를 사용하세요.",
                    "ELEMENTPAY_SETTLEMENT_CURRENCY");
        }
        return Resolved.of(settle, false, settle);
    }

    /** 조직 체인 기준 일반 체크아웃 통화(비 DP). */
    public Resolved resolveOrgCheckout(Long orgUnitId, String bodyCurrency) {
        return resolve(orgUnitId, bodyCurrency,
                () -> urlPayCheckoutCurrencyService.resolveCheckoutCurrency(orgUnitId, bodyCurrency));
    }

    /** prepare 성공 data 에 공통 금액모드 필드 부여(전 PG 동일 키). */
    public void putPublicFields(Map<String, Object> data, Resolved r) {
        if (data == null || r == null || !r.ok()) {
            return;
        }
        data.put("currency", r.sessionCurrency());
        data.put("urlPayPricingMode", r.displayFx()
                ? UrlPayDisplayFxService.MODE_DISPLAY_FX_THB
                : "CHECKOUT_CURRENCY");
        if (r.displayFx()) {
            data.put("displayCurrency", r.sessionCurrency());
            if (r.settlementCurrencyHint() != null && !r.settlementCurrencyHint().isBlank()) {
                data.put("settlementCurrencyHint", r.settlementCurrencyHint());
            }
        }
    }

    public Map<String, Object> failMap(Resolved r) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", false);
        out.put("message", r.failMessage() != null ? r.failMessage() : "INVALID_CURRENCY");
        out.put("errorCode", r.failCode() != null ? r.failCode() : "INVALID_CURRENCY");
        return out;
    }
}
