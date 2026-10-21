package com.pg.receipt;

import com.pg.splitpay.SplitPayMailLocaleUtil;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** 고객 거래명세서 이메일 — KOR/ENG/JPN/CHN/THA */
public final class TransactionReceiptEmailI18n {

    private TransactionReceiptEmailI18n() {
    }

    public static String subject(String lang) {
        return subject(lang, TransactionReceiptOutcome.PAID);
    }

    public static String subject(String lang, TransactionReceiptOutcome outcome) {
        TransactionReceiptOutcome o = outcome != null ? outcome : TransactionReceiptOutcome.PAID;
        return switch (o) {
            case FAILED -> subjectFailed(lang);
            case CANCELLED -> subjectCancelled(lang);
            case REFUNDED -> subjectRefunded(lang);
            case VOIDED -> subjectVoided(lang);
            default -> subjectPaid(lang);
        };
    }

    public static String subjectFailed(String lang) {
        String l = SplitPayMailLocaleUtil.normalize(lang);
        return switch (l) {
            case "ENG" -> "ICOPAY | Payment failed";
            case "JPN" -> "ICOPAY | 決済失敗";
            case "CHN" -> "ICOPAY | 支付失败";
            case "THA" -> "ICOPAY | ชำระไม่สำเร็จ";
            default -> "ICOPAY | 결제실패";
        };
    }

    public static String subjectCancelled(String lang) {
        String l = SplitPayMailLocaleUtil.normalize(lang);
        return switch (l) {
            case "ENG" -> "ICOPAY | Payment cancelled";
            case "JPN" -> "ICOPAY | 決済取消";
            case "CHN" -> "ICOPAY | 支付取消";
            case "THA" -> "ICOPAY | ยกเลิกการชำระ";
            default -> "ICOPAY | 결제취소";
        };
    }

    public static String subjectPaid(String lang) {
        String l = SplitPayMailLocaleUtil.normalize(lang);
        return switch (l) {
            case "ENG" -> "ICOPAY | Payment successful";
            case "JPN" -> "ICOPAY | 決済成功";
            case "CHN" -> "ICOPAY | 支付成功";
            case "THA" -> "ICOPAY | ชำระสำเร็จ";
            default -> "ICOPAY | 결제성공";
        };
    }

    public static String subjectRefunded(String lang) {
        String l = SplitPayMailLocaleUtil.normalize(lang);
        return switch (l) {
            case "ENG" -> "ICOPAY | Credit Card Refund Receipt";
            case "JPN" -> "ICOPAY | クレジットカード返金明細";
            case "CHN" -> "ICOPAY | 信用卡退款收据";
            case "THA" -> "ICOPAY | ใบเสร็จคืนเงินบัตรเครดิต";
            default -> "ICOPAY | 신용카드 환불 거래명세서";
        };
    }

    public static String subjectVoided(String lang) {
        String l = SplitPayMailLocaleUtil.normalize(lang);
        return switch (l) {
            case "ENG" -> "ICOPAY | Credit Card Void Receipt";
            case "JPN" -> "ICOPAY | クレジットカード無効明細";
            case "CHN" -> "ICOPAY | 信用卡作废收据";
            case "THA" -> "ICOPAY | ใบเสร็จโมฆะบัตรเครดิต";
            default -> "ICOPAY | 신용카드 무효 거래명세서";
        };
    }

    public static String paymentSuccessful(String lang) {
        return label(lang, "Payment successful", "결제성공", "決済成功", "支付成功", "ชำระสำเร็จ");
    }

    public static String paymentFailed(String lang) {
        return label(lang, "Payment failed", "결제실패", "決済失敗", "支付失败", "ชำระไม่สำเร็จ");
    }

    public static String paymentCancelled(String lang) {
        return label(lang, "Payment cancelled", "결제취소", "決済取消", "支付取消", "ยกเลิกการชำระ");
    }

    public static String paymentRefunded(String lang) {
        return label(lang, "REFUND COMPLETED", "환불 완료", "返金完了", "退款完成", "คืนเงินสำเร็จ");
    }

    public static String paymentVoided(String lang) {
        return label(lang, "VOID COMPLETED", "무효 완료", "無効完了", "作废完成", "โมฆะสำเร็จ");
    }

    public static String outcomeBadge(String lang, TransactionReceiptOutcome outcome) {
        TransactionReceiptOutcome o = outcome != null ? outcome : TransactionReceiptOutcome.PAID;
        return switch (o) {
            case FAILED -> paymentFailed(lang);
            case CANCELLED -> paymentCancelled(lang);
            case REFUNDED -> paymentRefunded(lang);
            case VOIDED -> paymentVoided(lang);
            default -> paymentSuccessful(lang);
        };
    }

    /** 결제내역 상태 pill 바탕. 성공 연두, 실패 빨강, 취소 회색. */
    public static String outcomeBadgeBg(TransactionReceiptOutcome outcome) {
        TransactionReceiptOutcome o = outcome != null ? outcome : TransactionReceiptOutcome.PAID;
        return switch (o) {
            case FAILED -> "#fee2e2";
            case CANCELLED -> "#d4dce6";
            case REFUNDED -> "#dbeafe";
            case VOIDED -> "#ffedd5";
            default -> "#d1fae5";
        };
    }

    public static String outcomeBadgeFg(TransactionReceiptOutcome outcome) {
        TransactionReceiptOutcome o = outcome != null ? outcome : TransactionReceiptOutcome.PAID;
        return switch (o) {
            case FAILED -> "#b91c1c";
            case CANCELLED -> "#3d4d5c";
            case REFUNDED -> "#1e40af";
            case VOIDED -> "#c2410c";
            default -> "#047857";
        };
    }

    public static String outcomeBadgeBorder(TransactionReceiptOutcome outcome) {
        TransactionReceiptOutcome o = outcome != null ? outcome : TransactionReceiptOutcome.PAID;
        return switch (o) {
            case FAILED -> "#fecaca";
            case CANCELLED -> "#9aa8b8";
            case REFUNDED -> "#bfdbfe";
            case VOIDED -> "#fed7aa";
            default -> "#a7f3d0";
        };
    }

    public static String outcomeBadgeColor(TransactionReceiptOutcome outcome) {
        return outcomeBadgeFg(outcome);
    }

    public static String paymentDetails(String lang) {
        return label(lang, "PAYMENT DETAILS", "결제 상세", "お支払い詳細", "付款详情", "รายละเอียดการชำระ");
    }

    public static String serviceProvider(String lang) {
        return label(lang, "SERVICE PROVIDER", "서비스 제공자", "サービスプロバイダー", "服务提供方", "ผู้ให้บริการ");
    }

    public static String paymentDetail(String lang) {
        return label(lang, "PAYMENT DETAIL", "결제 상세", "お支払い詳細", "付款详情", "รายละเอียดการชำระ");
    }

    /** 표시통화와 다른 실청구 통화 라벨. */
    public static String chargeCurrencyLabel(String lang) {
        return label(lang, "Charge currency", "결제통화", "請求通貨", "支付币种", "สกุลเงินที่เรียกเก็บ");
    }

    public static Map<String, String> fieldLabels(String lang) {
        String l = SplitPayMailLocaleUtil.normalize(lang);
        Map<String, String> m = new LinkedHashMap<>();
        m.put("acquirer", field(l, "Acquirer", "카드매입사", "アクワイアラ", "收单行", "ผู้รับชำระ"));
        m.put("paymentSwitcher", field(l, "Payment Switcher", "결제중계사", "決済スイッチャー", "支付交换", "ตัวกลางชำระ"));
        m.put("paymentProvider", field(l, "Payment Provider", "결제대행사", "決済代行", "支付服务商", "ผู้ให้บริการชำระเงิน"));
        m.put("merchant", field(l, "Merchant", "가맹점", "加盟店", "商户", "ร้านค้า"));
        m.put("transactionId", field(l, "Transaction ID", "거래번호", "取引ID", "交易编号", "รหัสธุรกรรม"));
        m.put("email", field(l, "Email", "이메일", "メール", "邮箱", "อีเมล"));
        m.put("contactNo", field(l, "Contact No.", "연락처", "連絡先", "联系电话", "เบอร์ติดต่อ"));
        m.put("serviceItem", field(l, "Service item", "상품/서비스", "商品・サービス", "商品/服务", "รายการ"));
        m.put("orderNumber", field(l, "Order Number", "주문번호", "注文番号", "订单号", "หมายเลขคำสั่งซื้อ"));
        m.put("cardholder", field(l, "Cardholder", "카드소유자", "カード名義", "持卡人", "ผู้ถือบัตร"));
        m.put("authorizedDateTime", field(l, "Authorized Date/Time", "승인일시", "承認日時", "授权日期/时间", "วันเวลาอนุมัติ"));
        m.put("refundedDateTime", field(l, "Refunded Date/Time", "환불일시", "返金日時", "退款日期/时间", "วันเวลาคืนเงิน"));
        m.put("failedDateTime", field(l, "Failed Date/Time", "실패일시", "失敗日時", "失败日期/时间", "วันเวลาไม่สำเร็จ"));
        m.put("cancelledDateTime", field(l, "Cancelled Date/Time", "취소일시", "取消日時", "取消日期/时间", "วันเวลายกเลิก"));
        m.put("voidedDateTime", field(l, "Voided Date/Time", "무효일시", "無効日時", "作废日期/时间", "วันเวลาโมฆะ"));
        m.put("approvalCode", field(l, "Approval code", "승인번호", "承認番号", "授权码", "รหัสอนุมัติ"));
        m.put("paymentMethod", field(l, "Payment Method", "결제수단", "お支払い方法", "支付方式", "วิธีชำระ"));
        return m;
    }

    public static String resolveLangFromCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return SplitPayMailLocaleUtil.ENG;
        }
        return switch (currency.trim().toUpperCase(Locale.ROOT)) {
            case "JPY" -> SplitPayMailLocaleUtil.JPN;
            case "KRW" -> SplitPayMailLocaleUtil.KOR;
            case "THB" -> SplitPayMailLocaleUtil.THA;
            case "CNY", "HKD", "TWD" -> SplitPayMailLocaleUtil.CHN;
            default -> SplitPayMailLocaleUtil.ENG;
        };
    }

    private static String field(String l, String eng, String kor, String jpn, String chn, String tha) {
        return switch (l) {
            case "ENG" -> eng;
            case "JPN" -> jpn;
            case "CHN" -> chn;
            case "THA" -> tha;
            default -> kor;
        };
    }

    private static String label(String lang, String eng, String kor, String jpn, String chn, String tha) {
        return field(SplitPayMailLocaleUtil.normalize(lang), eng, kor, jpn, chn, tha);
    }
}
