package com.pg.urlpay;

import com.pg.entity.MerchantProfile;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import java.util.Optional;

/**
 * PG 벤더 전용 checkout-context 필드 보강.
 * <p>공통 필드(다통화·결제문구·본사 폼 모드)는 {@link UrlPayPublicCheckoutService} 가 채웁니다.
 */
public interface UrlPayCheckoutContextEnricher {

    boolean supports(UrlPayVendorCapability capability);

    void enrich(Map<String, Object> data,
                Long orgUnitId,
                Optional<MerchantProfile> profile,
                HttpServletRequest request);
}
