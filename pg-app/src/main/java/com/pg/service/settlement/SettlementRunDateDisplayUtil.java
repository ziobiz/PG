package com.pg.service.settlement;

import com.pg.entity.SettlementRun;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** 정산 실행 행 API — 마감일(settlementCloseDate)과 배치 예정일(settlementExecDate) 표시. */
public final class SettlementRunDateDisplayUtil {

    private SettlementRunDateDisplayUtil() {}

    public static void putCloseAndExecDates(Map<String, Object> m,
                                            SettlementRun r,
                                            String calcCycleRaw,
                                            Set<LocalDate> holidays) {
        if (m == null || r == null) {
            return;
        }
        LocalDate close = r.getPeriodTo() != null ? r.getPeriodTo() : r.getCalcDt();
        String closeStr = close != null ? close.toString() : "";
        m.put("settlementCloseDate", closeStr);
        Set<LocalDate> hol = holidays != null ? holidays : Collections.emptySet();
        String cycle = calcCycleRaw != null ? calcCycleRaw : "";
        Optional<LocalDate> exec = SettlementExpectedDateResolver.resolveExpectedSettlementDateForRun(
                r.getCalcDt(), r.getPeriodFrom(), r.getPeriodTo(), cycle, hol);
        m.put("settlementExecDate", exec.map(LocalDate::toString).orElse(closeStr));
    }
}
