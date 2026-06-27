package com.pg.repository;

import com.pg.entity.JpayPortalAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpayPortalAccountRepository extends JpaRepository<JpayPortalAccount, Long> {

    List<JpayPortalAccount> findByUseYnOrderBySortOrderAscIdAsc(String useYn);

    List<JpayPortalAccount> findAllByOrderBySortOrderAscIdAsc();

    Optional<JpayPortalAccount> findByMasterOrgUnitId(Long masterOrgUnitId);

    Optional<JpayPortalAccount> findByMasterOrgUnitIdAndPgCd(Long masterOrgUnitId, String pgCd);

    Optional<JpayPortalAccount> findByMasterOrgUnitIdAndPortalUsername(Long masterOrgUnitId, String portalUsername);

    long countByMasterOrgUnitId(Long masterOrgUnitId);
}
