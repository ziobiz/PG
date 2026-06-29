package com.pg.service;

import com.pg.entity.PgTrnsctn;
import com.pg.repository.PgTrnsctnRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpayPendingReconcileSchedulerTest {

    @Mock
    private PgTrnsctnRepository pgTrnsctnRepository;

    @Mock
    private JpayTradeApiService jpayTradeApiService;

    @Mock
    private HqLedgerSysSettingsService hqLedgerSysSettingsService;

    @InjectMocks
    private JpayPendingReconcileScheduler scheduler;

    @Test
    void reconcileStalePending_queriesAndApplies() {
        ReflectionTestUtils.setField(scheduler, "enabled", true);
        ReflectionTestUtils.setField(scheduler, "maxAgeDays", 14);
        ReflectionTestUtils.setField(scheduler, "batchSize", 10);
        when(hqLedgerSysSettingsService.resolveJpayPendingAutoCancelMin()).thenReturn(30);

        PgTrnsctn t = new PgTrnsctn();
        t.setTrnId("T1");
        t.setOrderNo("ORD1");
        when(pgTrnsctnRepository.findStaleJpayPendingForReconcile(
                any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of(t));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updated", true);
        when(jpayTradeApiService.queryAndApplyToTxn("T1")).thenReturn(result);

        scheduler.reconcileStalePending();

        verify(jpayTradeApiService).queryAndApplyToTxn("T1");
    }

    @Test
    void reconcileStalePending_skipsWhenDisabled() {
        ReflectionTestUtils.setField(scheduler, "enabled", false);

        scheduler.reconcileStalePending();

        verify(pgTrnsctnRepository, never()).findStaleJpayPendingForReconcile(
                any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class));
        verify(jpayTradeApiService, never()).queryAndApplyToTxn(any());
    }

    @Test
    void reconcileStalePending_skipsWhenLedgerSettingOff() {
        ReflectionTestUtils.setField(scheduler, "enabled", true);
        when(hqLedgerSysSettingsService.resolveJpayPendingAutoCancelMin()).thenReturn(0);

        scheduler.reconcileStalePending();

        verify(pgTrnsctnRepository, never()).findStaleJpayPendingForReconcile(
                any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class));
        verify(jpayTradeApiService, never()).queryAndApplyToTxn(any());
    }
}
