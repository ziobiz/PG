package com.pg.api.dto;

import com.pg.entity.MerchantProfile;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.PgTrnsctn;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 결제내역 그리드 한 행 — 엑셀 템플릿(가맹점 정산형 컬럼) + 칠페이(ChillPay) 동기화 컬럼.
 * <ul>
 *   <li>Merchant / 가맹점 — 우리 PG의 가맹점(고객사)</li>
 *   <li>Customer / 고객명 — 해당 가맹점의 결제 고객(거래 단위, 대표자와 무관)</li>
 * </ul>
 */
public class PayListItemDto {

    private static final DateTimeFormatter TRN_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TRN_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static Map<String, Object> from(PgTrnsctn t, PayListRowContext ctx) {
        Map<String, Object> row = new HashMap<>();
        String compNm = ctx != null && ctx.getCompNm() != null ? ctx.getCompNm() : t.getMerchantId();
        MerchantProfile mp = ctx != null ? ctx.getProfile() : null;
        MerchantPgBinding b = ctx != null ? ctx.getBinding() : null;

        LocalDateTime created = t.getCreatedAt();
        String payDtStr = created != null
                ? created.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).replace("T", " ")
                : "";
        if (created != null) {
            row.put("trnDate", created.toLocalDate().format(TRN_DATE));
            row.put("trnTime", created.toLocalTime().format(TRN_TIME));
        } else {
            row.put("trnDate", "");
            row.put("trnTime", "");
        }
        /** 우리 시스템 거래 PK (후속조치·내부 조회용) */
        String ourTrn = t.getTrnId() != null ? t.getTrnId() : "";
        row.put("trnId", ourTrn);
        /** 칠페이 TransactionId (없으면 표시만 '-' — 우리 번호와 혼동 금지) */
        row.put("chillTransactionId", chillTxnIdLabel(t));
        /** 하위 호환: 예전 단일 컬럼명 — 칠페이 쪽 ID만 의미 (우리 trnId는 trnId 사용) */
        row.put("transactionId", row.get("chillTransactionId"));
        row.put("chillCustomer", chillCustomerLabel(t));
        row.put("orderNo", orderNoLabel(t));
        row.put("paymentChannel", blank(t.getPaymentChannel()));
        row.put("payCompletedAt", formatDt(t.getPaidAt()));
        row.put("currency", resolveCurrencyCodeForDisplay(t, mp));
        row.put("routeNo", routeNoLabel(t, b));
        /** 그리드 행 상태색·뱃지 — 클라이언트 톤 매핑용(ICOPAY 내부 코드) */
        row.put("status", t.getStatus() != null ? t.getStatus() : "");
        row.put("chillPaymentStatus", chillStatusLabel(t));
        row.put("settledYn", t.getSettledYn() != null && !t.getSettledYn().isBlank() ? t.getSettledYn().trim() : "N");

        /** 표준 그리드: 번호·업체명·업체코드 중 업체명 */
        row.put("compNm", compNm);
        /** 하위 호환·칠페이 Merchant 컬럼 */
        row.put("merchantNm", compNm);
        row.put("compDivCode9", compNm);
        row.put("compId", t.getMerchantId());
        /** 사업자번호 노출 시 법인/개인 구분 미표시 — 번호 컬럼만 사용 */
        row.put("compRegDivNm", "-");
        row.put("compRegNo", regNo(mp));
        row.put("settleDiv", "정산");
        row.put("payDivNm", payDivLabel(t.getStatus()));
        row.put("payProcNm", payProcLabel(t.getStatus()));
        row.put("payCard", "-");
        row.put("cardAprvNo", blank(t.getApprovalNo()));
        row.put("payCardNo", "-");
        row.put("instalMonth", b != null && b.getMaxInstallmentMonths() != null ? String.valueOf(b.getMaxInstallmentMonths()) : "0");
        row.put("payMethod", b != null && b.getPayMethod() != null ? b.getPayMethod() : "카드");
        row.put("corpNm", compNm);
        row.put("pgNm", b != null && b.getPgCd() != null ? b.getPgCd() : (t.getVan() != null ? t.getVan() : "-"));
        row.put("terminalId", b != null && b.getMid() != null ? b.getMid() : "-");

        BigDecimal amtBd = t.getAmtKrw() != null ? t.getAmtKrw() : BigDecimal.ZERO;
        row.put("chillAmount", amtBd);
        String pgNo = t.getPayNo() != null && !t.getPayNo().isBlank()
                ? t.getPayNo()
                : (t.getTrnId() != null ? t.getTrnId() : "-");
        row.put("pgApproveAmt", amtBd);
        row.put("pgApproveNo", pgNo);
        row.put("payAmount", amtBd);
        row.put("paySeq", ourTrn);
        row.put("payAprv", payDtStr);
        row.put("payDttm", payDtStr);

        boolean isApprove = "10".equals(t.getStatus());
        BigDecimal totalRate = totalFeeRate(ctx);
        BigDecimal feeAmtBd = BigDecimal.ZERO;
        BigDecimal feeVatBd = BigDecimal.ZERO;
        BigDecimal holdAmtBd = BigDecimal.ZERO;
        BigDecimal settleAmtBd = BigDecimal.ZERO;
        if (isApprove) {
            ApprovedSettlementParts p = approvedSettlementParts(amtBd, ctx);
            feeAmtBd = p.feeAmt;
            feeVatBd = p.feeVat;
            holdAmtBd = p.holdAmt;
            settleAmtBd = p.settleAmt;
        }

        row.put("icopayAmt", amountJson(t.getIcopayAmt()));
        if (t.getChillFeeAmt() != null) {
            row.put("chillFeeAmt", t.getChillFeeAmt());
        } else {
            row.put("chillFeeAmt", feeAmtBd);
        }
        if (t.getTotalAmt() != null) {
            row.put("totalAmt", t.getTotalAmt());
        } else {
            BigDecimal cf = t.getChillFeeAmt() != null ? t.getChillFeeAmt() : feeAmtBd;
            row.put("totalAmt", amtBd.add(cf));
        }

        row.put("feeCnt", isApprove ? 1 : 0);
        row.put("feeRate", totalRate);
        row.put("feeAmt", feeAmtBd);
        row.put("feeVat", feeVatBd);
        row.put("holdRate", resolveRollingPct(ctx));
        row.put("holdAmt", holdAmtBd);
        row.put("holdDttm", holdAmtBd.compareTo(BigDecimal.ZERO) > 0 ? payDtStr : "");

        row.put("calcCycle", ctx != null && ctx.getSettlement() != null && ctx.getSettlement().getCalcCycle() != null
                ? ctx.getSettlement().getCalcCycle() : "-");
        row.put("settleAmt", settleAmtBd);
        row.put("calcDt", payDtStr);
        row.put("approveDt", isApprove ? payDtStr : "");
        row.put("cancelDt", "20".equals(t.getStatus()) ? payDtStr : "");
        row.put("payStatus", payStatusLabel(t.getStatus()));

        row.put("productNm", mp != null && mp.getProduct() != null ? mp.getProduct() : "-");
        /** 가맹점의 결제 고객(칠페이 customer 등). 가맹 대표자(CEO)와 구분 */
        row.put("customerNm", payerDisplayName(t));
        /** 결제자 연락처 — 거래에 없으면 '-' (가맹 대표 휴대폰과 별도) */
        row.put("customerTel", payerTelPlaceholder());

        row.put("regionalNm", ctx != null ? ctx.getRegionalNm() : "");
        row.put("masterNm", ctx != null ? ctx.getMasterNm() : "");
        row.put("branchNm", ctx != null ? ctx.getBranchNm() : "");

        row.put("origin", t.getOrigin() != null ? t.getOrigin() : "CHILL");
        row.put("notifyChannelType", notifyChannelTypeLabel(t));
        return row;
    }

    /**
     * 승인(status=10) 건의 수수료·부가세·보류·예상 지급액 — 그리드 행·목록 상단 집계 공통.
     */
    public static final class ApprovedSettlementParts {
        public final BigDecimal feeAmt;
        public final BigDecimal feeVat;
        public final BigDecimal holdAmt;
        public final BigDecimal settleAmt;

        public ApprovedSettlementParts(BigDecimal feeAmt, BigDecimal feeVat, BigDecimal holdAmt, BigDecimal settleAmt) {
            this.feeAmt = feeAmt;
            this.feeVat = feeVat;
            this.holdAmt = holdAmt;
            this.settleAmt = settleAmt;
        }
    }

    public static ApprovedSettlementParts approvedSettlementParts(BigDecimal amtBd, PayListRowContext ctx) {
        BigDecimal amt = amtBd != null ? amtBd : BigDecimal.ZERO;
        int feeScale = derivedFeeScale(amt);
        BigDecimal totalRate = totalFeeRate(ctx);
        BigDecimal feeAmtBd = amt.multiply(totalRate).divide(BigDecimal.valueOf(100), feeScale, RoundingMode.HALF_UP);
        BigDecimal feeVatBd = feeAmtBd.multiply(BigDecimal.valueOf(0.1)).setScale(feeScale, RoundingMode.HALF_UP);
        BigDecimal holdAmtBd = BigDecimal.ZERO;
        BigDecimal rollingPct = resolveRollingPct(ctx);
        if (rollingPct.compareTo(BigDecimal.ZERO) > 0) {
            holdAmtBd = amt.multiply(rollingPct).divide(BigDecimal.valueOf(100), feeScale, RoundingMode.HALF_UP);
        }
        BigDecimal settleAmtBd = amt.subtract(feeAmtBd).subtract(feeVatBd).subtract(holdAmtBd)
                .setScale(feeScale, RoundingMode.HALF_UP);
        return new ApprovedSettlementParts(feeAmtBd, feeVatBd, holdAmtBd, settleAmtBd);
    }

    /** 목록 상단 취소 금액 합산용(결제취소·무효·환불 등) */
    public static boolean isCancelAmountStatus(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        return switch (status.trim()) {
            case "20", "21", "22", "30", "31", "40", "41", "42" -> true;
            default -> false;
        };
    }

    /**
     * NOTI 출처만 수신 채널 표시. ziobiz/NOTI 노티거래내역과 동일: CALL·RESULT·BOTH (DB CALLBACK·CALL → CALL).
     */
    private static String notifyChannelTypeLabel(PgTrnsctn t) {
        String o = t.getOrigin();
        if (o == null || !"NOTI".equalsIgnoreCase(o.trim())) {
            return "-";
        }
        String ch = t.getNotifyChannelType();
        if (ch == null || ch.isBlank()) {
            return "CALL";
        }
        String u = ch.trim().toUpperCase(Locale.ROOT);
        if ("CALL".equals(u) || "CALLBACK".equals(u)) {
            return "CALL";
        }
        if ("RESULT".equals(u)) {
            return "RESULT";
        }
        if ("BOTH".equals(u)) {
            return "BOTH";
        }
        return u;
    }

    /** 유통 수수료율(있으면) 또는 결제수수료율 + 정책의 USDT·FX·3DS 율(%) 합 — 승인 건 수수료 추정에 사용 */
    private static BigDecimal totalFeeRate(PayListRowContext ctx) {
        if (ctx == null) return BigDecimal.ZERO;
        BigDecimal base;
        if (ctx.getDistFee() != null) {
            var d = ctx.getDistFee();
            base = nz(d.getHqRate()).add(nz(d.getRegionalRate())).add(nz(d.getMasterRate()))
                    .add(nz(d.getBranchRate())).add(nz(d.getAgencyRate()));
        } else if (ctx.getPolicy() != null) {
            base = nz(ctx.getPolicy().getPayRate());
        } else {
            base = BigDecimal.ZERO;
        }
        if (ctx.getPolicy() != null) {
            var p = ctx.getPolicy();
            base = base.add(nz(p.getFeeUsdt())).add(nz(p.getFeeFx())).add(nz(p.getFee3dsRate()));
        }
        return base;
    }

    private static BigDecimal resolveRollingPct(PayListRowContext ctx) {
        BigDecimal fromPolicy = ctx != null && ctx.getPolicy() != null && ctx.getPolicy().getRollingPct() != null
                ? ctx.getPolicy().getRollingPct() : BigDecimal.ZERO;
        if (ctx != null && ctx.getSettlement() != null) {
            String follow = ctx.getSettlement().getHoldRateFollowHq();
            if (follow != null && "N".equalsIgnoreCase(follow.trim())
                    && ctx.getSettlement().getHoldRate() != null
                    && ctx.getSettlement().getHoldRate().compareTo(BigDecimal.ZERO) > 0) {
                return ctx.getSettlement().getHoldRate();
            }
        }
        return fromPolicy;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static String blank(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }

    private static String formatDt(LocalDateTime dt) {
        if (dt == null) return "";
        return dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).replace("T", " ");
    }

    /** 노티·DB 소수 금액 그대로 JSON 숫자로 (없으면 빈 문자열) */
    private static Object amountJson(BigDecimal v) {
        return v != null ? v : "";
    }

    /**
     * 원금에 소수가 있으면 추정 수수료·지급액도 동일 스케일(최소 2, 최대 8)로 맞춤. KRW/JPY 등 정수 원금은 0.
     */
    private static int derivedFeeScale(BigDecimal principal) {
        if (principal == null) {
            return 0;
        }
        int s = principal.scale();
        return s > 0 ? Math.min(8, Math.max(2, s)) : 0;
    }

    /** 칠페이 Customer 컬럼용 — 식별자+표시명 (고객명과 동일 인물, 표기만 시트 형식) */
    private static String chillCustomerLabel(PgTrnsctn t) {
        String id = t.getCustomerId();
        String nm = t.getCustomerNm();
        boolean hasId = id != null && !id.isBlank();
        boolean hasNm = nm != null && !nm.isBlank();
        if (!hasId && !hasNm) return "-";
        if (hasId && hasNm) return id.trim() + " | " + nm.trim();
        return hasId ? id.trim() : nm.trim();
    }

    /** 그리드 고객명 — 결제자 이름 우선, 없으면 고객 ID */
    private static String payerDisplayName(PgTrnsctn t) {
        if (t.getCustomerNm() != null && !t.getCustomerNm().isBlank()) {
            return t.getCustomerNm().trim();
        }
        if (t.getCustomerId() != null && !t.getCustomerId().isBlank()) {
            return t.getCustomerId().trim();
        }
        return "-";
    }

    /** 향후 거래 단위 결제자 연락처 컬럼 연동 시 확장 */
    private static String payerTelPlaceholder() {
        return "-";
    }

    private static String orderNoLabel(PgTrnsctn t) {
        if (t.getOrderNo() != null && !t.getOrderNo().isBlank()) return t.getOrderNo().trim();
        return blank(t.getPayNo());
    }

    private static String chillTxnIdLabel(PgTrnsctn t) {
        if (t.getChillTransactionId() != null && !t.getChillTransactionId().isBlank()) {
            return t.getChillTransactionId().trim();
        }
        return "-";
    }

    private static String routeNoLabel(PgTrnsctn t, MerchantPgBinding b) {
        if (t.getRouteNo() != null && !t.getRouteNo().isBlank()) return t.getRouteNo().trim();
        if (b != null && b.getRootNo() != null && !b.getRootNo().isBlank()) return b.getRootNo().trim();
        return "-";
    }

    /**
     * 노티 적재 시 {@code cur_type} 이 KRW/410 기본값으로만 남는 경우, 가맹점 {@code baseCurrency}로 표시를 보강합니다.
     * <ul>
     *   <li>기준통화가 하나면 그 코드</li>
     *   <li>KRW·410과 JPY·USD 등이 섞여 있으면 KRW 계열만 제외한 뒤 남은 코드가 하나면 그것</li>
     *   <li>KRW를 제외한 복수 통화만 있으면 정렬 후 {@code JPY/USD} 형태(행마다 구분 불가 시 노티 Currency 매핑 권장)</li>
     * </ul>
     */
    private static String resolveCurrencyCodeForDisplay(PgTrnsctn t, MerchantProfile mp) {
        String db = t.getCurType() != null ? t.getCurType().trim().toUpperCase(Locale.ROOT) : "";
        if (!looksLikeWeakDefaultKrw(db)) {
            return db.isEmpty() ? "KRW" : db;
        }
        List<String> bases = parseBaseCurrencyTokens(mp);
        List<String> nonKrwBases = new ArrayList<>();
        for (String b : bases) {
            if (!looksLikeWeakDefaultKrw(b)) {
                nonKrwBases.add(b);
            }
        }
        if (nonKrwBases.size() == 1) {
            return nonKrwBases.get(0);
        }
        if (nonKrwBases.size() >= 2) {
            nonKrwBases.sort(String::compareTo);
            return String.join("/", nonKrwBases);
        }
        if (bases.size() == 1) {
            return bases.get(0);
        }
        return db.isEmpty() ? "KRW" : db;
    }

    private static boolean looksLikeWeakDefaultKrw(String c) {
        return c == null || c.isEmpty() || "KRW".equals(c) || "410".equals(c);
    }

    private static List<String> parseBaseCurrencyTokens(MerchantProfile mp) {
        if (mp == null || mp.getBaseCurrency() == null || mp.getBaseCurrency().isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String p : mp.getBaseCurrency().split(",")) {
            String s = p != null ? p.trim().toUpperCase(Locale.ROOT) : "";
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
        return out;
    }

    /**
     * 결제내역 그리드 Status — 노티 수신 성공이 아니라 {@code chillPaymentStatus}(PaymentStatus) 의미와 동일하게 표시.
     * 콜백 숫자: 0 성공, 1·3 실패, 2 취소, 4 오류(PG 부록). 무효·환불 등은 ICOPAY 내부 코드(21·30 등) 원문 또는 내부 {@code status}로 노출.
     */
    private static String chillStatusLabel(PgTrnsctn t) {
        String rawStored = t.getChillPaymentStatus() != null ? t.getChillPaymentStatus().trim() : "";
        if (!rawStored.isEmpty()) {
            String fromCallback = chillNotiPaymentStatusDigitToKo(rawStored);
            if (fromCallback != null) {
                return fromCallback;
            }
            String fromIcCode = chillIcPayStatusCodeTokenToKo(rawStored);
            if (fromIcCode != null) {
                return fromIcCode;
            }
            if (!isBareNumericChillStatus(rawStored)) {
                if (!isAmbiguousProgressChillDisplay(rawStored) || !hasDefinitiveInternalPayStatus(t.getStatus())) {
                    return rawStored;
                }
                /* DB에 Processing 등만 남고 내부 상태는 이미 승인·취소 등으로 맞춰진 레거시 행 */
            }
        }
        return chillInternalPayStatusToKo(t.getStatus());
    }

    /** NOTI/콜백 PaymentStatus 한 자리 숫자만 — 다자리 숫자는 null(내부 코드 경로). */
    private static String chillNotiPaymentStatusDigitToKo(String raw) {
        if (raw == null || !raw.matches("^[0-4]$")) {
            return null;
        }
        return switch (raw) {
            case "0" -> "성공";
            case "1", "3" -> "실패";
            case "2" -> "취소";
            case "4" -> "오류";
            default -> null;
        };
    }

    /** DB에 내부 상태 문자열이 그대로 들어간 경우(08·10·21·30 …). */
    private static String chillIcPayStatusCodeTokenToKo(String raw) {
        if (raw == null) {
            return null;
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        return switch (u) {
            case "08" -> "요청";
            case "10" -> "성공";
            case "20" -> "취소";
            case "21" -> "무효";
            case "22" -> "이메일무효";
            case "40" -> "자동무효";
            case "41" -> "이메일무효";
            case "42" -> "자동환불";
            case "30" -> "환불";
            case "31" -> "강제환불";
            case "99", "F0" -> "실패";
            default -> null;
        };
    }

    private static String chillInternalPayStatusToKo(String st) {
        return switch (st != null ? st : "") {
            case "10" -> "성공";
            case "08" -> "요청";
            case "20" -> "취소";
            case "21" -> "무효";
            case "22" -> "이메일무효";
            case "30" -> "환불";
            case "31" -> "강제환불";
            case "F0", "99", "f0" -> "실패";
            default -> st != null && !st.isBlank() ? st : "-";
        };
    }

    /** PG가 숫자만 넣은 경우(Status/PaymentStatus 코드) — 화면에는 내부 상태 기반 문구로 바꿈 */
    private static boolean isBareNumericChillStatus(String s) {
        if (s == null) {
            return false;
        }
        return s.trim().matches("^\\d{1,3}$");
    }

    private static boolean isAmbiguousProgressChillDisplay(String raw) {
        if (raw == null) {
            return false;
        }
        String u = raw.trim().toLowerCase(Locale.ROOT);
        return u.equals("processing") || u.equals("pending") || u.equals("request")
                || u.equals("waitauthorize") || u.equals("wait_authorize");
    }

    private static boolean hasDefinitiveInternalPayStatus(String st) {
        if (st == null) {
            return false;
        }
        return switch (st) {
            case "10", "20", "21", "22", "30", "31", "40", "41", "42", "99", "F0", "f0" -> true;
            default -> false;
        };
    }

    private static String regNo(MerchantProfile mp) {
        if (mp == null || mp.getRegNo() == null) return "-";
        return mp.getRegNo().contains("|") ? mp.getRegNo().split("\\|", 2)[1] : mp.getRegNo();
    }

    private static String payDivLabel(String status) {
        if (status == null) return "-";
        return switch (status) {
            case "10" -> "결제";
            case "08" -> "인증대기";
            case "20" -> "취소";
            case "21" -> "무효";
            case "22" -> "수동무효";
            case "40" -> "자동무효";
            case "41" -> "이메일무효";
            case "42" -> "자동환불";
            case "30", "31" -> "환불";
            case "F0", "99" -> "실패";
            default -> status;
        };
    }

    private static String payProcLabel(String status) {
        if ("10".equals(status)) return "정산대기";
        if ("08".equals(status)) return "인증대기";
        if ("20".equals(status)) return "결제취소";
        return "정산대기";
    }

    private static String payStatusLabel(String status) {
        if ("10".equals(status)) return "정산대기";
        if ("08".equals(status)) return "인증대기";
        if ("20".equals(status)) return "취소";
        return payDivLabel(status);
    }
}
