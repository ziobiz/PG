package com.pg.controller;

import com.pg.entity.OrgUnit;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.ChillPayService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * PG 무관 중립 결제창 라우트 — {@code /checkout/{compId}}.
 * 가맹점·구매자에게 결제 대행사를 노출하지 않기 위해, 운영 PG를 서버에서 판별한 뒤
 * 실제 결제 페이지로 <b>내부 forward</b> 한다(브라우저 URL 은 {@code /checkout/...} 로 유지).
 */
@Controller
public class NeutralCheckoutRouteController {

    private final OrgUnitRepository orgUnitRepository;
    private final ChillPayService chillPayService;

    public NeutralCheckoutRouteController(OrgUnitRepository orgUnitRepository,
                                          ChillPayService chillPayService) {
        this.orgUnitRepository = orgUnitRepository;
        this.chillPayService = chillPayService;
    }

    @GetMapping("/checkout/{compId}")
    public String checkout(@PathVariable("compId") String compId) {
        return "forward:" + resolvePagePath(compId);
    }

    /** 운영 WEB PG → 실제 결제 페이지(static). 미확인/기본은 통합 결제창(pay.html). */
    private String resolvePagePath(String compId) {
        String mid = compId != null ? compId.trim() : "";
        String q = mid.isEmpty() ? "" : ("?m=" + URLEncoder.encode(mid, StandardCharsets.UTF_8));
        try {
            Long orgUnitId = orgUnitRepository.findByCode(mid)
                    .map(OrgUnit::getId)
                    .orElse(null);
            if (orgUnitId != null) {
                String opPg = chillPayService.resolveUrlPayOperationalPgCd(orgUnitId);
                if (opPg != null && !opPg.isBlank()) {
                    if (PgVendor.isJpayFamily(opPg)) {
                        return "/jpay-pay.html" + q;
                    }
                    if (PgVendor.isEximbayFamily(opPg)) {
                        return "/eximbay-pay.html" + q;
                    }
                    if (PgVendor.isElementPayFamily(opPg)) {
                        return "/elementpay-pay.html" + q;
                    }
                    if (PgVendor.isIlkFamily(opPg)) {
                        return "/ilk-pay.html" + q;
                    }
                }
            }
        } catch (RuntimeException ignore) {
            // 조회 실패 시 기본 결제창으로 forward — PG 노출 없이 안전하게 처리
        }
        /* ChillPay 등: pay.html 은 m 있으면 /checkout 로 리다이렉트하므로, forward 시 루프 방지 플래그 */
        return "/pay.html" + (q.isEmpty() ? "?pgCheckoutFwd=1" : (q + "&pgCheckoutFwd=1"));
    }
}
