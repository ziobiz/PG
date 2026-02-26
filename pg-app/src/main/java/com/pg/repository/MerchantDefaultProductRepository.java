package com.pg.repository;

import com.pg.entity.MerchantDefaultProduct;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MerchantDefaultProductRepository extends JpaRepository<MerchantDefaultProduct, Long> {
    Optional<MerchantDefaultProduct> findByOrgUnitId(Long orgUnitId);
}
