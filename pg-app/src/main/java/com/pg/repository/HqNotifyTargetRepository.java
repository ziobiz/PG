package com.pg.repository;

import com.pg.entity.HqNotifyTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HqNotifyTargetRepository extends JpaRepository<HqNotifyTarget, Long> {
    List<HqNotifyTarget> findAllByOrderByIdDesc();
    Optional<HqNotifyTarget> findByTargetCode(String targetCode);

    Optional<HqNotifyTarget> findByTargetUrl(String targetUrl);

    @Modifying
    @Query("update HqNotifyTarget t set t.orgUnitId = null where t.orgUnitId = :orgUnitId")
    void clearOrgUnitIdByOrgUnitId(@Param("orgUnitId") Long orgUnitId);
}

