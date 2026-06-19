package com.pg.service;

import com.pg.entity.HqLedgerSysSettings;
import com.pg.repository.PgNotifyInboundRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.repository.RollingReserveRepository;
import com.pg.repository.SettlementRecoveryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 본사 전산설정 — 특정 일자의 결제내역·노티수령정보를 삭제하여 NOTI 재전송 후 재처리할 수 있게 합니다.
 * <p>삭제 범위: {@code pg_trnsctn.created_at} 기준 해당 일(표시 타임존) + 연동 환수·롤링,
 * 선택 시 {@code tb_pg_notify_inbound.created_at} 동일 구간.
 */
@Service
public class HqPayNotifyDayPurgeService {

    private static final Logger log = LoggerFactory.getLogger(HqPayNotifyDayPurgeService.class);

    private final HqLedgerSysSettingsService hqLedgerSysSettingsService;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final PgNotifyInboundRepository pgNotifyInboundRepository;
    private final SettlementRecoveryRepository settlementRecoveryRepository;
    private final RollingReserveRepository rollingReserveRepository;

    public HqPayNotifyDayPurgeService(HqLedgerSysSettingsService hqLedgerSysSettingsService,
                                        PgTrnsctnRepository pgTrnsctnRepository,
                                        PgNotifyInboundRepository pgNotifyInboundRepository,
                                        SettlementRecoveryRepository settlementRecoveryRepository,
                                        RollingReserveRepository rollingReserveRepository) {
        this.hqLedgerSysSettingsService = hqLedgerSysSettingsService;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.pgNotifyInboundRepository = pgNotifyInboundRepository;
        this.settlementRecoveryRepository = settlementRecoveryRepository;
        this.rollingReserveRepository = rollingReserveRepository;
    }

    /**
     * @param date        삭제 대상 일자(표시 타임존 기준)
     * @param merchantId  null·빈값이면 전체 가맹
     * @param purgeInbound true면 동일 구간 노티수령정보도 삭제
     */
    @Transactional
    public Map<String, Object> purgeForDay(LocalDate date, String merchantId, boolean purgeInbound) {
        if (date == null) {
            throw new IllegalArgumentException("date(YYYY-MM-DD)가 필요합니다.");
        }
        String mid = normalizeMerchantId(merchantId);
        HqLedgerSysSettings settings = hqLedgerSysSettingsService.getOrCreate();
        ZoneId zone = HqLedgerSysSettingsService.resolveDisplayZoneIdFromSettings(settings);
        LocalDateTime from = date.atStartOfDay(zone).toLocalDateTime();
        LocalDateTime to = date.plusDays(1).atStartOfDay(zone).toLocalDateTime();

        List<String> trnIds = pgTrnsctnRepository.findTrnIdsByCreatedAtRange(from, to, mid);
        int recoveryDeleted = 0;
        int rollingDeleted = 0;
        if (!trnIds.isEmpty()) {
            recoveryDeleted = settlementRecoveryRepository.deleteByTrnIdIn(trnIds);
            rollingDeleted = rollingReserveRepository.deleteByTrnIdIn(trnIds);
            pgTrnsctnRepository.deleteAllById(trnIds);
        }

        int inboundDeleted = 0;
        if (purgeInbound) {
            inboundDeleted = pgNotifyInboundRepository.deleteByCreatedAtRange(from, to, mid);
        }

        log.warn("pay/notify day purge date={} merchantId={} zone={} trnsctn={} recovery={} rolling={} inbound={}",
                date, mid != null ? mid : "*", zone.getId(), trnIds.size(), recoveryDeleted, rollingDeleted, inboundDeleted);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", date.toString());
        result.put("merchantId", mid);
        result.put("timezone", zone.getId());
        result.put("purgeInbound", purgeInbound);
        result.put("transactionsDeleted", trnIds.size());
        result.put("settlementRecoveryDeleted", recoveryDeleted);
        result.put("rollingReserveDeleted", rollingDeleted);
        result.put("notifyInboundDeleted", inboundDeleted);
        return result;
    }

    private static String normalizeMerchantId(String merchantId) {
        if (merchantId == null) {
            return null;
        }
        String t = merchantId.trim();
        return t.isEmpty() ? null : t;
    }
}
