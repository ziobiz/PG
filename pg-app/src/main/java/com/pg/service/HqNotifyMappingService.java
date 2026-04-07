package com.pg.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pg.integration.pg.PgVendor;
import com.pg.entity.HqNotifyMappingConfig;
import com.pg.entity.PgNotifyInbound;
import com.pg.repository.HqNotifyMappingConfigRepository;
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.repository.PgNotifyInboundRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pg.entity.PgTrnsctn;
import com.pg.util.ChillPayNotifyOutcomeAdjust;
import com.pg.util.NotifyAmountParse;
import com.pg.util.NotifyChannelMerge;
import com.pg.util.NotifyToTxnStatusMerge;
import com.pg.util.PgNotifyInternalStatusMapper;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class HqNotifyMappingService {

    /** 노티 JSON → 거래 적재 시 기존 행 탐색 (칠페이 전용 레포 메서드와 동일 시그니처) */
    @FunctionalInterface
    public interface PgTrnsctnLookup {
        Optional<PgTrnsctn> find(String merchantId, String chillTransactionId, String orderNo);
    }

    public static final String DEFAULT_CATALOG_ID = "cat_pay_integrated_default";

    /** 결제내역(통합) 기본 카탈로그 — 프론트 `site/js/pay-list-integrated-catalog.js` 와 동기 유지 */
    private static final String PAY_LIST_INTEGRATED_CATALOG_RESOURCE = "catalog/pay-list-integrated-default.json";

    private static final DateTimeFormatter PAY_DD_MM = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH);

    /**
     * 통화 표시 폴백: PG {@code displayMaps.currency}에 키가 없거나 매핑이 없을 때만 사용.
     * 그리드에는 ISO 알파 3자(JPY·USD·KRW·THB)만 노출(ziobiz/노티미들웨어·칠페이 매뉴얼과 동일 표기).
     */
    private static final Map<String, String> GLOBAL_CURRENCY_DISPLAY = Map.ofEntries(
            Map.entry("392", "JPY"),
            Map.entry("410", "KRW"),
            Map.entry("764", "THB"),
            Map.entry("840", "USD"),
            Map.entry("JPY", "JPY"),
            Map.entry("KRW", "KRW"),
            Map.entry("THB", "THB"),
            Map.entry("USD", "USD"),
            Map.entry("jpy", "JPY"),
            Map.entry("krw", "KRW"),
            Map.entry("thb", "THB"),
            Map.entry("usd", "USD")
    );

    /**
     * 상태(chillPaymentStatus) 표시 폴백: PG {@code displayMaps.chillPaymentStatus}에 없을 때.
     * CHILLPAY에서 JSON 맵이 비어 있으면 캐시 단계에서 동일 맵을 채워 넣습니다({@link #buildDisplayTransformCache}).
     */
    private static final Map<String, String> EMBEDDED_CHILLPAY_STATUS_LABELS = Map.ofEntries(
            Map.entry("Paid", "성공"),
            Map.entry("paid", "성공"),
            Map.entry("Success", "성공"),
            Map.entry("success", "성공"),
            Map.entry("Complete", "성공"),
            Map.entry("WaitAuthorize", "요청"),
            Map.entry("waitauthorize", "요청"),
            Map.entry("Pending", "요청"),
            Map.entry("Request", "요청"),
            Map.entry("Cancelled", "취소"),
            Map.entry("cancelled", "취소"),
            Map.entry("Cancel", "취소"),
            Map.entry("Canceled", "취소"),
            Map.entry("Failed", "실패"),
            Map.entry("failed", "실패"),
            Map.entry("Fail", "실패"),
            Map.entry("Error", "실패"),
            Map.entry("Voided", "무효"),
            Map.entry("voided", "무효"),
            Map.entry("Void", "무효"),
            Map.entry("Manual void", "수동무효"),
            Map.entry("manual void", "수동무효"),
            Map.entry("EmailVoid", "이메일무효"),
            Map.entry("emailvoid", "이메일무효"),
            Map.entry("Email void", "이메일무효"),
            Map.entry("Refunded", "환불"),
            Map.entry("refunded", "환불"),
            Map.entry("Refund", "환불"),
            Map.entry("RefundRequested", "환불요청"),
            Map.entry("Refund Requested", "환불요청"),
            Map.entry("RefundPending", "환불요청"),
            Map.entry("Processing", "요청"),
            Map.entry("processing", "요청"),
            Map.entry("Authorized", "성공"),
            Map.entry("authorized", "성공"),
            Map.entry("Declined", "실패"),
            Map.entry("declined", "실패")
    );

    /** 내부 거래 status·콜백 PaymentStatus 한 자리가 그리드 원문으로 들어온 경우 */
    private static final Map<String, String> INTERNAL_STATUS_CHILL_DISPLAY_KR = Map.ofEntries(
            Map.entry("0", "성공"),
            Map.entry("1", "실패"),
            Map.entry("2", "취소"),
            Map.entry("3", "실패"),
            Map.entry("4", "오류"),
            Map.entry("10", "성공"),
            Map.entry("08", "요청"),
            Map.entry("20", "취소"),
            Map.entry("21", "무효"),
            Map.entry("22", "이메일무효"),
            Map.entry("30", "환불"),
            Map.entry("31", "강제환불"),
            Map.entry("99", "실패"),
            Map.entry("F0", "실패"),
            Map.entry("f0", "실패")
    );

    /** 결제내역 그리드에 표시값 가공을 적용할 때 캐시 (search 1회당 1번 로드) */
    public record DisplayTransformCache(Map<String, Map<String, Map<String, String>>> byVendorUpper) { }

    /** 결제관리 — 통합 결제내역과 동일 그리드·payListVariant 를 쓰는 화면 URL */
    public static final String[] PAY_LIST_LAYOUT_PAGE_URLS = {
            "/calc/payList",
            "/calc/payNotiList",
            "/calc/paySuccessList",
            "/calc/payFailList",
            "/calc/payRefundList",
            "/calc/payForceRefundList",
            "/calc/payCancelList",
            "/calc/payVoidList",
            "/calc/offsetCancList",
            "/pay/easyPay",
            "/pay/chatbotPay"
    };

    private final HqNotifyMappingConfigRepository repository;
    private final NotifyMappingAiService notifyMappingAiService;
    private final PgNotifyInboundRepository pgNotifyInboundRepository;
    private final MerchantPgBindingRepository merchantPgBindingRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HqNotifyMappingService(HqNotifyMappingConfigRepository repository,
                                  NotifyMappingAiService notifyMappingAiService,
                                  PgNotifyInboundRepository pgNotifyInboundRepository,
                                  MerchantPgBindingRepository merchantPgBindingRepository) {
        this.repository = repository;
        this.notifyMappingAiService = notifyMappingAiService;
        this.pgNotifyInboundRepository = pgNotifyInboundRepository;
        this.merchantPgBindingRepository = merchantPgBindingRepository;
    }

    public boolean isNotifyMappingAiConfigured() {
        return notifyMappingAiService != null && notifyMappingAiService.isConfigured();
    }

    /**
     * AI(OpenAI 호환)로 1차 매핑 후, 남은 파라미터는 휴리스틱으로 보완. API 키 없으면 전부 휴리스틱.
     *
     * @param lockedFieldMappings {@code lockAi=true} 인 행은 AI·휴리스틱이 덮어쓰지 않고 먼저 결과에 포함합니다.
     */
    public Map<String, Object> suggestFieldMappingsAiThenHeuristic(String vendorCode,
                                                                   String catalogId,
                                                                   List<String> paramNames,
                                                                   String sampleJson,
                                                                   boolean preferAi,
                                                                   List<Map<String, Object>> lockedFieldMappings) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (paramNames == null) {
            paramNames = List.of();
        }
        Set<String> catalogKeys = loadCatalogColumnKeys(catalogId);
        if (catalogKeys.isEmpty()) {
            catalogKeys = loadCatalogColumnKeys(DEFAULT_CATALOG_ID);
        }
        List<Map<String, Object>> combined = new ArrayList<>();
        Set<String> usedKeys = new LinkedHashSet<>();
        Set<String> usedPg = new LinkedHashSet<>();
        String source = "heuristic";

        if (lockedFieldMappings != null) {
            for (Map<String, Object> row : lockedFieldMappings) {
                if (row == null || !isLockAiRow(row)) {
                    continue;
                }
                String pf = stringVal(row.get("pgField")).trim();
                String ik = stringVal(row.get("internalKey")).trim();
                if (pf.isEmpty() || ik.isEmpty()) {
                    continue;
                }
                Map<String, Object> copy = new LinkedHashMap<>();
                copy.put("pgField", pf);
                copy.put("internalKey", ik);
                copy.put("note", stringVal(row.get("note")));
                copy.put("lockAi", Boolean.TRUE);
                combined.add(copy);
                usedKeys.add(ik);
                usedPg.add(pf);
            }
        }

        if (preferAi && notifyMappingAiService != null && notifyMappingAiService.isConfigured()) {
            List<Map<String, String>> labels = exportCatalogColumnLabels(catalogId);
            List<Map<String, Object>> aiRows = notifyMappingAiService.suggestWithAi(
                    vendorCode, paramNames, sampleJson, labels, catalogKeys);
            if (!aiRows.isEmpty()) {
                for (Map<String, Object> m : aiRows) {
                    combined.add(m);
                    Object ik = m.get("internalKey");
                    Object pf = m.get("pgField");
                    if (ik != null) {
                        usedKeys.add(ik.toString());
                    }
                    if (pf != null) {
                        usedPg.add(pf.toString().trim());
                    }
                }
                source = "ai";
            }
        }
        List<String> remaining = new ArrayList<>();
        for (String p : paramNames) {
            if (p == null || p.isBlank()) {
                continue;
            }
            String t = p.trim();
            if (!usedPg.contains(t)) {
                remaining.add(t);
            }
        }
        for (String pname : remaining) {
            Optional<String> best = bestCatalogKeyForParam(pname, catalogKeys, usedKeys);
            if (best.isPresent()) {
                usedKeys.add(best.get());
                usedPg.add(pname);
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("pgField", pname);
                row.put("internalKey", best.get());
                row.put("note", "규칙 기반 보완");
                combined.add(row);
                if ("ai".equals(source)) {
                    source = "ai+heuristic";
                }
            }
        }
        if (combined.isEmpty()) {
            source = "none";
        }
        out.put("fieldMappings", combined);
        out.put("source", source);
        out.put("paramCount", paramNames.size());
        return out;
    }

    /** AI 프롬프트용: 카탈로그 열 key + 화면 라벨 */
    public List<Map<String, String>> exportCatalogColumnLabels(String catalogId) {
        List<Map<String, String>> list = new ArrayList<>();
        String cid = catalogId == null || catalogId.isBlank() ? DEFAULT_CATALOG_ID : catalogId.trim();
        try {
            HqNotifyMappingConfig c = getOrCreate();
            JsonNode eff = effectiveRootForLayout(c.getMappingJson());
            JsonNode catalog = findCatalog(eff, cid);
            if (catalog == null || !catalog.isObject()) {
                catalog = buildDefaultIntegratedCatalog();
            }
            JsonNode cols = catalog.get("columns");
            if (cols != null && cols.isArray()) {
                for (JsonNode col : cols) {
                    if (!col.path("visible").asBoolean(true)) {
                        continue;
                    }
                    String k = col.path("key").asText("").trim();
                    if (k.isEmpty() || "_chk".equals(k) || "payActions".equals(k)) {
                        continue;
                    }
                    Map<String, String> row = new LinkedHashMap<>();
                    row.put("key", k);
                    row.put("label", col.path("label").asText(k));
                    list.add(row);
                }
            }
        } catch (Exception ignored) {
            // ignore
        }
        return list;
    }

    @Transactional
    public HqNotifyMappingConfig getOrCreate() {
        HqNotifyMappingConfig c = repository.findFirstByOrderByIdAsc().orElseGet(() -> {
            HqNotifyMappingConfig x = new HqNotifyMappingConfig();
            x.setMappingJson(buildDefaultMappingJson());
            return repository.save(x);
        });
        if (c.getMappingJson() == null || c.getMappingJson().isBlank()) {
            c.setMappingJson(buildDefaultMappingJson());
            c = repository.save(c);
        }
        return c;
    }

    /**
     * 신규·초기화용 기본 JSON (v2).
     * columnCatalogs: 결제내역(통합) 컬럼 정의.
     * pageCatalogAssignments: 결제관리 각 하위 메뉴별 적용 카탈로그.
     * vendors: PG별 CALLBACK·RESULT·RETURN 채널 및 필드 매핑.
     */
    public String buildDefaultMappingJson() {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("version", 2);
            root.put("memo", "columnCatalogs·pageCatalogAssignments·vendors.fieldMappings·vendors.displayMaps(그리드 표시값 가공) — 결제내역 API 응답 직전에 displayMaps 적용.");
            root.set("columnCatalogs", buildDefaultColumnCatalogsArray());
            root.set("pageCatalogAssignments", buildDefaultPageAssignmentsArray());
            ArrayNode vendors = root.putArray("vendors");
            vendors.add(buildVendor(PgVendor.CHILLPAY, "칠페이", true));
            vendors.add(buildVendor(PgVendor.JPAY, "제이페이", false));
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            return "{\"version\":2,\"memo\":\"\",\"columnCatalogs\":[],\"pageCatalogAssignments\":[],\"vendors\":[]}";
        }
    }

    private ArrayNode buildDefaultColumnCatalogsArray() {
        ArrayNode arr = objectMapper.createArrayNode();
        arr.add(buildDefaultIntegratedCatalog());
        return arr;
    }

    /** 노티매핑 UI: [기본 카탈로그·화면연결] 버튼용 */
    public List<Map<String, Object>> exportDefaultColumnCatalogs() {
        return arrayNodeToMapList(buildDefaultColumnCatalogsArray());
    }

    public List<Map<String, Object>> exportDefaultPageAssignments() {
        return arrayNodeToMapList(buildDefaultPageAssignmentsArray());
    }

    private List<Map<String, Object>> arrayNodeToMapList(ArrayNode arr) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (arr == null) {
            return list;
        }
        for (JsonNode n : arr) {
            if (n.isObject()) {
                try {
                    list.add(objectMapper.convertValue(n, new TypeReference<Map<String, Object>>() { }));
                } catch (Exception ignored) {
                    // skip
                }
            }
        }
        return list;
    }

    private ObjectNode buildDefaultIntegratedCatalog() {
        try (InputStream in = new ClassPathResource(PAY_LIST_INTEGRATED_CATALOG_RESOURCE).getInputStream()) {
            JsonNode n = objectMapper.readTree(in);
            if (!n.isObject()) {
                throw new IllegalStateException(PAY_LIST_INTEGRATED_CATALOG_RESOURCE + " must be a JSON object");
            }
            return (ObjectNode) n;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load " + PAY_LIST_INTEGRATED_CATALOG_RESOURCE, e);
        }
    }

    private ArrayNode buildDefaultPageAssignmentsArray() {
        ArrayNode a = objectMapper.createArrayNode();
        for (String u : PAY_LIST_LAYOUT_PAGE_URLS) {
            ObjectNode row = objectMapper.createObjectNode();
            row.put("pageUrl", u);
            row.put("catalogId", DEFAULT_CATALOG_ID);
            a.add(row);
        }
        return a;
    }

    private ObjectNode buildVendor(String code, String name, boolean chillPaySampleMappings) {
        ObjectNode v = objectMapper.createObjectNode();
        v.put("vendorCode", code);
        v.put("vendorName", name);
        ArrayNode channels = v.putArray("channels");
        ArrayNode cbMaps = chillPaySampleMappings ? chillpayCallbackMappings() : objectMapper.createArrayNode();
        channels.add(buildChannel("CALLBACK", "CALLBACK (서버 노티)", "/calc/payList", "통합 결제내역", cbMaps));
        channels.add(buildChannel("RESULT", "RESULT (브라우저 리다이렉트·클라이언트)", "/pay/pay.html", "결제(리다이렉트) 화면",
                objectMapper.createArrayNode()));
        channels.add(buildChannel("RETURN", "RETURN (동기 응답·return_url 등)", "", "", objectMapper.createArrayNode()));
        v.set("displayMaps", chillPaySampleMappings ? defaultChillPayDisplayMaps() : objectMapper.createObjectNode());
        return v;
    }

    private ObjectNode defaultChillPayDisplayMaps() {
        ObjectNode dm = objectMapper.createObjectNode();
        ObjectNode cur = dm.putObject("currency");
        cur.put("392", "JPY");
        cur.put("410", "KRW");
        cur.put("764", "THB");
        cur.put("840", "USD");
        cur.put("JPY", "JPY");
        cur.put("KRW", "KRW");
        cur.put("THB", "THB");
        cur.put("USD", "USD");
        ObjectNode st = dm.putObject("chillPaymentStatus");
        EMBEDDED_CHILLPAY_STATUS_LABELS.forEach(st::put);
        return dm;
    }

    private ArrayNode chillpayCallbackMappings() {
        ArrayNode a = objectMapper.createArrayNode();
        a.add(mapping("TransactionId", "chillTransactionId", "칠페이 거래 ID → 그리드 TransactionId(칠페이)"));
        a.add(mapping("RouteNo", "routeNo", "라우트 번호"));
        a.add(mapping("Amount", "chillAmount", "금액(칠페이 시트)"));
        a.add(mapping("OrderNo", "orderNo", "주문번호"));
        a.add(mapping("Status / PaymentStatus", "chillPaymentStatus", "상태"));
        return a;
    }

    private ObjectNode mapping(String pgField, String internalKey, String note) {
        ObjectNode m = objectMapper.createObjectNode();
        m.put("pgField", pgField);
        m.put("internalKey", internalKey);
        m.put("note", note);
        return m;
    }

    private ObjectNode buildChannel(String channelCode, String channelName, String targetUrl, String targetLabel, ArrayNode fieldMappings) {
        ObjectNode ch = objectMapper.createObjectNode();
        ch.put("channelCode", channelCode);
        ch.put("channelName", channelName);
        ch.put("targetPageUrl", targetUrl == null ? "" : targetUrl);
        ch.put("targetPageLabel", targetLabel == null ? "" : targetLabel);
        ch.set("fieldMappings", fieldMappings);
        return ch;
    }

    public Map<String, Object> toMap(HqNotifyMappingConfig c) {
        Map<String, Object> m = new LinkedHashMap<>();
        String json = c.getMappingJson();
        if (json == null || json.isBlank()) {
            json = buildDefaultMappingJson();
        }
        m.put("mappingDefinitionJson", json);
        m.put("updatedAt", c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : "");
        return m;
    }

    @Transactional
    public HqNotifyMappingConfig saveFromBody(Map<String, Object> body) {
        HqNotifyMappingConfig c = getOrCreate();
        Object raw = body != null ? body.get("mappingDefinitionJson") : null;
        if (raw == null) {
            throw new IllegalArgumentException("mappingDefinitionJson 이 필요합니다.");
        }
        String s = raw.toString().trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException("매핑 JSON 이 비어 있습니다.");
        }
        validateJson(s);
        c.setMappingJson(s);
        return repository.save(c);
    }

    private void validateJson(String s) {
        try {
            JsonNode n = objectMapper.readTree(s);
            if (!n.isObject()) {
                throw new IllegalArgumentException("JSON 은 객체 형태여야 합니다.");
            }
            JsonNode vendors = n.get("vendors");
            if (vendors != null && vendors.isArray()) {
                for (JsonNode v : vendors) {
                    JsonNode dm = v.get("displayMaps");
                    if (dm != null && !dm.isObject()) {
                        throw new IllegalArgumentException("vendors[].displayMaps 는 객체(JSON)여야 합니다.");
                    }
                    if (dm != null && dm.isObject()) {
                        for (Iterator<Map.Entry<String, JsonNode>> it = dm.fields(); it.hasNext(); ) {
                            Map.Entry<String, JsonNode> en = it.next();
                            if (!en.getValue().isObject()) {
                                throw new IllegalArgumentException("displayMaps." + en.getKey() + " 는 { raw: label } 객체여야 합니다.");
                            }
                        }
                    }
                    JsonNode chs = v.get("channels");
                    if (chs != null && chs.isArray()) {
                        Set<String> allowedIk = allowedInternalKeysForValidation(n);
                        for (JsonNode ch : chs) {
                            JsonNode fm = ch.get("fieldMappings");
                            if (fm == null || !fm.isArray()) {
                                continue;
                            }
                            for (JsonNode row : fm) {
                                String ik = row.path("internalKey").asText("").trim();
                                if (ik.isEmpty()) {
                                    throw new IllegalArgumentException("fieldMappings.internalKey 은 비울 수 없습니다.");
                                }
                                if (ik.startsWith("hqExt_")) {
                                    continue;
                                }
                                if (!allowedIk.contains(ik)) {
                                    throw new IllegalArgumentException(
                                            "fieldMappings.internalKey \"" + ik + "\" 는 본 JSON의 columnCatalogs·기본 통합 카탈로그 열 key에 없습니다. "
                                                    + "VIEW SETTING(조직항목설정)과 동일한 열 key로 매핑하세요.");
                                }
                            }
                        }
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON 형식이 올바르지 않습니다: " + e.getMessage());
        }
    }

    /** 저장 검증: 매핑 대상 internalKey 가 카탈로그에 정의된 열과 일치하는지 */
    private Set<String> allowedInternalKeysForValidation(JsonNode root) {
        Set<String> keys = new LinkedHashSet<>();
        try {
            JsonNode def = buildDefaultIntegratedCatalog();
            JsonNode cols0 = def.get("columns");
            if (cols0 != null && cols0.isArray()) {
                for (JsonNode c : cols0) {
                    String k = c.path("key").asText("").trim();
                    if (!k.isEmpty()) {
                        keys.add(k);
                    }
                }
            }
        } catch (Exception ignored) {
            // ignore
        }
        JsonNode catalogs = root.get("columnCatalogs");
        if (catalogs != null && catalogs.isArray()) {
            for (JsonNode cat : catalogs) {
                JsonNode cl = cat.get("columns");
                if (cl != null && cl.isArray()) {
                    for (JsonNode c : cl) {
                        String k = c.path("key").asText("").trim();
                        if (!k.isEmpty()) {
                            keys.add(k);
                        }
                    }
                }
            }
        }
        keys.remove("_chk");
        keys.remove("payActions");
        keys.remove("commissionInlineActions");
        return keys;
    }

    /**
     * 저장된 매핑 + 내장 기본값을 합쳐 결제내역 계열 화면의 그리드 레이아웃을 계산합니다.
     */
    public Map<String, Object> resolvePayListScreenLayout(String pageUrl) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (pageUrl == null || pageUrl.isBlank()) {
            out.put("error", "pageUrl 필요");
            return out;
        }
        String norm = pageUrl.trim();
        boolean supported = false;
        for (String u : PAY_LIST_LAYOUT_PAGE_URLS) {
            if (u.equals(norm)) {
                supported = true;
                break;
            }
        }
        if (!supported) {
            out.put("error", "지원하지 않는 화면 URL");
            out.put("pageUrl", norm);
            return out;
        }
        try {
            HqNotifyMappingConfig c = getOrCreate();
            JsonNode eff = effectiveRootForLayout(c.getMappingJson());
            String catalogId = resolveCatalogIdForPage(eff, norm);
            JsonNode catalog = findCatalog(eff, catalogId);
            if (catalog == null || !catalog.isObject()) {
                catalog = buildDefaultIntegratedCatalog();
            }
            String title = catalog.path("displayTitle").asText("결제내역(기본)");
            out.put("pageUrl", norm);
            out.put("catalogId", catalog.path("catalogId").asText(DEFAULT_CATALOG_ID));
            out.put("catalogDisplayTitle", title);
            out.put("headerGroups", jsonToList(catalog.get("headerGroups")));
            out.put("columns", sortedVisibleColumns(catalog.get("columns")));
            return out;
        } catch (Exception e) {
            out.put("error", e.getMessage() != null ? e.getMessage() : "layout 실패");
            out.put("pageUrl", norm);
            return out;
        }
    }

    private JsonNode effectiveRootForLayout(String storedJson) throws Exception {
        JsonNode root = objectMapper.readTree(storedJson == null || storedJson.isBlank() ? "{}" : storedJson);
        ObjectNode eff = root.isObject() ? (ObjectNode) root.deepCopy() : objectMapper.createObjectNode();
        if (!eff.has("columnCatalogs") || !eff.get("columnCatalogs").isArray() || eff.get("columnCatalogs").size() == 0) {
            eff.set("columnCatalogs", buildDefaultColumnCatalogsArray());
        }
        if (!eff.has("pageCatalogAssignments") || !eff.get("pageCatalogAssignments").isArray()
                || eff.get("pageCatalogAssignments").size() == 0) {
            eff.set("pageCatalogAssignments", buildDefaultPageAssignmentsArray());
        }
        return eff;
    }

    private String resolveCatalogIdForPage(JsonNode eff, String pageUrl) {
        JsonNode assigns = eff.get("pageCatalogAssignments");
        if (assigns != null && assigns.isArray()) {
            for (JsonNode a : assigns) {
                if (pageUrl.equals(a.path("pageUrl").asText())) {
                    String cid = a.path("catalogId").asText("").trim();
                    if (!cid.isEmpty()) {
                        return cid;
                    }
                }
            }
        }
        return DEFAULT_CATALOG_ID;
    }

    private JsonNode findCatalog(JsonNode eff, String catalogId) {
        if (catalogId == null || catalogId.isBlank()) {
            return null;
        }
        JsonNode cats = eff.get("columnCatalogs");
        if (cats == null || !cats.isArray()) {
            return null;
        }
        for (JsonNode c : cats) {
            if (catalogId.equals(c.path("catalogId").asText())) {
                return c;
            }
        }
        return null;
    }

    private List<Map<String, Object>> jsonToList(JsonNode arr) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (arr == null || !arr.isArray()) {
            return list;
        }
        for (JsonNode n : arr) {
            if (!n.isObject()) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("label", n.path("label").asText(""));
            List<String> keys = new ArrayList<>();
            JsonNode ks = n.get("keys");
            if (ks != null && ks.isArray()) {
                for (JsonNode k : ks) {
                    keys.add(k.asText());
                }
            }
            row.put("keys", keys);
            list.add(row);
        }
        return list;
    }

    private List<Map<String, Object>> sortedVisibleColumns(JsonNode colsNode) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (colsNode == null || !colsNode.isArray()) {
            return rows;
        }
        for (JsonNode c : colsNode) {
            if (!c.isObject()) {
                continue;
            }
            if (!c.path("visible").asBoolean(true)) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("key", c.path("key").asText(""));
            row.put("label", c.path("label").asText(""));
            row.put("visible", true);
            row.put("order", c.path("order").asInt(9999));
            rows.add(row);
        }
        rows.sort(Comparator.comparingInt(a -> ((Number) a.get("order")).intValue()));
        return rows;
    }

    // ——— 노티 매핑 런타임 적용 · 자동 제안 ———

    /**
     * 해당 PG에 CALLBACK 채널 fieldMappings 가 1건 이상 정의되어 있으면 true.
     *
     * @deprecated {@link #hasMappableNotifyMapping(String, String)} 사용
     */
    @Deprecated
    public boolean hasVendorCallbackMappings(String vendorCode) {
        return hasMappableNotifyMapping(vendorCode, "CALLBACK");
    }

    /**
     * 노티 수신 채널(CALLBACK / RESULT / RETURN 등)에 맞는 매핑이 있거나, 해당 채널이 비어 있으면 CALLBACK 매핑으로 대체 가능하면 true.
     */
    public boolean hasMappableNotifyMapping(String vendorCode, String notifyChannel) {
        if (vendorCode == null || vendorCode.isBlank()) {
            return false;
        }
        return !resolveEffectiveFieldMappings(vendorCode.trim(), notifyChannel).isEmpty();
    }

    /**
     * 저장된 매핑으로 노티 JSON 본문을 {@link PgTrnsctn}에 반영합니다.
     * 매핑이 없거나 필수 값(식별·금액)이 부족하면 empty.
     *
     * @param notifyChannel 수신 경로별 채널. {@code RESULT} 등 전용 매핑이 없으면 {@code CALLBACK} 매핑으로 폴백합니다.
     */
    public Optional<PgTrnsctn> tryBuildTransactionFromMappedCallback(String vendorCode,
                                                                   JsonNode notifyRoot,
                                                                   PgNotifyInbound in,
                                                                   PgTrnsctnLookup lookup,
                                                                   String notifyChannel) {
        if (in == null || notifyRoot == null || !notifyRoot.isObject()) {
            return Optional.empty();
        }
        if (!"PARSED".equalsIgnoreCase(String.valueOf(in.getProcessStatus()).trim())) {
            return Optional.empty();
        }
        String merchant = in.getMerchantId();
        if (merchant == null || merchant.isBlank()) {
            return Optional.empty();
        }
        if (vendorCode == null || vendorCode.isBlank()) {
            return Optional.empty();
        }
        List<FieldMappingRow> mappings = resolveEffectiveFieldMappings(vendorCode.trim(), notifyChannel);
        if (mappings.isEmpty()) {
            return Optional.empty();
        }
        Map<String, String> byKey = new LinkedHashMap<>();
        String chillPsFromPaymentField = null;
        String chillPsFromOtherField = null;
        for (FieldMappingRow m : mappings) {
            String val = extractMappedValue(notifyRoot, m.pgField);
            if (val == null || val.isBlank()) {
                continue;
            }
            val = val.trim();
            String nk = normalizeInternalKey(m.internalKey);
            if ("chillPaymentStatus".equals(nk)) {
                /* PaymentStatus·paystatus 계열을 Status 단독 필드보다 우선(둘 다 chillPaymentStatus로 잡히면 기존엔 후자가 덮어써 전부 요청으로 보일 수 있음) */
                String pf = m.pgField == null ? "" : m.pgField.toLowerCase(Locale.ROOT);
                if (pf.contains("paymentstatus") || pf.contains("paystatus")) {
                    chillPsFromPaymentField = val;
                } else {
                    chillPsFromOtherField = val;
                }
            } else {
                byKey.put(nk, val);
            }
        }
        String mappedPsMerged = chillPsFromPaymentField != null ? chillPsFromPaymentField : chillPsFromOtherField;
        if (mappedPsMerged != null) {
            byKey.put("chillPaymentStatus", mappedPsMerged);
        }
        String chillTxn = firstNonBlank(byKey, "chillTransactionId");
        String orderNo = firstNonBlank(byKey, "orderNo");
        if ((chillTxn == null || chillTxn.isBlank()) && (orderNo == null || orderNo.isBlank())) {
            return Optional.empty();
        }
        Optional<BigDecimal> amountOpt = Optional.empty();
        String amtStr = firstNonBlank(byKey, "chillAmount");
        if (amtStr != null) {
            amountOpt = NotifyAmountParse.parsePlain(amtStr);
        }
        Optional<PgTrnsctn> existingOpt = lookup.find(merchant.trim(), chillTxn, orderNo);
        if (!NotifyAmountParse.isPositive(amountOpt) && existingOpt.isEmpty()) {
            return Optional.empty();
        }

        PgTrnsctn t = existingOpt.orElseGet(() -> {
            PgTrnsctn x = new PgTrnsctn();
            x.setTrnId(newTrnId());
            return x;
        });

        BigDecimal amountBd;
        if (NotifyAmountParse.isPositive(amountOpt)) {
            amountBd = amountOpt.get();
        } else {
            BigDecimal prev = t.getAmtKrw();
            if (prev != null && prev.compareTo(BigDecimal.ZERO) > 0) {
                amountBd = prev;
            } else if (existingOpt.isPresent()) {
                /* 기존 행 금액이 비어 있어도 무효·취소 등 후속 노티는 반영(노티거래내역과 동일하게 상태만 갱신) */
                amountBd = BigDecimal.ZERO;
            } else {
                return Optional.empty();
            }
        }

        t.setMerchantId(merchant.trim());
        t.setServiceType("NOTI");
        t.setOrigin("NOTI");
        String chStore = notifyChannel == null || notifyChannel.isBlank()
                ? "CALLBACK" : notifyChannel.trim().toUpperCase(Locale.ROOT);
        t.setNotifyChannelType(NotifyChannelMerge.mergeStored(t.getNotifyChannelType(), chStore));
        t.setVan(vendorCode.trim().length() > 10 ? vendorCode.trim().substring(0, 10) : vendorCode.trim());
        t.setAmtKrw(amountBd);
        String curRaw = firstNonBlank(byKey, "currency");
        if (curRaw == null || curRaw.isBlank()) {
            /* 매핑에 currency 열이 없어도 노티 본문 표준 키는 읽습니다(기존 칠페이 경로와 동일). */
            curRaw = extractNotifyCurrencyRaw(notifyRoot);
        }
        if (curRaw != null && !curRaw.isBlank()) {
            String u = curRaw.toUpperCase(Locale.ROOT).trim();
            t.setCurType(u.length() > 3 ? u.substring(0, 3) : u);
        } else {
            t.setCurType("KRW");
        }

        String payNo = orderNo != null && !orderNo.isBlank() ? orderNo : (chillTxn != null ? chillTxn : t.getTrnId());
        t.setPayNo(truncate(payNo, 50));
        if (orderNo != null && !orderNo.isBlank()) {
            t.setOrderNo(truncate(orderNo, 64));
        } else if (chillTxn != null) {
            String synthetic = "NM" + chillTxn.trim();
            t.setOrderNo(truncate(synthetic, 64));
        }
        if (chillTxn != null && !chillTxn.isBlank()) {
            t.setChillTransactionId(truncate(chillTxn, 64));
        }

        String custId = firstNonBlank(byKey, "customerId");
        if (custId == null || custId.isBlank() || "guest".equalsIgnoreCase(custId.trim())) {
            String fromRawId = extractNotifyCustomerIdRaw(notifyRoot);
            if (fromRawId != null && !fromRawId.isBlank()) {
                custId = fromRawId;
            }
        }
        if (custId != null && !custId.isBlank()) {
            t.setCustomerId(truncate(custId, 100));
        } else {
            t.setCustomerId("guest");
        }
        String custNm = firstNonBlank(byKey, "customerNm");
        if (custNm == null || custNm.isBlank()) {
            custNm = extractNotifyCustomerNameRaw(notifyRoot);
        }
        if (custNm != null && !custNm.isBlank()) {
            t.setCustomerNm(truncate(custNm, 200));
        }
        String pch = firstNonBlank(byKey, "paymentChannel");
        if (pch != null) {
            t.setPaymentChannel(truncate(pch, 80));
        }
        String route = firstNonBlank(byKey, "routeNo");
        if (route != null && !route.isBlank()) {
            t.setRouteNo(truncate(route, 32));
        } else if (in.getRootNo() != null && !in.getRootNo().isBlank()) {
            t.setRouteNo(truncate(in.getRootNo().trim(), 32));
        }
        parseOptionalDecimal(firstNonBlank(byKey, "chillFeeAmt")).ifPresent(t::setChillFeeAmt);
        parseOptionalDecimal(firstNonBlank(byKey, "totalAmt")).ifPresent(t::setTotalAmt);
        if (t.getTotalAmt() == null) {
            t.setTotalAmt(amountBd);
        }
        parseOptionalDecimal(firstNonBlank(byKey, "icopayAmt")).ifPresent(t::setIcopayAmt);

        String aprv = firstNonBlank(byKey, "cardAprvNo", "pgApproveNo");
        if (aprv != null) {
            t.setApprovalNo(truncate(aprv, 20));
        }

        String mappedPs = firstNonBlank(byKey, "chillPaymentStatus");
        String jsonPayStat = extractNotifyPaymentStatusText(notifyRoot);
        String jsonStatField = extractNotifyStatusCodeField(notifyRoot);
        String payStForInternal = firstNonBlankString(jsonPayStat, mappedPs);
        String statusFieldForInternal = firstNonBlankString(jsonStatField, firstNonBlank(byKey, "status"));
        String internalComputed = PgNotifyInternalStatusMapper.mapForMappedNotify(
                payStForInternal, statusFieldForInternal, vendorCode);
        String rawPaymentForReclass = firstNonBlankString(jsonPayStat, mappedPs);
        internalComputed = ChillPayNotifyOutcomeAdjust.reclassifyPaymentStatusTwoAfterPaid(
                existingOpt, rawPaymentForReclass, internalComputed);
        String mergedStatus = NotifyToTxnStatusMerge.merge(t.getStatus(), internalComputed, notifyChannel);
        if (mergedStatus == null || mergedStatus.isBlank()) {
            mergedStatus = "08";
        }
        t.setStatus(mergedStatus);

        String chillDisplay = resolveChillPaymentStatusForStorage(mappedPs, jsonPayStat, mergedStatus);
        if (chillDisplay != null && !chillDisplay.isBlank()) {
            t.setChillPaymentStatus(truncate(chillDisplay, 50));
        }
        if ("10".equals(mergedStatus)) {
            LocalDateTime paid = parsePaymentDate(firstNonBlank(byKey, "payCompletedAt", "paidAt", "paymentDate"));
            t.setPaidAt(paid != null ? paid : LocalDateTime.now());
        } else {
            t.setPaidAt(null);
        }
        String sy = firstNonBlank(byKey, "settledYn");
        if (sy != null && !sy.isBlank()) {
            t.setSettledYn(truncate(sy, 1));
        } else if (t.getSettledYn() == null || t.getSettledYn().isBlank()) {
            t.setSettledYn("N");
        }
        return Optional.of(t);
    }

    /** 하위 호환: 채널을 CALLBACK 로 가정 */
    public Optional<PgTrnsctn> tryBuildTransactionFromMappedCallback(String vendorCode,
                                                                   JsonNode notifyRoot,
                                                                   PgNotifyInbound in,
                                                                   PgTrnsctnLookup lookup) {
        return tryBuildTransactionFromMappedCallback(vendorCode, notifyRoot, in, lookup, "CALLBACK");
    }

    private List<FieldMappingRow> resolveEffectiveFieldMappings(String vendorCode, String notifyChannel) {
        String ch = notifyChannel == null || notifyChannel.isBlank() ? "CALLBACK" : notifyChannel.trim().toUpperCase(Locale.ROOT);
        List<FieldMappingRow> rows = loadFieldMappingsForChannel(vendorCode, ch);
        if (!rows.isEmpty()) {
            return rows;
        }
        if (!"CALLBACK".equals(ch)) {
            return loadFieldMappingsForChannel(vendorCode, "CALLBACK");
        }
        return List.of();
    }

    /**
     * UI·도구용: PG 파라미터 이름 목록을 카탈로그 열 key 후보에 맞춰 1차 자동 매핑(휴리스틱)합니다.
     */
    public List<Map<String, Object>> suggestFieldMappings(String vendorCode,
                                                          String catalogId,
                                                          List<String> paramNames) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (paramNames == null || paramNames.isEmpty()) {
            return out;
        }
        Set<String> catalogKeys = loadCatalogColumnKeys(catalogId);
        if (catalogKeys.isEmpty()) {
            catalogKeys = loadCatalogColumnKeys(DEFAULT_CATALOG_ID);
        }
        Set<String> usedTargets = new LinkedHashSet<>();
        for (String rawName : paramNames) {
            if (rawName == null || rawName.isBlank()) {
                continue;
            }
            String pgField = rawName.trim();
            Optional<String> best = bestCatalogKeyForParam(pgField, catalogKeys, usedTargets);
            if (best.isPresent()) {
                usedTargets.add(best.get());
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("pgField", pgField);
                row.put("internalKey", best.get());
                row.put("note", "자동 제안 (" + (vendorCode != null ? vendorCode : "") + ")");
                out.add(row);
            }
        }
        return out;
    }

    /** 샘플 JSON에서 최상위 + data.* 키 이름을 수집 */
    public List<String> collectJsonParamNames(String json) {
        List<String> names = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return names;
        }
        try {
            JsonNode n = objectMapper.readTree(json.trim());
            if (!n.isObject()) {
                return names;
            }
            collectObjectKeys(n, names);
            JsonNode data = n.get("data");
            if (data != null && data.isObject()) {
                collectObjectKeys(data, names);
            }
        } catch (Exception ignored) {
            // ignore
        }
        return names;
    }

    private static void collectObjectKeys(JsonNode obj, List<String> names) {
        Iterator<String> it = obj.fieldNames();
        while (it.hasNext()) {
            names.add(it.next());
        }
    }

    private static String stringVal(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static boolean isLockAiRow(Map<String, Object> row) {
        Object v = row.get("lockAi");
        if (v instanceof Boolean b) {
            return b;
        }
        String s = stringVal(v).trim();
        return "true".equalsIgnoreCase(s) || "1".equals(s);
    }

    /** application/x-www-form-urlencoded·JSON 혼합 노티 본문에서 파라미터 이름 수집 */
    public List<String> collectParamNamesFromRawPayload(String rawBody, String contentType) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        if (rawBody == null || rawBody.isBlank()) {
            return List.of();
        }
        String t = rawBody.trim();
        String ct = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (ct.contains("json") || t.startsWith("{") || t.startsWith("[")) {
            keys.addAll(collectJsonParamNames(t));
        }
        if (t.contains("=")) {
            try {
                for (String part : t.split("&")) {
                    if (part == null || part.isBlank()) {
                        continue;
                    }
                    int eq = part.indexOf('=');
                    String name = (eq <= 0) ? part.trim() : part.substring(0, eq).trim();
                    if (name.isEmpty()) {
                        continue;
                    }
                    keys.add(URLDecoder.decode(name, StandardCharsets.UTF_8).trim());
                }
            } catch (Exception ignored) {
                // ignore
            }
        }
        if (keys.isEmpty()) {
            keys.addAll(collectJsonParamNames(t));
        }
        return new ArrayList<>(keys);
    }

    /**
     * 최근 수신 노티 원문에서 관찰된 파라미터 키 — 해당 PG에 연결된 MID가 있으면 그 MID 노티만, 없으면 최근 전체 노티로 폴백.
     */
    public Map<String, Object> listObservedInboundParamKeys(String vendorCode, int maxBodies) {
        Map<String, Object> out = new LinkedHashMap<>();
        int cap = Math.min(500, Math.max(1, maxBodies));
        String vc = vendorCode == null ? "" : vendorCode.trim();
        if (vc.isEmpty()) {
            out.put("keys", List.of());
            out.put("source", "empty_vendor");
            out.put("inboundRowsScanned", 0);
            return out;
        }
        List<String> mids = merchantPgBindingRepository.findDistinctMidsByPgCdLikeVendor(vc);
        PageRequest pr = PageRequest.of(0, cap);
        List<PgNotifyInbound> rows = mids.isEmpty()
                ? pgNotifyInboundRepository.findAllByOrderByIdDesc(pr)
                : pgNotifyInboundRepository.findByMidInOrderByIdDesc(mids, pr);
        LinkedHashSet<String> all = new LinkedHashSet<>();
        for (PgNotifyInbound in : rows) {
            all.addAll(collectParamNamesFromRawPayload(in.getRawBody(), in.getContentType()));
        }
        List<String> sorted = new ArrayList<>(all);
        Collections.sort(sorted);
        out.put("keys", sorted);
        out.put("source", mids.isEmpty() ? "inbound_recent_all" : "inbound_mid_for_vendor");
        out.put("inboundRowsScanned", rows.size());
        out.put("midCountUsedForFilter", mids.size());
        return out;
    }

    /** PG·채널(CALLBACK/RESULT/RETURN)별 저장된 fieldMappings */
    private List<FieldMappingRow> loadFieldMappingsForChannel(String vendorCode, String channelCode) {
        List<FieldMappingRow> list = new ArrayList<>();
        String wantCh = channelCode == null || channelCode.isBlank() ? "CALLBACK" : channelCode.trim().toUpperCase(Locale.ROOT);
        try {
            HqNotifyMappingConfig c = getOrCreate();
            JsonNode root = objectMapper.readTree(c.getMappingJson() == null ? "{}" : c.getMappingJson());
            JsonNode vendors = root.get("vendors");
            if (vendors == null || !vendors.isArray()) {
                return list;
            }
            for (JsonNode v : vendors) {
                if (!vendorCode.equalsIgnoreCase(v.path("vendorCode").asText("").trim())) {
                    continue;
                }
                JsonNode chs = v.get("channels");
                if (chs == null || !chs.isArray()) {
                    return list;
                }
                for (JsonNode ch : chs) {
                    if (!wantCh.equalsIgnoreCase(ch.path("channelCode").asText("").trim())) {
                        continue;
                    }
                    JsonNode fm = ch.get("fieldMappings");
                    if (fm == null || !fm.isArray()) {
                        return list;
                    }
                    for (JsonNode m : fm) {
                        String pf = m.path("pgField").asText("").trim();
                        String ik = m.path("internalKey").asText("").trim();
                        if (!pf.isEmpty() && !ik.isEmpty()) {
                            list.add(new FieldMappingRow(pf, ik));
                        }
                    }
                    return list;
                }
            }
        } catch (Exception ignored) {
            return List.of();
        }
        return list;
    }

    private Set<String> loadCatalogColumnKeys(String catalogId) {
        Set<String> keys = new LinkedHashSet<>();
        String cid = catalogId == null || catalogId.isBlank() ? DEFAULT_CATALOG_ID : catalogId.trim();
        try {
            HqNotifyMappingConfig c = getOrCreate();
            JsonNode eff = effectiveRootForLayout(c.getMappingJson());
            JsonNode catalog = findCatalog(eff, cid);
            if (catalog == null || !catalog.isObject()) {
                catalog = buildDefaultIntegratedCatalog();
            }
            JsonNode cols = catalog.get("columns");
            if (cols != null && cols.isArray()) {
                for (JsonNode col : cols) {
                    String k = col.path("key").asText("").trim();
                    if (!k.isEmpty() && col.path("visible").asBoolean(true)) {
                        keys.add(k);
                    }
                }
            }
        } catch (Exception ignored) {
            // ignore
        }
        keys.remove("_chk");
        keys.remove("payActions");
        return keys;
    }

    private static Optional<String> bestCatalogKeyForParam(String pgField, Set<String> catalogKeys, Set<String> usedTargets) {
        String norm = normalizeParamToken(pgField);
        if (norm.isEmpty()) {
            return Optional.empty();
        }
        String syn = synonymTarget(norm);
        if (syn != null && catalogKeys.contains(syn) && !usedTargets.contains(syn)) {
            return Optional.of(syn);
        }
        for (String k : catalogKeys) {
            if (usedTargets.contains(k)) {
                continue;
            }
            String nk = normalizeParamToken(k);
            if (norm.equals(nk)) {
                return Optional.of(k);
            }
        }
        for (String k : catalogKeys) {
            if (usedTargets.contains(k)) {
                continue;
            }
            String nk = normalizeParamToken(k);
            if (norm.contains(nk) || nk.contains(norm)) {
                return Optional.of(k);
            }
        }
        return Optional.empty();
    }

    private static String normalizeParamToken(String s) {
        if (s == null) {
            return "";
        }
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /** 흔한 PG 파라미터 → 카탈로그 internalKey (pay-list-integrated-default 기준) */
    private static String synonymTarget(String norm) {
        return switch (norm) {
            case "transactionid", "txnid", "txid", "pgtransactionid" -> "chillTransactionId";
            case "orderno", "order_id", "merchantorderno" -> "orderNo";
            case "amount", "amt", "payamt", "paymentamount", "totalamount" -> "chillAmount";
            case "routeno", "route", "rootno" -> "routeNo";
            case "paymentstatus", "paystatus" -> "chillPaymentStatus";
            case "status" -> "chillPaymentStatus";
            case "paymentchannel", "channel", "channelcode" -> "paymentChannel";
            case "customerid", "custid", "customer" -> "customerId";
            case "customername", "payername", "username" -> "customerNm";
            case "currency", "currencycode" -> "currency";
            case "fee" -> "chillFeeAmt";
            case "icopay" -> "icopayAmt";
            case "paymentdate", "paidat", "transactiondate" -> "payCompletedAt";
            case "approvalno", "authno", "apprno" -> "cardAprvNo";
            default -> null;
        };
    }

    private static String normalizeInternalKey(String k) {
        if (k == null) {
            return "";
        }
        String t = k.trim();
        if ("transactionId".equals(t)) {
            return "chillTransactionId";
        }
        return t;
    }

    private static String firstNonBlank(Map<String, String> m, String... keys) {
        for (String key : keys) {
            String v = m.get(key);
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    private static String firstNonBlankString(String... vals) {
        if (vals == null) {
            return null;
        }
        for (String v : vals) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    private static String extractMappedValue(JsonNode root, String pgFieldSpec) {
        if (pgFieldSpec == null || pgFieldSpec.isBlank()) {
            return null;
        }
        String[] parts = pgFieldSpec.split("\\s*[/|]\\s*");
        for (String part : parts) {
            String name = part.trim();
            if (name.isEmpty()) {
                continue;
            }
            String v = textDeep(root, name);
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    private static String textDeep(JsonNode root, String name) {
        String t = textAt(root, name);
        if (t != null) {
            return t;
        }
        JsonNode d = root.get("data");
        if (d != null && d.isObject()) {
            return textAt(d, name);
        }
        return null;
    }

    /**
     * 노티 JSON 루트·{@code data} 에서 통화 원문 추출(칠페이 노티 Currency/currency/CurrencyCode/currencyCode 와 동일 후보).
     */
    private static String extractNotifyCurrencyRaw(JsonNode notifyRoot) {
        if (notifyRoot == null || !notifyRoot.isObject()) {
            return null;
        }
        for (String n : new String[] { "Currency", "currency", "CurrencyCode", "currencyCode" }) {
            String v = textDeep(notifyRoot, n);
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    /** 노티 본문 PaymentStatus 계열(칠페이·노티미들웨어 공통 후보) — 표시·내부상태 해석에 사용 */
    private static String extractNotifyPaymentStatusText(JsonNode notifyRoot) {
        if (notifyRoot == null || !notifyRoot.isObject()) {
            return null;
        }
        for (String n : new String[] {
                "PaymentStatus", "paymentStatus", "Paymentstatus",
                "PayResult", "payResult", "TxnStatus", "txnStatus",
                "PaymentResult", "paymentResult", "PayStatus", "payStatus"
        }) {
            String v = textDeep(notifyRoot, n);
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    /** 노티 본문 Status 계열(숫자·코드가 올 수 있음) */
    private static String extractNotifyStatusCodeField(JsonNode notifyRoot) {
        if (notifyRoot == null || !notifyRoot.isObject()) {
            return null;
        }
        for (String n : new String[] {
                "Status", "status",
                "ResultCode", "resultCode", "RespCode", "respCode",
                "ResponseCode", "responseCode"
        }) {
            String v = textDeep(notifyRoot, n);
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    /**
     * 노티 본문에서 CustomerId 계열 — 매핑에 customerId 열이 없어도 결제관리 그리드 chillCustomer·고객명(결제자)에 반영.
     * {@link ChillPayNotifyToTrnsctnService} 와 동일 후보 키.
     */
    private static String extractNotifyCustomerIdRaw(JsonNode notifyRoot) {
        if (notifyRoot == null || !notifyRoot.isObject()) {
            return null;
        }
        for (String n : new String[] { "CustomerId", "customerId", "Customer", "customer" }) {
            String v = textDeep(notifyRoot, n);
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    /** 노티 본문 CustomerName 계열 — 고객명(결제자) 컬럼용 */
    private static String extractNotifyCustomerNameRaw(JsonNode notifyRoot) {
        if (notifyRoot == null || !notifyRoot.isObject()) {
            return null;
        }
        for (String n : new String[] { "CustomerName", "customerName", "PayerName", "payerName" }) {
            String v = textDeep(notifyRoot, n);
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return null;
    }

    /** PG 숫자 코드만 매핑된 경우 등: 저장용 chillPaymentStatus 는 사람이 읽을 문구(또는 칠페이 관용 표기)로 맞춤 */
    private static String resolveChillPaymentStatusForStorage(String mappedPs, String jsonPayStat, String internalStatus) {
        /* 내부는 승인(10)인데 PG 문구만 진행중(Processing 등)으로 온 레거시·매핑 역전 보정 */
        if ("10".equals(internalStatus)) {
            if (mappedPs != null && isAmbiguousProgressPaymentStatusToken(mappedPs)) {
                return chillDisplayEnglishFromInternal("10");
            }
            if (jsonPayStat != null && isAmbiguousProgressPaymentStatusToken(jsonPayStat)) {
                return chillDisplayEnglishFromInternal("10");
            }
        }
        if (mappedPs != null && !mappedPs.isBlank() && !isBareNumericStatusToken(mappedPs)) {
            return mappedPs;
        }
        if (jsonPayStat != null && !jsonPayStat.isBlank() && !isBareNumericStatusToken(jsonPayStat)) {
            return jsonPayStat;
        }
        if (mappedPs != null && !mappedPs.isBlank()) {
            return chillDisplayEnglishFromInternal(internalStatus);
        }
        if (jsonPayStat != null && !jsonPayStat.isBlank()) {
            return chillDisplayEnglishFromInternal(internalStatus);
        }
        return chillDisplayEnglishFromInternal(internalStatus);
    }

    private static boolean isBareNumericStatusToken(String s) {
        if (s == null) {
            return false;
        }
        return s.trim().matches("^\\d{1,3}$");
    }

    /** PG가 성공 후에도 Processing·Pending 등을 보내는 경우 — 저장·표시를 승인과 맞출 때 구분 */
    private static boolean isAmbiguousProgressPaymentStatusToken(String raw) {
        if (raw == null) {
            return false;
        }
        String u = raw.trim().toLowerCase(Locale.ROOT);
        return u.equals("processing") || u.equals("pending") || u.equals("request")
                || u.equals("waitauthorize") || u.equals("wait_authorize");
    }

    /** PayListItemDto.chillStatusLabel 폴백과 동일한 관용 표기(표시맵·한글 폴백과 연동) */
    private static String chillDisplayEnglishFromInternal(String internalStatus) {
        if (internalStatus == null) {
            return "WaitAuthorize";
        }
        return switch (internalStatus) {
            case "10" -> "Paid";
            case "08" -> "WaitAuthorize";
            case "20" -> "Cancelled";
            case "21" -> "Voided";
            case "22" -> "Manual void";
            case "30", "31" -> "Refunded";
            case "F0", "99" -> "Failed";
            default -> internalStatus;
        };
    }

    private static String textAt(JsonNode n, String name) {
        if (n == null || !n.isObject() || name == null) {
            return null;
        }
        JsonNode x = n.get(name);
        if (x == null || x.isNull()) {
            return null;
        }
        if (x.isTextual()) {
            String s = x.asText().trim();
            return s.isEmpty() ? null : s;
        }
        if (x.isNumber() || x.isBoolean()) {
            return x.asText();
        }
        return null;
    }

    private static Optional<BigDecimal> parseOptionalDecimal(String s) {
        if (s == null || s.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new BigDecimal(s.trim().replace(",", "")));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static LocalDateTime parsePaymentDate(String pd) {
        if (pd == null || pd.isBlank()) {
            return null;
        }
        String t = pd.trim();
        try {
            return LocalDateTime.parse(t, PAY_DD_MM);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(t, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
        }
        try {
            if (t.length() >= 10) {
                return LocalDateTime.parse(t.substring(0, 10) + "T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        } catch (DateTimeParseException ignored) {
        }
        try {
            if (t.matches("^\\d{14}$")) {
                return LocalDateTime.parse(t, DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.ROOT));
            }
        } catch (DateTimeParseException ignored) {
        }
        return null;
    }

    private static String newTrnId() {
        String u = UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
        return u.length() <= 20 ? u : u.substring(0, 20);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private record FieldMappingRow(String pgField, String internalKey) { }

    // ——— 결제내역 그리드 표시값 가공 (displayMaps) ———

    public DisplayTransformCache loadDisplayTransformCache() {
        return buildDisplayTransformCache(getOrCreate().getMappingJson());
    }

    public void applyDisplayTransform(DisplayTransformCache cache, String pgCd, Map<String, Object> row) {
        if (cache == null || row == null || row.isEmpty()) {
            return;
        }
        String pg = pgCd == null ? "" : pgCd.trim().toUpperCase(Locale.ROOT);
        Map<String, Map<String, String>> vm = cache.byVendorUpper().getOrDefault(pg, Map.of());
        LinkedHashSet<String> fieldKeys = new LinkedHashSet<>(vm.keySet());
        fieldKeys.add("currency");
        fieldKeys.add("chillPaymentStatus");
        for (String fk : fieldKeys) {
            if ("notifyChannelType".equals(fk)) {
                continue;
            }
            if (!row.containsKey(fk)) {
                continue;
            }
            Object val = row.get(fk);
            String raw = stringifyRowValueForDisplay(val);
            if (raw.isEmpty() || "-".equals(raw)) {
                continue;
            }
            Map<String, String> vendorField = vm.get(fk);
            String label;
            if ("currency".equals(fk)) {
                label = resolveCurrencyDisplayLabel(vendorField, raw);
            } else {
                label = lookupDisplayMap(vendorField, raw);
                if (label == null && "chillPaymentStatus".equals(fk)) {
                    label = lookupDisplayMap(EMBEDDED_CHILLPAY_STATUS_LABELS, raw);
                }
                if (label == null && "chillPaymentStatus".equals(fk)) {
                    label = lookupDisplayMap(INTERNAL_STATUS_CHILL_DISPLAY_KR, raw);
                }
            }
            if (label != null) {
                if ("currency".equals(fk)) {
                    label = normalizeCurrencyDisplayForGrid(label);
                }
                row.put(fk, label);
            }
        }
    }

    public DisplayTransformCache buildDisplayTransformCache(String mappingJson) {
        Map<String, Map<String, Map<String, String>>> byVendor = new LinkedHashMap<>();
        try {
            JsonNode root = objectMapper.readTree(mappingJson == null || mappingJson.isBlank() ? "{}" : mappingJson);
            JsonNode vendors = root.get("vendors");
            if (vendors != null && vendors.isArray()) {
                for (JsonNode v : vendors) {
                    String code = v.path("vendorCode").asText("").trim().toUpperCase(Locale.ROOT);
                    if (code.isEmpty()) {
                        continue;
                    }
                    Map<String, Map<String, String>> fields = parseDisplayMapsObject(v.get("displayMaps"));
                    if (PgVendor.CHILLPAY.equals(code)) {
                        Map<String, String> st = fields.get("chillPaymentStatus");
                        if (st == null || st.isEmpty()) {
                            Map<String, Map<String, String>> copy = new LinkedHashMap<>(fields);
                            copy.put("chillPaymentStatus", Map.copyOf(EMBEDDED_CHILLPAY_STATUS_LABELS));
                            fields = copy;
                        }
                    }
                    byVendor.put(code, Collections.unmodifiableMap(fields));
                }
            }
        } catch (Exception ignored) {
            return new DisplayTransformCache(Map.of());
        }
        return new DisplayTransformCache(Collections.unmodifiableMap(byVendor));
    }

    private static Map<String, Map<String, String>> parseDisplayMapsObject(JsonNode dm) {
        Map<String, Map<String, String>> fields = new LinkedHashMap<>();
        if (dm == null || !dm.isObject()) {
            return fields;
        }
        for (Iterator<Map.Entry<String, JsonNode>> it = dm.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> en = it.next();
            String fk = en.getKey();
            JsonNode obj = en.getValue();
            if (!obj.isObject()) {
                continue;
            }
            Map<String, String> m = new LinkedHashMap<>();
            for (Iterator<Map.Entry<String, JsonNode>> it2 = obj.fields(); it2.hasNext(); ) {
                Map.Entry<String, JsonNode> e2 = it2.next();
                m.put(e2.getKey(), e2.getValue().asText(""));
            }
            fields.put(fk, Collections.unmodifiableMap(m));
        }
        return fields;
    }

    private static String stringifyRowValueForDisplay(Object val) {
        if (val == null) {
            return "";
        }
        if (val instanceof Number n) {
            return n.toString();
        }
        return val.toString().trim();
    }

    private static String lookupDisplayMap(Map<String, String> map, String raw) {
        if (map == null || map.isEmpty() || raw == null) {
            return null;
        }
        String t = raw.trim();
        if (map.containsKey(t)) {
            return map.get(t);
        }
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (e.getKey() != null && e.getKey().equalsIgnoreCase(t)) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * PG {@code displayMaps.currency}를 먼저 조회하고, 없으면 숫자 코드 앞자리 0 제거 형태로 한 번 더 조회한 뒤
     * {@link #GLOBAL_CURRENCY_DISPLAY}로 폴백합니다.
     */
    private static String resolveCurrencyDisplayLabel(Map<String, String> vendorCurrency, String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return null;
        }
        String label = lookupDisplayMap(vendorCurrency, t);
        if (label != null) {
            return label;
        }
        String normDigits = normalizeIso4217NumericCode(t);
        if (normDigits != null) {
            label = lookupDisplayMap(vendorCurrency, normDigits);
            if (label != null) {
                return label;
            }
        }
        label = lookupDisplayMap(GLOBAL_CURRENCY_DISPLAY, t);
        if (label != null) {
            return label;
        }
        if (normDigits != null) {
            label = lookupDisplayMap(GLOBAL_CURRENCY_DISPLAY, normDigits);
        }
        return normalizeCurrencyDisplayForGrid(label);
    }

    /** 기존 저장값 {@code JPY (392)} 형태를 알파 코드만 남기도록 정규화(자유 표기는 유지). */
    private static String normalizeCurrencyDisplayForGrid(String label) {
        if (label == null) {
            return null;
        }
        String s = label.trim();
        if (s.matches("^[A-Za-z]+\\s+\\(\\d+\\)$")) {
            return s.replaceFirst("\\s+\\(\\d+\\)$", "").trim().toUpperCase(Locale.ROOT);
        }
        return s;
    }

    /** 순수 숫자 통화 코드에서 선행 0을 제거한 값(원본과 다를 때만). 예: {@code "0392"} → {@code "392"}. */
    private static String normalizeIso4217NumericCode(String trimmedRaw) {
        if (trimmedRaw == null || trimmedRaw.isEmpty() || !trimmedRaw.matches("\\d+")) {
            return null;
        }
        String stripped = trimmedRaw.replaceFirst("^0+(?!$)", "");
        return stripped.equals(trimmedRaw) ? null : stripped;
    }
}
