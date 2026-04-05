package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.service.HqNotifyMappingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 본사설정 — 노티매핑 (PG CALLBACK/RESULT → 전산 화면·필드)
 */
@RestController
@RequestMapping(value = "/api/hq/notifyMapping", produces = "application/json")
public class ApiHqNotifyMappingController {

    private final HqNotifyMappingService hqNotifyMappingService;

    public ApiHqNotifyMappingController(HqNotifyMappingService hqNotifyMappingService) {
        this.hqNotifyMappingService = hqNotifyMappingService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> get() {
        var c = hqNotifyMappingService.getOrCreate();
        return ResponseEntity.ok(ApiResponse.ok(hqNotifyMappingService.toMap(c)));
    }

    /** 편집기용: 결제내역 기본 columnCatalogs·pageCatalogAssignments (v1 JSON 보강 등) */
    @GetMapping("/defaults")
    public ResponseEntity<ApiResponse<Map<String, Object>>> defaults() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("columnCatalogs", hqNotifyMappingService.exportDefaultColumnCatalogs());
        m.put("pageCatalogAssignments", hqNotifyMappingService.exportDefaultPageAssignments());
        return ResponseEntity.ok(ApiResponse.ok(m));
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> save(@RequestBody Map<String, Object> body) {
        try {
            var c = hqNotifyMappingService.saveFromBody(body);
            return ResponseEntity.ok(ApiResponse.ok(hqNotifyMappingService.toMap(c)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    /**
     * 샘플 노티 JSON 또는 파라미터 이름 목록으로 PG 필드 → 카탈로그 열 key 자동 제안.
     * body: vendorCode?, catalogId?, sampleJson?, paramNames?, useAi? (기본 true — API 키 없으면 규칙만)
     */
    @PostMapping("/suggest")
    public ResponseEntity<ApiResponse<Map<String, Object>>> suggest(@RequestBody Map<String, Object> body) {
        try {
            String vendor = body != null && body.get("vendorCode") != null ? String.valueOf(body.get("vendorCode")) : "";
            String catalogId = body != null && body.get("catalogId") != null ? String.valueOf(body.get("catalogId")) : HqNotifyMappingService.DEFAULT_CATALOG_ID;
            String sampleJson = body != null && body.get("sampleJson") != null ? String.valueOf(body.get("sampleJson")) : "";
            @SuppressWarnings("unchecked")
            List<String> paramNames = body != null && body.get("paramNames") instanceof List
                    ? (List<String>) body.get("paramNames") : null;
            if (paramNames == null || paramNames.isEmpty()) {
                paramNames = hqNotifyMappingService.collectJsonParamNames(sampleJson);
            }
            Object useAiRaw = body != null ? body.get("useAi") : null;
            boolean preferAi = useAiRaw == null || Boolean.parseBoolean(String.valueOf(useAiRaw));
            Map<String, Object> m = hqNotifyMappingService.suggestFieldMappingsAiThenHeuristic(
                    vendor, catalogId, paramNames, sampleJson, preferAi);
            return ResponseEntity.ok(ApiResponse.ok(m));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        }
    }

    /** OpenAI(호환) API 키 설정 여부 — UI에서 AI 버튼 안내용 */
    @GetMapping("/aiStatus")
    public ResponseEntity<ApiResponse<Map<String, Object>>> aiStatus() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("aiConfigured", hqNotifyMappingService.isNotifyMappingAiConfigured());
        return ResponseEntity.ok(ApiResponse.ok(m));
    }
}
