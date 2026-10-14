package com.pg.repository;

import com.pg.entity.CryptoNetworkGasFee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CryptoNetworkGasFeeRepository extends JpaRepository<CryptoNetworkGasFee, Long> {

    List<CryptoNetworkGasFee> findAllByOrderByAssetCodeAscNetworkCodeAsc();

    Optional<CryptoNetworkGasFee> findByAssetCodeAndNetworkCode(String assetCode, String networkCode);

    Optional<CryptoNetworkGasFee> findByAssetCodeAndNetworkCodeAndUseYn(String assetCode, String networkCode, String useYn);
}
