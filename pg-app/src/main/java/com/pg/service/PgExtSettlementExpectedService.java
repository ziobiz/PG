package com.pg.service;

import com.pg.entity.MerchantPgBinding;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgAgency;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgAgencyRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * API연동설정({@code tb_pg_agency})·가맹 바인딩({@code tb_merchant_pg_binding})의 T+N / D+N 규칙으로
 * 통합정산 그리드용 <strong>예상 PG 정산 시각</strong>을 채운다. {@code OFF} 또는 미설정은 비움.
 */
@Service
public class PgExtSettlementExpectedService {

    private static final DateTimeFormatter CHILLPAY_TXN_DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter DISPLAY_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT);

    private final PgAgencyRepository pgAgencyRepository;
    private final MerchantPgBindingRepository merchantPgBindingRepository;
    private final OrgUnitRepository orgUnitRepository;

    public PgExtSettlementExpectedService(
            PgAgencyRepository pgAgencyRepository,
            MerchantPgBindingRepository merchantPgBindingRepository,
            OrgUnitRepository orgUnitRepository) {
        this.pgAgencyRepository = pgAgencyRepository;
        this.merchantPgBindingRepository = merchantPgBindingRepository;
        this.orgUnitRepository = orgUnitRepository;
    }

    public void enrichChillPaySettlementRows(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Map<String, PgAgency> agencyByPgCd = new HashMap<>();
        for (PgAgency a : pgAgencyRepository.findAllByOrderByPgCdAsc()) {
            if (a.getPgCd() != null && !a.getPgCd().isBlank()) {
                agencyByPgCd.putIfAbsent(a.getPgCd().trim().toUpperCase(Locale.ROOT), a);
            }
        }
        for (Map<String, Object> row : rows) {
            if (row == null) {
                continue;
            }
            row.put("icopayExpectedSettleAt", "");
            row.put("icopayExpectedSettleRule", "");
            String compId = firstNonBlankString(row, "compId", "comp_id");
            String rowMid = chillTrMerchantRaw(row);
            LocalDateTime pay = parseChillPayApiDateTime(firstNonBlankString(row, "paymentDate", "PaymentDate"));
            if (pay == null) {
                pay = parseChillPayApiDateTime(firstNonBlankString(row, "transactionDate", "TransactionDate"));
            }
            ResolvedExtSettle rule = resolveRule(compId, rowMid, agencyByPgCd);
            if (rule == null || rule.mode() == ExtSettleMode.OFF) {
                continue;
            }
            LocalDateTime at = computeExpected(pay, rule);
            if (at == null) {
                row.put("icopayExpectedSettleRule", rule.ruleLabel());
            } else {
                row.put("icopayExpectedSettleAt", at.format(DISPLAY_DT));
                row.put("icopayExpectedSettleRule", rule.ruleLabel());
            }
        }
    }

    private ResolvedExtSettle resolveRule(String compId, String rowMid, Map<String, PgAgency> agencyByPgCd) {
        if (compId == null || compId.isBlank()) {
            return null;
        }
        Optional<OrgUnit> ou = orgUnitRepository.findByCodeIgnoreCase(compId.trim());
        if (ou.isEmpty()) {
            return null;
        }
        List<MerchantPgBinding> binds = merchantPgBindingRepository.findByOrgUnitIdOrderBySortOrderAsc(ou.get().getId());
        MerchantPgBinding pick = pickChillBinding(binds, rowMid);
        if (pick == null) {
            return null;
        }
        PgAgency agency = agencyByPgCd.get(pick.getPgCd() != null ? pick.getPgCd().trim().toUpperCase(Locale.ROOT) : "");
        if (agency == null) {
            agency = pgAgencyRepository.findByPgCd(pick.getPgCd().trim()).orElse(null);
        }
        if (pick.getExtSettleMode() != null && !pick.getExtSettleMode().isBlank()) {
            ExtSettleMode m = ExtSettleMode.parse(pick.getExtSettleMode());
            if (m == ExtSettleMode.OFF) {
                return new ResolvedExtSettle(m, null, null, "가맹:사용안함");
            }
            ResolvedExtSettle fromPick = buildFromFields(m, pick.getExtSettleLag(), pick.getExtSettleBatchTime(), "가맹:덮어쓰기");
            return fromPick;
        }
        if (agency == null) {
            return null;
        }
        return buildFromFields(
                ExtSettleMode.parse(agency.getExtSettleMode()),
                agency.getExtSettleLag(),
                agency.getExtSettleBatchTime(),
                "연동기본:" + agency.getPgCd());
    }

    private static MerchantPgBinding pickChillBinding(List<MerchantPgBinding> binds, String rowMid) {
        if (binds == null || binds.isEmpty()) {
            return null;
        }
        String midNorm = rowMid != null ? rowMid.trim() : "";
        List<MerchantPgBinding> chill = binds.stream()
                .filter(b -> b.getPgCd() != null && PgVendor.isChillPayFamily(b.getPgCd()))
                .toList();
        if (chill.isEmpty()) {
            return null;
        }
        if (!midNorm.isEmpty()) {
            Optional<MerchantPgBinding> exactOp = chill.stream()
                    .filter(b -> b.getMid() != null && b.getMid().trim().equalsIgnoreCase(midNorm))
                    .filter(b -> "Y".equalsIgnoreCase(String.valueOf(b.getOperationalYn()).trim()))
                    .findFirst();
            if (exactOp.isPresent()) {
                return exactOp.get();
            }
            exactOp = chill.stream()
                    .filter(b -> b.getMid() != null && b.getMid().trim().equalsIgnoreCase(midNorm))
                    .findFirst();
            if (exactOp.isPresent()) {
                return exactOp.get();
            }
        }
        return chill.stream()
                .filter(b -> "Y".equalsIgnoreCase(String.valueOf(b.getOperationalYn()).trim()))
                .findFirst()
                .orElse(chill.get(0));
    }

    private static ResolvedExtSettle buildFromFields(
            ExtSettleMode mode,
            Integer lag,
            LocalTime batchTime,
            String sourceLabel) {
        if (mode == null || mode == ExtSettleMode.OFF) {
            return new ResolvedExtSettle(ExtSettleMode.OFF, null, null, nvl(sourceLabel));
        }
        if (lag == null || lag < 1 || lag > 10) {
            return null;
        }
        if (mode == ExtSettleMode.D && batchTime == null) {
            return null;
        }
        String label = nvl(sourceLabel) + "(" + mode.name() + "+" + lag + ")";
        return new ResolvedExtSettle(mode, lag, batchTime, label);
    }

    private static String nvl(String s) {
        return s != null ? s : "";
    }

    private static LocalDateTime computeExpected(LocalDateTime paymentWallClock, ResolvedExtSettle r) {
        if (paymentWallClock == null || r == null || r.mode() == ExtSettleMode.OFF) {
            return null;
        }
        int n = r.lag() != null ? r.lag() : 0;
        if (n < 1 || n > 10) {
            return null;
        }
        if (r.mode() == ExtSettleMode.T) {
            LocalDate d = addBusinessDays(paymentWallClock.toLocalDate(), n);
            return LocalDateTime.of(d, paymentWallClock.toLocalTime());
        }
        if (r.mode() == ExtSettleMode.D && r.batchTime() != null) {
            LocalDate d = paymentWallClock.toLocalDate().plusDays(n);
            return LocalDateTime.of(d, r.batchTime());
        }
        return null;
    }

    /** 주말 제외 영업일 가산 (공휴일 미반영) — {@link SettlementReportService#addBusinessDays} 와 동일 */
    private static LocalDate addBusinessDays(LocalDate start, int businessDays) {
        LocalDate d = start;
        int added = 0;
        while (added < businessDays) {
            d = d.plusDays(1);
            DayOfWeek w = d.getDayOfWeek();
            if (w != DayOfWeek.SATURDAY && w != DayOfWeek.SUNDAY) {
                added++;
            }
        }
        return d;
    }

    private static LocalDateTime parseChillPayApiDateTime(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw.trim(), CHILLPAY_TXN_DT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String firstNonBlankString(Map<String, Object> m, String... keys) {
        if (m == null || keys == null) {
            return "";
        }
        for (String k : keys) {
            Object v = m.get(k);
            if (v == null) {
                continue;
            }
            String s = String.valueOf(v).trim();
            if (!s.isEmpty()) {
                return s;
            }
        }
        return "";
    }

    private static String chillTrMerchantRaw(Map<String, Object> m) {
        return firstNonBlankString(m,
                "merchant", "Merchant", "merchantCode", "MerchantCode",
                "Mid", "MID", "MerchantID", "merchantID");
    }

    private enum ExtSettleMode {
        OFF, T, D;

        static ExtSettleMode parse(String raw) {
            if (raw == null || raw.isBlank()) {
                return OFF;
            }
            String u = raw.trim().toUpperCase(Locale.ROOT);
            if ("T".equals(u)) {
                return T;
            }
            if ("D".equals(u)) {
                return D;
            }
            return OFF;
        }
    }

    private record ResolvedExtSettle(ExtSettleMode mode, Integer lag, LocalTime batchTime, String ruleLabel) { }
}
