package com.pg.repository;

import com.pg.entity.CryptoCustodyAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CryptoCustodyAddressRepository extends JpaRepository<CryptoCustodyAddress, Long> {
    List<CryptoCustodyAddress> findByUseYnOrderByIdAsc(String useYn);

    Optional<CryptoCustodyAddress> findFirstByNetworkCodeAndAssetCodeAndUseYnOrderByIdAsc(
            String networkCode, String assetCode, String useYn);

    Optional<CryptoCustodyAddress> findFirstByNetworkCodeAndAssetCodeIsNullAndUseYnOrderByIdAsc(
            String networkCode, String useYn);
}
