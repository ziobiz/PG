package com.pg.repository;

import com.pg.entity.SplitPayEmailPhase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SplitPayEmailPhaseRepository extends JpaRepository<SplitPayEmailPhase, String> {

    List<SplitPayEmailPhase> findAllByOrderByPhaseAsc();
}
