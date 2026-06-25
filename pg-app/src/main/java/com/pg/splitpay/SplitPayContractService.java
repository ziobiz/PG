package com.pg.splitpay;

import com.pg.entity.CommissionPolicy;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgUnit;
import com.pg.entity.SplitPayContract;
import com.pg.entity.SplitPayInstallment;
import com.pg.repository.CommissionPolicyRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.SplitPayContractRepository;
import com.pg.repository.SplitPayInstallmentRepository;
import com.pg.service.ChillPayService;
import com.pg.service.PublicCustomerSiteBaseService;
import com.pg.service.settlement.SettlementBusinessHolidayService;
import com.pg.urlpay.CheckoutHeaderLogoResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SplitPayContractService {

    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final CommissionPolicyRepository commissionPolicyRepository;
    private final SplitPayContractRepository contractRepository;
    private final SplitPayInstallmentRepository installmentRepository;
    private final SplitPayScheduleService scheduleService;
    private final SettlementBusinessHolidayService holidayService;
    private final PublicCustomerSiteBaseService publicCustomerSiteBaseService;
    private final SplitPayMailService splitPayMailService;
    private final ChillPayService chillPayService;
    private final CheckoutHeaderLogoResolver checkoutHeaderLogoResolver;

    public SplitPayContractService(OrgUnitRepository orgUnitRepository,
                                   MerchantProfileRepository merchantProfileRepository,
                                   CommissionPolicyRepository commissionPolicyRepository,
                                   SplitPayContractRepository contractRepository,
                                   SplitPayInstallmentRepository installmentRepository,
                                   SplitPayScheduleService scheduleService,
                                   SettlementBusinessHolidayService holidayService,
                                   PublicCustomerSiteBaseService publicCustomerSiteBaseService,
                                   SplitPayMailService splitPayMailService,
                                   ChillPayService chillPayService,
                                   CheckoutHeaderLogoResolver checkoutHeaderLogoResolver) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.commissionPolicyRepository = commissionPolicyRepository;
        this.contractRepository = contractRepository;
        this.installmentRepository = installmentRepository;
        this.scheduleService = scheduleService;
        this.holidayService = holidayService;
        this.publicCustomerSiteBaseService = publicCustomerSiteBaseService;
        this.splitPayMailService = splitPayMailService;
        this.chillPayService = chillPayService;
        this.checkoutHeaderLogoResolver = checkoutHeaderLogoResolver;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> merchantConfig(String compCode) {
        OrgUnit ou = resolveMerchant(compCode);
        MerchantProfile mp = merchantProfileRepository.findByOrgUnitId(ou.getId()).orElse(null);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("compId", ou.getCode());
        m.put("compNm", ou.getName());
        m.put("splitPayEnabledYn", SplitPayMerchantUtil.isEnabled(mp) ? "Y" : "N");
        m.put("apiUrlPayCheckoutMode", SplitPayMerchantUtil.resolveApiCheckoutModeForDisplay(mp));
        m.put("splitPayIntervalMonthYn", yn(mp != null ? mp.getSplitPayIntervalMonthYn() : "Y"));
        m.put("splitPayIntervalDayYn", yn(mp != null ? mp.getSplitPayIntervalDayYn() : "N"));
        m.put("splitPayIntervalMultiYn", yn(mp != null ? mp.getSplitPayIntervalMultiYn() : "N"));
        m.put("splitPayMultiMaxMonths", mp != null && mp.getSplitPayMultiMaxMonths() != null ? mp.getSplitPayMultiMaxMonths() : 6);
        m.put("splitPayDayIntervalDays", mp != null && mp.getSplitPayDayIntervalDays() != null ? mp.getSplitPayDayIntervalDays() : 10);
        m.put("splitPayMonthIntervalMonths", mp != null && mp.getSplitPayMonthIntervalMonths() != null ? mp.getSplitPayMonthIntervalMonths() : 1);
        m.put("splitPayFirstPayMode", mp != null && mp.getSplitPayFirstPayMode() != null ? mp.getSplitPayFirstPayMode() : SplitPayContract.FIRST_IMMEDIATE);
        String opPg = chillPayService.resolveUrlPayOperationalPgCd(ou.getId());
        m.put("operationalPgCd", opPg != null ? opPg : "");
        m.put("checkoutPage", SplitPayCheckoutPageUtil.resolveCheckoutPage(opPg));
        checkoutHeaderLogoResolver.applySplitPayToCheckoutMap(m, ou.getId());
        return m;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> preview(String compCode, BigDecimal totalAmount, int installmentCount,
                                       String intervalType, Integer intervalValue, LocalDate contractDate) {
        OrgUnit ou = resolveMerchant(compCode);
        assertSplitPayEnabled(ou.getId());
        validateCreateInput(totalAmount, installmentCount, intervalType, ou.getId());
        int iv = intervalValue != null && intervalValue > 0 ? intervalValue : 1;
        LocalDate baseDate = contractDate != null ? contractDate : LocalDate.now();
        Set<java.time.LocalDate> holidays = holidayService.resolveNonBusinessDatesForMerchantOrgUnitId(ou.getId());
        List<BigDecimal> amounts = SplitPayAmountUtil.divideInstallmentAmounts(totalAmount, installmentCount);
        List<LocalDate> dates = scheduleService.buildScheduledDates(baseDate, installmentCount, intervalType, iv, holidays);
        List<Map<String, Object>> rows = new ArrayList<>();
        String previewNo = "PREVIEW";
        for (int i = 0; i < installmentCount; i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("installmentNo", i + 1);
            row.put("orderNo", previewNo + "-" + (i + 1));
            row.put("amount", amounts.get(i));
            row.put("dueDate", dates.get(i).toString());
            rows.add(row);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("installments", rows);
        out.put("totalAmount", totalAmount);
        out.put("installmentCount", installmentCount);
        out.put("intervalType", intervalType);
        out.put("intervalValue", iv);
        return out;
    }

    @Transactional
    public Map<String, Object> createContract(String compCode,
                                                String customerEmail,
                                                String customerName,
                                                BigDecimal totalAmount,
                                                int installmentCount,
                                                String intervalType,
                                                Integer intervalValue,
                                                String currencyCode,
                                                String customerLocale,
                                                String entryChannel,
                                                HttpServletRequest request) {
        OrgUnit ou = resolveMerchant(compCode);
        assertSplitPayEnabled(ou.getId());
        assertUrlPayOperationalPg(ou.getId());
        validateCreateInput(totalAmount, installmentCount, intervalType, ou.getId());
        MerchantProfile mp = merchantProfileRepository.findByOrgUnitId(ou.getId()).orElse(null);
        String firstMode = mp != null && mp.getSplitPayFirstPayMode() != null
                ? mp.getSplitPayFirstPayMode() : SplitPayContract.FIRST_IMMEDIATE;
        int iv = resolveIntervalValue(intervalType, intervalValue, mp);
        CommissionPolicy pol = commissionPolicyRepository.findByScope(ou.getCode())
                .or(() -> commissionPolicyRepository.findByScope("DEFAULT"))
                .orElse(null);
        BigDecimal feePct = pol != null && pol.getSplitPayFeePct() != null ? pol.getSplitPayFeePct() : BigDecimal.ZERO;
        BigDecimal fixedPer = pol != null && pol.getSplitPayFixedFeePerInst() != null ? pol.getSplitPayFixedFeePerInst() : BigDecimal.ZERO;
        BigDecimal fixedTotal = fixedPer.multiply(BigDecimal.valueOf(installmentCount));

        String contractNo = generateContractNo();
        LocalDate contractDate = LocalDate.now();
        Set<java.time.LocalDate> holidays = holidayService.resolveNonBusinessDatesForMerchantOrgUnitId(ou.getId());
        List<BigDecimal> amounts = SplitPayAmountUtil.divideInstallmentAmounts(totalAmount, installmentCount);
        List<LocalDate> dates = scheduleService.buildScheduledDates(contractDate, installmentCount, intervalType, iv, holidays);

        SplitPayContract c = new SplitPayContract();
        c.setContractNo(contractNo);
        c.setOrgUnitId(ou.getId());
        c.setMerchantCode(ou.getCode());
        c.setCustomerEmail(customerEmail.trim());
        c.setCustomerName(customerName != null ? customerName.trim() : null);
        String loc = customerLocale != null && !customerLocale.isBlank()
                ? SplitPayMailLocaleUtil.normalize(customerLocale)
                : SplitPayMailLocaleUtil.fromAcceptLanguage(
                        request != null ? request.getHeader("Accept-Language") : null);
        c.setCustomerLocale(loc);
        c.setTotalAmount(totalAmount);
        c.setCurrencyCode(currencyCode != null && !currencyCode.isBlank() ? currencyCode.trim().toUpperCase(Locale.ROOT) : "JPY");
        c.setInstallmentCount(installmentCount);
        c.setIntervalType(intervalType);
        c.setIntervalValue(iv);
        c.setFirstPayMode(firstMode);
        c.setSnapSplitPayFeePct(feePct);
        c.setSnapSplitFixedPerInst(fixedPer);
        c.setSnapSplitFixedTotal(fixedTotal);
        c.setContractDate(contractDate);
        c.setStatus(SplitPayContract.STATUS_ACTIVE);
        c.setChannel(resolveContractChannel(entryChannel));
        contractRepository.save(c);

        String base = publicCustomerSiteBaseService.resolvePublicCustomerSiteBase(request).replaceAll("/+$", "");
        String contractChannel = c.getChannel();
        List<Map<String, Object>> instRows = new ArrayList<>();
        SplitPayInstallment firstInst = null;
        for (int i = 0; i < installmentCount; i++) {
            SplitPayInstallment inst = new SplitPayInstallment();
            inst.setContractId(c.getId());
            inst.setInstallmentNo(i + 1);
            inst.setOrderNo(contractNo + "-" + (i + 1));
            inst.setAmount(amounts.get(i));
            inst.setScheduledDate(dates.get(i));
            inst.setDueDateAdjusted(dates.get(i));
            inst.setPayToken(newToken());
            inst.setStatus(SplitPayInstallment.STATUS_PENDING);
            if (i == 0) {
                inst.setFeeFixedAmount(fixedTotal);
            } else {
                inst.setFeeFixedAmount(BigDecimal.ZERO);
            }
            inst.setFeePctAmount(calcPctFee(amounts.get(i), feePct));
            installmentRepository.save(inst);
            if (i == 0) {
                firstInst = inst;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("installmentNo", inst.getInstallmentNo());
            row.put("orderNo", inst.getOrderNo());
            row.put("amount", inst.getAmount());
            row.put("dueDate", inst.getDueDateAdjusted().toString());
            row.put("payUrl", appendChatbotPayEntryIfNeeded(base + "/split-pay.html?token=" + inst.getPayToken(), contractChannel));
            instRows.add(row);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("contractNo", contractNo);
        out.put("installments", instRows);
        if (firstInst != null) {
            out.put("firstPayToken", firstInst.getPayToken());
            out.put("firstPayUrl", appendChatbotPayEntryIfNeeded(
                    base + "/split-pay.html?token=" + firstInst.getPayToken(), contractChannel));
            if (SplitPayContract.FIRST_LINK.equalsIgnoreCase(firstMode)) {
                splitPayMailService.sendInstallmentLink(c, firstInst, base, "CREATE");
            }
        }
        return out;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> installmentCheckoutContext(String payToken, HttpServletRequest request) {
        SplitPayInstallment inst = installmentRepository.findByPayToken(payToken.trim())
                .orElseThrow(() -> new IllegalArgumentException("NOT_FOUND"));
        SplitPayContract c = contractRepository.findById(inst.getContractId())
                .orElseThrow(() -> new IllegalArgumentException("NOT_FOUND"));
        if (SplitPayInstallment.STATUS_PAID.equals(inst.getStatus())) {
            throw new IllegalStateException("ALREADY_PAID");
        }
        if (!SplitPayContract.STATUS_ACTIVE.equals(c.getStatus())) {
            throw new IllegalStateException("CONTRACT_STOPPED");
        }
        List<SplitPayInstallment> all = installmentRepository.findByContractIdOrderByInstallmentNoAsc(c.getId());
        SplitPayInstallment next = null;
        for (SplitPayInstallment i : all) {
            if (SplitPayInstallment.STATUS_PENDING.equals(i.getStatus()) && i.getInstallmentNo() > inst.getInstallmentNo()) {
                next = i;
                break;
            }
        }
        OrgUnit ou = orgUnitRepository.findById(c.getOrgUnitId()).orElse(null);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("contractNo", c.getContractNo());
        m.put("compId", c.getMerchantCode());
        m.put("compNm", ou != null ? ou.getName() : "");
        m.put("orderNo", inst.getOrderNo());
        m.put("amount", inst.getAmount());
        m.put("currencyCode", c.getCurrencyCode());
        m.put("totalAmount", c.getTotalAmount());
        m.put("installmentCount", c.getInstallmentCount());
        m.put("currentInstallmentNo", inst.getInstallmentNo());
        m.put("currentAmount", inst.getAmount());
        m.put("remainingInstallments", Math.max(0, c.getInstallmentCount() - inst.getInstallmentNo()));
        m.put("nextAmount", next != null ? next.getAmount() : null);
        m.put("nextDueDate", next != null ? next.getDueDateAdjusted().toString() : null);
        m.put("dueDate", inst.getDueDateAdjusted().toString());
        m.put("status", inst.getStatus());
        m.put("payToken", inst.getPayToken());
        m.put("checkoutKind", "SPLIT_PAY");
        m.put("contractChannel", c.getChannel() != null ? c.getChannel() : "URL");
        String opPg = chillPayService.resolveUrlPayOperationalPgCd(c.getOrgUnitId());
        m.put("operationalPgCd", opPg != null ? opPg : "");
        m.put("checkoutPage", SplitPayCheckoutPageUtil.resolveCheckoutPage(opPg));
        m.put("customerEmail", c.getCustomerEmail());
        m.put("customerName", c.getCustomerName() != null ? c.getCustomerName() : "");
        checkoutHeaderLogoResolver.applySplitPayToCheckoutMap(m, c.getOrgUnitId());
        return m;
    }

    @Transactional
    public void stopContractOnFirstInstallmentReversal(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            return;
        }
        installmentRepository.findByOrderNo(orderNo.trim()).ifPresent(inst -> {
            if (inst.getInstallmentNo() != null && inst.getInstallmentNo() == 1) {
                contractRepository.findById(inst.getContractId()).ifPresent(c -> {
                    if (SplitPayContract.STATUS_ACTIVE.equals(c.getStatus())) {
                        c.setStatus(SplitPayContract.STATUS_STOPPED);
                        c.setCancelledAt(LocalDateTime.now());
                        contractRepository.save(c);
                    }
                });
            }
        });
    }

    private static BigDecimal calcPctFee(BigDecimal amount, BigDecimal pct) {
        if (amount == null || pct == null || pct.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(pct).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
    }

    private OrgUnit resolveMerchant(String compCode) {
        if (compCode == null || compCode.isBlank()) {
            throw new IllegalArgumentException("COMP_REQUIRED");
        }
        return orgUnitRepository.findByCode(compCode.trim())
                .orElseThrow(() -> new IllegalArgumentException("NOT_FOUND"));
    }

    private void assertSplitPayEnabled(Long orgUnitId) {
        MerchantProfile mp = merchantProfileRepository.findByOrgUnitId(orgUnitId).orElse(null);
        if (!SplitPayMerchantUtil.isEnabled(mp)) {
            throw new IllegalStateException("SPLIT_PAY_DISABLED");
        }
    }

    /** ChillPay·JPAY 운영 URL 결제 PG 바인딩 필수 */
    private void assertUrlPayOperationalPg(Long orgUnitId) {
        String opPg = chillPayService.resolveUrlPayOperationalPgCd(orgUnitId);
        if (!SplitPayCheckoutPageUtil.hasSupportedOperationalPg(opPg)) {
            throw new IllegalStateException("URL_PAYMENT_PG_MISSING");
        }
    }

    private void validateCreateInput(BigDecimal totalAmount, int installmentCount, String intervalType, Long orgUnitId) {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("INVALID_AMOUNT");
        }
        MerchantProfile mp = merchantProfileRepository.findByOrgUnitId(orgUnitId).orElse(null);
        String it = intervalType != null ? intervalType.trim().toUpperCase(Locale.ROOT) : SplitPayContract.INTERVAL_MONTH;
        if (SplitPayContract.INTERVAL_MULTI.equals(it)) {
            if (mp == null || !"Y".equalsIgnoreCase(yn(mp.getSplitPayIntervalMultiYn()))) {
                throw new IllegalStateException("INTERVAL_MULTI_DISABLED");
            }
            int maxMonths = mp.getSplitPayMultiMaxMonths() != null ? mp.getSplitPayMultiMaxMonths() : 6;
            if (installmentCount < 1 || installmentCount > maxMonths) {
                throw new IllegalArgumentException("INVALID_MULTI_COUNT");
            }
            return;
        }
        if (installmentCount < 2 || installmentCount > 60) {
            throw new IllegalArgumentException("INVALID_COUNT");
        }
        if (SplitPayContract.INTERVAL_DAY.equals(it)) {
            if (mp == null || !"Y".equalsIgnoreCase(yn(mp.getSplitPayIntervalDayYn()))) {
                throw new IllegalStateException("INTERVAL_DAY_DISABLED");
            }
        } else if (!"Y".equalsIgnoreCase(yn(mp != null ? mp.getSplitPayIntervalMonthYn() : "Y"))) {
            throw new IllegalStateException("INTERVAL_MONTH_DISABLED");
        }
    }

    private static int resolveIntervalValue(String intervalType, Integer intervalValue, MerchantProfile mp) {
        if (SplitPayContract.INTERVAL_MULTI.equalsIgnoreCase(intervalType)) {
            return 1;
        }
        if (SplitPayContract.INTERVAL_DAY.equalsIgnoreCase(intervalType)) {
            if (intervalValue != null && intervalValue > 0) {
                return intervalValue;
            }
            return mp != null && mp.getSplitPayDayIntervalDays() != null && mp.getSplitPayDayIntervalDays() > 0
                    ? mp.getSplitPayDayIntervalDays() : 10;
        }
        if (intervalValue != null && intervalValue > 0) {
            return intervalValue;
        }
        return mp != null && mp.getSplitPayMonthIntervalMonths() != null && mp.getSplitPayMonthIntervalMonths() > 0
                ? mp.getSplitPayMonthIntervalMonths() : 1;
    }

    private static String generateContractNo() {
        String day = LocalDate.now().toString().replace("-", "");
        int r = ThreadLocalRandom.current().nextInt(100000, 999999);
        return "SP-" + day + "-" + r;
    }

    private static String newToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    static String resolveContractChannel(String entryChannel) {
        if (entryChannel == null || entryChannel.isBlank()) {
            return "URL";
        }
        String v = entryChannel.trim().toLowerCase(Locale.ROOT);
        if ("chatbot".equals(v) || "CHATBOT".equalsIgnoreCase(entryChannel.trim())) {
            return "CHATBOT";
        }
        return "URL";
    }

    static String appendChatbotPayEntryIfNeeded(String payUrl, String contractChannel) {
        if (payUrl == null || payUrl.isBlank()) {
            return payUrl;
        }
        if (contractChannel == null || !"CHATBOT".equalsIgnoreCase(contractChannel.trim())) {
            return payUrl;
        }
        if (payUrl.contains("entry=chatbot")) {
            return payUrl;
        }
        return payUrl + (payUrl.contains("?") ? "&" : "?") + "entry=chatbot";
    }

    private static String yn(String v) {
        return v != null && "Y".equalsIgnoreCase(v.trim()) ? "Y" : "N";
    }
}
