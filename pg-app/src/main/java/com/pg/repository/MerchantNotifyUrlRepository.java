package com.pg.repository;

import com.pg.entity.MerchantNotifyUrl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MerchantNotifyUrlRepository extends JpaRepository<MerchantNotifyUrl, Long> {
    List<MerchantNotifyUrl> findByOrgUnitIdOrderByUrlTypeAsc(Long orgUnitId);

    Optional<MerchantNotifyUrl> findByOrgUnitIdAndUrlType(Long orgUnitId, String urlType);

    void deleteByOrgUnitIdAndUrlTypeIn(Long orgUnitId, java.util.Collection<String> urlTypes);
}
