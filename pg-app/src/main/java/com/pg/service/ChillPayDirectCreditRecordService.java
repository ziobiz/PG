package com.pg.service;

import com.pg.integration.pg.PgVendor;
import com.pg.dto.ChillPayDirectCreditResponse;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * ChillPay DirectCredit 동기 응답을 {@link PgTrnsctn}에 반영해 결제내역·정산 파이프라인과 맞춥니다.
 * <p><b>PCI:</b> 카드번호·유효기간·CVC 등 카드 인증 데이터는 CCD/ChillPay 측에서만 처리하고,
 * 본 서비스 DB에는 저장하지 않습니다. 전산에는 주문·금액·가맹·ChillPay 거래식별자·결제자 표시명(폼/응답 메타) 등만 적재합니다.
 * <p><b>URL 결제 인라인:</b> CCD 입력 → DirectCreditToken → 본 API 동기 응답 → 여기서 {@code PgTrnsctn} 생성({@code serviceType}=URL_INLINE 권장).
 * <p><b>URL 결제 리다이렉트(호스티드):</b> 매뉴얼의 {@code data-*}/폼 POST는 ChillPay 호스티드로 직행하며, 동기 DirectCredit 응답이 없으면
 * 본 메서드가 아니라 <b>PG 노티 URL</b> 수신 처리로 전산을 맞추는 것이 일반적입니다(별도 노티→거래 적재 연동).
 */
@Service
public class ChillPayDirectCreditRecordService {

    private static final Logger log = LoggerFactory.getLogger(ChillPayDirectCreditRecordService.class);

    /** 승인 완료 — 정산 후보 */
    private static final String STATUS_PAID = "10";
    /** OTP·추가 인증 대기 — 아직 매출 확정 아님 */
    private static final String STATUS_AUTH_PENDING = "08";

    private static final Map<String, String> CHILL_ISO_NUM_TO_ALPHA = Map.of(
            "392", "JPY",
            "764", "THB",
            "840", "USD",
            "410", "KRW"
    );

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final SettlementCalcService settlementCalcService;

    public ChillPayDirectCreditRecordService(PgTrnsctnRepository pgTrnsctnRepository,
                                            OrgUnitRepository orgUnitRepository,
                                            SettlementCalcService settlementCalcService) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.settlementCalcService = settlementCalcService;
    }

    /**
     * ChillPay 본문 status=200 이고 data.paymentStatus 가 Paid 또는 WaitAuthorize 일 때만 행을 생성합니다.
     * 저장 실패는 로그만 남기고 호출부 결제 응답에는 영향을 주지 않습니다.
     */
    @Transactional
    public void recordAfterDirectCreditResponse(Long merchantOrgUnitId,
                                                ChillPayDirectCreditResponse res,
                                                long requestAmount,
                                                String requestOrderNo,
                                                String requestCustomerId,
                                                int routeNo,
                                                String urlPayIntegrationMode,
                                                String payerDisplayName,
                                                String checkoutCurrencyAlpha) {
        try {
            doRecord(merchantOrgUnitId, res, requestAmount, requestOrderNo, requestCustomerId, routeNo,
                    urlPayIntegrationMode, payerDisplayName, checkoutCurrencyAlpha);
        } catch (Exception e) {
            log.warn("DirectCredit 거래 적재 실패 (결제 API 응답은 유지): {}", e.getMessage());
        }
    }

    private void doRecord(Long merchantOrgUnitId,
                          ChillPayDirectCreditResponse res,
                          long requestAmount,
                          String requestOrderNo,
                          String requestCustomerId,
                          int routeNo,
                          String urlPayIntegrationMode,
                          String payerDisplayName,
                          String checkoutCurrencyAlpha) {
        if (res == null || res.getStatus() != 200 || res.getData() == null) {
            return;
        }
        ChillPayDirectCreditResponse.Data d = res.getData();
        String ps = d.getPaymentStatus();
        if (ps == null || ps.isBlank()) {
            return;
        }
        String psl = ps.trim();
        boolean paid = "Paid".equalsIgnoreCase(psl);
        boolean waitAuth = "WaitAuthorize".equalsIgnoreCase(psl);
        if (!paid && !waitAuth) {
            return;
        }

        String merchantId = resolveMerchantId(merchantOrgUnitId);
        String orderNo = firstNonBlank(d.getOrderNo(), requestOrderNo);
        if (orderNo == null || orderNo.isBlank()) {
            if (d.getTransactionId() != null && !d.getTransactionId().isBlank()) {
                orderNo = "CP" + d.getTransactionId().trim();
            } else {
                orderNo = "ORD" + System.currentTimeMillis();
            }
        }
        if (orderNo.length() > 64) {
            orderNo = orderNo.substring(0, 64);
        }
        String payNo = orderNo.length() > 50 ? orderNo.substring(0, 50) : orderNo;
        String customerId = firstNonBlank(d.getCustomerId(), requestCustomerId);
        if (customerId == null) {
            customerId = "guest";
        }
        String custNm = firstNonBlank(trimToMax(payerDisplayName, 200), trimToMax(d.getCustomer(), 200));

        long amountVal = d.getAmount() != null ? d.getAmount() : requestAmount;
        if (amountVal <= 0) {
            return;
        }

        PgTrnsctn t = new PgTrnsctn();
        t.setTrnId(newTrnId());
        t.setMerchantId(merchantId);
        t.setServiceType(resolveUrlPayServiceType(urlPayIntegrationMode));
        t.setStatus(paid ? STATUS_PAID : STATUS_AUTH_PENDING);
        t.setCurType(resolveCurTypeForStorage(d.getCurrency(), checkoutCurrencyAlpha));
        t.setAmtKrw(BigDecimal.valueOf(amountVal));
        t.setPayNo(payNo);
        t.setOrderNo(orderNo);
        t.setCustomerId(customerId);
        if (custNm != null) {
            t.setCustomerNm(custNm);
        }
        t.setVan(PgVendor.CHILLPAY);
        t.setOrigin("URL");
        t.setChillPaymentStatus(psl);
        t.setRouteNo(String.valueOf(routeNo));
        if (d.getTransactionId() != null && !d.getTransactionId().isBlank()) {
            String tid = d.getTransactionId().trim();
            t.setChillTransactionId(tid.length() > 64 ? tid.substring(0, 64) : tid);
        }
        if (d.getChannelCode() != null && !d.getChannelCode().isBlank()) {
            t.setPaymentChannel(d.getChannelCode().trim());
        }
        if (d.getFee() != null) {
            t.setChillFeeAmt(BigDecimal.valueOf(d.getFee()));
        }
        if (d.getTotalAmount() != null) {
            t.setTotalAmt(BigDecimal.valueOf(d.getTotalAmount()));
        } else {
            t.setTotalAmt(BigDecimal.valueOf(amountVal));
        }
        if (d.getIcopay() != null) {
            t.setIcopayAmt(BigDecimal.valueOf(d.getIcopay()));
        }
        if (paid) {
            t.setPaidAt(LocalDateTime.now());
        }
        t.setSettledYn("N");
        pgTrnsctnRepository.save(t);
        if (paid && merchantId != null && !merchantId.isBlank() && !"UNKNOWN".equalsIgnoreCase(merchantId.trim())) {
            try {
                settlementCalcService.triggerRealtimeAutoSettlementIfDue(merchantId.trim(), t);
            } catch (Exception ex) {
                log.warn("실시간 자동정산 트리거 실패 merchantId={}: {}", merchantId, ex.getMessage());
            }
        }
        log.info("DirectCredit 거래 적재 trnId={} merchantId={} orderNo={} status={}", t.getTrnId(), merchantId, orderNo, t.getStatus());
    }

    private static String newTrnId() {
        String u = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return u.length() <= 20 ? u : u.substring(0, 20);
    }

    private String resolveMerchantId(Long merchantOrgUnitId) {
        if (merchantOrgUnitId == null) {
            return "UNKNOWN";
        }
        Optional<OrgUnit> ou = orgUnitRepository.findById(merchantOrgUnitId);
        if (ou.isEmpty()) {
            return "UNKNOWN";
        }
        String code = ou.get().getCode();
        return (code != null && !code.isBlank()) ? code.trim() : "UNKNOWN";
    }

    /**
     * ChillPay 응답 Currency(문자·ISO 숫자) 우선, 없으면 요청 시 체크아웃 통화. 레거시 호환 기본 JPY.
     */
    private static String resolveCurTypeForStorage(String chillPayCurrency, String requestCurrency) {
        String fromChill = normalizeCurrencyAlpha(chillPayCurrency);
        if (fromChill != null) {
            return fromChill;
        }
        String fromReq = normalizeCurrencyAlpha(requestCurrency);
        if (fromReq != null) {
            return fromReq;
        }
        return "JPY";
    }

    private static String normalizeCurrencyAlpha(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if (CHILL_ISO_NUM_TO_ALPHA.containsKey(u)) {
            return CHILL_ISO_NUM_TO_ALPHA.get(u);
        }
        if (u.length() == 3 && Character.isLetter(u.charAt(0)) && Character.isLetter(u.charAt(1))
                && Character.isLetter(u.charAt(2))) {
            return u;
        }
        return null;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return null;
    }

    /** {@code INLINE}·{@code REDIRECT}·그 외(레거시 {@code API}) */
    private static String resolveUrlPayServiceType(String mode) {
        if (mode == null || mode.isBlank()) {
            return "API";
        }
        String u = mode.trim().toUpperCase();
        if ("INLINE".equals(u)) {
            return "URL_INLINE";
        }
        if ("REDIRECT".equals(u)) {
            return "URL_REDIRECT";
        }
        return "API";
    }

    private static String trimToMax(String s, int max) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t.length() <= max ? t : t.substring(0, max);
    }
}
