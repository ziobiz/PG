package com.pg.repository;

import com.pg.entity.RollingReserve;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RollingReserveRepository extends JpaRepository<RollingReserve, Long> {

    List<RollingReserve> findByMerchantIdAndStatusOrderByCreatedAtDesc(String merchantId, String status);
}
