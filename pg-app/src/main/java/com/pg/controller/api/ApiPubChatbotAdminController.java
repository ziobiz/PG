package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.entity.OrgUnit;
import com.pg.service.ChatbotAdminAuthService;
import com.pg.service.MerchantChatbotProductService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 공개 챗봇 — 챗봇 관리자 전용 로그인·상품 CRUD ({@link ChatbotAdminAuthService#TOKEN_HEADER}).
 */
@RestController
@RequestMapping(value = "/api/pub/chatbot/admin", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiPubChatbotAdminController {

    private static final long IMAGE_MAX_BYTES = 2 * 1024 * 1024;

    private final ChatbotAdminAuthService chatbotAdminAuthService;
    private final MerchantChatbotProductService productService;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public ApiPubChatbotAdminController(ChatbotAdminAuthService chatbotAdminAuthService,
                                        MerchantChatbotProductService productService) {
        this.chatbotAdminAuthService = chatbotAdminAuthService;
        this.productService = productService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@RequestBody Map<String, Object> body) {
        try {
            String compId = str(body != null ? body.get("compId") : null);
            String username = str(body != null ? body.get("username") : null);
            String password = body != null && body.get("password") != null ? String.valueOf(body.get("password")) : "";
            String totp = str(body != null ? body.get("totpCode") : null);
            if (totp == null && body != null && body.get("totp") != null) {
                totp = str(body.get("totp"));
            }
            Map<String, Object> data = chatbotAdminAuthService.login(compId, username, password,
                    totp != null ? totp : "");
            return ResponseEntity.ok(ApiResponse.ok(data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "AUTH_FAIL"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Boolean>> logout(HttpServletRequest request) {
        String t = resolveToken(request);
        chatbotAdminAuthService.logout(t);
        return ResponseEntity.ok(ApiResponse.ok(Boolean.TRUE));
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listProducts(
            @RequestParam String compId, HttpServletRequest request) {
        return sessionForMerchant(compId, request).map(session -> ResponseEntity.ok(
                        ApiResponse.ok(productService.listAllForOrg(session.orgUnitId()))))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.fail("인증이 필요합니다.", "UNAUTHORIZED")));
    }

    @PostMapping("/products/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveProduct(@RequestBody Map<String, Object> body,
                                                                        HttpServletRequest request) {
        Optional<ChatbotAdminAuthService.ValidSession> sess = validateBodyComp(body, request);
        if (sess.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("인증이 필요합니다.", "UNAUTHORIZED"));
        }
        try {
            body.put("compId", sess.get().compId());
            return ResponseEntity.ok(ApiResponse.ok(productService.saveRow(sess.get().orgUnitId(), body)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "INVALID"));
        }
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<ApiResponse<Boolean>> deleteProduct(@PathVariable Long id,
                                                             @RequestParam String compId,
                                                             HttpServletRequest request) {
        Optional<ChatbotAdminAuthService.ValidSession> session = sessionForMerchant(compId, request);
        if (session.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("인증이 필요합니다.", "UNAUTHORIZED"));
        }
        try {
            productService.deleteRow(session.get().orgUnitId(), id);
            return ResponseEntity.ok(ApiResponse.ok(Boolean.TRUE));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "INVALID"));
        }
    }

    @PostMapping(value = "/products/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> upload(@RequestParam String compId,
                                                                   @RequestParam(required = false) Long productId,
                                                                   @RequestParam("file") MultipartFile file,
                                                                   HttpServletRequest request) {
        Optional<ChatbotAdminAuthService.ValidSession> session = sessionForMerchant(compId, request);
        if (session.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("인증이 필요합니다.", "UNAUTHORIZED"));
        }
        if (file.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("파일을 선택하세요.", "EMPTY"));
        }
        if (file.getSize() > IMAGE_MAX_BYTES) {
            return ResponseEntity.ok(ApiResponse.fail("이미지는 2MB 이하여야 합니다.", "SIZE_EXCEEDED"));
        }
        String ext = extension(file.getOriginalFilename());
        if (ext == null || (!ext.equalsIgnoreCase("png") && !ext.equalsIgnoreCase("jpg")
                && !ext.equalsIgnoreCase("jpeg"))) {
            return ResponseEntity.ok(ApiResponse.fail("PNG 또는 JPG만 가능합니다.", "INVALID_TYPE"));
        }
        try {
            Path basePath = Paths.get(System.getProperty("user.dir"), uploadDir, "chatbot",
                    compId.trim()).normalize();
            Files.createDirectories(basePath);
            String fileName = "p" + (productId != null ? productId + "_" : "")
                    + UUID.randomUUID().toString().substring(0, 8)
                    + "." + ext.toLowerCase(Locale.ROOT);
            Path targetPath = basePath.resolve(fileName);
            Files.copy(file.getInputStream(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            String url = "/uploads/chatbot/" + compId.trim() + "/" + fileName;
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("url", url);
            payload.put("storedFileName", fileName);
            payload.put("originalFileName", sanitizeName(file.getOriginalFilename()));
            return ResponseEntity.ok(ApiResponse.ok(payload));
        } catch (IOException e) {
            return ResponseEntity.ok(ApiResponse.fail("파일 저장 실패: " + e.getMessage(), "IO_ERROR"));
        }
    }

    private Optional<ChatbotAdminAuthService.ValidSession> validateBodyComp(Map<String, Object> body,
                                                                            HttpServletRequest request) {
        if (body == null) {
            return Optional.empty();
        }
        String compId = str(body.get("compId"));
        return sessionForMerchant(compId, request);
    }

    private Optional<ChatbotAdminAuthService.ValidSession> sessionForMerchant(String compId, HttpServletRequest request) {
        String token = resolveToken(request);
        if (compId == null || compId.isBlank()) {
            return Optional.empty();
        }
        Optional<ChatbotAdminAuthService.ValidSession> v = chatbotAdminAuthService.validateToken(token);
        if (v.isEmpty()) {
            return Optional.empty();
        }
        if (!v.get().compId().trim().equalsIgnoreCase(compId.trim())) {
            return Optional.empty();
        }
        Optional<OrgUnit> ou = productService.requireMerchantOrgByCode(compId.trim());
        if (ou.isEmpty()) {
            return Optional.empty();
        }
        if (!productService.isChatbotPaymentOpenForMerchant(ou.get().getId())) {
            return Optional.empty();
        }
        return v;
    }

    private static String resolveToken(HttpServletRequest request) {
        String h = request.getHeader(ChatbotAdminAuthService.TOKEN_HEADER);
        if (h != null && !h.isBlank()) {
            return h.trim();
        }
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.regionMatches(true, 0, "Chatbot ", 0, 9)) {
            return auth.substring(9).trim();
        }
        return "";
    }

    private static String str(Object o) {
        if (o == null) {
            return null;
        }
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : s;
    }

    private static String extension(String name) {
        if (name == null) {
            return null;
        }
        int i = name.lastIndexOf('.');
        if (i < 0 || i >= name.length() - 1) {
            return null;
        }
        return name.substring(i + 1);
    }

    private static String sanitizeName(String name) {
        if (name == null) {
            return "";
        }
        String n = name.replace("..", "").trim();
        return n.length() > 200 ? n.substring(0, 200) : n;
    }
}
