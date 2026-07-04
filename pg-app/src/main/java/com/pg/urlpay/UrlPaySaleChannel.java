package com.pg.urlpay;

/**
 * URL 결제 승인 API 라우팅 — {@link com.pg.urlpay.UrlPaySaleDispatcher} 가 사용합니다.
 */
public enum UrlPaySaleChannel {

    /** ChillPay DirectCredit + CCD 토큰 ({@code POST /api/pay/chillpay/direct-credit}) */
    CHILLPAY_DIRECT_CREDIT,
    /** JPAY pay_index 서버 프록시 ({@code POST /api/pay/jpay/sale} 또는 통합 {@code /api/pay/url/sale}) */
    JPAY_INLINE_SALE,
    /** Eximbay 결제준비(ready→fgkey) 후 JS SDK 결제창 ({@code /api/pay/url/sale} → fgkey 반환) */
    EXIMBAY_READY_SALE,
    /** 아직 ICOPAY URL 승인 어댑터 미등록 */
    NOT_REGISTERED
}
