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
    /**
     * {@code true}: 승인 건 수수료 분해에서 정책의 {@code fee_settlement_per_tx} 금액을 넣지 않음.
     * 해당 금액은 정산 실행(tb_settlement_run)당 1회({@code settlement_batch_fee_amt})로만 부과되며,
     * 정산실행 상세 거래 목록은 건별이므로 여기서는 제외한다.
     */
    private final boolean omitSettlementFeeFromApprovedTxnBreakdown;

    /** null이면 레거시 JP/TH 고정 2줄 표시. */
    private final TxnDualLineSpec txnDualLineSpec;

    public PayListRowContext(String compNm, MerchantProfile profile, MerchantPgBinding binding,
                             DistributionFeeConfig distFee, CommissionPolicy policy,
                             SettlementSetting settlement,
                             String regionalNm, String masterNm, String branchNm,
                             String regionalBaseCurrency, String masterDistBaseCurrency, String merchantBaseCurrency) {
        this(compNm, profile, binding, distFee, policy, settlement, regionalNm, masterNm, branchNm,
                regionalBaseCurrency, masterDistBaseCurrency, merchantBaseCurrency, false, null);
    }

    public PayListRowContext(String compNm, MerchantProfile profile, MerchantPgBinding binding,
                             DistributionFeeConfig distFee, CommissionPolicy policy,
                             SettlementSetting settlement,
                             String regionalNm, String masterNm, String branchNm,
                             String regionalBaseCurrency, String masterDistBaseCurrency, String merchantBaseCurrency,
                             boolean omitSettlementFeeFromApprovedTxnBreakdown) {
        this(compNm, profile, binding, distFee, policy, settlement, regionalNm, masterNm, branchNm,
                regionalBaseCurrency, masterDistBaseCurrency, merchantBaseCurrency,
                omitSettlementFeeFromApprovedTxnBreakdown, null);
    }

    public PayListRowContext(String compNm, MerchantProfile profile, MerchantPgBinding binding,
                             DistributionFeeConfig distFee, CommissionPolicy policy,
                             SettlementSetting settlement,
                             String regionalNm, String masterNm, String branchNm,
                             String regionalBaseCurrency, String masterDistBaseCurrency, String merchantBaseCurrency,
                             boolean omitSettlementFeeFromApprovedTxnBreakdown,
                             TxnDualLineSpec txnDualLineSpec) {
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
        this.omitSettlementFeeFromApprovedTxnBreakdown = omitSettlementFeeFromApprovedTxnBreakdown;
        this.txnDualLineSpec = txnDualLineSpec;
    }

    /** 정산실행 상세 등 — 건별 행에 정산 실행당 1회 정산료를 반복 넣지 않을 때 사용 */
    public PayListRowContext withOmitSettlementFeeFromApprovedTxnBreakdown(boolean omit) {
        if (omit == this.omitSettlementFeeFromApprovedTxnBreakdown) {
            return this;
        }
        return new PayListRowContext(compNm, profile, binding, distFee, policy, settlement,
                regionalNm, masterNm, branchNm, regionalBaseCurrency, masterDistBaseCurrency, merchantBaseCurrency, omit,
                txnDualLineSpec);
    }

    public TxnDualLineSpec getTxnDualLineSpec() {
        return txnDualLineSpec;
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

    public boolean isOmitSettlementFeeFromApprovedTxnBreakdown() {
        return omitSettlementFeeFromApprovedTxnBreakdown;
    }
}
