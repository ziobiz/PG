package com.pg.service.ops;

import com.pg.service.PayListService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/**
 * 운영관리 — 통합 리포트(일자별 결제·수수료·가맹 동적 열).
 * 접근 권한은 {@link TaxReportService} 와 동일(ADMIN·총본사·본사·총판)입니다.
 */
@Service
public class OpsIntegratedReportService {

    private final TaxReportService taxReportService;
    private final PayListService payListService;

    public OpsIntegratedReportService(TaxReportService taxReportService, PayListService payListService) {
        this.taxReportService = taxReportService;
        this.payListService = payListService;
    }

    public Map<String, Object> accessMeta(Authentication authentication) {
        return taxReportService.accessMeta(authentication);
    }

    public Map<String, Object> dailyReport(LocalDate searchFromDate, LocalDate searchToDate, Authentication authentication) {
        Optional<String> deny = taxReportService.accessDeniedReason(authentication);
        if (deny.isPresent()) {
            throw new IllegalStateException(deny.get());
        }
        return payListService.buildOpsIntegratedReport(searchFromDate, searchToDate, authentication);
    }
}
