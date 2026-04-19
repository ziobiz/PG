package com.pg.merchantdeploy;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 가맹점 배포 문서·키트에 노출할 PG 브로커 라우트 목록.
 * JPAY 등 신규 PG 추가 시 이 카탈로그에 {@link MerchantPgBrokerRouteDefinition} 행을 추가합니다.
 */
@Component
public class MerchantPgBrokerCatalog {

    private final List<MerchantPgBrokerRouteDefinition> definitions;

    public MerchantPgBrokerCatalog() {
        List<MerchantPgBrokerRouteDefinition> list = new ArrayList<>();
        list.add(new MerchantPgBrokerRouteDefinition(
                MerchantPgBrokerVendor.CHILLPAY,
                "ChillPay (DirectCredit·CCD)",
                "chillpay",
                List.of(
                        "/api/middleware/v1/pg/chillpay/config",
                        "/api/middleware/v1/pg/chillpay/checkout-context",
                        "/api/middleware/v1/pg/chillpay/display-fx-quote",
                        "/api/middleware/v1/pg/chillpay/url-result-copy",
                        "/api/middleware/v1/pg/chillpay/request"
                ),
                List.of(
                        "/api/pay/chillpay/config",
                        "/api/pay/chillpay/checkout-context",
                        "/api/pay/chillpay/display-fx-quote",
                        "/api/pay/chillpay/url-result-copy",
                        "/api/pay/chillpay/request"
                )
        ));
        list.add(new MerchantPgBrokerRouteDefinition(
                MerchantPgBrokerVendor.JPAY,
                "JPAY (직매출)",
                "jpay",
                List.of("/api/middleware/v1/pg/jpay/sale"),
                List.of("/api/pay/jpay/sale")
        ));
        this.definitions = Collections.unmodifiableList(list);
    }

    public List<MerchantPgBrokerRouteDefinition> definitions() {
        return definitions;
    }

    public MerchantPgBrokerRouteDefinition findByVendorScope(String vendorScope) {
        String v = MerchantPgBrokerVendor.normalizeScope(vendorScope);
        for (MerchantPgBrokerRouteDefinition d : definitions) {
            if (d.vendorScope().equalsIgnoreCase(v)) {
                return d;
            }
        }
        return null;
    }

    public MerchantPgBrokerRouteDefinition findByPathSegment(String segment) {
        String key = MerchantPgBrokerVendor.fromBrokerPathSegment(segment);
        return findByVendorScope(key);
    }
}
