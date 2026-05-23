package com.pg.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 가맹점 연동 샘플 README — {@code text/plain} 을 UTF-8 charset 으로 명시해 브라우저·Windows 메모장에서 한글이 깨지지 않게 합니다.
 */
@RestController
public class MerchantApiSamplesController {

    private static final String README_CLASSPATH = "static/merchant-api-samples/README.txt";

    @GetMapping(value = "/merchant-api-samples/README.txt", produces = "text/plain;charset=UTF-8")
    public ResponseEntity<String> readme() throws IOException {
        ClassPathResource resource = new ClassPathResource(README_CLASSPATH);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        String body = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                .body(body);
    }
}
