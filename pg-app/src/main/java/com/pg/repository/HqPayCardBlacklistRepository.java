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

    @Query("""
            SELECT b FROM HqPayCardBlacklist b
            WHERE b.activeYn = 'Y' AND b.matchMode = 'MASK_6_4' AND b.panDisplay = :maskKey
            AND (b.pgVendor IS NULL OR TRIM(b.pgVendor) = '' OR UPPER(TRIM(b.pgVendor)) = UPPER(TRIM(:pg)))
            """)
    Optional<HqPayCardBlacklist> findActiveMaskDisplayHit(@Param("maskKey") String maskKey, @Param("pg") String pgVendor);

    @Query("""
            SELECT b FROM HqPayCardBlacklist b
            WHERE b.activeYn = 'Y'
            AND (b.panHash = :panHash OR b.panDisplay = :panDisplay)
            """)
    List<HqPayCardBlacklist> findActiveSiblingsByPanIdentity(@Param("panHash") String panHash,
                                                             @Param("panDisplay") String panDisplay);

    long countByRegisteredOrgUnitIdAndActiveYn(Long registeredOrgUnitId, String activeYn);

    long countByRegisteredOrgUnitIdAndActiveYnAndSource(Long registeredOrgUnitId, String activeYn, String source);

    @Query(value = """
            SELECT b.* FROM tb_hq_pay_card_blacklist b
            WHERE (:activeYn = 'ALL' OR b.active_yn = :activeYn)
            AND (CAST(:fromDate AS varchar) = '' OR b.created_at >= CAST(:fromDate AS date))
            AND (CAST(:toDate AS varchar) = '' OR b.created_at < (CAST(:toDate AS date) + INTERVAL '1 day'))
            AND (
              :keyword = ''
              OR COALESCE(b.registered_comp_nm, '') ILIKE CONCAT('%', :keyword, '%')
              OR COALESCE(b.registered_comp_id, '') ILIKE CONCAT('%', :keyword, '%')
              OR COALESCE(b.holder_name, '') ILIKE CONCAT('%', :keyword, '%')
              OR COALESCE(b.pan_display, '') ILIKE CONCAT('%', :keyword, '%')
              OR (
                LENGTH(:panDigits) >= 3
                AND regexp_replace(COALESCE(b.pan_display, ''), '[^0-9]', '', 'g')
                    LIKE CONCAT('%', :panDigits, '%')
              )
            )
            ORDER BY b.created_at DESC, b.id DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM tb_hq_pay_card_blacklist b
            WHERE (:activeYn = 'ALL' OR b.active_yn = :activeYn)
            AND (CAST(:fromDate AS varchar) = '' OR b.created_at >= CAST(:fromDate AS date))
            AND (CAST(:toDate AS varchar) = '' OR b.created_at < (CAST(:toDate AS date) + INTERVAL '1 day'))
            AND (
              :keyword = ''
              OR COALESCE(b.registered_comp_nm, '') ILIKE CONCAT('%', :keyword, '%')
              OR COALESCE(b.registered_comp_id, '') ILIKE CONCAT('%', :keyword, '%')
              OR COALESCE(b.holder_name, '') ILIKE CONCAT('%', :keyword, '%')
              OR COALESCE(b.pan_display, '') ILIKE CONCAT('%', :keyword, '%')
              OR (
                LENGTH(:panDigits) >= 3
                AND regexp_replace(COALESCE(b.pan_display, ''), '[^0-9]', '', 'g')
                    LIKE CONCAT('%', :panDigits, '%')
              )
            )
            """,
            nativeQuery = true)
    Page<HqPayCardBlacklist> searchFilteredDesc(@Param("activeYn") String activeYn,
                                                @Param("keyword") String keyword,
                                                @Param("panDigits") String panDigits,
                                                @Param("fromDate") String fromDate,
                                                @Param("toDate") String toDate,
                                                Pageable pageable);

    @Query(value = """
            SELECT b.* FROM tb_hq_pay_card_blacklist b
            WHERE (:activeYn = 'ALL' OR b.active_yn = :activeYn)
            AND (CAST(:fromDate AS varchar) = '' OR b.created_at >= CAST(:fromDate AS date))
            AND (CAST(:toDate AS varchar) = '' OR b.created_at < (CAST(:toDate AS date) + INTERVAL '1 day'))
            AND (
              :keyword = ''
              OR COALESCE(b.registered_comp_nm, '') ILIKE CONCAT('%', :keyword, '%')
              OR COALESCE(b.registered_comp_id, '') ILIKE CONCAT('%', :keyword, '%')
              OR COALESCE(b.holder_name, '') ILIKE CONCAT('%', :keyword, '%')
              OR COALESCE(b.pan_display, '') ILIKE CONCAT('%', :keyword, '%')
              OR (
                LENGTH(:panDigits) >= 3
                AND regexp_replace(COALESCE(b.pan_display, ''), '[^0-9]', '', 'g')
                    LIKE CONCAT('%', :panDigits, '%')
              )
            )
            ORDER BY b.created_at ASC, b.id ASC
            """,
            countQuery = """
            SELECT COUNT(*) FROM tb_hq_pay_card_blacklist b
            WHERE (:activeYn = 'ALL' OR b.active_yn = :activeYn)
            AND (CAST(:fromDate AS varchar) = '' OR b.created_at >= CAST(:fromDate AS date))
            AND (CAST(:toDate AS varchar) = '' OR b.created_at < (CAST(:toDate AS date) + INTERVAL '1 day'))
            AND (
              :keyword = ''
              OR COALESCE(b.registered_comp_nm, '') ILIKE CONCAT('%', :keyword, '%')
              OR COALESCE(b.registered_comp_id, '') ILIKE CONCAT('%', :keyword, '%')
              OR COALESCE(b.holder_name, '') ILIKE CONCAT('%', :keyword, '%')
              OR COALESCE(b.pan_display, '') ILIKE CONCAT('%', :keyword, '%')
              OR (
                LENGTH(:panDigits) >= 3
                AND regexp_replace(COALESCE(b.pan_display, ''), '[^0-9]', '', 'g')
                    LIKE CONCAT('%', :panDigits, '%')
              )
            )
            """,
            nativeQuery = true)
    Page<HqPayCardBlacklist> searchFilteredAsc(@Param("activeYn") String activeYn,
                                               @Param("keyword") String keyword,
                                               @Param("panDigits") String panDigits,
                                               @Param("fromDate") String fromDate,
                                               @Param("toDate") String toDate,
                                               Pageable pageable);

    @Query(value = """
            SELECT b.id FROM tb_hq_pay_card_blacklist b
            WHERE b.active_yn = 'Y'
            AND (:activeYn = 'ALL' OR :activeYn = 'Y')
            AND (CAST(:fromDate AS varchar) = '' OR b.created_at >= CAST(:fromDate AS date))
            AND (CAST(:toDate AS varchar) = '' OR b.created_at < (CAST(:toDate AS date) + INTERVAL '1 day'))
            AND (
              :keyword = ''
              OR COALESCE(b.registered_comp_nm, '') ILIKE CONCAT('%', :keyword, '%')
              OR COALESCE(b.registered_comp_id, '') ILIKE CONCAT('%', :keyword, '%')
              OR COALESCE(b.holder_name, '') ILIKE CONCAT('%', :keyword, '%')
              OR COALESCE(b.pan_display, '') ILIKE CONCAT('%', :keyword, '%')
              OR (
                LENGTH(:panDigits) >= 3
                AND regexp_replace(COALESCE(b.pan_display, ''), '[^0-9]', '', 'g')
                    LIKE CONCAT('%', :panDigits, '%')
              )
            )
            ORDER BY b.id
            """,
            nativeQuery = true)
    List<Long> findActiveIdsByFilter(@Param("activeYn") String activeYn,
                                     @Param("keyword") String keyword,
                                     @Param("panDigits") String panDigits,
                                     @Param("fromDate") String fromDate,
                                     @Param("toDate") String toDate);

    Optional<HqPayCardBlacklist> findTopByRegisteredOrgUnitIdOrderByCreatedAtDesc(Long registeredOrgUnitId);

    default Optional<java.time.LocalDateTime> findLatestCreatedAtByOrg(Long orgUnitId) {
        return findTopByRegisteredOrgUnitIdOrderByCreatedAtDesc(orgUnitId).map(HqPayCardBlacklist::getCreatedAt);
    }

    default Optional<String> findLatestSourceByOrg(Long orgUnitId) {
        return findTopByRegisteredOrgUnitIdOrderByCreatedAtDesc(orgUnitId).map(HqPayCardBlacklist::getSource);
    }
}
