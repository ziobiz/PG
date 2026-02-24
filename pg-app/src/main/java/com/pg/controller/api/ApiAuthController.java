package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.api.dto.LoginRequest;
import com.pg.api.dto.LoginResponse;
import com.pg.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    private final AuthService authService;

    public ApiAuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest req) {
        if (req == null || req.getUsername() == null || req.getPassword() == null) {
            return ResponseEntity.ok(ApiResponse.fail("아이디와 비밀번호를 입력하세요.", "INVALID_INPUT"));
        }
        return authService.login(req.getUsername().trim(), req.getPassword())
                .map(res -> ResponseEntity.ok(ApiResponse.ok(res)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.fail("아이디 또는 비밀번호가 올바르지 않습니다.", "AUTH_FAIL")));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Object>> me() {
        return ResponseEntity.ok(ApiResponse.ok(java.util.Map.of("ok", true)));
    }
}
