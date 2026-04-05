package com.pg.repository;

import com.pg.entity.PgNotifyInbound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PgNotifyInboundRepository extends JpaRepository<PgNotifyInbound, Long>, JpaSpecificationExecutor<PgNotifyInbound> {
}
