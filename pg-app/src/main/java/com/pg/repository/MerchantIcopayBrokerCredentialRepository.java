package com.pg.repository;

import com.pg.entity.MerchantIcopayBrokerCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MerchantIcopayBrokerCredentialRepository extends JpaRepository<MerchantIcopayBrokerCredential, Long> {

    List<MerchantIcopayBrokerCredential> findByOrgUnitIdAndUseYnOrderByIdDesc(Long orgUnitId, String useYn);

    Optional<MerchantIcopayBrokerCredential> findByOrgUnitIdAndVendorScopeAndUseYn(
            Long orgUnitId, String vendorScope, String useYn);

    Optional<MerchantIcopayBrokerCredential> findByBrokerSecretAndUseYn(String brokerSecret, String useYn);
}
