package com.pg.util;

import com.pg.entity.CommissionPolicy;
import com.pg.entity.HqLedgerSysSettings;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgUnit;
import com.pg.repository.HqLedgerSysSettingsRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.CommissionService;

import java.util.Locale;
import java.util.Optional;

/**
 * 미수금 잔액 표시·집계용 청구 통화 — billing_ccy 미설정 시 가맹 기준통화·수수료정책·전산 표시통화 순.
 */
public final class ReceivableBillingCurrencyResolver {

    private ReceivableBillingCurrencyResolver() {
    }

    public static String resolve(String billingCcy,
                                 String merchantCode,
                                 OrgUnitRepository orgUnitRepository,
                                 MerchantProfileRepository merchantProfileRepository,
                                 CommissionService commissionService,
                                 HqLedgerSysSettingsRepository ledgerSysSettingsRepository) {
        if (billingCcy != null && !billingCcy.isBlank()) {
            return PayListStatusBarBuckets.normalizeCurrency(billingCcy.trim());
        }
        String mc = merchantCode != null ? merchantCode.trim() : "";
        if (!mc.isEmpty()) {
            String fromMerchant = resolveFromMerchantProfile(mc, orgUnitRepository, merchantProfileRepository);
            if (fromMerchant != null && !fromMerchant.isBlank()) {
                return PayListStatusBarBuckets.normalizeCurrency(fromMerchant);
            }
            CommissionPolicy pol = commissionService.resolveCommissionPolicyForSettlement(mc);
            String polCur = pol != null ? pol.getCurrencyCode() : null;
            if (polCur != null && !polCur.isBlank()) {
                return PayListStatusBarBuckets.normalizeCurrency(polCur.trim());
            }
        }
        HqLedgerSysSettings ledger = ledgerSysSettingsRepository != null
                ? ledgerSysSettingsRepository.findFirstByOrderByIdAsc().orElse(null) : null;
        return PayListStatusBarBuckets.normalizeCurrency(PayDisplayCurrency.alphaFromSettings(ledger));
    }

    private static String resolveFromMerchantProfile(String merchantCode,
                                                     OrgUnitRepository orgUnitRepository,
                                                     MerchantProfileRepository merchantProfileRepository) {
        if (orgUnitRepository == null || merchantProfileRepository == null) {
            return null;
        }
        Optional<OrgUnit> ou = orgUnitRepository.findByCodeIgnoreCase(merchantCode);
        if (ou.isEmpty()) {
            return null;
        }
        Optional<MerchantProfile> mp = merchantProfileRepository.findByOrgUnitId(ou.get().getId());
        if (mp.isEmpty()) {
            return null;
        }
        String base = ChatbotProductPricingUtil.firstIsoCurrencyToken(mp.get().getBaseCurrency());
        if (base == null || base.isBlank()) {
            return null;
        }
        return base.trim().toUpperCase(Locale.ROOT);
    }
}
