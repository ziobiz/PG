package com.pg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pg.entity.HqNotifyMappingConfig;
import com.pg.repository.HqNotifyMappingConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class HqNotifyMappingService {

    private final HqNotifyMappingConfigRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HqNotifyMappingService(HqNotifyMappingConfigRepository repository) {
        this.repository = repository;
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

    /** PG사 추가 시 vendors 배열에 동일 구조로 확장 */
    public String buildDefaultMappingJson() {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("version", 1);
            root.put("memo", "결제대행사가 늘어나면 vendors 배열에 vendorCode·channels·fieldMappings를 추가하세요.");
            ArrayNode vendors = root.putArray("vendors");

            vendors.add(buildVendor("CHILLPAY", "칠페이", true));
            vendors.add(buildVendor("JPAY", "제이페이", false));

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            return "{\"version\":1,\"vendors\":[],\"memo\":\"\"}";
        }
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

        return v;
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
        ch.put("targetPageUrl", targetUrl);
        ch.put("targetPageLabel", targetLabel);
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
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON 형식이 올바르지 않습니다: " + e.getMessage());
        }
    }
}
