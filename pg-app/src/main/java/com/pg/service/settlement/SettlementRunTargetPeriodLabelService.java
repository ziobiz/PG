package com.pg.service.settlement;

import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.entity.SettlementRun;
import com.pg.repository.HqLedgerSysSettingsRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.repository.SettlementSettingRepository;
import com.pg.service.HqLedgerSysSettingsService;
import com.pg.util.TrnTimeDualZoneDisplay;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 정산 실행 행의 정산대상기간 표시 — 가맹점정산·정산리포트와 동일 규칙.
 */
@Service
public class SettlementRunTargetPeriodLabelService {

    private static final String SETTLEMENT_GRID_MISSING_PERIOD_MSG =
            " ~ (미기록) — M/H 격자(H12 등)는 마감시각(period_end_at)이 있어야 12시간·N분 단위 구간으로 표시됩니다.";

    private final HqLedgerSysSettingsRepository hqLedgerSysSettingsRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final SettlementSettingRepository settlementSettingRepository;
    private final PgTrnsctnRepository pgTrnsctnRepository;

    public SettlementRunTargetPeriodLabelService(HqLedgerSysSettingsRepository hqLedgerSysSettingsRepository,
                                                 OrgUnitRepository orgUnitRepository,
                                                 SettlementSettingRepository settlementSettingRepository,
                                                 PgTrnsctnRepository pgTrnsctnRepository) {
        this.hqLedgerSysSettingsRepository = hqLedgerSysSettingsRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.settlementSettingRepository = settlementSettingRepository;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
    }

    private ZoneId resolveLedgerDisplayZoneId() {
        return HqLedgerSysSettingsService.resolveDisplayZoneIdFromSettings(
                hqLedgerSysSettingsRepository.findFirstByOrderByIdAsc().orElse(null));
    }

    private String formatSettlementTargetRangeDual(LocalDateTime startDt, LocalDateTime endDt) {
        return TrnTimeDualZoneDisplay.formatDualLineDateTimeRange(startDt, endDt, resolveLedgerDisplayZoneId());
    }

    private static String appendSameSuffixToDualPeriodLines(String dualTwoLines, String suffix) {
        if (dualTwoLines == null || dualTwoLines.isBlank()) {
            return "";
        }
        int nl = dualTwoLines.indexOf('\n');
        if (nl < 0) {
            return dualTwoLines + suffix;
        }
        return dualTwoLines.substring(0, nl) + suffix + "\n" + dualTwoLines.substring(nl + 1) + suffix;
    }

    /**
     * 가맹 정산설정 주기(정규화 코드)를 부모 실행 표시용 보조값으로 넘깁니다.
     */
    public String settlementCycleFallbackForMerchant(String merchantId) {
        if (merchantId == null || merchantId.isBlank()) {
            return "";
        }
        Optional<OrgUnit> ou = orgUnitRepository.findByCode(merchantId.trim());
        return ou.flatMap(o -> settlementSettingRepository.findByOrgUnitId(o.getId()))
                .map(ss -> {
                    String c = ss.getCalcCycle();
                    return c != null && !c.isBlank() ? SettlementPeriodResolver.normalizeCalcCycle(c.trim()) : "";
                })
                .orElse("");
    }

    public static String resolveRunCalcCycleRaw(SettlementRun r) {
        if (r == null) {
            return "";
        }
        String s = r.getCalcCycleSnapshot();
        return s != null && !s.isBlank() ? s.trim() : "";
    }

    public String resolveRunCalcCycleForExecuteDisplay(SettlementRun r, String settingsCycleFallback) {
        String snap = resolveRunCalcCycleRaw(r);
        if (!snap.isEmpty()) {
            return snap;
        }
        if (settingsCycleFallback != null && !settingsCycleFallback.isBlank()) {
            return SettlementPeriodResolver.normalizeCalcCycle(settingsCycleFallback.trim());
        }
        return "";
    }

    /**
     * 정산대상기간 표시. RT(건당)은 거래번호·승인번호 + 마감 시각, 그 외는 구간을 동일 두 줄로 표시.
     */
    public String buildSettlementTargetPeriodLabel(SettlementRun r, String settingsCycleFallback) {
        String snap = resolveRunCalcCycleRaw(r);
        String display = resolveRunCalcCycleForExecuteDisplay(r, settingsCycleFallback);
        String labelCycleRaw = !snap.isEmpty() ? snap : display;
        String norm = SettlementPeriodResolver.normalizeCalcCycle(labelCycleRaw != null ? labelCycleRaw : "");
        if (SettlementCycleTiming.isRtPerTransactionCode(norm)
                && r.getPeriodFrom() != null
                && r.getPeriodTo() != null
                && r.getPeriodFrom().equals(r.getPeriodTo())
                && r.getPeriodEndAt() != null) {
            return buildRtSettlementTargetPeriodLine(r);
        }
        return buildSettlementTargetPeriodLabelNonRt(r, norm, labelCycleRaw);
    }

    private String buildRtSettlementTargetPeriodLine(SettlementRun r) {
        LocalDateTime closeAt = r.getPeriodEndAt();
        Optional<PgTrnsctn> txOpt = resolvePgTxnForRtRun(r);
        if (txOpt.isEmpty()) {
            return formatRtPeriodLine("-", "-", closeAt);
        }
        PgTrnsctn t = txOpt.get();
        String trn = blankToDash(t.getTrnId());
        String appr = firstNonBlank(t.getApprovalNo(), t.getChillTransactionId());
        return formatRtPeriodLine(trn, appr, closeAt);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return "-";
    }

    private static String blankToDash(String s) {
        if (s == null || s.isBlank()) {
            return "-";
        }
        return s.trim();
    }

    private String formatRtPeriodLine(String trnId, String approvalLabel, LocalDateTime closeAt) {
        String head = "거래번호 " + trnId + " / 승인번호 " + approvalLabel;
        if (closeAt == null) {
            return head + " / 마감 -";
        }
        return head + "\n마감\n" + TrnTimeDualZoneDisplay.formatDualLineDateTime(closeAt, resolveLedgerDisplayZoneId());
    }

    private Optional<PgTrnsctn> resolvePgTxnForRtRun(SettlementRun r) {
        if (r.getMerchantId() == null || r.getMerchantId().isBlank() || r.getPeriodEndAt() == null) {
            return Optional.empty();
        }
        String mid = r.getMerchantId().trim();
        LocalDateTime end = r.getPeriodEndAt();
        LocalDateTime winStart = end.minusSeconds(5);
        LocalDateTime winEnd = end.plusSeconds(5);
        List<PgTrnsctn> inWin = pgTrnsctnRepository.findForSettlement(mid, winStart, winEnd);
        List<PgTrnsctn> candidates = inWin.stream()
                .filter(t -> "10".equals(t.getStatus() != null ? t.getStatus().trim() : ""))
                .filter(t -> "Y".equalsIgnoreCase(String.valueOf(t.getSettledYn()).trim()))
                .toList();
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        Optional<PgTrnsctn> exactTime = candidates.stream()
                .filter(t -> t.getCreatedAt() != null && t.getCreatedAt().equals(end))
                .findFirst();
        if (exactTime.isPresent()) {
            return exactTime;
        }
        BigDecimal ap = r.getApproveAmt();
        if (ap != null && ap.signum() > 0) {
            Optional<PgTrnsctn> byAmt = candidates.stream()
                    .filter(t -> {
                        BigDecimal amt = t.getAmtKrw() != null ? t.getAmtKrw() : BigDecimal.ZERO;
                        return amt.compareTo(ap) == 0;
                    })
                    .findFirst();
            if (byAmt.isPresent()) {
                return byAmt;
            }
        }
        return candidates.stream().min(Comparator.comparingLong(t -> {
            if (t.getCreatedAt() == null) {
                return Long.MAX_VALUE;
            }
            return Math.abs(Duration.between(t.getCreatedAt(), end).toNanos());
        }));
    }

    private String buildSettlementTargetPeriodLabelNonRt(SettlementRun r,
                                                         String calcCycleNorm, String labelCycleRaw) {
        if (r.getPeriodFrom() != null && r.getPeriodTo() != null) {
            LocalDateTime startDt;
            LocalDateTime endDt;
            if (r.getPeriodEndAt() != null) {
                LocalDateTime endExclusive = r.getPeriodEndAt();
                LocalDateTime inferredStart = SettlementCycleTiming.subDailySlotStartInclusiveFromEndExclusive(
                        endExclusive, calcCycleNorm != null ? calcCycleNorm : "");
                if (inferredStart != null) {
                    startDt = inferredStart.truncatedTo(ChronoUnit.SECONDS);
                    endDt = endExclusive.truncatedTo(ChronoUnit.SECONDS);
                } else {
                    startDt = r.getPeriodFrom().atStartOfDay();
                    endDt = endExclusive.truncatedTo(ChronoUnit.SECONDS);
                }
                return formatSettlementTargetRangeDual(startDt, endDt);
            }
            startDt = r.getPeriodFrom().atStartOfDay();
            if (SettlementCycleTiming.isPlainSubDailyGridClosingCode(calcCycleNorm)) {
                return appendSameSuffixToDualPeriodLines(
                        TrnTimeDualZoneDisplay.formatDualLineDateTime(startDt, resolveLedgerDisplayZoneId()),
                        SETTLEMENT_GRID_MISSING_PERIOD_MSG);
            }
            endDt = r.getPeriodTo().atTime(23, 59, 59);
            return formatSettlementTargetRangeDual(startDt, endDt);
        }
        if (r.getCalcDt() != null) {
            LocalDate d = r.getCalcDt();
            LocalDateTime startDt = d.atStartOfDay();
            LocalDateTime endDt = d.atTime(23, 59, 59);
            return formatSettlementTargetRangeDual(startDt, endDt);
        }
        return "";
    }
}
