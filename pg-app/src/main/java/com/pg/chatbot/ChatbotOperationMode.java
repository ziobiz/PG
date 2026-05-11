package com.pg.chatbot;

import java.util.Locale;
import java.util.Optional;

/**
 * 챗봇관리 기본설정 — 가맹점 운영방식(판매/예약×선불·후불·혼합 등).
 * 공개 챗봇 LLM 시스템 프롬프트에 반영됩니다.
 */
public enum ChatbotOperationMode {

    SALE_PREPAID(
            "SALE_PREPAID",
            "상품판매 · 선불",
            """
                    고객 응대는 「일반 상품 판매」에 맞추고, 결제·주문 확정은 선불(선결제) 원칙으로 안내합니다.
                    카탈로그 listingType이 RESERVATION이어도 이 가맹점 설정상 선결제 후 이용·발송·예약 확정 흐름으로 설명합니다.
                    후불·방문 후 전액 결제만 가능하다는 식의 안내는 하지 마세요."""),

    SALE_POSTPAID(
            "SALE_POSTPAID",
            "상품판매 · 후불",
            """
                    「상품 판매」이며 후불·청구 후 제공·배송 등 후불 원칙으로 안내합니다.
                    URL 결제 링크는 예약금·선입금·부분 선결제가 실제로 있는 경우에만 제안하고, 전액 선결제가 필수인 것처럼 말하지 마세요."""),

    RESERVATION_PREPAID(
            "RESERVATION_PREPAID",
            "예약방식 · 선불",
            """
                    「예약」 중심 운영으로 안내합니다. 예약 확정·슬롯 확보 전 선결제를 원칙으로 설명합니다.
                    후불 예약만 가능하다고 말하지 마세요."""),

    RESERVATION_POSTPAID(
            "RESERVATION_POSTPAID",
            "예약방식 · 후불",
            """
                    「예약」 중심이며 방문·이용 후 또는 현장에서 결제하는 후불 흐름으로 안내합니다.
                    선결제 URL을 적극 권하지 말고, 예약금 등 선결제가 있는 경우에만 예외적으로 안내합니다."""),

    HYBRID_RESERVATION_PREPAID(
            "HYBRID_RESERVATION_PREPAID",
            "하이브리드 (판매+예약 · 예약은 선불 고정)",
            """
                    상품판매(SALE)와 예약(RESERVATION)을 함께 다룹니다.
                    listingType이 RESERVATION인 항목은 반드시 선불(결제 후 예약 확정)로만 안내합니다.
                    SALE 항목은 일반 선결제형 구매로 안내합니다."""),

    FACE_TO_FACE_POSTPAID(
            "FACE_TO_FACE_POSTPAID",
            "대면거래 (판매+예약 · 후불)",
            """
                    대면(방문) 거래 중심입니다. SALE·RESERVATION 모두 현장 방문 후 결제·후불 원칙으로 안내합니다.
                    온라인 URL 결제는 가맹이 별도로 안내하는 경우에만 예외적으로 언급합니다.""");

    private final String code;
    private final String labelKo;
    private final String llmDirectiveKo;

    ChatbotOperationMode(String code, String labelKo, String llmDirectiveKo) {
        this.code = code;
        this.labelKo = labelKo;
        this.llmDirectiveKo = llmDirectiveKo.stripIndent().trim();
    }

    public String getCode() {
        return code;
    }

    public String getLabelKo() {
        return labelKo;
    }

    /** 공개 챗봇 system 블록에 넣는 행동 규칙(한국어 문단). */
    public String getLlmDirectiveKo() {
        return llmDirectiveKo;
    }

    /** DB 미설정·알 수 없는 값 → 기본(선불 상품판매). */
    public static ChatbotOperationMode resolveStored(String dbCode) {
        return fromCode(dbCode).orElse(SALE_PREPAID);
    }

    public static Optional<ChatbotOperationMode> fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        for (ChatbotOperationMode m : values()) {
            if (m.code.equals(u)) {
                return Optional.of(m);
            }
        }
        return Optional.empty();
    }

    public static ChatbotOperationMode fromCodeStrict(String raw) {
        return fromCode(raw).orElseThrow(() ->
                new IllegalArgumentException(
                        "지원하지 않는 챗봇 운영방식 코드입니다. 허용: SALE_PREPAID, SALE_POSTPAID, "
                                + "RESERVATION_PREPAID, RESERVATION_POSTPAID, HYBRID_RESERVATION_PREPAID, "
                                + "FACE_TO_FACE_POSTPAID"));
    }
}
