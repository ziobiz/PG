package com.pg.repository;

import com.pg.entity.SplitPayContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface SplitPayContractRepository extends JpaRepository<SplitPayContract, Long>,
        JpaSpecificationExecutor<SplitPayContract> {

    Optional<SplitPayContract> findByContractNo(String contractNo);
}
