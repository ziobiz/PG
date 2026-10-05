package com.pg.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_crypto_network")
public class CryptoNetwork {

    @Id
    @Column(name = "code", length = 16)
    private String code;

    @Column(name = "display_name", nullable = false, length = 64)
    private String displayName;

    @Column(name = "chain_hint", length = 64)
    private String chainHint;

    @Column(name = "confirmations", nullable = false)
    private Integer confirmations = 1;

    @Column(name = "pastel_bg", nullable = false, length = 16)
    private String pastelBg;

    @Column(name = "pastel_fg", nullable = false, length = 16)
    private String pastelFg;

    @Column(name = "sort_ord", nullable = false)
    private Integer sortOrd = 0;

    @Column(name = "use_yn", nullable = false, length = 1)
    private String useYn = "Y";

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getChainHint() { return chainHint; }
    public void setChainHint(String chainHint) { this.chainHint = chainHint; }
    public Integer getConfirmations() { return confirmations; }
    public void setConfirmations(Integer confirmations) { this.confirmations = confirmations; }
    public String getPastelBg() { return pastelBg; }
    public void setPastelBg(String pastelBg) { this.pastelBg = pastelBg; }
    public String getPastelFg() { return pastelFg; }
    public void setPastelFg(String pastelFg) { this.pastelFg = pastelFg; }
    public Integer getSortOrd() { return sortOrd; }
    public void setSortOrd(Integer sortOrd) { this.sortOrd = sortOrd; }
    public String getUseYn() { return useYn; }
    public void setUseYn(String useYn) { this.useYn = useYn; }
}
