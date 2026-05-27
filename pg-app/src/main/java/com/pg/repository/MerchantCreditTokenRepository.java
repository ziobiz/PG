package com.pg.repository;

import com.pg.entity.MerchantCreditToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MerchantCreditTokenRepository extends JpaRepository<MerchantCreditToken, Long> {

    List<MerchantCreditToken> findByOrgUnitIdAndPgCdAndCustomerIdAndActiveYnOrderByUpdatedAtDesc(
            Long orgUnitId, String pgCd, String customerId, String activeYn);

    Optional<MerchantCreditToken> findFirstByOrgUnitIdAndPgCdAndCustomerIdAndCreditToken(
            Long orgUnitId, String pgCd, String customerId, String creditToken);
}
