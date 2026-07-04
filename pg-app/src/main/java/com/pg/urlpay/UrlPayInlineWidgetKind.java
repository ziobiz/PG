package com.pg.urlpay;

/**
 * 공개 URL 결제 페이지 인라인 카드 UI 종류.
 * <p>프론트 {@code pay.html} / {@code jpay-pay.html} / 향후 PG 전용 페이지는
 * {@link UrlPayVendorCapabilityRegistry} 가 반환하는 값으로 분기합니다.
 */
public final class UrlPayInlineWidgetKind {

    /** ChillPay CCD 인라인 위젯 */
    public static final String CHILLPAY_CCD = "CHILLPAY_CCD";
    /** JPAY 서버 프록시 카드 폼({@code jpay-pay.html}) */
    public static final String JPAY_INLINE = "JPAY_INLINE";
    /** Eximbay JS SDK 결제창({@code eximbay-pay.html}) — 카드정보 미보유(호스티드) */
    public static final String EXIMBAY_SDK = "EXIMBAY_SDK";
    /** 본사 REDIRECT 플로우 — 카드 인라인 없음 */
    public static final String REDIRECT_ONLY = "REDIRECT_ONLY";
    /** 인라인 미구현 — 안내 배너만 */
    public static final String UNSUPPORTED_INLINE = "UNSUPPORTED_INLINE";

    private UrlPayInlineWidgetKind() {
    }
}
