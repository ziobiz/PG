package com.pg.repository;

import com.pg.entity.CryptoAsset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CryptoAssetRepository extends JpaRepository<CryptoAsset, String> {
    List<CryptoAsset> findByUseYnOrderBySortOrdAsc(String useYn);
}
