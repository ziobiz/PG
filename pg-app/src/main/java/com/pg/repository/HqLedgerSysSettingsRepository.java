package com.pg.repository;

import com.pg.entity.HqLedgerSysSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HqLedgerSysSettingsRepository extends JpaRepository<HqLedgerSysSettings, Long> {
    Optional<HqLedgerSysSettings> findFirstByOrderByIdAsc();
}
