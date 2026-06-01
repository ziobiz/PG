package com.pg.repository;

import com.pg.entity.HqPayCardBlacklist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HqPayCardBlacklistRepository extends JpaRepository<HqPayCardBlacklist, Long> {

    List<HqPayCardBlacklist> findByActiveYnOrderByIdDesc(String activeYn);

    Page<HqPayCardBlacklist> findByActiveYnOrderByIdDesc(String activeYn, Pageable pageable);

    @Query("""
            SELECT b FROM HqPayCardBlacklist b
            WHERE b.activeYn = 'Y' AND b.panHash = :hash
            AND (b.pgVendor IS NULL OR TRIM(b.pgVendor) = '' OR UPPER(TRIM(b.pgVendor)) = UPPER(TRIM(:pg)))
            """)
    Optional<HqPayCardBlacklist> findActiveHit(@Param("hash") String panHash, @Param("pg") String pgVendor);
}
