package com.pg.repository;

import com.pg.entity.MerchantProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MerchantProfileRepository extends JpaRepository<MerchantProfile, Long> {

    Optional<MerchantProfile> findByOrgUnitId(Long orgUnitId);

    Optional<MerchantProfile> findByLoginId(String loginId);

    @Query("select mp.orgUnitId from MerchantProfile mp where mp.regNo is not null and lower(mp.regNo) like lower(concat('%', :q, '%'))")
    List<Long> findOrgUnitIdsByRegNoContainingIgnoreCase(@Param("q") String q);
}

