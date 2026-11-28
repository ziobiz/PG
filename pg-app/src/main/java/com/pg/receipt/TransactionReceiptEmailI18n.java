package com.pg.receipt;

import com.pg.splitpay.SplitPayMailLocaleUtil;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** 고객 거래명세서 이메일 — KOR/ENG/JPN/CHN/THA. 본문 라벨은 영어ㅣ선택언어. */
public final class TransactionReceiptEmailI18n {

    /** 영어와 선택 언어 구분 (전각 세로선) */
    public static final String LANG_SEP = "ㅣ";

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
        return "ICOPAY | " + bilingual(lang, "Payment failed", "결제실패", "決済失敗", "支付失败", "ชำระไม่สำเร็จ");
    }

    public static String subjectCancelled(String lang) {
        return "ICOPAY | " + bilingual(lang, "Payment cancelled", "결제취소", "決済取消", "支付取消", "ยกเลิกการชำระ");
    }

    public static String subjectPaid(String lang) {
        return "ICOPAY | " + bilingual(lang, "Payment successful", "결제성공", "決済成功", "支付成功", "ชำระสำเร็จ");
    }

    public static String subjectRefunded(String lang) {
        return "ICOPAY | " + bilingual(lang,
                "Credit Card Refund Receipt", "신용카드 환불 거래명세서", "クレジットカード返金明細",
                "信用卡退款收据", "ใบเสร็จคืนเงินบัตรเครดิต");
    }

    public static String subjectVoided(String lang) {
        return "ICOPAY | " + bilingual(lang,
                "Credit Card Void Receipt", "신용카드 무효 거래명세서", "クレジットカード無効明細",
                "信用卡作废收据", "ใบเสร็จโมฆะบัตรเครดิต");
    }

    public static String paymentSuccessful(String lang) {
        return bilingual(lang, "Payment successful", "결제성공", "決済成功", "支付成功", "ชำระสำเร็จ");
    }

    public static String paymentFailed(String lang) {
        return bilingual(lang, "Payment failed", "결제실패", "決済失敗", "支付失败", "ชำระไม่สำเร็จ");
    }

    public static String paymentCancelled(String lang) {
        return bilingual(lang, "Payment cancelled", "결제취소", "決済取消", "支付取消", "ยกเลิกการชำระ");
    }

    public static String paymentRefunded(String lang) {
        return bilingual(lang, "REFUND COMPLETED", "환불 완료", "返金完了", "退款完成", "คืนเงินสำเร็จ");
    }

    public static String paymentVoided(String lang) {
        return bilingual(lang, "VOID COMPLETED", "무효 완료", "無効完了", "作废完成", "โมฆะสำเร็จ");
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
        return bilingual(lang, "PAYMENT DETAILS", "결제 상세", "お支払い詳細", "付款详情", "รายละเอียดการชำระ");
    }

    public static String serviceProvider(String lang) {
        return bilingual(lang, "SERVICE PROVIDER", "서비스 제공자", "サービスプロバイダー", "服务提供方", "ผู้ให้บริการ");
    }

    public static String paymentDetail(String lang) {
        return bilingual(lang, "PAYMENT DETAIL", "결제 상세", "お支払い詳細", "付款详情", "รายละเอียดการชำระ");
    }

    /** 표시통화와 다른 실청구 통화 라벨. */
    public static String chargeCurrencyLabel(String lang) {
        return bilingual(lang, "Charge currency", "결제통화", "請求通貨", "支付币种", "สกุลเงินที่เรียกเก็บ");
    }

    public static Map<String, String> fieldLabels(String lang) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("acquirer", bilingual(lang, "Acquirer", "카드매입사", "アクワイアラ", "收单行", "ผู้รับชำระ"));
        m.put("paymentSwitcher", bilingual(lang, "Payment Switcher", "결제중계사", "決済スイッチャー", "支付交换", "ตัวกลางชำระ"));
        m.put("paymentProvider", bilingual(lang, "Payment Provider", "결제대행사", "決済代行", "支付服务商", "ผู้ให้บริการชำระเงิน"));
        m.put("merchant", bilingual(lang, "Merchant", "가맹점", "加盟店", "商户", "ร้านค้า"));
        m.put("transactionId", bilingual(lang, "Transaction ID", "거래번호", "取引ID", "交易编号", "รหัสธุรกรรม"));
        m.put("email", bilingual(lang, "Email", "이메일", "メール", "邮箱", "อีเมล"));
        m.put("contactNo", bilingual(lang, "Contact No.", "연락처", "連絡先", "联系电话", "เบอร์ติดต่อ"));
        m.put("serviceItem", bilingual(lang, "Service item", "상품/서비스", "商品・サービス", "商品/服务", "รายการ"));
        m.put("orderNumber", bilingual(lang, "Order Number", "주문번호", "注文番号", "订单号", "หมายเลขคำสั่งซื้อ"));
        m.put("cardholder", bilingual(lang, "Cardholder", "카드소유자", "カード名義", "持卡人", "ผู้ถือบัตร"));
        m.put("authorizedDateTime", bilingual(lang, "Authorized Date/Time", "승인일시", "承認日時", "授权日期/时间", "วันเวลาอนุมัติ"));
        m.put("refundedDateTime", bilingual(lang, "Refunded Date/Time", "환불일시", "返金日時", "退款日期/时间", "วันเวลาคืนเงิน"));
        m.put("failedDateTime", bilingual(lang, "Failed Date/Time", "실패일시", "失敗日時", "失败日期/时间", "วันเวลาไม่สำเร็จ"));
        m.put("cancelledDateTime", bilingual(lang, "Cancelled Date/Time", "취소일시", "取消日時", "取消日期/时间", "วันเวลายกเลิก"));
        m.put("voidedDateTime", bilingual(lang, "Voided Date/Time", "무효일시", "無効日時", "作废日期/时间", "วันเวลาโมฆะ"));
        m.put("approvalCode", bilingual(lang, "Approval code", "승인번호", "承認番号", "授权码", "รหัสอนุมัติ"));
        m.put("paymentMethod", bilingual(lang, "Payment Method", "결제수단", "お支払い方法", "支付方式", "วิธีชำระ"));
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

    /**
     * ENG만이면 영어 단독. 그 외는 {@code Englishㅣ선택언어}.
     */
    static String bilingual(String lang, String eng, String kor, String jpn, String chn, String tha) {
        String l = SplitPayMailLocaleUtil.normalize(lang);
        if ("ENG".equals(l)) {
            return eng;
        }
        String local = switch (l) {
            case "JPN" -> jpn;
            case "CHN" -> chn;
            case "THA" -> tha;
            default -> kor;
        };
        if (local == null || local.isBlank() || local.equals(eng)) {
            return eng;
        }
        return eng + LANG_SEP + local;
    }
}
