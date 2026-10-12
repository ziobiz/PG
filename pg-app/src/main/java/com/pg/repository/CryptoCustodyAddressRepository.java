package com.pg.repository;

import com.pg.entity.CryptoCustodyAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CryptoCustodyAddressRepository extends JpaRepository<CryptoCustodyAddress, Long> {
    List<CryptoCustodyAddress> findByUseYnOrderByIdAsc(String useYn);

    List<CryptoCustodyAddress> findByAssetCodeAndUseYnOrderByPrimaryYnDescIdAsc(String assetCode, String useYn);

    List<CryptoCustodyAddress> findByNetworkCodeAndAssetCodeAndUseYn(
            String networkCode, String assetCode, String useYn);

    List<CryptoCustodyAddress> findByNetworkCodeAndAssetCodeIsNullAndUseYn(String networkCode, String useYn);

    Optional<CryptoCustodyAddress> findFirstByNetworkCodeAndAssetCodeAndUseYnAndPrimaryYnOrderByIdAsc(
            String networkCode, String assetCode, String useYn, String primaryYn);

    Optional<CryptoCustodyAddress> findFirstByNetworkCodeAndAssetCodeAndUseYnOrderByIdAsc(
            String networkCode, String assetCode, String useYn);

    Optional<CryptoCustodyAddress> findFirstByNetworkCodeAndAssetCodeIsNullAndUseYnAndPrimaryYnOrderByIdAsc(
            String networkCode, String useYn, String primaryYn);

    Optional<CryptoCustodyAddress> findFirstByNetworkCodeAndAssetCodeIsNullAndUseYnOrderByIdAsc(
            String networkCode, String useYn);
}
