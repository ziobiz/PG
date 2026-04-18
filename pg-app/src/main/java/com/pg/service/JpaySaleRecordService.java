package com.pg.service;

import com.pg.integration.pg.PgVendor;
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
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

/**
 * JPAY {@code pay_index} 직접 호출(서버 프록시) 직후 {@link PgTrnsctn} 에 URL 결제 출처 행을 남깁니다.
 * 3DS·비동기 노티는 {@link JpayNotifyToTrnsctnService} 가 후속 갱신합니다.
 */
@Service
public class JpaySaleRecordService {

    private static final Logger log = LoggerFactory.getLogger(JpaySaleRecordService.class);
    private static final String ORIGIN_URL = "URL";
    private static final String ST_PAID = "10";
    private static final String ST_PENDING = "08";
    private static final String ST_FAIL = "99";

    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final SettlementCalcService settlementCalcService;
    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;

    public JpaySaleRecordService(PgTrnsctnRepository pgTrnsctnRepository,
                                 OrgUnitRepository orgUnitRepository,
                                 SettlementCalcService settlementCalcService,
                                 HqLedgerSysSettingsService hqLedgerSysSettingsService) {
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.settlementCalcService = settlementCalcService;
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
    }

    @Transactional
    public void recordOrTouchPending(Long orgUnitId,
                                     String orderNo,
                                     BigDecimal amount,
                                     String currency,
                                     int routeNo,
                                     String customerHint,
                                     String productName) {
        try {
            doRecord(orgUnitId, orderNo, amount, currency, routeNo, customerHint, productName);
        } catch (Exception e) {
            log.warn("JPAY sale 거래 적재(대기) 실패: {}", e.getMessage());
        }
    }

    private void doRecord(Long orgUnitId,
                          String orderNo,
                          BigDecimal amount,
                          String currency,
                          int routeNo,
                          String customerHint,
                          String productName) {
        if (orgUnitId == null || orderNo == null || orderNo.isBlank()) {
            return;
        }
        Optional<OrgUnit> ou = orgUnitRepository.findById(orgUnitId);
        if (ou.isEmpty()) {
            return;
        }
        String merchantId = ou.get().getCode();
        if (merchantId == null || merchantId.isBlank()) {
            return;
        }
        String on = orderNo.trim();
        if (on.length() > 64) {
            on = on.substring(0, 64);
        }
        Optional<PgTrnsctn> ex = pgTrnsctnRepository.findFirstByMerchantIdAndOrderNoAndOrigin(merchantId, on, ORIGIN_URL);
        PgTrnsctn t = ex.orElseGet(() -> {
            PgTrnsctn x = new PgTrnsctn();
            x.setTrnId(newTrnId());
            x.setMerchantId(merchantId.trim());
            x.setServiceType("URL_JPAY");
            x.setOrigin(ORIGIN_URL);
            return x;
        });
        t.setStatus(ST_PENDING);
        t.setVan(PgVendor.JPAY.length() > 10 ? PgVendor.JPAY.substring(0, 10) : PgVendor.JPAY);
        t.setOrderNo(on);
        String payNo = on.length() > 50 ? on.substring(0, 50) : on;
        t.setPayNo(payNo);
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            t.setAmtKrw(amount);
        }
        String cur = currency != null ? currency.trim().toUpperCase() : "USD";
        t.setCurType(cur.length() > 3 ? cur.substring(0, 3) : cur);
        t.setRouteNo(String.valueOf(routeNo));
        String cid = customerHint != null && !customerHint.isBlank() ? customerHint.trim() : "guest";
        t.setCustomerId(cid.length() > 100 ? cid.substring(0, 100) : cid);
        String desc = "JPAY_URL";
        if (productName != null && !productName.isBlank()) {
            desc = desc + " " + productName.trim();
        }
        t.setChillPaymentStatus(desc.length() > 50 ? desc.substring(0, 50) : desc);
        t.setPaymentChannel("CARD");
        if (t.getSettledYn() == null || t.getSettledYn().isBlank()) {
            t.setSettledYn("N");
        }
        pgTrnsctnRepository.save(t);
    }

    @Transactional
    public void applySyncApiOutcome(String merchantId, String orderNo, int status, String msg) {
        if (merchantId == null || merchantId.isBlank() || orderNo == null || orderNo.isBlank()) {
            return;
        }
        try {
            String on = orderNo.trim();
            Optional<PgTrnsctn> ex = pgTrnsctnRepository.findFirstByMerchantIdAndOrderNoAndOrigin(
                    merchantId.trim(), on, ORIGIN_URL);
            if (ex.isEmpty()) {
                return;
            }
            PgTrnsctn t = ex.get();
            if (status == 0) {
                t.setStatus(ST_PAID);
                ZoneId wall = hqLedgerSysSettingsService.resolveLedgerDisplayZoneId();
                t.setPaidAt(LocalDateTime.now(wall));
                String m = msg != null ? msg.trim() : "OK";
                t.setChillPaymentStatus(truncate(m, 50));
            } else if (status == 2) {
                t.setStatus(ST_FAIL);
                t.setPaidAt(null);
                String m = msg != null ? msg.trim() : "FAIL";
                t.setChillPaymentStatus(truncate(m, 50));
            }
            /* status==1 (3DS): pending 유지 */
            pgTrnsctnRepository.save(t);
            if (status == 0 && t.getMerchantId() != null && !t.getMerchantId().isBlank()) {
                try {
                    settlementCalcService.triggerRealtimeAutoSettlementIfDue(t.getMerchantId().trim(), t);
                } catch (Exception rtEx) {
                    log.warn("실시간 자동정산 트리거 실패 merchantId={}: {}", t.getMerchantId(), rtEx.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("JPAY 동기 응답 반영 실패: {}", e.getMessage());
        }
    }

    private static String newTrnId() {
        String u = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return u.length() <= 20 ? u : u.substring(0, 20);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
