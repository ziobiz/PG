package com.pg.repository;

import com.pg.entity.MerchantPgBinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MerchantPgBindingRepository extends JpaRepository<MerchantPgBinding, Long> {
    List<MerchantPgBinding> findByOrgUnitIdOrderBySortOrderAsc(Long orgUnitId);
    void deleteByOrgUnitId(Long orgUnitId);
    Optional<MerchantPgBinding> findFirstByOrgUnitIdAndPgCdAndOperationalYn(Long orgUnitId, String pgCd, String operationalYn);

    Optional<MerchantPgBinding> findByIdAndOrgUnitId(Long id, Long orgUnitId);

    List<MerchantPgBinding> findByMidOrderByOperationalYnDescIdAsc(String mid);

    boolean existsByOrgUnitIdAndPgCdAndPayMethod(Long orgUnitId, String pgCd, String payMethod);

    boolean existsByOrgUnitIdAndPgCdAndPayMethodAndIdNot(Long orgUnitId, String pgCd, String payMethod, Long id);

    /** PG사 삭제 전: 가맹점 결제대행사 설정 참조 여부 */
    boolean existsByPgCd(String pgCd);
}
