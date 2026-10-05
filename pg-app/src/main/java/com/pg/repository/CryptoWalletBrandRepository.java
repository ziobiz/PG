package com.pg.repository;

import com.pg.entity.CryptoWalletBrand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CryptoWalletBrandRepository extends JpaRepository<CryptoWalletBrand, String> {
    List<CryptoWalletBrand> findByUseYnOrderBySortOrdAsc(String useYn);
}
