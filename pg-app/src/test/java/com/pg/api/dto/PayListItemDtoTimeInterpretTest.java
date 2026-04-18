package com.pg.api.dto;



import com.pg.entity.PgTrnsctn;

import com.pg.integration.pg.PgVendor;

import org.junit.jupiter.api.Test;



import java.time.LocalDateTime;

import java.time.ZoneId;

import java.util.Map;



import static org.junit.jupiter.api.Assertions.assertEquals;



/**

 * {@link PayListItemDto}: naive 시각은 전달된 표준 구역(전산설정과 동일)으로 해석하고, 표준 한 줄 + JP 한 줄로 표시한다.

 */

class PayListItemDtoTimeInterpretTest {



    @Test

    void createdAtOnly_thb_chillPay_interpretsAsBangkok() {

        LocalDateTime bkkWall = LocalDateTime.of(2026, 4, 18, 12, 0, 0);

        PgTrnsctn t = new PgTrnsctn();

        t.setTrnId("T1");

        t.setMerchantId("6000000001");

        t.setVan(PgVendor.CHILLPAY);

        t.setCurType("THB");

        t.setPaidAt(null);

        t.setCreatedAt(bkkWall);



        Map<String, Object> row = PayListItemDto.from(t, null, ZoneId.of("Asia/Bangkok"));

        assertEquals("JP 14:00:00\nTH 12:00:00", row.get("trnTime"));

    }



    @Test

    void paidAt_thb_chillPay_bangkokWall_dualLine() {

        LocalDateTime bkkWall = LocalDateTime.of(2026, 4, 18, 23, 34, 18);

        PgTrnsctn t = new PgTrnsctn();

        t.setTrnId("T2");

        t.setMerchantId("6000000001");

        t.setVan(PgVendor.CHILLPAY);

        t.setCurType("THB");

        t.setPaidAt(bkkWall);

        t.setCreatedAt(LocalDateTime.of(2026, 4, 18, 23, 40, 0));



        Map<String, Object> row = PayListItemDto.from(t, null, ZoneId.of("Asia/Bangkok"));

        assertEquals("JP 01:34:18\nTH 23:34:18", row.get("trnTime"));

    }



    @Test

    void twoArg_from_defaults_to_bangkok_interpret() {

        LocalDateTime bkkWall = LocalDateTime.of(2026, 4, 18, 12, 0, 0);

        PgTrnsctn t = new PgTrnsctn();

        t.setTrnId("T0");

        t.setMerchantId("6000000001");

        t.setCurType("THB");

        t.setPaidAt(bkkWall);

        Map<String, Object> row = PayListItemDto.from(t, null);

        assertEquals("JP 14:00:00\nTH 12:00:00", row.get("trnTime"));

    }



    @Test

    void explicit_tokyo_zone_naive_matches_tokyo_wall() {

        LocalDateTime tokyoWall = LocalDateTime.of(2026, 4, 18, 15, 0, 0);

        PgTrnsctn t = new PgTrnsctn();

        t.setTrnId("T3");

        t.setMerchantId("6000000001");

        t.setVan(PgVendor.CHILLPAY);

        t.setCurType("JPY");

        t.setPaidAt(tokyoWall);

        t.setCreatedAt(tokyoWall);



        Map<String, Object> row = PayListItemDto.from(t, null, ZoneId.of("Asia/Tokyo"));

        assertEquals("JP 15:00:00\nTH 13:00:00", row.get("trnTime"));

    }

}

