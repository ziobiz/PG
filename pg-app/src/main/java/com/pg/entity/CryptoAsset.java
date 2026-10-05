package com.pg.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_crypto_asset")
public class CryptoAsset {

    @Id
    @Column(name = "code", length = 16)
    private String code;

    @Column(name = "display_name", nullable = false, length = 64)
    private String displayName;

    @Column(name = "symbol", nullable = false, length = 16)
    private String symbol;

    @Column(name = "pastel_bg", nullable = false, length = 16)
    private String pastelBg;

    @Column(name = "pastel_fg", nullable = false, length = 16)
    private String pastelFg;

    @Column(name = "icon_key", nullable = false, length = 32)
    private String iconKey;

    @Column(name = "sort_ord", nullable = false)
    private Integer sortOrd = 0;

    @Column(name = "use_yn", nullable = false, length = 1)
    private String useYn = "Y";

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public String getPastelBg() { return pastelBg; }
    public void setPastelBg(String pastelBg) { this.pastelBg = pastelBg; }
    public String getPastelFg() { return pastelFg; }
    public void setPastelFg(String pastelFg) { this.pastelFg = pastelFg; }
    public String getIconKey() { return iconKey; }
    public void setIconKey(String iconKey) { this.iconKey = iconKey; }
    public Integer getSortOrd() { return sortOrd; }
    public void setSortOrd(Integer sortOrd) { this.sortOrd = sortOrd; }
    public String getUseYn() { return useYn; }
    public void setUseYn(String useYn) { this.useYn = useYn; }
}
