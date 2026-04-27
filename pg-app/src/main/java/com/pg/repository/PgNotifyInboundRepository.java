package com.pg.repository;

import com.pg.entity.PgNotifyInbound;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface PgNotifyInboundRepository extends JpaRepository<PgNotifyInbound, Long>, JpaSpecificationExecutor<PgNotifyInbound> {

    List<PgNotifyInbound> findByMidInOrderByIdDesc(Collection<String> mids, Pageable pageable);

    List<PgNotifyInbound> findAllByOrderByIdDesc(Pageable pageable);

    long deleteByCreatedAtBefore(LocalDateTime cutoff);

    @Query("SELECT COUNT(n) FROM PgNotifyInbound n WHERE n.createdAt >= :f AND n.createdAt < :t AND (n.processStatus IS NULL OR UPPER(TRIM(n.processStatus)) <> 'PARSED')")
    long countNotParsedBetweenAll(@Param("f") LocalDateTime f, @Param("t") LocalDateTime t);

    @Query("SELECT COUNT(n) FROM PgNotifyInbound n WHERE n.createdAt >= :f AND n.createdAt < :t AND (n.processStatus IS NULL OR UPPER(TRIM(n.processStatus)) <> 'PARSED') AND n.merchantId IN :mids")
    long countNotParsedBetweenIn(@Param("mids") Collection<String> mids, @Param("f") LocalDateTime f, @Param("t") LocalDateTime t);

    @Query("SELECT n FROM PgNotifyInbound n WHERE n.createdAt >= :since AND (n.processStatus IS NULL OR UPPER(TRIM(n.processStatus)) <> 'PARSED') ORDER BY n.createdAt DESC")
    List<PgNotifyInbound> findRecentNotParsedAll(@Param("since") LocalDateTime since, Pageable pageable);

    @Query("SELECT n FROM PgNotifyInbound n WHERE n.createdAt >= :since AND (n.processStatus IS NULL OR UPPER(TRIM(n.processStatus)) <> 'PARSED') AND n.merchantId IN :mids ORDER BY n.createdAt DESC")
    List<PgNotifyInbound> findRecentNotParsedIn(@Param("since") LocalDateTime since, @Param("mids") Collection<String> mids, Pageable pageable);
}
