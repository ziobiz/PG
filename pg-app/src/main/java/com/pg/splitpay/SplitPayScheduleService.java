package com.pg.splitpay;

import com.pg.entity.SplitPayContract;
import com.pg.util.BusinessDayCalendar;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class SplitPayScheduleService {

    public List<LocalDate> buildScheduledDates(LocalDate contractDate,
                                               int installmentCount,
                                               String intervalType,
                                               int intervalValue,
                                               Set<LocalDate> holidays) {
        if (contractDate == null) {
            contractDate = LocalDate.now();
        }
        List<LocalDate> raw = new ArrayList<>(installmentCount);
        for (int i = 1; i <= installmentCount; i++) {
            LocalDate d = switch (intervalType != null ? intervalType.trim().toUpperCase() : SplitPayContract.INTERVAL_MONTH) {
                case SplitPayContract.INTERVAL_DAY -> contractDate.plusDays((long) (i - 1) * Math.max(intervalValue, 1));
                case SplitPayContract.INTERVAL_MULTI -> contractDate.plusMonths((long) (i - 1));
                default -> contractDate.plusMonths((long) (i - 1) * Math.max(intervalValue, 1));
            };
            raw.add(BusinessDayCalendar.adjustToNextBusinessDay(d, holidays));
        }
        return raw;
    }
}
