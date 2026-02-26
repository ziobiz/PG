package com.pg.repository;

import com.pg.entity.MerchantPgBinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MerchantPgBindingRepository extends JpaRepository<MerchantPgBinding, Long> {
    List<MerchantPgBinding> findByOrgUnitIdOrderBySortOrderAsc(Long orgUnitId);
    void deleteByOrgUnitId(Long orgUnitId);
    Optional<MerchantPgBinding> findFirstByOrgUnitIdAndPgCdAndOperationalYn(Long orgUnitId, String pgCd, String operationalYn);
}
