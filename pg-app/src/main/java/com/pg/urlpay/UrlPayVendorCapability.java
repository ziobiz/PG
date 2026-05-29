package com.pg.urlpay;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 결제대행사(PG)별 URL 결제 플랫폼 기능 선언.
 * <p><b>본사 URL결제설정</b>(표시통화·결제통화·결제문구·폼 모드)은 PG와 무관하게 동일 적용되고,
 * 이 레코드는 <b>PG API로만 가능한 기능</b>(재결제 URL·인라인 위젯·승인 채널)을 명시합니다.
 *
 * @param vendorFamily       {@link com.pg.integration.pg.PgVendor} 계열 식별자
 * @param operationalPgCd    운영 바인딩 {@code pg_cd}
 * @param inlineWidgetKind   {@link UrlPayInlineWidgetKind}
 * @param checkoutPagePath   공개 결제 페이지 경로 접두 (예: {@code /pay/}, {@code /jpay-pay/})
 * @param embedPagePath      가맹 API iframe 경로 접두
 * @param saleChannel        승인 API 라우팅
 * @param urlPayIntegration  {@code tb_pg_agency.integ_url_pay_yn=Y}
 * @param urlRepayIntegration {@code tb_pg_agency.integ_url_pay_repay_yn=Y} (PG API 재결제 지원 시에만 유효)
 * @param repayUrlEnabled    운영·본사 설정 + PG 재결제 API 지원 모두 충족 시 {@code true}
 */
public record UrlPayVendorCapability(
        String vendorFamily,
        String operationalPgCd,
        String inlineWidgetKind,
        String checkoutPagePath,
        String embedPagePath,
        UrlPaySaleChannel saleChannel,
        boolean urlPayIntegration,
        boolean urlRepayIntegration,
        boolean repayUrlEnabled) {

    /** JSON( checkout-context )용 맵 — 관리자·프론트 공통 계약 */
    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("vendorFamily", vendorFamily);
        m.put("operationalPgCd", operationalPgCd);
        m.put("inlineWidgetKind", inlineWidgetKind);
        m.put("checkoutPagePath", checkoutPagePath);
        m.put("embedPagePath", embedPagePath);
        m.put("saleChannel", saleChannel.name());
        m.put("urlPayIntegration", urlPayIntegration);
        m.put("urlRepayIntegration", urlRepayIntegration);
        m.put("repayUrlEnabled", repayUrlEnabled);
        m.put("supportsDisplayFx", true);
        m.put("supportsMultiLanguage", true);
        m.put("supportsHqUrlPayFormMode", true);
        m.put("supportsHqCardCopy", true);
        return m;
    }

    public boolean supportsInlineCheckoutPage() {
        return UrlPayInlineWidgetKind.CHILLPAY_CCD.equals(inlineWidgetKind)
                || UrlPayInlineWidgetKind.JPAY_INLINE.equals(inlineWidgetKind);
    }
}
