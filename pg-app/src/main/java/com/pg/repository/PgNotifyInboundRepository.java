package com.pg.repository;

import com.pg.entity.PgNotifyInbound;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface PgNotifyInboundRepository extends JpaRepository<PgNotifyInbound, Long>, JpaSpecificationExecutor<PgNotifyInbound> {

    List<PgNotifyInbound> findByMidInOrderByIdDesc(Collection<String> mids, Pageable pageable);

    List<PgNotifyInbound> findAllByOrderByIdDesc(Pageable pageable);

    long deleteByCreatedAtBefore(LocalDateTime cutoff);
}
