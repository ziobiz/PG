package com.pg.repository;

import com.pg.entity.ServerUsageState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServerUsageStateRepository extends JpaRepository<ServerUsageState, Short> {
}
