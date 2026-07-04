package com.pg.controller;

import com.pg.entity.OrgUnit;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.JpaySubscriptionConfigService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * PG 무관 중립 구독(정기결제) 결제창 — {@code /checkout-subscribe/{compId}}.
 * 운영 구독 PG를 서버에서 판별한 뒤 실제 구독 결제 페이지로 내부 forward 한다.
 */
@Controller
public class NeutralCheckoutSubscribeRouteController {

    private final OrgUnitRepository orgUnitRepository;
    private final JpaySubscriptionConfigService subscriptionConfigService;

    public NeutralCheckoutSubscribeRouteController(OrgUnitRepository orgUnitRepository,
                                                   JpaySubscriptionConfigService subscriptionConfigService) {
        this.orgUnitRepository = orgUnitRepository;
        this.subscriptionConfigService = subscriptionConfigService;
    }

    @GetMapping("/checkout-subscribe/{compId}")
    public String checkoutSubscribe(@PathVariable("compId") String compId) {
        return "forward:" + resolveSubscribePagePath(compId);
    }

    private String resolveSubscribePagePath(String compId) {
        try {
            Long orgUnitId = orgUnitRepository.findByCode(compId != null ? compId.trim() : "")
                    .map(OrgUnit::getId)
                    .orElse(null);
            if (orgUnitId != null) {
                var bind = subscriptionConfigService.findOperationalSubscriptionBinding(orgUnitId);
                if (bind.isPresent()) {
                    String pgCd = bind.get().getPgCd();
                    if (pgCd != null && PgVendor.isJpayFamily(pgCd.trim())) {
                        return "/jpay-subscribe.html";
                    }
                    if (pgCd != null && PgVendor.isEximbayFamily(pgCd.trim())) {
                        return "/eximbay-subscribe.html";
                    }
                }
            }
        } catch (RuntimeException ignore) {
            /* fallback */
        }
        return "/jpay-subscribe.html";
    }
}
