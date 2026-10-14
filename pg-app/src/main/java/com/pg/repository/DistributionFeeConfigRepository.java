package com.pg.repository;

import com.pg.entity.DistributionFeeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DistributionFeeConfigRepository extends JpaRepository<DistributionFeeConfig, Long> {

    /**
     * 레거시 호환: FIAT 채널 행만 (comp_id 당 CRYPTO 행이 생겨도 NonUnique 방지).
     */
    @Query("SELECT d FROM DistributionFeeConfig d WHERE d.compId = :compId AND UPPER(COALESCE(d.feeChannel, 'FIAT')) = 'FIAT'")
    Optional<DistributionFeeConfig> findByCompId(@Param("compId") String compId);

    Optional<DistributionFeeConfig> findByCompIdAndFeeChannel(String compId, String feeChannel);

    @Query("SELECT d FROM DistributionFeeConfig d WHERE d.compId IN :compIds AND UPPER(COALESCE(d.feeChannel, 'FIAT')) = 'FIAT'")
    List<DistributionFeeConfig> findByCompIdIn(@Param("compIds") Collection<String> compIds);

    List<DistributionFeeConfig> findByCompIdInAndFeeChannel(Collection<String> compIds, String feeChannel);
}
