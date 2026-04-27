package com.pg.repository;

import com.pg.entity.MerchantNotifyUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MerchantNotifyUrlRepository extends JpaRepository<MerchantNotifyUrl, Long> {
    List<MerchantNotifyUrl> findByOrgUnitIdOrderByUrlTypeAsc(Long orgUnitId);

    Optional<MerchantNotifyUrl> findByOrgUnitIdAndUrlType(Long orgUnitId, String urlType);

    void deleteByOrgUnitIdAndUrlTypeIn(Long orgUnitId, java.util.Collection<String> urlTypes);

    /**
     * 노티 수신 경로 코드(cb…/rs…)가 포함된 {@code noti_url} 을 가진 조직(총판 NOTIFY_1·2 등) — 본사 노티대상 행의
     * {@code org_unit_id} 가 비어 있을 때 ingress 총판 스코프 보강용. 단일 행만 신뢰합니다.
     */
    @Query("SELECT DISTINCT m.orgUnitId FROM MerchantNotifyUrl m WHERE m.notiUrl IS NOT NULL "
            + "AND m.notiUrl LIKE CONCAT('%/', :segment, '%') "
            + "AND (m.useYn IS NULL OR UPPER(TRIM(m.useYn)) = 'Y')")
    List<Long> findDistinctOrgUnitIdsByNotiUrlContainingSegment(@Param("segment") String segment);
}
