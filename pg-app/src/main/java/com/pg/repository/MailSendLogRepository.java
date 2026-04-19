package com.pg.repository;

import com.pg.entity.MailSendLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MailSendLogRepository extends JpaRepository<MailSendLog, Long>, JpaSpecificationExecutor<MailSendLog> {
}
