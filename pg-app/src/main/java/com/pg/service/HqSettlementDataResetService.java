package com.pg.service;

import com.pg.repository.BalanceDeductionRepository;
import com.pg.repository.MerchantReceivableRecoveryRequestRepository;
import com.pg.repository.MerchantReceivableRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.repository.RollingReserveRepository;
import com.pg.repository.SettlementRecoveryRepository;
import com.pg.repository.SettlementRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 본사 전산설정 — 정산 운영 데이터만 삭제.
 * <p><b>유지:</b> 수수료내역({@code tb_commission_history}), 거래 원장({@code pg_trnsctn} 행 자체),
 * 통합정산(칠페이 API 원문·ICOPAY DB 비저장), 본사·가맹 <strong>정산 설정</strong>
 * ({@code tb_settlement_setting}, {@code tb_commission_policy}, {@code tb_distribution_fee_config},
 * {@code tb_hq_settlement_cycle_def}, {@code tb_master_dist_settlement_cycle_config} 등 HQ가 세팅한 정책 테이블).
 * <p><b>삭제(범위별):</b> 정산 실행·가맹정산내역·정산보류·유통망 집계의 근거가 되는 실행 행, 담보(롤링),
 * 환수금, 미수금·미수금 환수요청, 잔액 공제 로그, 거래의 {@code settled_yn}만 N으로 초기화.
 */
@Service
public class HqSettlementDataResetService {

    private static final Logger log = LoggerFactory.getLogger(HqSettlementDataResetService.class);

    public enum Scope {
        /** 미수금 환수요청 + 미수금 */
        RECEIVABLES,
        /** 환수금(tb_settlement_recovery) */
        RECOVERY,
        /** 담보·롤링(tb_rolling_reserve) */
        ROLLING,
        /** 잔액 공제 로그(tb_balance_deduction) */
        DEDUCTIONS,
        /**
         * 정산 실행 및 연동: 미수·환수·담보·공제·실행 행 + 거래 settled_yn 초기화.
         * (통합정산·수수료내역·본사 정산 설정은 유지)
         */
        RUNS,
        /** RUNS 와 동일(전체 정산 운영 데이터 일괄) */
        ALL
    }

    private final MerchantReceivableRecoveryRequestRepository merchantReceivableRecoveryRequestRepository;
    private final MerchantReceivableRepository merchantReceivableRepository;
    private final SettlementRecoveryRepository settlementRecoveryRepository;
    private final RollingReserveRepository rollingReserveRepository;
    private final BalanceDeductionRepository balanceDeductionRepository;
    private final SettlementRunRepository settlementRunRepository;
    private final PgTrnsctnRepository pgTrnsctnRepository;

    public HqSettlementDataResetService(
            MerchantReceivableRecoveryRequestRepository merchantReceivableRecoveryRequestRepository,
            MerchantReceivableRepository merchantReceivableRepository,
            SettlementRecoveryRepository settlementRecoveryRepository,
            RollingReserveRepository rollingReserveRepository,
            BalanceDeductionRepository balanceDeductionRepository,
            SettlementRunRepository settlementRunRepository,
            PgTrnsctnRepository pgTrnsctnRepository) {
        this.merchantReceivableRecoveryRequestRepository = merchantReceivableRecoveryRequestRepository;
        this.merchantReceivableRepository = merchantReceivableRepository;
        this.settlementRecoveryRepository = settlementRecoveryRepository;
        this.rollingReserveRepository = rollingReserveRepository;
        this.balanceDeductionRepository = balanceDeductionRepository;
        this.settlementRunRepository = settlementRunRepository;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
    }

    public static Scope parseScope(String raw) {
        if (raw == null || raw.isBlank()) {
            return Scope.ALL;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        return switch (u) {
            case "RECEIVABLES" -> Scope.RECEIVABLES;
            case "RECOVERY" -> Scope.RECOVERY;
            case "ROLLING" -> Scope.ROLLING;
            case "DEDUCTIONS" -> Scope.DEDUCTIONS;
            case "RUNS" -> Scope.RUNS;
            case "ALL" -> Scope.ALL;
            default -> throw new IllegalArgumentException(
                    "scope는 ALL, RUNS, RECEIVABLES, RECOVERY, ROLLING, DEDUCTIONS 중 하나입니다.");
        };
    }

    @Transactional
    public Map<String, Object> reset(Scope scope) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("scope", scope.name());
        long rr = 0, mr = 0, sr = 0, roll = 0, bd = 0, runs = 0;
        int txFlags = 0;
        switch (scope) {
            case RECEIVABLES -> {
                rr = merchantReceivableRecoveryRequestRepository.count();
                mr = merchantReceivableRepository.count();
                merchantReceivableRecoveryRequestRepository.deleteAllInBatch();
                merchantReceivableRepository.deleteAllInBatch();
            }
            case RECOVERY -> {
                sr = settlementRecoveryRepository.count();
                settlementRecoveryRepository.deleteAllInBatch();
            }
            case ROLLING -> {
                roll = rollingReserveRepository.count();
                rollingReserveRepository.deleteAllInBatch();
            }
            case DEDUCTIONS -> {
                bd = balanceDeductionRepository.count();
                balanceDeductionRepository.deleteAllInBatch();
            }
            case RUNS, ALL -> {
                rr = merchantReceivableRecoveryRequestRepository.count();
                mr = merchantReceivableRepository.count();
                sr = settlementRecoveryRepository.count();
                roll = rollingReserveRepository.count();
                bd = balanceDeductionRepository.count();
                runs = settlementRunRepository.count();
                log.warn("HQ settlement data reset ({}): receivableReq={}, receivable={}, recovery={}, rolling={}, deduction={}, runs, then txn settled flags",
                        scope, rr, mr, sr, roll, bd);
                merchantReceivableRecoveryRequestRepository.deleteAllInBatch();
                merchantReceivableRepository.deleteAllInBatch();
                settlementRecoveryRepository.deleteAllInBatch();
                rollingReserveRepository.deleteAllInBatch();
                balanceDeductionRepository.deleteAllInBatch();
                settlementRunRepository.deleteAllInBatch();
                txFlags = pgTrnsctnRepository.clearAllSettlementFlagsOnTransactions();
            }
        }
        out.put("deletedReceivableRecoveryRequests", rr);
        out.put("deletedReceivables", mr);
        out.put("deletedSettlementRecoveries", sr);
        out.put("deletedRollingReserves", roll);
        out.put("deletedBalanceDeductions", bd);
        out.put("deletedSettlementRuns", runs);
        out.put("transactionsSettlementFlagCleared", txFlags);
        out.put("note", "tb_commission_history·pg_trnsctn 행·정산 설정·통합정산(외부)은 유지했습니다.");
        log.warn("HQ settlement data reset completed: scope={}", scope);
        return out;
    }
}
