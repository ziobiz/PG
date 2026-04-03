package com.pg.repository;

import com.pg.entity.MerchantPgBinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MerchantPgBindingRepository extends JpaRepository<MerchantPgBinding, Long> {
    List<MerchantPgBinding> findByOrgUnitIdOrderBySortOrderAsc(Long orgUnitId);

    /** 삭제 후 즉시 flush — 재삽입 시 (org_unit_id, pg_cd, pay_method) 유니크 충돌 방지 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from MerchantPgBinding b where b.orgUnitId = :orgUnitId")
    void deleteByOrgUnitId(@Param("orgUnitId") Long orgUnitId);
    Optional<MerchantPgBinding> findFirstByOrgUnitIdAndPgCdAndOperationalYn(Long orgUnitId, String pgCd, String operationalYn);

    Optional<MerchantPgBinding> findByIdAndOrgUnitId(Long id, Long orgUnitId);

    List<MerchantPgBinding> findByMidOrderByOperationalYnDescIdAsc(String mid);

    boolean existsByOrgUnitIdAndPgCdAndPayMethod(Long orgUnitId, String pgCd, String payMethod);

    boolean existsByOrgUnitIdAndPgCdAndPayMethodAndIdNot(Long orgUnitId, String pgCd, String payMethod, Long id);

    /** PG사 삭제 전: 가맹점 결제대행사 설정 참조 여부 */
    boolean existsByPgCd(String pgCd);
}
