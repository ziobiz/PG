package com.pg.repository;

import com.pg.entity.CryptoNetwork;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CryptoNetworkRepository extends JpaRepository<CryptoNetwork, String> {
    List<CryptoNetwork> findByUseYnOrderBySortOrdAsc(String useYn);
}
