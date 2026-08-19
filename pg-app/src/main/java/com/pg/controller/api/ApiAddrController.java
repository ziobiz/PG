package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.service.JapanZipLookupService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 업체등록 주소 — 일본 우편번호 검색.
 */
@RestController
@RequestMapping(value = "/api/addr", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiAddrController {

    private final JapanZipLookupService japanZipLookupService;

    public ApiAddrController(JapanZipLookupService japanZipLookupService) {
        this.japanZipLookupService = japanZipLookupService;
    }

    @GetMapping("/jp-zip")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> jpZip(
            @RequestParam(required = false) String zip) {
        try {
            List<Map<String, String>> list = japanZipLookupService.lookup(zip);
            return ResponseEntity.ok(ApiResponse.ok(list));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(
                    "일본 우편번호(7자리)를 입력한 뒤 검색하세요.", "JP_ZIP_INVALID"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.fail(
                    "일본 우편번호 조회에 실패했습니다.", "JP_ZIP_LOOKUP_FAILED"));
        }
    }
}
