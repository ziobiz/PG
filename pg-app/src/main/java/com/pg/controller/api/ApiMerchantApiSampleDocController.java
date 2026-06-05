package com.pg.controller.api;

import com.pg.merchantdeploy.MerchantApiSampleDocService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자·가맹 API 포털 — 연동 샘플 문서를 인증 API로 제공 (브라우저 직접 URL·iframe 세션 이슈 방지).
 */
@RestController
@RequestMapping("/api/merchant-api-samples")
public class ApiMerchantApiSampleDocController {

    private final MerchantApiSampleDocService sampleDocService;

    public ApiMerchantApiSampleDocController(MerchantApiSampleDocService sampleDocService) {
        this.sampleDocService = sampleDocService;
    }

    @GetMapping("/doc")
    public ResponseEntity<String> doc(@RequestParam("path") String path) {
        return sampleDocService.load(path)
                .map(doc -> ResponseEntity.ok()
                        .contentType(doc.contentType())
                        .body(doc.body()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
