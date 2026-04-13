package com.pg.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 본사설정 &gt; 전산설정관리 — 데이터 보관 기간 리스트(유형·기본일·자동삭제 연동 여부).
 */
public final class DataRetentionCatalog {

    private static final ObjectMapper OM = new ObjectMapper();
    public static final int MIN_DAYS = 1;
    public static final int MAX_DAYS = 36500;

    public record Entry(String id, String label, String description, int defaultDays, boolean schedulerPurge) {
    }

    /**
     * 저장 JSON 한 항목의 해석 결과. {@code autoDeleteEnabled} 는 {@link Entry#schedulerPurge()} 가 true 인 유형에서만 의미가 있습니다.
     */
    public record RetentionPolicy(int retainDays, boolean autoDeleteEnabled, int purgeDays) {
    }

    /**
     * {@code schedulerPurge}: 매일 스케줄에서 해당 일수 초과분 삭제를 시도하는 항목만 true.
     * 그 외는 정책 값만 저장(추후 배치·수동 정리·외부 시스템 연동용).
     */
    public static final List<Entry> ENTRIES = List.of(
            new Entry(
                    "PG_NOTIFY_INBOUND",
                    "PG 노티 수신 원문(DB)",
                    "tb_pg_notify_inbound. PG·NOTI 등에서 수신한 노티 본문·메타. 자동삭제: created_at 기준.",
                    90,
                    true),
            new Entry(
                    "PG_TRNSCTN",
                    "결제 거래 내역(DB)",
                    "pg_trnsctn. 승인·취소·무효·환불 등 거래 마스터. 자동삭제 미연동(법적·정산 보존 — 값은 정책·감사용).",
                    2555,
                    false),
            new Entry(
                    "MERCHANT_REGISTRATION",
                    "업체정보(등록)",
                    "tb_merchant_profile 등 가맹·법인 최초 등록·신청 단계 마스터. 자동삭제 미연동(법적·계약 보존 — 보관 목표 일수만 정책 저장).",
                    2555,
                    false),
            new Entry(
                    "MERCHANT_MANAGEMENT",
                    "업체관리",
                    "tb_org_unit, tb_merchant_pg_binding, tb_merchant_notify_url, 권한·VIEW 설정 등 가맹 운영 데이터. 자동삭제 미연동.",
                    2555,
                    false),
            new Entry(
                    "SETTLEMENT_RUN",
                    "정산 실행 이력",
                    "tb_settlement_run. 정산 배치 실행 기록. 자동삭제 미연동.",
                    365,
                    false),
            new Entry(
                    "SETTLEMENT_MANAGEMENT",
                    "정산관리",
                    "tb_settlement_setting, tb_rolling_reserve, tb_settlement_recovery, tb_merchant_receivable 등 정산 설정·잔액·회수·미수. 자동삭제 미연동.",
                    2555,
                    false),
            new Entry(
                    "COMMISSION_HISTORY",
                    "수수료·정산 연동 이력",
                    "tb_commission_history. 자동삭제 미연동.",
                    365,
                    false),
            new Entry(
                    "MERCHANT_SETTLEMENT_LIST",
                    "가맹점 정산내역(수수료내역)",
                    "정산관리 수수료내역·가맹 단위 정산 명세에 해당하는 조회·집계 근거(거래·정책·통화별 라운딩 산출 등). 「결제 거래 내역」과 연계되나 화면·감사 관점의 보관 목표는 별도 표기. 자동삭제 미연동.",
                    2555,
                    false),
            new Entry(
                    "SETTLEMENT_REPORT_DATA",
                    "정산 리포트",
                    "통합정산·정산리포트(집계·요약·실행·명세 등) 조회·산출·저장 데이터. 「정산 실행 이력(tb_settlement_run)」과 구분 — 리포트·집계 결과물 보관 목표. 자동삭제 미연동.",
                    1825,
                    false),
            new Entry(
                    "SERVER_USAGE_DAILY",
                    "서버 사용량 일별 집계",
                    "tb_server_usage_daily. 트래픽·메모리 피크. 자동삭제: usage_date 기준.",
                    365,
                    true),
            new Entry(
                    "ORG_CHANGE_LOG",
                    "조직·코드 변경 로그",
                    "tb_org_unit_change_log. 자동삭제 미연동.",
                    730,
                    false),
            new Entry(
                    "AUTH_TOKEN",
                    "로그인·API 토큰",
                    "auth_token. 만료 시 무효화 중심. 정책 일수는 보관 목표 안내용(자동삭제 미연동).",
                    30,
                    false),
            new Entry(
                    "NOTICE_BOARD",
                    "공지사항",
                    "pg_notice. 자동삭제 미연동.",
                    730,
                    false),
            new Entry(
                    "NOTIFY_LOG_MEMORY",
                    "노티·로그 메모리(버퍼)",
                    "애플리케이션·미들웨어 메모리 보관 목표(일). NOTI 유사 설정. DB와 별도 — 정책만 저장.",
                    15,
                    false),
            new Entry(
                    "NOTIFY_LOG_FILE",
                    "노티·감사 파일 로그",
                    "VPS·외부 로그 파일 보관(일). 파일 시스템은 별도 — 정책만 저장.",
                    90,
                    false),
            new Entry(
                    "USER_VIEW_SETTING",
                    "사용자 VIEW·화면 설정",
                    "tb_user_view_setting. 자동삭제 미연동.",
                    730,
                    false),
            new Entry(
                    "HQ_NOTIFY_MAPPING_AUDIT",
                    "노티 매핑·전산 설정 변경 이력",
                    "매핑 JSON 스냅샷 등(추후 확장). 자동삭제 미연동.",
                    365,
                    false)
    );

    private DataRetentionCatalog() {
    }

    private static Optional<Entry> entryById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        for (Entry e : ENTRIES) {
            if (e.id().equals(id)) {
                return Optional.of(e);
            }
        }
        return Optional.empty();
    }

    /**
     * JSON 한 필드(숫자 또는 객체)를 정책으로 파싱합니다.
     * <ul>
     *   <li>숫자만 있던 기존 데이터: 보관·삭제(스케줄) 일수 동일, 자동삭제는 스케줄 대상만 켜진 것으로 간주합니다.</li>
     *   <li>객체: {@code retain} 또는 {@code days}, {@code auto}, {@code purge} 또는 {@code deleteDays}</li>
     * </ul>
     */
    public static RetentionPolicy parsePolicyValue(Entry e, JsonNode v) {
        if (v == null || v.isNull()) {
            return defaultPolicyForEntry(e);
        }
        if (v.isNumber()) {
            int d = clampDays(v.intValue());
            return new RetentionPolicy(d, e.schedulerPurge(), d);
        }
        if (!v.isObject()) {
            return defaultPolicyForEntry(e);
        }
        int retain = e.defaultDays();
        if (v.has("retain") && v.get("retain").isNumber()) {
            retain = clampDays(v.get("retain").intValue());
        } else if (v.has("days") && v.get("days").isNumber()) {
            retain = clampDays(v.get("days").intValue());
        }
        boolean auto = e.schedulerPurge();
        if (v.has("auto")) {
            auto = v.get("auto").asBoolean(false);
        }
        int purge = retain;
        if (v.has("purge") && v.get("purge").isNumber()) {
            purge = clampDays(v.get("purge").intValue());
        } else if (v.has("deleteDays") && v.get("deleteDays").isNumber()) {
            purge = clampDays(v.get("deleteDays").intValue());
        }
        if (!e.schedulerPurge()) {
            auto = false;
            purge = retain;
        }
        return new RetentionPolicy(retain, auto, purge);
    }

    public static RetentionPolicy defaultPolicyForEntry(Entry e) {
        int d = clampDays(e.defaultDays());
        return new RetentionPolicy(d, e.schedulerPurge(), d);
    }

    /** 저장된 JSON → id별 정책(등록 id만). */
    public static Map<String, RetentionPolicy> parseRetentionPolicies(String json) {
        Map<String, RetentionPolicy> out = new LinkedHashMap<>();
        if (json == null || json.isBlank()) {
            return out;
        }
        try {
            JsonNode root = OM.readTree(json);
            if (!root.isObject()) {
                return out;
            }
            Iterator<String> it = root.fieldNames();
            while (it.hasNext()) {
                String k = it.next();
                entryById(k).ifPresent(e -> out.put(k, parsePolicyValue(e, root.get(k))));
            }
        } catch (Exception ignored) {
            /* keep partial */
        }
        return out;
    }

    /**
     * @deprecated 스케줄 삭제 일수만 필요할 때는 {@link #effectivePurgeDays(Entry, String)} 사용.
     */
    @Deprecated
    public static Map<String, Integer> parseOverridesJson(String json) {
        Map<String, Integer> out = new HashMap<>();
        Map<String, RetentionPolicy> pol = parseRetentionPolicies(json);
        for (Entry e : ENTRIES) {
            RetentionPolicy p = pol.get(e.id());
            if (p != null) {
                out.put(e.id(), p.retainDays());
            }
        }
        return out;
    }

    /**
     * 스케줄 자동삭제 대상 항목의 실제 삭제 기준 일수. 자동삭제가 꺼져 있으면 empty (삭제하지 않음).
     * JSON 에 해당 키가 없으면 기본값과 동일하게 자동삭제 켜진 것으로 간주(기존 운영 호환).
     */
    public static Optional<Integer> effectivePurgeDays(Entry e, String json) {
        if (!e.schedulerPurge()) {
            return Optional.empty();
        }
        Map<String, RetentionPolicy> pol = parseRetentionPolicies(json);
        RetentionPolicy p = pol.get(e.id());
        if (p == null) {
            int d = clampDays(e.defaultDays());
            return Optional.of(d);
        }
        if (!p.autoDeleteEnabled()) {
            return Optional.empty();
        }
        int del = p.purgeDays() > 0 ? p.purgeDays() : p.retainDays();
        return Optional.of(Math.max(1, del));
    }

    /**
     * 클라이언트가 보낸 JSON을 검증·정규화. 등록된 id만 유지, 값은 숫자(레거시) 또는 객체.
     */
    public static String normalizePolicyJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            JsonNode n = OM.readTree(raw);
            if (!n.isObject()) {
                return null;
            }
            ObjectNode out = OM.createObjectNode();
            for (Entry e : ENTRIES) {
                if (!n.has(e.id())) {
                    continue;
                }
                JsonNode rawV = n.get(e.id());
                RetentionPolicy p = parsePolicyValue(e, rawV);
                if (e.schedulerPurge()) {
                    ObjectNode o = OM.createObjectNode();
                    o.put("retain", p.retainDays());
                    o.put("auto", p.autoDeleteEnabled());
                    if (p.autoDeleteEnabled()) {
                        int pd = p.purgeDays() > 0 ? p.purgeDays() : p.retainDays();
                        o.put("purge", clampDays(pd));
                    }
                    out.set(e.id(), o);
                } else {
                    if (rawV.isNumber()) {
                        out.put(e.id(), p.retainDays());
                    } else {
                        ObjectNode o = OM.createObjectNode();
                        o.put("retain", p.retainDays());
                        out.set(e.id(), o);
                    }
                }
            }
            if (out.isEmpty()) {
                return null;
            }
            return OM.writeValueAsString(out);
        } catch (Exception e) {
            return null;
        }
    }

    /** @deprecated {@link #normalizePolicyJson(String)} */
    @Deprecated
    public static String normalizeOverridesJson(String raw) {
        return normalizePolicyJson(raw);
    }

    public static boolean isKnownId(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        for (Entry e : ENTRIES) {
            if (e.id().equals(id)) {
                return true;
            }
        }
        return false;
    }

    public static int clampDays(int d) {
        if (d < MIN_DAYS) {
            return MIN_DAYS;
        }
        if (d > MAX_DAYS) {
            return MAX_DAYS;
        }
        return d;
    }
}
