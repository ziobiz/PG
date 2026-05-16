package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.service.LoginNoticePublicService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 로그인 페이지 공개 공지 — 인증 불필요. 단말 {@code Accept-Language} 에 맞춘 문구.
 */
@RestController
@RequestMapping(value = "/api/pub/login-notice", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiPubLoginNoticeController {

    private final LoginNoticePublicService loginNoticePublicService;

    public ApiPubLoginNoticeController(LoginNoticePublicService loginNoticePublicService) {
        this.loginNoticePublicService = loginNoticePublicService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> get(
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        Map<String, Object> payload = loginNoticePublicService.resolveForAcceptLanguage(acceptLanguage);
        return ResponseEntity.ok(ApiResponse.ok(payload));
    }
}
