package com.pg.merchantdeploy;

import com.pg.integration.pg.PgVendor;
import com.pg.service.ChillPayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 가맹점에 등록·운영 중인 URL 결제 PG와 요청 API 벤더(ChillPay/JPAY 등)가 다를 때
 * PG 전문 전송 전에 결제 중계를 중단합니다.
 */
@Service
public class MerchantOperationalPgGuard {

    public static final String ERROR_CODE = "PG_VENDOR_MISMATCH";

    private static final Logger log = LoggerFactory.getLogger(MerchantOperationalPgGuard.class);

    private final ChillPayService chillPayService;

    public MerchantOperationalPgGuard(ChillPayService chillPayService) {
        this.chillPayService = chillPayService;
    }

    public Optional<Map<String, Object>> denyIfUrlPayVendorMismatch(Long orgUnitId, String requestedVendorScope) {
        return denyIfUrlPayVendorMismatch(orgUnitId, requestedVendorScope, false);
    }

    public Optional<Map<String, Object>> denyIfUrlPayVendorMismatch(Long orgUnitId,
                                                                    String requestedVendorScope,
                                                                    boolean repayScope) {
        if (orgUnitId == null || requestedVendorScope == null || requestedVendorScope.isBlank()) {
            return Optional.empty();
        }
        if (MerchantPgBrokerVendor.ALL.equalsIgnoreCase(requestedVendorScope.trim())) {
            return Optional.empty();
        }
        String opPg = repayScope
                ? chillPayService.resolveUrlPayRepayOperationalPgCd(orgUnitId)
                : chillPayService.resolveUrlPayOperationalPgCd(orgUnitId);
        if (opPg == null || opPg.isBlank()) {
            return Optional.empty();
        }
        boolean opJpay = PgVendor.isJpayFamily(opPg);
        boolean opChill = PgVendor.isChillPayFamily(opPg);
        boolean reqJpay = PgVendor.isJpayFamily(requestedVendorScope);
        boolean reqChill = PgVendor.isChillPayFamily(requestedVendorScope);
        if (!reqJpay && !reqChill) {
            return Optional.empty();
        }
        boolean match = (reqJpay && opJpay) || (reqChill && opChill);
        if (match) {
            return Optional.empty();
        }
        String configuredLabel = vendorLabel(opJpay, opChill, opPg);
        String requestedLabel = vendorLabel(reqJpay, reqChill, requestedVendorScope);
        log.warn("PG vendor mismatch blocked: orgUnitId={} operationalPgCd={} configured={} requested={}",
                orgUnitId, opPg.trim(), configuredLabel, requestedLabel);
        return Optional.of(buildDenyMap(opPg.trim(), configuredLabel, requestedLabel));
    }

    public Map<String, Object> buildDenyMap(String operationalPgCd, String configuredLabel, String requestedLabel) {
        Map<String, String> messages = MerchantOperationalPgGuardI18n.allLang(
                configuredLabel, requestedLabel, operationalPgCd);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", false);
        out.put("errorCode", ERROR_CODE);
        out.put("messageKey", MerchantOperationalPgGuardI18n.KEY_PG_VENDOR_MISMATCH);
        out.put("message", messages.get("KO"));
        out.put("messages", messages);
        out.put("operationalPgCd", operationalPgCd);
        out.put("configuredVendor", configuredLabel);
        out.put("requestedVendor", requestedLabel);
        return out;
    }

    private static String vendorLabel(boolean jpay, boolean chillPay, String pgCd) {
        if (jpay) {
            return MerchantPgBrokerVendor.JPAY;
        }
        if (chillPay) {
            return MerchantPgBrokerVendor.CHILLPAY;
        }
        return pgCd != null ? pgCd.trim() : "";
    }
}
