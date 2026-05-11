package com.pg.chatbot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.MerchantProfile;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 가맹 {@code chatbot_order_sheet_ui_json} 과 업체성격을 병합해 공개 챗봇 주문 시트에 내려줄 규칙을 만듭니다.
 * <p>스키마(최상위): {@code { "fields": { "&lt;fieldKey&gt;": { ... } } }}
 * 필드 키: ordererName, ordererEmail, ordererPhone, ordererAddr, orderMemo,
 * reservationLocal, reservationCheckout, guestCount, serviceMinutes.
 * 공통 속성: {@code hidden}(boolean), {@code labelKo}, {@code placeholderKo},
 * {@code prefillWhenHidden}(주소 숨김 시 서버 검증용, 4자 이상),
 * {@code showWhenReservation}(serviceMinutes 전용: 예약 상품일 때 이용시간(분) 필드 표시).</p>
 */
public final class ChatbotOrderSheetUiResolver {

    private static final ObjectMapper OM = new ObjectMapper();

    private ChatbotOrderSheetUiResolver() {
    }

    public static Map<String, Object> resolvePublicUi(MerchantProfile mp) {
        ChatbotMerchantVertical v = ChatbotMerchantVertical.resolveStored(
                mp != null ? mp.getChatbotMerchantVertical() : null);
        Map<String, Map<String, Object>> fields = buildBaseFields(v);
        mergeMerchantJson(fields, mp != null ? mp.getChatbotOrderSheetUiJson() : null);
        sanitizeAddrHidden(fields);
        disallowHiddenForRequiredContact(fields);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("version", 1);
        out.put("fields", fields);
        return out;
    }

    private static Map<String, Map<String, Object>> buildBaseFields(ChatbotMerchantVertical v) {
        Map<String, Map<String, Object>> m = new LinkedHashMap<>();
        m.put("ordererName", field(false));
        m.put("ordererEmail", field(false));
        m.put("ordererPhone", field(false));
        m.put("ordererAddr", field(false));
        m.put("orderMemo", field(false));
        m.put("reservationLocal", field(false));
        m.put("reservationCheckout", field(false));
        m.put("guestCount", field(false));
        Map<String, Object> svc = field(false);
        svc.put("showWhenReservation", defaultServiceMinutesForReservation(v));
        m.put("serviceMinutes", svc);
        return m;
    }

    private static Map<String, Object> field(boolean hidden) {
        Map<String, Object> x = new LinkedHashMap<>();
        x.put("hidden", hidden);
        return x;
    }

    /** 예약형 상품일 때 이용 시간(분) 블록을 기본으로 켤 업체성격(클라이언트 기존 로직과 동일 범위). */
    public static boolean defaultServiceMinutesForReservation(ChatbotMerchantVertical v) {
        if (v == null) {
            return false;
        }
        return switch (v) {
            case MASSAGE_GENERAL, COSMETIC, CLUB_MASSAGE, CLUB_ENTERTAINMENT, VIP_CLUB, RESTAURANT, SERVICE_TRADE -> true;
            default -> false;
        };
    }

    private static void mergeMerchantJson(Map<String, Map<String, Object>> base, String jsonRaw) {
        if (jsonRaw == null || jsonRaw.isBlank()) {
            return;
        }
        try {
            JsonNode root = OM.readTree(jsonRaw);
            JsonNode fo = root.get("fields");
            if (fo == null || !fo.isObject()) {
                return;
            }
            fo.fields().forEachRemaining(e -> {
                String k = e.getKey();
                Map<String, Object> tgt = base.get(k);
                if (tgt == null) {
                    return;
                }
                JsonNode patch = e.getValue();
                if (!patch.isObject()) {
                    return;
                }
                patch.fields().forEachRemaining(p -> tgt.put(p.getKey(), jsonScalar(p.getValue())));
            });
        } catch (Exception ignored) {
            /* 잘못된 JSON은 무시하고 기본만 사용 */
        }
    }

    private static Object jsonScalar(JsonNode n) {
        if (n == null || n.isNull()) {
            return null;
        }
        if (n.isBoolean()) {
            return n.booleanValue();
        }
        if (n.isInt() || n.isLong()) {
            return n.longValue();
        }
        if (n.isNumber()) {
            return n.doubleValue();
        }
        return n.asText();
    }

    /** 주문 서버에서 필수인 연락처·이메일은 숨김 불가 */
    private static void disallowHiddenForRequiredContact(Map<String, Map<String, Object>> fields) {
        for (String k : new String[] {"ordererEmail", "ordererPhone"}) {
            Map<String, Object> x = fields.get(k);
            if (x != null) {
                x.put("hidden", false);
                x.remove("prefillWhenHidden");
            }
        }
    }

    private static void sanitizeAddrHidden(Map<String, Map<String, Object>> fields) {
        Map<String, Object> addr = fields.get("ordererAddr");
        if (addr == null) {
            return;
        }
        if (Boolean.TRUE.equals(addr.get("hidden"))) {
            String pf = addr.get("prefillWhenHidden") != null ? String.valueOf(addr.get("prefillWhenHidden")).trim() : "";
            if (pf.length() < 4) {
                addr.put("hidden", false);
                addr.remove("prefillWhenHidden");
            }
        }
    }

    /** 저장 전 검증 — 실패 시 IllegalArgumentException */
    public static void validateMerchantJsonOrThrow(String jsonRaw) {
        if (jsonRaw == null || jsonRaw.isBlank()) {
            return;
        }
        if (jsonRaw.length() > 12000) {
            throw new IllegalArgumentException("주문 시트 UI JSON은 12000자 이내로 입력하세요.");
        }
        try {
            OM.readTree(jsonRaw);
        } catch (Exception e) {
            throw new IllegalArgumentException("주문 시트 UI JSON 형식이 올바르지 않습니다.");
        }
        Map<String, Map<String, Object>> probe = new LinkedHashMap<>();
        probe.put("ordererAddr", field(false));
        mergeMerchantJson(probe, jsonRaw);
        sanitizeAddrHidden(probe);
        Map<String, Object> addr = probe.get("ordererAddr");
        if (addr != null && Boolean.TRUE.equals(addr.get("hidden"))) {
            String pf = addr.get("prefillWhenHidden") != null ? String.valueOf(addr.get("prefillWhenHidden")).trim() : "";
            if (pf.length() < 4) {
                throw new IllegalArgumentException(
                        "주소(ordererAddr)를 숨길 때는 prefillWhenHidden 을 4자 이상으로 지정하세요.");
            }
        }
    }

    public static String clampStoredJson(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return null;
        }
        if (t.length() > 12000) {
            return t.substring(0, 12000);
        }
        return t;
    }
}
