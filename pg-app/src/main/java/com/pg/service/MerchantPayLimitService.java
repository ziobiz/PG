package com.pg.service;

import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.entity.SettlementSetting;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgTrnsctnRepository;
import com.pg.repository.SettlementSettingRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 총판 기준통화로 가맹 결제 한도를 저장하고, 승인 전에 1회·일·월·연 한도를 검사합니다.
 * 가맹이 직접설정을 고르면 그 금액이 총판 값보다 우선합니다.
 */
@Service
public class MerchantPayLimitService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final SettlementSettingRepository settlementSettingRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final PgTrnsctnRepository pgTrnsctnRepository;
    private final UrlPayDisplayFxService urlPayDisplayFxService;

    public MerchantPayLimitService(SettlementSettingRepository settlementSettingRepository,
                                   OrgUnitRepository orgUnitRepository,
                                   MerchantProfileRepository merchantProfileRepository,
                                   PgTrnsctnRepository pgTrnsctnRepository,
                                   UrlPayDisplayFxService urlPayDisplayFxService) {
        this.settlementSettingRepository = settlementSettingRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.pgTrnsctnRepository = pgTrnsctnRepository;
        this.urlPayDisplayFxService = urlPayDisplayFxService;
    }

    @Transactional
    public void saveFromRequest(Long orgUnitId, String compDiv, HttpServletRequest request) {
        if (orgUnitId == null || request == null || compDiv == null) {
            return;
        }
        String div = compDiv.trim().toUpperCase(Locale.ROOT);
        boolean merchant = "MERCHANT".equals(div);
        boolean dist = "MASTER_DIST".equals(div);
        if (!merchant && !dist) {
            return;
        }
        if (merchant && request.getParameter("payLmtTxMaxMode") == null) {
            return;
        }
        if (dist && request.getParameter("payLmtTxMax") == null) {
            return;
        }
        SettlementSetting ss = settlementSettingRepository.findByOrgUnitId(orgUnitId).orElseGet(() -> {
            SettlementSetting created = new SettlementSetting();
            created.setOrgUnitId(orgUnitId);
            return created;
        });
        if (dist) {
            ss.setPayLmtTxMax(parseAmt(request.getParameter("payLmtTxMax")));
            ss.setPayLmtTxMin(parseAmt(request.getParameter("payLmtTxMin")));
            ss.setPayLmtDay(parseAmt(request.getParameter("payLmtDay")));
            ss.setPayLmtMonth(parseAmt(request.getParameter("payLmtMonth")));
            ss.setPayLmtYearCorp(parseAmt(request.getParameter("payLmtYearCorp")));
            ss.setPayLmtYearInd(parseAmt(request.getParameter("payLmtYearInd")));
            ss.setPayLmtUiMode(normalizeUiMode(request.getParameter("payLmtUiMode"), false));
        } else {
            applyMerchant(ss, request, "payLmtTxMax");
            applyMerchant(ss, request, "payLmtTxMin");
            applyMerchant(ss, request, "payLmtDay");
            applyMerchant(ss, request, "payLmtMonth");
            applyMerchant(ss, request, "payLmtYearCorp");
            applyMerchant(ss, request, "payLmtYearInd");
            ss.setPayLmtUiMode(normalizeUiMode(request.getParameter("payLmtUiMode"), true));
        }
        settlementSettingRepository.save(ss);
    }

    public void saveFromRequestByCode(String compCode, String compDiv, HttpServletRequest request) {
        if (compCode == null || compCode.isBlank()) {
            return;
        }
        orgUnitRepository.findByCode(compCode.trim()).ifPresent(ou -> saveFromRequest(ou.getId(), compDiv, request));
    }

    public void attachDetail(Map<String, Object> detail) {
        if (detail == null || detail.get("compId") == null) {
            return;
        }
        orgUnitRepository.findByCode(String.valueOf(detail.get("compId")).trim()).ifPresent(ou -> {
            SettlementSetting ss = settlementSettingRepository.findByOrgUnitId(ou.getId()).orElse(null);
            boolean merchant = ou.getOrgLevel() == OrgLevel.MERCHANT;
            OrgUnit dist = findMasterDist(ou);
            String ccy = baseCurrency(dist != null ? dist : ou);
            detail.put("payLmtCurrency", ccy);
            putAmt(detail, "payLmtTxMax", ss != null ? ss.getPayLmtTxMax() : null);
            putAmt(detail, "payLmtTxMin", ss != null ? ss.getPayLmtTxMin() : null);
            putAmt(detail, "payLmtDay", ss != null ? ss.getPayLmtDay() : null);
            putAmt(detail, "payLmtMonth", ss != null ? ss.getPayLmtMonth() : null);
            putAmt(detail, "payLmtYearCorp", ss != null ? ss.getPayLmtYearCorp() : null);
            putAmt(detail, "payLmtYearInd", ss != null ? ss.getPayLmtYearInd() : null);
            if (merchant) {
                detail.put("payLmtTxMaxMode", modeOrFollow(ss != null ? ss.getPayLmtTxMaxMode() : null));
                detail.put("payLmtTxMinMode", modeOrFollow(ss != null ? ss.getPayLmtTxMinMode() : null));
                detail.put("payLmtDayMode", modeOrFollow(ss != null ? ss.getPayLmtDayMode() : null));
                detail.put("payLmtMonthMode", modeOrFollow(ss != null ? ss.getPayLmtMonthMode() : null));
                detail.put("payLmtYearCorpMode", modeOrFollow(ss != null ? ss.getPayLmtYearCorpMode() : null));
                detail.put("payLmtYearIndMode", modeOrFollow(ss != null ? ss.getPayLmtYearIndMode() : null));
                detail.put("payLmtUiMode", normalizeUiMode(ss != null ? ss.getPayLmtUiMode() : null, true));
            } else if (ou.getOrgLevel() == OrgLevel.MASTER_DIST) {
                detail.put("payLmtUiMode", normalizeUiMode(ss != null ? ss.getPayLmtUiMode() : null, false));
            }
        });
    }

    /**
     * URL·챗봇 결제창용 공개 한도(1회 최소·최대 + 표시 방식). 일·월·연은 내려주지 않습니다.
     */
    public void putPublicCheckoutFields(Map<String, Object> data, Long merchantOrgId) {
        if (data == null || merchantOrgId == null) {
            return;
        }
        OrgUnit merchant = orgUnitRepository.findById(merchantOrgId).orElse(null);
        if (merchant == null || merchant.getOrgLevel() != OrgLevel.MERCHANT) {
            return;
        }
        OrgUnit dist = findMasterDist(merchant);
        if (dist == null) {
            return;
        }
        String limitCcy = baseCurrency(dist);
        SettlementSetting own = settlementSettingRepository.findByOrgUnitId(merchant.getId()).orElse(null);
        SettlementSetting parent = settlementSettingRepository.findByOrgUnitId(dist.getId()).orElse(null);
        BigDecimal txMax = effective(own, parent, Slot.TX_MAX);
        BigDecimal txMin = effective(own, parent, Slot.TX_MIN);
        String uiMode = effectiveUiMode(own, parent);
        data.put("payLimitCurrency", limitCcy);
        data.put("payLimitTxMax", txMax != null ? txMax.stripTrailingZeros().toPlainString() : "");
        data.put("payLimitTxMin", txMin != null ? txMin.stripTrailingZeros().toPlainString() : "");
        data.put("payLimitUiMode", uiMode);
        data.put("payLimitUiAlways", "ALWAYS".equals(uiMode));
    }

    /**
     * @return empty if allowed, else a failure map ({@code success}=false, {@code errorCode}, {@code message})
     */
    public Optional<Map<String, Object>> check(Long merchantOrgId, BigDecimal chargeAmount, String chargeCurrency, String lang) {
        if (merchantOrgId == null || chargeAmount == null || chargeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }
        OrgUnit merchant = orgUnitRepository.findById(merchantOrgId).orElse(null);
        if (merchant == null || merchant.getOrgLevel() != OrgLevel.MERCHANT) {
            return Optional.empty();
        }
        OrgUnit dist = findMasterDist(merchant);
        if (dist == null) {
            return Optional.empty();
        }
        String limitCcy = baseCurrency(dist);
        if (limitCcy.isBlank()) {
            return Optional.empty();
        }
        SettlementSetting own = settlementSettingRepository.findByOrgUnitId(merchant.getId()).orElse(null);
        SettlementSetting parent = settlementSettingRepository.findByOrgUnitId(dist.getId()).orElse(null);
        boolean personal = isPersonal(merchant.getId());
        BigDecimal txMax = effective(own, parent, Slot.TX_MAX);
        BigDecimal txMin = effective(own, parent, Slot.TX_MIN);
        BigDecimal day = effective(own, parent, Slot.DAY);
        BigDecimal month = effective(own, parent, Slot.MONTH);
        BigDecimal year = personal ? effective(own, parent, Slot.YEAR_IND) : effective(own, parent, Slot.YEAR_CORP);
        if (txMax == null && txMin == null && day == null && month == null && year == null) {
            return Optional.empty();
        }
        Optional<BigDecimal> nowOpt = urlPayDisplayFxService.convertAmount(chargeAmount, chargeCurrency, limitCcy);
        if (nowOpt.isEmpty()) {
            return Optional.of(fail("PAY_LIMIT_FX_UNAVAILABLE", lang, limitCcy, null));
        }
        BigDecimal now = nowOpt.get();
        if (txMax != null && now.compareTo(txMax) > 0) {
            return Optional.of(fail("PAY_LIMIT_TX_MAX", lang, limitCcy, txMax));
        }
        if (txMin != null && now.compareTo(txMin) < 0) {
            return Optional.of(fail("PAY_LIMIT_TX_MIN", lang, limitCcy, txMin));
        }
        LocalDateTime seoulNow = LocalDateTime.now(SEOUL);
        if (day != null || month != null || year != null) {
            LocalDateTime from = seoulNow.toLocalDate().atStartOfDay();
            if (year != null) {
                from = seoulNow.toLocalDate().withDayOfYear(1).atStartOfDay();
            } else if (month != null) {
                from = seoulNow.toLocalDate().withDayOfMonth(1).atStartOfDay();
            }
            BigDecimal spentDay = BigDecimal.ZERO;
            BigDecimal spentMonth = BigDecimal.ZERO;
            BigDecimal spentYear = BigDecimal.ZERO;
            LocalDateTime dayStart = seoulNow.toLocalDate().atStartOfDay();
            LocalDateTime monthStart = seoulNow.toLocalDate().withDayOfMonth(1).atStartOfDay();
            List<Object[]> rows = pgTrnsctnRepository.findApprovedAmountsSince(merchant.getCode(), from);
            for (Object[] row : rows) {
                if (row == null || row.length < 2 || !(row[0] instanceof BigDecimal amt)) {
                    continue;
                }
                String cur = row[1] != null ? row[1].toString() : limitCcy;
                Optional<BigDecimal> conv = urlPayDisplayFxService.convertAmount(amt, cur, limitCcy);
                if (conv.isEmpty()) {
                    continue;
                }
                spentYear = spentYear.add(conv.get());
            }
            if (day != null || month != null) {
                List<Object[]> dayRows = day != null
                        ? pgTrnsctnRepository.findApprovedAmountsSince(merchant.getCode(), dayStart)
                        : List.of();
                for (Object[] row : dayRows) {
                    if (row == null || row.length < 2 || !(row[0] instanceof BigDecimal amt)) {
                        continue;
                    }
                    Optional<BigDecimal> conv = urlPayDisplayFxService.convertAmount(amt, row[1] != null ? row[1].toString() : limitCcy, limitCcy);
                    if (conv.isPresent()) {
                        spentDay = spentDay.add(conv.get());
                    }
                }
                if (month != null) {
                    List<Object[]> monthRows = pgTrnsctnRepository.findApprovedAmountsSince(merchant.getCode(), monthStart);
                    for (Object[] row : monthRows) {
                        if (row == null || row.length < 2 || !(row[0] instanceof BigDecimal amt)) {
                            continue;
                        }
                        Optional<BigDecimal> conv = urlPayDisplayFxService.convertAmount(amt, row[1] != null ? row[1].toString() : limitCcy, limitCcy);
                        if (conv.isPresent()) {
                            spentMonth = spentMonth.add(conv.get());
                        }
                    }
                }
            }
            if (day != null && spentDay.add(now).compareTo(day) > 0) {
                return Optional.of(fail("PAY_LIMIT_DAY", lang, limitCcy, day));
            }
            if (month != null && spentMonth.add(now).compareTo(month) > 0) {
                return Optional.of(fail("PAY_LIMIT_MONTH", lang, limitCcy, month));
            }
            if (year != null && spentYear.add(now).compareTo(year) > 0) {
                return Optional.of(fail("PAY_LIMIT_YEAR", lang, limitCcy, year));
            }
        }
        return Optional.empty();
    }

    private void applyMerchant(SettlementSetting ss, HttpServletRequest request, String name) {
        String mode = modeOrFollow(request.getParameter(name + "Mode"));
        BigDecimal amt = parseAmt(request.getParameter(name));
        switch (name) {
            case "payLmtTxMax" -> { ss.setPayLmtTxMaxMode(mode); ss.setPayLmtTxMax(amt); }
            case "payLmtTxMin" -> { ss.setPayLmtTxMinMode(mode); ss.setPayLmtTxMin(amt); }
            case "payLmtDay" -> { ss.setPayLmtDayMode(mode); ss.setPayLmtDay(amt); }
            case "payLmtMonth" -> { ss.setPayLmtMonthMode(mode); ss.setPayLmtMonth(amt); }
            case "payLmtYearCorp" -> { ss.setPayLmtYearCorpMode(mode); ss.setPayLmtYearCorp(amt); }
            case "payLmtYearInd" -> { ss.setPayLmtYearIndMode(mode); ss.setPayLmtYearInd(amt); }
            default -> { }
        }
    }

    private BigDecimal effective(SettlementSetting own, SettlementSetting parent, Slot slot) {
        boolean direct = own != null && "DIRECT".equals(modeOf(own, slot));
        SettlementSetting src = direct ? own : parent;
        if (src == null) {
            return null;
        }
        return amountOf(src, slot);
    }

    private static String modeOf(SettlementSetting ss, Slot slot) {
        return switch (slot) {
            case TX_MAX -> ss.getPayLmtTxMaxMode();
            case TX_MIN -> ss.getPayLmtTxMinMode();
            case DAY -> ss.getPayLmtDayMode();
            case MONTH -> ss.getPayLmtMonthMode();
            case YEAR_CORP -> ss.getPayLmtYearCorpMode();
            case YEAR_IND -> ss.getPayLmtYearIndMode();
        };
    }

    private static BigDecimal amountOf(SettlementSetting ss, Slot slot) {
        return switch (slot) {
            case TX_MAX -> ss.getPayLmtTxMax();
            case TX_MIN -> ss.getPayLmtTxMin();
            case DAY -> ss.getPayLmtDay();
            case MONTH -> ss.getPayLmtMonth();
            case YEAR_CORP -> ss.getPayLmtYearCorp();
            case YEAR_IND -> ss.getPayLmtYearInd();
        };
    }

    private boolean isPersonal(Long orgId) {
        String reg = merchantProfileRepository.findByOrgUnitId(orgId).map(MerchantProfile::getRegNo).orElse("");
        return reg != null && reg.toUpperCase(Locale.ROOT).startsWith("PERSONAL");
    }

    private OrgUnit findMasterDist(OrgUnit start) {
        OrgUnit cur = start;
        for (int i = 0; i < 12 && cur != null; i++) {
            if (cur.getOrgLevel() == OrgLevel.MASTER_DIST) {
                return cur;
            }
            Long parentId = cur.getParentId();
            if (parentId == null) {
                return null;
            }
            cur = orgUnitRepository.findById(parentId).orElse(null);
        }
        return null;
    }

    private String baseCurrency(OrgUnit ou) {
        if (ou == null) {
            return "";
        }
        String raw = merchantProfileRepository.findByOrgUnitId(ou.getId()).map(MerchantProfile::getBaseCurrency).orElse("");
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String first = raw.split(",")[0].trim().toUpperCase(Locale.ROOT);
        return first.length() > 8 ? first.substring(0, 8) : first;
    }

    private static void putAmt(Map<String, Object> detail, String key, BigDecimal amt) {
        detail.put(key, amt != null ? amt.stripTrailingZeros().toPlainString() : "");
    }

    private static String modeOrFollow(String raw) {
        if (raw != null && "DIRECT".equalsIgnoreCase(raw.trim())) {
            return "DIRECT";
        }
        return "FOLLOW";
    }

    private static String normalizeUiMode(String raw, boolean allowFollow) {
        if (raw == null || raw.isBlank()) {
            return allowFollow ? "FOLLOW" : "WARN_ONLY";
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if ("ALWAYS".equals(u)) {
            return "ALWAYS";
        }
        if ("WARN_ONLY".equals(u) || "WARN".equals(u)) {
            return "WARN_ONLY";
        }
        if (allowFollow && "FOLLOW".equals(u)) {
            return "FOLLOW";
        }
        return allowFollow ? "FOLLOW" : "WARN_ONLY";
    }

    private static String effectiveUiMode(SettlementSetting own, SettlementSetting parent) {
        String ownMode = own != null ? normalizeUiMode(own.getPayLmtUiMode(), true) : "FOLLOW";
        if (!"FOLLOW".equals(ownMode)) {
            return ownMode;
        }
        return normalizeUiMode(parent != null ? parent.getPayLmtUiMode() : null, false);
    }

    private static BigDecimal parseAmt(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Map<String, Object> fail(String code, String lang, String ccy, BigDecimal limit) {
        String lim = limit != null ? limit.stripTrailingZeros().toPlainString() + " " + ccy : ccy;
        Map<String, String> messages = new LinkedHashMap<>();
        messages.put("KOR", kor(code, lim));
        messages.put("ENG", eng(code, lim));
        messages.put("JPN", jpn(code, lim));
        messages.put("CHN", chn(code, lim));
        messages.put("THA", tha(code, lim));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", false);
        out.put("errorCode", code);
        out.put("messages", messages);
        out.put("message", pick(messages, lang));
        return out;
    }

    private static String pick(Map<String, String> messages, String lang) {
        String u = lang != null ? lang.trim().toUpperCase(Locale.ROOT) : "";
        if (u.startsWith("EN") || "ENG".equals(u)) return messages.get("ENG");
        if (u.startsWith("JA") || u.startsWith("JP") || "JPN".equals(u)) return messages.get("JPN");
        if (u.startsWith("ZH") || u.startsWith("CH") || "CHN".equals(u)) return messages.get("CHN");
        if (u.startsWith("TH") || "THA".equals(u)) return messages.get("THA");
        return messages.get("KOR");
    }

    private static String kor(String code, String lim) {
        return switch (code) {
            case "PAY_LIMIT_TX_MAX" -> "1회 최대 한도(" + lim + ")를 초과했습니다.";
            case "PAY_LIMIT_TX_MIN" -> "1회 최소 한도(" + lim + ")보다 작습니다.";
            case "PAY_LIMIT_DAY" -> "일 한도(" + lim + ")를 초과했습니다.";
            case "PAY_LIMIT_MONTH" -> "월 한도(" + lim + ")를 초과했습니다.";
            case "PAY_LIMIT_YEAR" -> "연 한도(" + lim + ")를 초과했습니다.";
            default -> "한도 통화를 환산할 수 없습니다. 기준 통화 " + lim;
        };
    }

    private static String eng(String code, String lim) {
        return switch (code) {
            case "PAY_LIMIT_TX_MAX" -> "Exceeds the per-transaction maximum (" + lim + ").";
            case "PAY_LIMIT_TX_MIN" -> "Below the per-transaction minimum (" + lim + ").";
            case "PAY_LIMIT_DAY" -> "Exceeds the daily limit (" + lim + ").";
            case "PAY_LIMIT_MONTH" -> "Exceeds the monthly limit (" + lim + ").";
            case "PAY_LIMIT_YEAR" -> "Exceeds the yearly limit (" + lim + ").";
            default -> "Cannot convert the charge into the limit currency " + lim + ".";
        };
    }

    private static String jpn(String code, String lim) {
        return switch (code) {
            case "PAY_LIMIT_TX_MAX" -> "1回の上限(" + lim + ")を超えています。";
            case "PAY_LIMIT_TX_MIN" -> "1回の下限(" + lim + ")を下回っています。";
            case "PAY_LIMIT_DAY" -> "1日の上限(" + lim + ")を超えています。";
            case "PAY_LIMIT_MONTH" -> "月間上限(" + lim + ")を超えています。";
            case "PAY_LIMIT_YEAR" -> "年間上限(" + lim + ")を超えています。";
            default -> "限度通貨 " + lim + " へ換算できません。";
        };
    }

    private static String chn(String code, String lim) {
        return switch (code) {
            case "PAY_LIMIT_TX_MAX" -> "超过单笔最高限额(" + lim + ")。";
            case "PAY_LIMIT_TX_MIN" -> "低于单笔最低限额(" + lim + ")。";
            case "PAY_LIMIT_DAY" -> "超过每日限额(" + lim + ")。";
            case "PAY_LIMIT_MONTH" -> "超过每月限额(" + lim + ")。";
            case "PAY_LIMIT_YEAR" -> "超过每年限额(" + lim + ")。";
            default -> "无法换算为限额货币 " + lim + "。";
        };
    }

    private static String tha(String code, String lim) {
        return switch (code) {
            case "PAY_LIMIT_TX_MAX" -> "เกินวงเงินสูงสุดต่อครั้ง (" + lim + ")";
            case "PAY_LIMIT_TX_MIN" -> "ต่ำกว่าวงเงินขั้นต่ำต่อครั้ง (" + lim + ")";
            case "PAY_LIMIT_DAY" -> "เกินวงเงินรายวัน (" + lim + ")";
            case "PAY_LIMIT_MONTH" -> "เกินวงเงินรายเดือน (" + lim + ")";
            case "PAY_LIMIT_YEAR" -> "เกินวงเงินรายปี (" + lim + ")";
            default -> "แปลงเป็นสกุลเงินวงเงิน " + lim + " ไม่ได้";
        };
    }

    private enum Slot { TX_MAX, TX_MIN, DAY, MONTH, YEAR_CORP, YEAR_IND }
}
