package com.pg.repository;

import com.pg.entity.HqPayCardBlockBrand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HqPayCardBlockBrandRepository extends JpaRepository<HqPayCardBlockBrand, Long> {

    List<HqPayCardBlockBrand> findByActiveYnOrderByPgVendorAscBrandCodeAsc(String activeYn);

    List<HqPayCardBlockBrand> findByPgVendorAndActiveYnOrderByBrandCodeAsc(String pgVendor, String activeYn);

    boolean existsByPgVendorAndBrandCodeAndActiveYn(String pgVendor, String brandCode, String activeYn);

    java.util.Optional<HqPayCardBlockBrand> findFirstByPgVendorAndBrandCodeOrderByIdDesc(String pgVendor, String brandCode);
}
