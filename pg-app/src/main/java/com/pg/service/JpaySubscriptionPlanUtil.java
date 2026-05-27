package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** J-Pay {@code subscription_plan} JSON 검증·병합. */
public final class JpaySubscriptionPlanUtil {

    private static final ObjectMapper OM = new ObjectMapper();

    private JpaySubscriptionPlanUtil() {
    }

    public static Map<String, Object> fail(String message, String code) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", false);
        out.put("message", message);
        out.put("errorCode", code);
        return out;
    }

    /**
     * @param body prepare/subscribe 요청 본문
     * @param hqDefaults 본사 jpay_subscription_config_json (nullable)
     * @return success=true + planJson / success=false + message
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> buildPlanJson(Map<String, Object> body, String hqDefaults) {
        Object rawPlan = body != null ? body.get("subscriptionPlan") : null;
        if (rawPlan == null && body != null) {
            rawPlan = body.get("subscription_plan");
        }
        if (rawPlan == null) {
            return fail("subscriptionPlan이 필요합니다.", "SUBSCRIPTION_PLAN_REQUIRED");
        }
        ObjectNode plan;
        try {
            if (rawPlan instanceof Map) {
                plan = OM.valueToTree(rawPlan);
            } else {
                plan = (ObjectNode) OM.readTree(rawPlan.toString());
            }
        } catch (Exception e) {
            return fail("subscriptionPlan JSON 형식이 올바르지 않습니다.", "INVALID_SUBSCRIPTION_PLAN");
        }
        mergeHqDefaults(plan, hqDefaults);
        String id = text(plan, "id");
        if (id.isBlank()) {
            plan.put("id", UUID.randomUUID().toString());
        }
        String name = text(plan, "name");
        if (name.isBlank()) {
            return fail("subscriptionPlan.name이 필요합니다.", "INVALID_SUBSCRIPTION_PLAN");
        }
        String planType = text(plan, "plan_type");
        if (planType.isBlank()) {
            planType = text(plan, "planType");
        }
        if (planType.isBlank()) {
            return fail("subscriptionPlan.plan_type이 필요합니다.", "INVALID_SUBSCRIPTION_PLAN");
        }
        plan.put("plan_type", normalizePlanType(planType));
        String desc = text(plan, "description");
        if (desc.isBlank()) {
            plan.put("description", name);
        }
        if (text(plan, "attempts").isBlank()) {
            plan.put("attempts", "3");
        }
        if (!plan.has("interval_time") || plan.get("interval_time").isNull()) {
            plan.put("interval_time", 3600);
        }
        if (!plan.has("total_count") || plan.get("total_count").isNull()) {
            plan.put("total_count", 12);
        }
        String firstAmt = text(plan, "first_period_amount");
        if (!firstAmt.isBlank()) {
            try {
                new BigDecimal(firstAmt.replace(",", ""));
            } catch (Exception e) {
                return fail("first_period_amount 형식이 올바르지 않습니다.", "INVALID_SUBSCRIPTION_PLAN");
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        try {
            out.put("planJson", OM.writeValueAsString(plan));
            out.put("planName", name);
            out.put("planType", plan.get("plan_type").asText());
        } catch (Exception e) {
            return fail("subscriptionPlan 직렬화 실패", "INVALID_SUBSCRIPTION_PLAN");
        }
        return out;
    }

    private static void mergeHqDefaults(ObjectNode plan, String hqDefaults) {
        if (hqDefaults == null || hqDefaults.isBlank()) {
            return;
        }
        try {
            JsonNode def = OM.readTree(hqDefaults);
            if (!plan.has("attempts") && def.has("attempts")) {
                plan.set("attempts", def.get("attempts"));
            }
            if (!plan.has("interval_time") && def.has("interval_time")) {
                plan.set("interval_time", def.get("interval_time"));
            }
            if (!plan.has("total_count") && def.has("total_count")) {
                plan.set("total_count", def.get("total_count"));
            }
        } catch (Exception ignored) {
            /* keep plan as-is */
        }
    }

    private static String normalizePlanType(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private static String text(JsonNode n, String key) {
        if (n == null || key == null) {
            return "";
        }
        JsonNode v = n.get(key);
        return v != null && !v.isNull() ? v.asText("").trim() : "";
    }
}
