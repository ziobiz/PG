package com.pg.api.dto;

import com.pg.entity.CommissionPolicy;
import com.pg.entity.DistributionFeeConfig;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.MerchantProfile;
import com.pg.entity.SettlementSetting;

/**
 * 결제내역 한 행 조립용 컨텍스트 (가맹점 프로필·PG바인딩·수수료·보류·상위조직명)
 */
public class PayListRowContext {

    private final String compNm;
    private final MerchantProfile profile;
    private final MerchantPgBinding binding;
    private final DistributionFeeConfig distFee;
    private final CommissionPolicy policy;
    private final SettlementSetting settlement;
    private final String regionalNm;
    private final String masterNm;
    private final String branchNm;
    /** 본사(REGIONAL) 프로필 기준통화(첫 토큰) */
    private final String regionalBaseCurrency;
    /** 총판(MASTER_DIST) 프로필 기준통화(첫 토큰) */
    private final String masterDistBaseCurrency;
    /** 가맹점(MERCHANT) 프로필 기준통화(첫 토큰) — 상위 총판 귀속 */
    private final String merchantBaseCurrency;

    public PayListRowContext(String compNm, MerchantProfile profile, MerchantPgBinding binding,
                             DistributionFeeConfig distFee, CommissionPolicy policy,
                             SettlementSetting settlement,
                             String regionalNm, String masterNm, String branchNm,
                             String regionalBaseCurrency, String masterDistBaseCurrency, String merchantBaseCurrency) {
        this.compNm = compNm;
        this.profile = profile;
        this.binding = binding;
        this.distFee = distFee;
        this.policy = policy;
        this.settlement = settlement;
        this.regionalNm = regionalNm != null ? regionalNm : "";
        this.masterNm = masterNm != null ? masterNm : "";
        this.branchNm = branchNm != null ? branchNm : "";
        this.regionalBaseCurrency = regionalBaseCurrency != null ? regionalBaseCurrency : "";
        this.masterDistBaseCurrency = masterDistBaseCurrency != null ? masterDistBaseCurrency : "";
        this.merchantBaseCurrency = merchantBaseCurrency != null ? merchantBaseCurrency : "";
    }

    public String getCompNm() { return compNm; }
    public MerchantProfile getProfile() { return profile; }
    public MerchantPgBinding getBinding() { return binding; }
    public DistributionFeeConfig getDistFee() { return distFee; }
    public CommissionPolicy getPolicy() { return policy; }
    public SettlementSetting getSettlement() { return settlement; }
    public String getRegionalNm() { return regionalNm; }
    public String getMasterNm() { return masterNm; }
    public String getBranchNm() { return branchNm; }
    public String getRegionalBaseCurrency() { return regionalBaseCurrency; }
    public String getMasterDistBaseCurrency() { return masterDistBaseCurrency; }
    public String getMerchantBaseCurrency() { return merchantBaseCurrency; }
}
