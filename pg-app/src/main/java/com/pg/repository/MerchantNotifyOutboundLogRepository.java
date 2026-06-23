package com.pg.repository;

import com.pg.entity.MerchantNotifyOutboundLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MerchantNotifyOutboundLogRepository extends JpaRepository<MerchantNotifyOutboundLog, Long>,
        JpaSpecificationExecutor<MerchantNotifyOutboundLog> {
}
