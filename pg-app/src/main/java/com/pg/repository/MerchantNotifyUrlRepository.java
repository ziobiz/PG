package com.pg.repository;

import com.pg.entity.MerchantNotifyUrl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MerchantNotifyUrlRepository extends JpaRepository<MerchantNotifyUrl, Long> {
    List<MerchantNotifyUrl> findByOrgUnitIdOrderByUrlTypeAsc(Long orgUnitId);
}
