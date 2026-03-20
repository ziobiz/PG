package com.pg.api.dto;

import com.pg.entity.MerchantProfile;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.PgTrnsctn;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
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
        row.put("currency", t.getCurType() != null ? t.getCurType() : "KRW");
        row.put("routeNo", routeNoLabel(t, b));
        row.put("chillPaymentStatus", chillStatusLabel(t));
        row.put("settledYn", t.getSettledYn() != null && !t.getSettledYn().isBlank() ? t.getSettledYn().trim() : "N");

        /** 표준 그리드: 번호·업체명·업체코드 중 업체명 */
        row.put("compNm", compNm);
        /** 하위 호환·칠페이 Merchant 컬럼 */
        row.put("merchantNm", compNm);
        row.put("compDivCode9", compNm);
        row.put("compId", t.getMerchantId());
        row.put("compRegDivNm", regType(mp));
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

        long amount = t.getAmtKrw() != null ? t.getAmtKrw().longValue() : 0L;
        row.put("chillAmount", amount);
        String pgNo = t.getPayNo() != null && !t.getPayNo().isBlank()
                ? t.getPayNo()
                : (t.getTrnId() != null ? t.getTrnId() : "-");
        row.put("pgApproveAmt", amount);
        row.put("pgApproveNo", pgNo);
        row.put("payAmount", amount);
        row.put("paySeq", ourTrn);
        row.put("payAprv", payDtStr);
        row.put("payDttm", payDtStr);

        boolean isApprove = "10".equals(t.getStatus());
        BigDecimal amtBd = t.getAmtKrw() != null ? t.getAmtKrw() : BigDecimal.ZERO;
        BigDecimal totalRate = totalFeeRate(ctx);
        BigDecimal feeAmtBd = BigDecimal.ZERO;
        BigDecimal feeVatBd = BigDecimal.ZERO;
        BigDecimal holdAmtBd = BigDecimal.ZERO;
        if (isApprove) {
            feeAmtBd = amtBd.multiply(totalRate).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            feeVatBd = feeAmtBd.multiply(BigDecimal.valueOf(0.1)).setScale(0, RoundingMode.HALF_UP);
            BigDecimal rollingPct = resolveRollingPct(ctx);
            if (rollingPct.compareTo(BigDecimal.ZERO) > 0) {
                holdAmtBd = amtBd.multiply(rollingPct).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            }
        }
        long feeAmt = feeAmtBd.longValue();
        long feeVat = feeVatBd.longValue();
        long holdAmt = holdAmtBd.longValue();
        long settleAmt = isApprove ? amtBd.subtract(feeAmtBd).subtract(feeVatBd).subtract(holdAmtBd).longValue() : 0L;

        row.put("icopayAmt", longOrEmpty(t.getIcopayAmt()));
        long chillFeeStored = t.getChillFeeAmt() != null ? t.getChillFeeAmt().longValue() : -1L;
        row.put("chillFeeAmt", chillFeeStored >= 0 ? chillFeeStored : feeAmt);
        long totalStored = t.getTotalAmt() != null ? t.getTotalAmt().longValue() : -1L;
        if (totalStored >= 0) {
            row.put("totalAmt", totalStored);
        } else {
            long cf = chillFeeStored >= 0 ? chillFeeStored : feeAmt;
            row.put("totalAmt", amount + cf);
        }

        row.put("feeCnt", isApprove ? 1 : 0);
        row.put("feeRate", totalRate);
        row.put("feeAmt", feeAmt);
        row.put("feeVat", feeVat);
        row.put("holdRate", resolveRollingPct(ctx));
        row.put("holdAmt", holdAmt);
        row.put("holdDttm", holdAmt > 0 ? payDtStr : "");

        row.put("calcCycle", ctx != null && ctx.getSettlement() != null && ctx.getSettlement().getCalcCycle() != null
                ? ctx.getSettlement().getCalcCycle() : "-");
        row.put("settleAmt", settleAmt);
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
        return row;
    }

    private static BigDecimal totalFeeRate(PayListRowContext ctx) {
        if (ctx == null || ctx.getDistFee() == null) {
            if (ctx != null && ctx.getPolicy() != null && ctx.getPolicy().getPayRate() != null) {
                return ctx.getPolicy().getPayRate();
            }
            return BigDecimal.ZERO;
        }
        var d = ctx.getDistFee();
        return nz(d.getHqRate()).add(nz(d.getRegionalRate())).add(nz(d.getMasterRate()))
                .add(nz(d.getBranchRate())).add(nz(d.getAgencyRate()));
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

    private static Object longOrEmpty(BigDecimal v) {
        return v != null ? v.longValue() : "";
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

    /** ChillPay Status 컬럼: 원문 우선, 없으면 내부 코드 매핑 */
    private static String chillStatusLabel(PgTrnsctn t) {
        if (t.getChillPaymentStatus() != null && !t.getChillPaymentStatus().isBlank()) {
            return t.getChillPaymentStatus().trim();
        }
        return switch (t.getStatus() != null ? t.getStatus() : "") {
            case "10" -> "Paid";
            case "20" -> "Cancelled";
            case "30", "31" -> "Refunded";
            case "F0", "99" -> "Failed";
            default -> t.getStatus() != null ? t.getStatus() : "-";
        };
    }

    private static String regType(MerchantProfile mp) {
        if (mp == null || mp.getRegNo() == null || !mp.getRegNo().contains("|")) return "-";
        String t = mp.getRegNo().split("\\|", 2)[0];
        return "PERSONAL".equalsIgnoreCase(t) ? "개인" : "법인";
    }

    private static String regNo(MerchantProfile mp) {
        if (mp == null || mp.getRegNo() == null) return "-";
        return mp.getRegNo().contains("|") ? mp.getRegNo().split("\\|", 2)[1] : mp.getRegNo();
    }

    private static String payDivLabel(String status) {
        if (status == null) return "-";
        return switch (status) {
            case "10" -> "결제";
            case "20" -> "취소";
            case "30", "31" -> "환불";
            case "F0", "99" -> "실패";
            default -> status;
        };
    }

    private static String payProcLabel(String status) {
        if ("10".equals(status)) return "정산대기";
        if ("20".equals(status)) return "결제취소";
        return "정산대기";
    }

    private static String payStatusLabel(String status) {
        if ("10".equals(status)) return "정산대기";
        if ("20".equals(status)) return "취소";
        return payDivLabel(status);
    }
}
