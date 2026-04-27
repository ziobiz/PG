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

    /** 총판 조직에 연결된 본사 노티 수신 URL 행(필수 CALLBACK·RESULT 판별용). */
    List<HqNotifyTarget> findByOrgUnitIdOrderByIdAsc(Long orgUnitId);

    List<HqNotifyTarget> findByTargetNameOrderByIdAsc(String targetName);

    Optional<HqNotifyTarget> findByTargetCode(String targetCode);

    Optional<HqNotifyTarget> findByTargetUrl(String targetUrl);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update HqNotifyTarget t set t.orgUnitId = null where t.orgUnitId = :orgUnitId")
    void clearOrgUnitIdByOrgUnitId(@Param("orgUnitId") Long orgUnitId);
}

