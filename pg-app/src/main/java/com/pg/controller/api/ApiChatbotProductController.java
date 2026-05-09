package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.entity.AppUser;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.AuthService;
import com.pg.service.CompService;
import com.pg.service.MerchantChatbotProductService;
import com.pg.util.ChatbotMerchantAdminConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 가맹점 챗봇 상품 CRUD·이미지 업로드 (로그인·조직 범위).
 */
@RestController
@RequestMapping(value = "/api/chatbot/products", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiChatbotProductController {

    private static final long IMAGE_MAX_BYTES = 2 * 1024 * 1024;

    private final MerchantChatbotProductService productService;
    private final AuthService authService;
    private final CompService compService;
    private final OrgUnitRepository orgUnitRepository;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public ApiChatbotProductController(MerchantChatbotProductService productService,
                                      AuthService authService,
                                      CompService compService,
                                      OrgUnitRepository orgUnitRepository) {
        this.productService = productService;
        this.authService = authService;
        this.compService = compService;
        this.orgUnitRepository = orgUnitRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> list(
            @RequestParam(required = false) String compId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUser user)) {
            return ResponseEntity.ok(ApiResponse.fail("인증이 필요합니다.", "UNAUTHORIZED"));
        }
        String cid = compId != null ? compId.trim() : "";
        if (cid.isEmpty()) {
            if (!viewerCanListSubtreeChatbotProducts(user)) {
                return ResponseEntity.ok(ApiResponse.fail(
                        "가맹점 코드가 필요합니다.", "INVALID"));
            }
            List<Long> merchantIds = resolveSubtreeMerchantOrgIds(user);
            return ResponseEntity.ok(ApiResponse.ok(productService.listProductsForMerchantOrgIds(merchantIds)));
        }
        if (!canAccessComp(cid)) {
            return ResponseEntity.ok(ApiResponse.fail("권한이 없습니다.", "FORBIDDEN"));
        }
        Optional<OrgUnit> ou = productService.requireMerchantOrgByCode(cid);
        if (ou.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점 코드를 확인하세요.", "NOT_FOUND"));
        }
        if (!merchantMayUseChatbotProductCrud(user, cid)) {
            return ResponseEntity.ok(ApiResponse.fail(
                    "챗봇 상품관리는 업체 대표 또는 권한그룹 CHATBOT 계정만 사용할 수 있습니다.", "FORBIDDEN"));
        }
        return ResponseEntity.ok(ApiResponse.ok(productService.listAllForOrg(ou.get().getId())));
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> save(@RequestBody Map<String, Object> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUser user)) {
            return ResponseEntity.ok(ApiResponse.fail("인증이 필요합니다.", "UNAUTHORIZED"));
        }
        String compId = str(body != null ? body.get("compId") : null);
        if (compId == null) {
            return ResponseEntity.ok(ApiResponse.fail("compId가 필요합니다.", "INVALID"));
        }
        if (!canAccessComp(compId)) {
            return ResponseEntity.ok(ApiResponse.fail("권한이 없습니다.", "FORBIDDEN"));
        }
        if (!merchantMayUseChatbotProductCrud(user, compId)) {
            return ResponseEntity.ok(ApiResponse.fail(
                    "챗봇 상품관리는 업체 대표 또는 권한그룹 CHATBOT 계정만 사용할 수 있습니다.", "FORBIDDEN"));
        }
        Optional<OrgUnit> ou = productService.requireMerchantOrgByCode(compId);
        if (ou.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점만 상품을 등록할 수 있습니다.", "NOT_FOUND"));
        }
        try {
            boolean hqFields = viewerAllowsHqChatbotCatalogFields(user);
            return ResponseEntity.ok(ApiResponse.ok(productService.saveRow(ou.get().getId(), body, hqFields)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "INVALID"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> delete(@PathVariable Long id, @RequestParam String compId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUser user)) {
            return ResponseEntity.ok(ApiResponse.fail("인증이 필요합니다.", "UNAUTHORIZED"));
        }
        if (!canAccessComp(compId)) {
            return ResponseEntity.ok(ApiResponse.fail("권한이 없습니다.", "FORBIDDEN"));
        }
        if (!merchantMayUseChatbotProductCrud(user, compId)) {
            return ResponseEntity.ok(ApiResponse.fail(
                    "챗봇 상품관리는 업체 대표 또는 권한그룹 CHATBOT 계정만 사용할 수 있습니다.", "FORBIDDEN"));
        }
        Optional<OrgUnit> ou = productService.requireMerchantOrgByCode(compId);
        if (ou.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점 코드를 확인하세요.", "NOT_FOUND"));
        }
        try {
            productService.deleteRow(ou.get().getId(), id);
            return ResponseEntity.ok(ApiResponse.ok(Boolean.TRUE));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "INVALID"));
        }
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> upload(
            @RequestParam String compId,
            @RequestParam(required = false) Long productId,
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("파일을 선택하세요.", "EMPTY"));
        }
        if (!canAccessComp(compId)) {
            return ResponseEntity.ok(ApiResponse.fail("권한이 없습니다.", "FORBIDDEN"));
        }
        Authentication authUl = SecurityContextHolder.getContext().getAuthentication();
        if (authUl == null || !(authUl.getPrincipal() instanceof AppUser uploader)) {
            return ResponseEntity.ok(ApiResponse.fail("인증이 필요합니다.", "UNAUTHORIZED"));
        }
        if (!merchantMayUseChatbotProductCrud(uploader, compId)) {
            return ResponseEntity.ok(ApiResponse.fail(
                    "챗봇 상품관리는 업체 대표 또는 권한그룹 CHATBOT 계정만 사용할 수 있습니다.", "FORBIDDEN"));
        }
        Optional<OrgUnit> ou = productService.requireMerchantOrgByCode(compId);
        if (ou.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점만 업로드할 수 있습니다.", "NOT_FOUND"));
        }
        if (file.getSize() > IMAGE_MAX_BYTES) {
            return ResponseEntity.ok(ApiResponse.fail("이미지는 2MB 이하여야 합니다.", "SIZE_EXCEEDED"));
        }
        String ext = getExtension(file.getOriginalFilename());
        if (ext == null || (!ext.equalsIgnoreCase("png") && !ext.equalsIgnoreCase("jpg") && !ext.equalsIgnoreCase("jpeg"))) {
            return ResponseEntity.ok(ApiResponse.fail("PNG 또는 JPG만 가능합니다.", "INVALID_TYPE"));
        }
        try {
            Path basePath = Paths.get(System.getProperty("user.dir"), uploadDir, "chatbot", compId.trim()).normalize();
            Files.createDirectories(basePath);
            String fileName = "p" + (productId != null ? productId + "_" : "") + UUID.randomUUID().toString().substring(0, 8)
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

    /** 가맹(MERCHANT) 로그인은 업체 대표 또는 CHATBOT 권한그룹만 상품 API 사용. 그 외 단계·ADMIN 은 통과. */
    private boolean merchantMayUseChatbotProductCrud(AppUser user, String targetCompId) {
        if (user == null || targetCompId == null || targetCompId.isBlank()) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return true;
        }
        Map<String, Object> org = authService.getOrgInfo(user.getUsername());
        String ol = org != null && org.get("orgLevel") != null
                ? String.valueOf(org.get("orgLevel")).trim().toUpperCase(Locale.ROOT) : "";
        if (!"MERCHANT".equals(ol)) {
            return true;
        }
        String mine = org != null && org.get("compId") != null ? org.get("compId").toString().trim() : "";
        if (!mine.equalsIgnoreCase(targetCompId.trim())) {
            return false;
        }
        return ChatbotMerchantAdminConstants.merchantAdminWebMayUseChatbotFeatures(user);
    }

    private boolean viewerAllowsHqChatbotCatalogFields(AppUser user) {
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return true;
        }
        Map<String, Object> org = authService.getOrgInfo(user.getUsername());
        String ol = org != null && org.get("orgLevel") != null
                ? String.valueOf(org.get("orgLevel")).trim().toUpperCase(Locale.ROOT) : "";
        return !"MERCHANT".equals(ol);
    }

    private boolean viewerCanListSubtreeChatbotProducts(AppUser user) {
        return viewerAllowsHqChatbotCatalogFields(user);
    }

    /** ADMIN: 전체 가맹 / 그 외(가맹 제외 로그인): 산하 가맹 */
    private List<Long> resolveSubtreeMerchantOrgIds(AppUser user) {
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return orgUnitRepository.findByOrgLevelOrderByCodeAsc(OrgLevel.MERCHANT).stream()
                    .map(OrgUnit::getId)
                    .toList();
        }
        Map<String, Object> org = authService.getOrgInfo(user.getUsername());
        String mine = org != null && org.get("compId") != null ? org.get("compId").toString().trim() : "";
        if (mine.isEmpty()) {
            return List.of();
        }
        return compService.collectMerchantOrgUnitIdsInViewerSubtree(mine);
    }

    private boolean canAccessComp(String targetCompId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUser u)) {
            return false;
        }
        if ("ADMIN".equalsIgnoreCase(u.getRole())) {
            return true;
        }
        Map<String, Object> org = authService.getOrgInfo(u.getUsername());
        String mine = org != null && org.get("compId") != null ? org.get("compId").toString().trim() : "";
        if (mine.isEmpty() || targetCompId == null || targetCompId.isBlank()) {
            return false;
        }
        String target = targetCompId.trim();
        String ol = org != null && org.get("orgLevel") != null
                ? String.valueOf(org.get("orgLevel")).trim().toUpperCase(Locale.ROOT) : "";
        if ("MERCHANT".equals(ol)) {
            return mine.equalsIgnoreCase(target);
        }
        return compService.isTargetUnderViewerOrg(mine, target);
    }

    private static String str(Object o) {
        if (o == null) {
            return null;
        }
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : s;
    }

    private static String getExtension(String name) {
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
