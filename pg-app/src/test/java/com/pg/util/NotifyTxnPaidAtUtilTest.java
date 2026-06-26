package com.pg.util;

import com.pg.entity.PgTrnsctn;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotifyTxnPaidAtUtilTest {

    @Test
    void resolvePaidAt_usesCreatedAtWhenNotifyDateMissing() {
        PgTrnsctn t = new PgTrnsctn();
        t.setCreatedAt(LocalDateTime.of(2026, 6, 23, 14, 30, 0));
        LocalDateTime out = NotifyTxnPaidAtUtil.resolvePaidAtForApproval(t, null, ZoneId.of("Asia/Bangkok"));
        assertEquals(t.getCreatedAt(), out);
    }

    @Test
    void applyTrnDateOverride_keepsTime() {
        PgTrnsctn t = new PgTrnsctn();
        t.setPaidAt(LocalDateTime.of(2026, 6, 26, 9, 15, 30));
        NotifyTxnPaidAtUtil.applyTrnDateOverride(t, LocalDate.of(2026, 6, 23), ZoneId.of("Asia/Bangkok"));
        assertEquals(LocalDateTime.of(2026, 6, 23, 9, 15, 30), t.getPaidAt());
    }
}
