package com.pg.repository;

import com.pg.entity.CryptoAssetNetwork;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CryptoAssetNetworkRepository extends JpaRepository<CryptoAssetNetwork, CryptoAssetNetwork.Pk> {
    List<CryptoAssetNetwork> findByUseYn(String useYn);
    List<CryptoAssetNetwork> findByAssetCodeAndUseYn(String assetCode, String useYn);
}
