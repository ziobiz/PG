package com.pg.repository;

import com.pg.entity.HqPayCardBlockPrefix;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HqPayCardBlockPrefixRepository extends JpaRepository<HqPayCardBlockPrefix, Long> {

    List<HqPayCardBlockPrefix> findByActiveYnOrderByPgVendorAscPrefixDigitsAsc(String activeYn);

    List<HqPayCardBlockPrefix> findByPgVendorAndActiveYnOrderByPrefixDigitsAsc(String pgVendor, String activeYn);

    boolean existsByPgVendorAndPrefixDigits(String pgVendor, String prefixDigits);
}
