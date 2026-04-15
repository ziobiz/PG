package com.pg.repository;

import com.pg.entity.SettlementSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SettlementSettingRepository extends JpaRepository<SettlementSetting, Long> {
    Optional<SettlementSetting> findByOrgUnitId(Long orgUnitId);

    List<SettlementSetting> findByOrgUnitIdIn(Collection<Long> orgUnitIds);

    List<SettlementSetting> findByReceivableRecoveryModeIgnoreCase(String receivableRecoveryMode);

    List<SettlementSetting> findByCalcCycle(String calcCycle);

    /** 가맹·정산구분 AUTO·calc_cycle 유효 건수(정산일정 요약용) */
    @Query(value = """
            SELECT ss.calc_cycle AS cycle_code, COUNT(*)::bigint AS cnt
            FROM tb_settlement_setting ss
            INNER JOIN tb_org_unit ou ON ou.id = ss.org_unit_id
            WHERE ou.org_level = 'MERCHANT'
              AND UPPER(TRIM(COALESCE(ss.calc_proc_type, ''))) = 'AUTO'
              AND ss.calc_cycle IS NOT NULL
              AND TRIM(ss.calc_cycle) <> ''
              AND UPPER(TRIM(ss.calc_cycle)) <> 'NONE'
            GROUP BY ss.calc_cycle
            """, nativeQuery = true)
    List<Object[]> countAutoMerchantsByCalcCycleNative();
}
