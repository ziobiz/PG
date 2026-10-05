package com.pg.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "tb_crypto_asset_network")
@IdClass(CryptoAssetNetwork.Pk.class)
public class CryptoAssetNetwork {

    @Id
    @Column(name = "asset_code", length = 16)
    private String assetCode;

    @Id
    @Column(name = "network_code", length = 16)
    private String networkCode;

    @Column(name = "contract_hint", length = 128)
    private String contractHint;

    @Column(name = "decimals", nullable = false)
    private Integer decimals = 6;

    @Column(name = "use_yn", nullable = false, length = 1)
    private String useYn = "Y";

    public String getAssetCode() { return assetCode; }
    public void setAssetCode(String assetCode) { this.assetCode = assetCode; }
    public String getNetworkCode() { return networkCode; }
    public void setNetworkCode(String networkCode) { this.networkCode = networkCode; }
    public String getContractHint() { return contractHint; }
    public void setContractHint(String contractHint) { this.contractHint = contractHint; }
    public Integer getDecimals() { return decimals; }
    public void setDecimals(Integer decimals) { this.decimals = decimals; }
    public String getUseYn() { return useYn; }
    public void setUseYn(String useYn) { this.useYn = useYn; }

    public static class Pk implements Serializable {
        private String assetCode;
        private String networkCode;
        public Pk() {}
        public Pk(String assetCode, String networkCode) {
            this.assetCode = assetCode;
            this.networkCode = networkCode;
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk pk)) return false;
            return Objects.equals(assetCode, pk.assetCode) && Objects.equals(networkCode, pk.networkCode);
        }
        @Override public int hashCode() { return Objects.hash(assetCode, networkCode); }
    }
}
