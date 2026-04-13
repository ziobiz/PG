package com.pg.repository;

import com.pg.entity.MerchantPgBinding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MerchantPgBindingRepository extends JpaRepository<MerchantPgBinding, Long> {
    List<MerchantPgBinding> findByOrgUnitIdOrderBySortOrderAsc(Long orgUnitId);

    @Query("SELECT b FROM MerchantPgBinding b WHERE b.orgUnitId IN :ids ORDER BY b.orgUnitId ASC, b.sortOrder ASC")
    List<MerchantPgBinding> findByOrgUnitIdInOrderByOrgUnitIdAscSortOrderAsc(@Param("ids") Collection<Long> ids);

    /** 삭제 후 즉시 flush — 재삽입 시 (org_unit_id, pg_cd, pay_method) 유니크 충돌 방지 */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from MerchantPgBinding b where b.orgUnitId = :orgUnitId")
    void deleteByOrgUnitId(@Param("orgUnitId") Long orgUnitId);
    Optional<MerchantPgBinding> findFirstByOrgUnitIdAndPgCdAndOperationalYn(Long orgUnitId, String pgCd, String operationalYn);

    Optional<MerchantPgBinding> findByIdAndOrgUnitId(Long id, Long orgUnitId);

    List<MerchantPgBinding> findByMidOrderByOperationalYnDescIdAsc(String mid);

    /** 통합내역 등: ChillPay MID 문자열이 DB와 대소문자만 다를 때 */
    List<MerchantPgBinding> findByMidIgnoreCaseOrderByOperationalYnDescIdAsc(String mid);

    boolean existsByOrgUnitIdAndPgCdAndPayMethod(Long orgUnitId, String pgCd, String payMethod);

    boolean existsByOrgUnitIdAndPgCdAndPayMethodAndIdNot(Long orgUnitId, String pgCd, String payMethod, Long id);

    /** PG사 삭제 전: 가맹점 결제대행사 설정 참조 여부 */
    boolean existsByPgCd(String pgCd);

    @Query("select distinct o.code from MerchantPgBinding b, OrgUnit o where o.id = b.orgUnitId and b.mid is not null and lower(b.mid) like lower(concat('%', :q, '%'))")
    List<String> findMerchantCodesByMidContaining(@Param("q") String q);

    @Query("select distinct o.code from MerchantPgBinding b, OrgUnit o where o.id = b.orgUnitId and b.pgCd = :pgCd")
    List<String> findMerchantCodesByPgCd(@Param("pgCd") String pgCd);

    /** 노티 수신 키 수집: 해당 PG 코드(또는 CHILLPAY_… 접두)에 연결된 MID 목록 */
    @Query("select distinct trim(b.mid) from MerchantPgBinding b where trim(coalesce(b.mid,'')) <> '' and upper(b.pgCd) like upper(concat(:vendorCode, '%'))")
    List<String> findDistinctMidsByPgCdLikeVendor(@Param("vendorCode") String vendorCode);
}
