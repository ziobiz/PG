package com.pg.repository;

import com.pg.entity.ServerUsageDaily;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ServerUsageDailyRepository extends JpaRepository<ServerUsageDaily, LocalDate> {

    List<ServerUsageDaily> findByUsageDateBetweenOrderByUsageDateAsc(LocalDate from, LocalDate to);

    long deleteByUsageDateBefore(LocalDate cutoff);
}
