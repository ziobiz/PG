package com.pg.api.dto;

import com.pg.entity.MerchantProfile;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.PgTrnsctn;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.pg.util.CommissionExtraFeeUtil;
import com.pg.util.MerchantFeeVatUtil;
import com.pg.util.PayListStatusBarBuckets;
import com.pg.util.TrnTimeDualZoneDisplay;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 결제내역 그리드 한 행 — 엑셀 템플릿(가맹점 정산형 컬럼) + 칠페이(ChillPay) 동기화 컬럼.
 * <ul>
 *   <li>Merchant / 가맹점 — 우리 PG의 가맹점(고객사)</li>
 *   <li>Customer / 고객명 — 해당 가맹점의 결제 고객(거래 단위, 대표자와 무관)</li>
 * </ul>
 */
public class PayListItemDto {

    private static final Pattern VOID_TOKEN_HINT = Pattern.compile("(^|[^a-z0-9_])void([^a-z0-9_]|$)", Pattern.CASE_INSENSITIVE);

    private static final DateTimeFormatter TRN_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    /**
     * 결제내역 그리드 시각: {@code ledgerNaiveWallClockZone} 은 본사 전산설정 표준시간대({@code display_timezone})와 동일하게 두고,
     * naive {@code paid_at}/{@code created_at} 을 그 벽시계로 해석합니다.
     * 총판 {@code tb_master_dist_settlement_cycle_config} 행이 있으면 1줄=거래시간 프리셋·2줄=정산 크론 Zone, 없으면 JP·TH 고정 2줄.
     */
    public static Map<String, Object> from(PgTrnsctn t, PayListRowContext ctx) {
        return from(t, ctx, ZoneId.of("Asia/Bangkok"));
    }

    public static Map<String, Object> from(PgTrnsctn t, PayListRowContext ctx, ZoneId ledgerNaiveWallClockZone) {
        Map<String, Object> row = new HashMap<>();
        String compNm = ctx != null && ctx.getCompNm() != null ? ctx.getCompNm() : t.getMerchantId();
        MerchantProfile mp = ctx != null ? ctx.getProfile() : null;
        MerchantPgBinding b = ctx != null ? ctx.getBinding() : null;

        LocalDateTime txnClock = t.getPaidAt() != null ? t.getPaidAt() : t.getCreatedAt();
        ZoneId interpret = ledgerNaiveWallClockZone != null ? ledgerNaiveWallClockZone : ZoneId.of("Asia/Bangkok");
        TxnDualLineSpec dual = ctx != null ? ctx.getTxnDualLineSpec() : null;
        String payDtStr = txnClock != null
                ? (dual != null
                ? TrnTimeDualZoneDisplay.formatConfigurableDualLineDateTime(txnClock, interpret,
                dual.tag1(), dual.displayZone1(), dual.tag2(), dual.displayZone2())
                : TrnTimeDualZoneDisplay.formatDualLineDateTime(txnClock, interpret))
                : "";
        if (txnClock != null) {
            row.put("trnDate", txnClock.toLocalDate().format(TRN_DATE));
            row.put("trnTime", dual != null
                    ? TrnTimeDualZoneDisplay.formatConfigurableDualLineTimeOnly(txnClock, interpret,
                    dual.tag1(), dual.displayZone1(), dual.tag2(), dual.displayZone2())
                    : TrnTimeDualZoneDisplay.formatDualLineTimeOnly(txnClock, interpret));
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
        /* 성공(승인)=paidAt, 실패·취소 등=paidAt 없으면 createdAt — trnTime(시각만)과 동일 이중 일시 표기 */
        row.put("payCompletedAt", payDtStr);
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
        String effStatus = effectiveStatusForPayLabels(t);
        row.put("payDivNm", payDivLabel(effStatus));
        row.put("statusNm", PayListStatusBarBuckets.pgStatusDisplayLabel(effStatus));
        row.put("payProcNm", payProcLabel(effStatus));
        row.put("payCard", "-");
        row.put("cardAprvNo", resolveApprovalNoForDisplay(t));
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
        int feeScaleExec = derivedFeeScale(amtBd);
        if (isApprove) {
            ApprovedSettlementParts p = approvedSettlementParts(amtBd, ctx);
            feeAmtBd = p.feeAmt;
            feeVatBd = p.feeVat;
            holdAmtBd = p.holdAmt;
            settleAmtBd = p.settleAmt;
            row.put("perTxFeeAmt", p.perTxAmt);
            row.put("settlementPerTxFeeAmt", p.settlementPerTxAmt);
            row.put("extraFeesAmt", p.extraPctAmt);
            BigDecimal ratePortion = p.feeAmt.subtract(p.perTxAmt).subtract(p.settlementPerTxAmt).subtract(p.extraPctAmt)
                    .setScale(feeScaleExec, RoundingMode.HALF_UP);
            if (ratePortion.compareTo(BigDecimal.ZERO) < 0) {
                ratePortion = BigDecimal.ZERO;
            }
            row.put("feeAmtPayRateOnly", ratePortion);
            row.put("settlementExecGrossAmt", amtBd);
            row.put("settlementExecMdrAmt", ratePortion);
            row.put("settlementExecMdrRatePct", totalRate);
            row.put("settlementExecFixedAmt", p.perTxAmt);
            row.put("settlementExecSettlementAmt", p.settlementPerTxAmt);
            row.put("settlementExecCollateralAmt", holdAmtBd);
            row.put("settlementExecOtherAmt", p.extraPctAmt.add(feeVatBd).setScale(feeScaleExec, RoundingMode.HALF_UP));
            row.put("settlementExecExpectedPayout", settleAmtBd);
        } else {
            row.put("perTxFeeAmt", BigDecimal.ZERO);
            row.put("settlementPerTxFeeAmt", BigDecimal.ZERO);
            row.put("extraFeesAmt", BigDecimal.ZERO);
            row.put("feeAmtPayRateOnly", BigDecimal.ZERO);
            /* 비승인: 시도 금액은 매출이 아님 — 정산실행 상세 매출 열은 null(화면에서 —). 건당 과금은 고정 열에만. */
            row.put("settlementExecGrossAmt", null);
            row.put("settlementExecMdrAmt", BigDecimal.ZERO);
            row.put("settlementExecMdrRatePct", BigDecimal.ZERO);
            row.put("settlementExecFixedAmt", settlementExecOtherFeeEstimate(t, ctx, feeScaleExec));
            row.put("settlementExecSettlementAmt", BigDecimal.ZERO);
            row.put("settlementExecCollateralAmt", BigDecimal.ZERO);
            row.put("settlementExecOtherAmt", BigDecimal.ZERO);
            row.put("settlementExecExpectedPayout", null);
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
        row.put("cancelDt", ("20".equals(t.getStatus()) || isVoidFamilyStatus(t.getStatus())) ? payDtStr : "");
        row.put("payStatus", payStatusLabel(effStatus));

        row.put("productNm", mp != null && mp.getProduct() != null ? mp.getProduct() : "-");
        /** 가맹점의 결제 고객(칠페이 customer 등). 가맹 대표자(CEO)와 구분 */
        row.put("customerNm", payerDisplayName(t));
        /** 결제자 연락처 — 거래에 없으면 '-' (가맹 대표 휴대폰과 별도) */
        row.put("customerTel", payerTelPlaceholder());

        row.put("regionalNm", ctx != null ? ctx.getRegionalNm() : "");
        row.put("masterNm", ctx != null ? ctx.getMasterNm() : "");
        row.put("branchNm", ctx != null ? ctx.getBranchNm() : "");
        row.put("regionalBaseCur", ctx != null ? ctx.getRegionalBaseCurrency() : "");
        row.put("masterDistBaseCur", ctx != null ? ctx.getMasterDistBaseCurrency() : "");
        String merchantBaseCur = ctx != null ? ctx.getMerchantBaseCurrency() : "";
        if ((merchantBaseCur == null || merchantBaseCur.isBlank()) && mp != null && mp.getBaseCurrency() != null) {
            String bc = mp.getBaseCurrency().trim();
            if (!bc.isEmpty()) {
                int c = bc.indexOf(',');
                merchantBaseCur = c > 0 ? bc.substring(0, c).trim() : bc;
            }
        }
        row.put("merchantBaseCur", merchantBaseCur != null ? merchantBaseCur : "");

        row.put("origin", t.getOrigin() != null ? t.getOrigin() : "CHILL");
        row.put("notifyChannelType", notifyChannelTypeLabel(t));
        applyDisplayPayRow(t, row);
        return row;
    }

    /** VIEW SETTING: URL·DISPLAY_FX·노티 등 고객금액·고객통화(PG 청구와 별도). */
    private static void applyDisplayPayRow(PgTrnsctn t, Map<String, Object> row) {
        if (t.getDisplayAmt() != null && t.getDisplayCurType() != null && !t.getDisplayCurType().isBlank()) {
            String dCur = normalizeDisplayCurrency(t.getDisplayCurType().trim());
            row.put("displayPayAmt", t.getDisplayAmt());
            row.put("displayPayCur", dCur);
            String plain = t.getDisplayAmt().stripTrailingZeros().toPlainString();
            row.put("displayPaySummary", dCur + " " + plain);
            String billCur = row.get("currency") != null
                    ? normalizeDisplayCurrency(String.valueOf(row.get("currency")).trim())
                    : "";
            Object ca = row.get("chillAmount");
            BigDecimal billingAmt = ca instanceof BigDecimal b ? b : null;
            boolean curMatch = !billCur.isBlank() && billCur.equalsIgnoreCase(dCur);
            boolean amtMatch = billingAmt != null && billingAmt.stripTrailingZeros().compareTo(t.getDisplayAmt().stripTrailingZeros()) == 0;
            row.put("payCustomerIndicator", (curMatch && amtMatch) ? "\uB3D9\uC77C" : "\uBCC4\uB3C4");
        } else {
            row.put("displayPayAmt", null);
            row.put("displayPayCur", "");
            row.put("displayPaySummary", "");
            row.put("payCustomerIndicator", "\u2014");
        }
    }

    /**
     * 승인(status=10) 건의 수수료·부가세·보류·예상 지급액 — 그리드 행·목록 상단 집계 공통.
     */
    public static final class ApprovedSettlementParts {
        public final BigDecimal feeAmt;
        public final BigDecimal feeVat;
        public final BigDecimal holdAmt;
        public final BigDecimal settleAmt;
        /** 건당·정산건당·기타(%) 부가 항목 — 가맹정산·집계 표시용 */
        public final BigDecimal perTxAmt;
        public final BigDecimal settlementPerTxAmt;
        public final BigDecimal extraPctAmt;

        public ApprovedSettlementParts(BigDecimal feeAmt, BigDecimal feeVat, BigDecimal holdAmt, BigDecimal settleAmt,
                                        BigDecimal perTxAmt, BigDecimal settlementPerTxAmt, BigDecimal extraPctAmt) {
            this.feeAmt = feeAmt;
            this.feeVat = feeVat;
            this.holdAmt = holdAmt;
            this.settleAmt = settleAmt;
            this.perTxAmt = perTxAmt != null ? perTxAmt : BigDecimal.ZERO;
            this.settlementPerTxAmt = settlementPerTxAmt != null ? settlementPerTxAmt : BigDecimal.ZERO;
            this.extraPctAmt = extraPctAmt != null ? extraPctAmt : BigDecimal.ZERO;
        }
    }

    public static ApprovedSettlementParts approvedSettlementParts(BigDecimal amtBd, PayListRowContext ctx) {
        BigDecimal amt = amtBd != null ? amtBd : BigDecimal.ZERO;
        int feeScale = derivedFeeScale(amt);
        BigDecimal totalRate = totalFeeRate(ctx);
        BigDecimal rateFee = amt.multiply(totalRate).divide(BigDecimal.valueOf(100), feeScale, RoundingMode.HALF_UP);
        BigDecimal perTxAmt = BigDecimal.ZERO;
        BigDecimal settlementPerTxAmt = BigDecimal.ZERO;
        BigDecimal extraPctAmt = BigDecimal.ZERO;
        if (ctx != null && ctx.getPolicy() != null) {
            var p = ctx.getPolicy();
            perTxAmt = nz(p.getPerTxFee());
            /* fee_settlement_per_tx 는 정산 실행당 1회(tb_settlement_run.settlement_batch_fee_amt) — 건별 상세에서는 제외 */
            if (!ctx.isOmitSettlementFeeFromApprovedTxnBreakdown()) {
                settlementPerTxAmt = nz(p.getFeeSettlementPerTx());
            }
            extraPctAmt = CommissionExtraFeeUtil.sumPctOnApprovedAmount(p, amt);
        }
        BigDecimal feeAmtBd = rateFee.add(perTxAmt).add(settlementPerTxAmt).add(extraPctAmt)
                .setScale(feeScale, RoundingMode.HALF_UP);
        BigDecimal feeVatBd = MerchantFeeVatUtil.vatOnFeeAmount(feeAmtBd, ctx != null ? ctx.getSettlement() : null, feeScale);
        BigDecimal holdAmtBd = BigDecimal.ZERO;
        BigDecimal rollingPct = resolveRollingPct(ctx);
        if (rollingPct.compareTo(BigDecimal.ZERO) > 0) {
            holdAmtBd = amt.multiply(rollingPct).divide(BigDecimal.valueOf(100), feeScale, RoundingMode.HALF_UP);
        }
        BigDecimal settleAmtBd = amt.subtract(feeAmtBd).subtract(feeVatBd).subtract(holdAmtBd)
                .setScale(feeScale, RoundingMode.HALF_UP);
        return new ApprovedSettlementParts(feeAmtBd, feeVatBd, holdAmtBd, settleAmtBd, perTxAmt, settlementPerTxAmt, extraPctAmt);
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

    /** 유통 수수료율(있으면) 또는 결제수수료율 + 정책의 USDT·FX 율(%) 합 — 승인 건 % 수수료 추정에 사용(3DS는 건당 고정이라 미포함) */
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
            base = base.add(nz(p.getFeeUsdt())).add(nz(p.getFeeFx()));
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

    /**
     * 정산실행 상세 표 「기타」열: 취소·무효·환불·강제환불·실패 등 건당(또는 건당 고정) 추정 수수료.
     * 승인(10) 건은 별도로 extra+VAT 를 넣으므로 여기서는 호출하지 않습니다.
     */
    private static BigDecimal settlementExecOtherFeeEstimate(PgTrnsctn t, PayListRowContext ctx, int feeScale) {
        if (ctx == null || ctx.getPolicy() == null || t.getStatus() == null) {
            return BigDecimal.ZERO;
        }
        var p = ctx.getPolicy();
        String st = t.getStatus().trim();
        String stU = st.toUpperCase(Locale.ROOT);
        BigDecimal v = switch (stU) {
            case "20" -> nz(p.getCancelRate());
            case "21", "40" -> nz(p.getVoidFeePerTx());
            case "22", "41" -> nz(p.getManualVoidFeePerTx());
            case "30", "42" -> nz(p.getRefundRate());
            case "31" -> nz(p.getChargebackFeePerTx());
            case "99", "F0" -> nz(p.getFailFee());
            default -> BigDecimal.ZERO;
        };
        return v.setScale(feeScale, RoundingMode.HALF_UP);
    }

    private static String blank(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }

    /**
     * 카드사 승인번호가 없으면 칠페이 TransactionId·PG 승인(대체) 번호를 표시합니다.
     */
    private static String resolveApprovalNoForDisplay(PgTrnsctn t) {
        if (t.getApprovalNo() != null && !t.getApprovalNo().isBlank()) {
            return t.getApprovalNo().trim();
        }
        if (t.getChillTransactionId() != null && !t.getChillTransactionId().isBlank()) {
            return t.getChillTransactionId().trim();
        }
        if (t.getPayNo() != null && !t.getPayNo().isBlank()) {
            return t.getPayNo().trim();
        }
        return "-";
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
        String assembled;
        if (!looksLikeWeakDefaultKrw(db)) {
            assembled = db.isEmpty() ? "KRW" : db;
        } else {
            List<String> bases = parseBaseCurrencyTokens(mp);
            List<String> nonKrwBases = new ArrayList<>();
            for (String b : bases) {
                if (!looksLikeWeakDefaultKrw(b)) {
                    nonKrwBases.add(b);
                }
            }
            if (nonKrwBases.size() == 1) {
                assembled = nonKrwBases.get(0);
            } else if (nonKrwBases.size() >= 2) {
                nonKrwBases.sort(String::compareTo);
                assembled = String.join("/", nonKrwBases);
            } else if (bases.size() == 1) {
                assembled = bases.get(0);
            } else {
                assembled = db.isEmpty() ? "KRW" : db;
            }
        }
        return normalizeDisplayCurrency(assembled);
    }

    /** ISO 4217 숫자(764 등)·복수 통화 조합을 알파 코드(THB 등)로 표시 */
    private static String normalizeDisplayCurrency(String assembled) {
        if (assembled == null || assembled.isBlank()) {
            return PayListStatusBarBuckets.normalizeCurrency("");
        }
        if (assembled.contains("/")) {
            List<String> parts = new ArrayList<>();
            for (String p : assembled.split("/")) {
                if (p != null && !p.isBlank()) {
                    parts.add(PayListStatusBarBuckets.normalizeCurrency(p.trim()));
                }
            }
            return parts.isEmpty() ? "KRW" : String.join("/", parts);
        }
        return PayListStatusBarBuckets.normalizeCurrency(assembled);
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
                out.add(PayListStatusBarBuckets.normalizeCurrency(s));
            }
        }
        return out;
    }

    /**
     * 결제내역 그리드 Status — 노티 수신 성공이 아니라 {@code chillPaymentStatus}(PaymentStatus) 의미와 동일하게 표시.
     * 콜백 숫자: 0 성공, 1·3 실패, 2 취소, 4 오류(PG 부록). 무효·환불 등은 ICOPAY 내부 코드(21·30 등) 원문 또는 내부 {@code status}로 노출.
     */
    private static String chillStatusLabel(PgTrnsctn t) {
        /* 내부 status·칠페이문구 조합이 이미 취소·무효·실패 등이면 한 자리 콜백(0=성공) 표기보다 우선 */
        String effFirst = effectiveStatusForPayLabels(t);
        if (effFirst != null && !effFirst.isBlank() && hasDefinitiveInternalPayStatus(effFirst)
                && !"10".equals(effFirst) && !"08".equals(effFirst)) {
            return chillInternalPayStatusToKo(effFirst);
        }
        String rawStored = t.getChillPaymentStatus() != null ? t.getChillPaymentStatus().trim() : "";
        /* NOTI는 PaymentStatus=2가 취소·무효 모두에 쓰일 수 있음 — 내부·유효상태(effective)로 무효를 취소와 구분 */
        if ("2".equals(rawStored)) {
            String eff = effectiveStatusForPayLabels(t);
            if (isVoidFamilyStatus(eff)) {
                return chillInternalPayStatusToKo(eff);
            }
            if ("20".equals(eff)) {
                return "취소";
            }
        }
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

    /**
     * 노티 원문(chillPaymentStatus)은 무효인데 과거 로직이 내부 상태를 취소(20)만 남긴 행 — 표시는 무효 계열로 맞춤.
     */
    private static String effectiveStatusForPayLabels(PgTrnsctn t) {
        if (t == null) {
            return "";
        }
        String st = t.getStatus() != null ? t.getStatus() : "";
        String mapped = legacyVoidStatusFromChillWhenCancel20(st, t.getChillPaymentStatus());
        if (mapped != null) {
            return mapped;
        }
        mapped = voidDisplayWhenPaidButChillVoided(st, t.getChillPaymentStatus());
        return mapped != null ? mapped : st;
    }

    /**
     * 내부 상태가 아직 승인(10)인데 칠페이 표시 필드가 무효·Void 계열이면 그리드 구분·행 색을 무효로 맞춤.
     */
    private static String voidDisplayWhenPaidButChillVoided(String status, String chill) {
        if (!"10".equals(status) || chill == null || chill.isBlank()) {
            return null;
        }
        String ul = chill.trim().toLowerCase(Locale.ROOT);
        if (VOID_TOKEN_HINT.matcher(ul).find() && !ul.contains("cancel")) {
            return "21";
        }
        if (ul.contains("voided") || ul.contains("emailvoid") || ul.contains("email_void")
                || (ul.contains("invalid") && !ul.contains("cancel"))) {
            return "21";
        }
        if (ul.contains("무효") && !ul.contains("취소")) {
            return "21";
        }
        return null;
    }

    private static boolean isVoidFamilyStatus(String status) {
        if (status == null) {
            return false;
        }
        return switch (status.trim()) {
            case "21", "22", "40", "41", "42" -> true;
            default -> false;
        };
    }

    /**
     * @return 무효 계열 내부코드, 또는 매핑 불가 시 null
     */
    private static String legacyVoidStatusFromChillWhenCancel20(String status, String chill) {
        if (!"20".equals(status) || chill == null || chill.isBlank()) {
            return null;
        }
        String u = chill.trim();
        String ul = u.toLowerCase(Locale.ROOT);
        if (u.matches("^(21|22|40|41|42)$")) {
            return u;
        }
        if (ul.contains("emailvoid") || ul.contains("email_void")) {
            return "22";
        }
        if (ul.contains("무효") && !ul.contains("취소")) {
            return "21";
        }
        if (VOID_TOKEN_HINT.matcher(ul).find() && !ul.contains("cancel")) {
            return "21";
        }
        return null;
    }

    private static String payDivLabel(String status) {
        if (status == null) return "-";
        return switch (status) {
            case "10" -> "결제";
            case "08" -> "인증대기";
            case "20" -> "취소";
            case "21" -> "무효";
            case "22" -> "이메일무효";
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
        if (isVoidFamilyStatus(status)) return payDivLabel(status);
        return "정산대기";
    }

    private static String payStatusLabel(String status) {
        if ("10".equals(status)) return "정산대기";
        if ("08".equals(status)) return "인증대기";
        if ("20".equals(status)) return "취소";
        if (isVoidFamilyStatus(status)) return payDivLabel(status);
        return payDivLabel(status);
    }
}
