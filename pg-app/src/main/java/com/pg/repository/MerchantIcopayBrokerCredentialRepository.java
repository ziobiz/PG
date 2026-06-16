package com.pg.repository;

import com.pg.entity.MerchantIcopayBrokerCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MerchantIcopayBrokerCredentialRepository extends JpaRepository<MerchantIcopayBrokerCredential, Long> {

    List<MerchantIcopayBrokerCredential> findByOrgUnitIdAndUseYnOrderByIdDesc(Long orgUnitId, String useYn);

    Optional<MerchantIcopayBrokerCredential> findByOrgUnitIdAndVendorScope(
            Long orgUnitId, String vendorScope);

    Optional<MerchantIcopayBrokerCredential> findByOrgUnitIdAndVendorScopeAndUseYn(
            Long orgUnitId, String vendorScope, String useYn);

    Optional<MerchantIcopayBrokerCredential> findByBrokerSecretAndUseYn(String brokerSecret, String useYn);

    List<MerchantIcopayBrokerCredential> findByOrgUnitIdInAndUseYn(Collection<Long> orgUnitIds, String useYn);
}
