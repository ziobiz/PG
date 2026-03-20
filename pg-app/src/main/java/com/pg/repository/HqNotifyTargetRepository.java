package com.pg.repository;

import com.pg.entity.HqNotifyTarget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HqNotifyTargetRepository extends JpaRepository<HqNotifyTarget, Long> {
    List<HqNotifyTarget> findAllByOrderByIdDesc();
    Optional<HqNotifyTarget> findByTargetCode(String targetCode);
}

