package com.pg.repository;

import com.pg.entity.PgAgency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PgAgencyRepository extends JpaRepository<PgAgency, Long> {
    Optional<PgAgency> findByPgCd(String pgCd);
    List<PgAgency> findByMerchantMidOrderByIdAsc(String merchantMid);
    List<PgAgency> findByUseYnOrderByPgCdAsc(String useYn);
    List<PgAgency> findAllByOrderByPgCdAsc();
}
