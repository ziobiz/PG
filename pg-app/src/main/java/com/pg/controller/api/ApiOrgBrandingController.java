package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.entity.AppUser;
import com.pg.entity.OrgBranding;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgBrandingRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.util.FaviconImageUtil;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 본사/총판 브랜딩 API
 * - GET /api/public/org/branding?compId=XXX : 로그인 페이지용 (인증 불필요)
 * - GET /api/org/branding?compId=XXX : 대시보드용
 * - POST /api/org/branding/upload : 이미지 업로드
 * - POST /api/org/branding/save : 테마 등 저장
 */
@RestController
@RequestMapping(value = "/api/org/branding", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiOrgBrandingController {

    private static final long MAIN_IMAGE_MAX_BYTES = 5 * 1024 * 1024;  // 5MB
    private static final long LOGO_IMAGE_MAX_BYTES = 1 * 1024 * 1024;  // 1MB
    private static final long POPCON_IMAGE_MAX_BYTES = 1 * 1024 * 1024;  // 1MB (UI 표기는 파비콘)
    private static final long FIRST_LOGO_IMAGE_MAX_BYTES = 1 * 1024 * 1024;  // 1MB

    private final OrgBrandingRepository brandingRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public ApiOrgBrandingController(OrgBrandingRepository brandingRepository, OrgUnitRepository orgUnitRepository,
                                    MerchantProfileRepository merchantProfileRepository) {
        this.brandingRepository = brandingRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> get(@RequestParam(required = false) String compId) {
        return getBranding(compId);
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> getBranding(String compId) {
        if (compId == null || compId.trim().isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("mainImageUrl", "");
            empty.put("logoImageUrl", "");
            empty.put("firstLogoImageUrl", "");
            empty.put("popconImageUrl", "");
            empty.put("theme", "DEFAULT");
            empty.put("brandHost", "");
            empty.put("siteName", "");
            return ResponseEntity.ok(ApiResponse.ok(empty));
        }
        return orgUnitRepository.findByCode(compId.trim())
                .filter(ou -> ou.getOrgLevel() == OrgLevel.HEADQUARTERS
                        || ou.getOrgLevel() == OrgLevel.REGIONAL
                        || ou.getOrgLevel() == OrgLevel.MASTER_DIST)
                .flatMap(ou -> brandingRepository.findByOrgUnitId(ou.getId())
                        .map(b -> {
                            Map<String, Object> m = new LinkedHashMap<>();
                            m.put("compId", compId);
                            m.put("mainImageUrl", b.getMainImageUrl() != null ? b.getMainImageUrl() : "");
                            m.put("logoImageUrl", b.getLogoImageUrl() != null ? b.getLogoImageUrl() : "");
                            m.put("firstLogoImageUrl", b.getFirstLogoImageUrl() != null ? b.getFirstLogoImageUrl() : "");
                            m.put("popconImageUrl", b.getPopconImageUrl() != null ? b.getPopconImageUrl() : "");
                            m.put("theme", b.getTheme() != null ? b.getTheme() : "DEFAULT");
                            m.put("brandHost", b.getBrandHost() != null ? b.getBrandHost() : "");
                            m.put("siteName", b.getSiteName() != null ? b.getSiteName() : "");
                            return m;
                        }))
                .map(ApiResponse::ok)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    Map<String, Object> empty = new LinkedHashMap<>();
                    empty.put("mainImageUrl", "");
                    empty.put("logoImageUrl", "");
                    empty.put("firstLogoImageUrl", "");
                    empty.put("popconImageUrl", "");
                    empty.put("theme", "DEFAULT");
                    empty.put("brandHost", "");
                    empty.put("siteName", "");
                    return ResponseEntity.ok(ApiResponse.ok(empty));
                });
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, Object>>> upload(
            @RequestParam String compId,
            @RequestParam String imageType,
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("파일을 선택하세요.", "EMPTY"));
        }
        Optional<OrgUnit> ouOpt = orgUnitRepository.findByCode(compId.trim());
        if (ouOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("업체를 찾을 수 없습니다.", "NOT_FOUND"));
        }
        OrgUnit ou = ouOpt.get();
        if (ou.getOrgLevel() != OrgLevel.HEADQUARTERS && ou.getOrgLevel() != OrgLevel.REGIONAL && ou.getOrgLevel() != OrgLevel.MASTER_DIST) {
            return ResponseEntity.ok(ApiResponse.fail("총본사, 본사 또는 총판만 브랜딩을 설정할 수 있습니다.", "FORBIDDEN"));
        }
        if (!isBrandingEditable(ou)) {
            return ResponseEntity.ok(ApiResponse.fail("브랜딩(배경/로고) 변경권한이 없습니다.", "FORBIDDEN"));
        }
        if (!"main".equals(imageType) && !"logo".equals(imageType) && !"first".equals(imageType) && !"popcon".equals(imageType)) {
            return ResponseEntity.ok(ApiResponse.fail("imageType은 main, logo, first 또는 popcon이어야 합니다.", "INVALID"));
        }
        long maxBytes = "main".equals(imageType)
                ? MAIN_IMAGE_MAX_BYTES
                : ("popcon".equals(imageType)
                    ? POPCON_IMAGE_MAX_BYTES
                    : ("first".equals(imageType) ? FIRST_LOGO_IMAGE_MAX_BYTES : LOGO_IMAGE_MAX_BYTES));
        if (file.getSize() > maxBytes) {
            String sizeMsg = "메인이미지는 5MB 이하여야 합니다.";
            if ("logo".equals(imageType)) sizeMsg = "로고이미지는 1MB 이하여야 합니다.";
            if ("first".equals(imageType)) sizeMsg = "첫화면 로고이미지는 1MB 이하여야 합니다.";
            if ("popcon".equals(imageType)) sizeMsg = "파비콘 이미지는 1MB 이하여야 합니다.";
            return ResponseEntity.ok(ApiResponse.fail(
                    sizeMsg,
                    "SIZE_EXCEEDED"));
        }
        String ext = getExtension(file.getOriginalFilename());
        if (ext == null || (!ext.equalsIgnoreCase("png") && !ext.equalsIgnoreCase("jpg") && !ext.equalsIgnoreCase("jpeg"))) {
            return ResponseEntity.ok(ApiResponse.fail("PNG 또는 JPG 파일만 업로드 가능합니다.", "INVALID_TYPE"));
        }
        try {
            Path basePath = Paths.get(System.getProperty("user.dir"), uploadDir, "org", compId.trim()).normalize();
            Files.createDirectories(basePath);
            String storedExt = "popcon".equals(imageType) ? "png" : ext.toLowerCase();
            String fileName = imageType + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + storedExt;
            Path targetPath = basePath.resolve(fileName);
            if ("popcon".equals(imageType)) {
                FaviconImageUtil.saveMultipartAsFaviconPng32(file, targetPath);
            } else {
                Files.copy(file.getInputStream(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            String url = "/uploads/org/" + compId.trim() + "/" + fileName;

            OrgBranding b = brandingRepository.findByOrgUnitId(ou.getId())
                    .orElseGet(() -> {
                        OrgBranding nb = new OrgBranding();
                        nb.setOrgUnitId(ou.getId());
                        return nb;
                    });
            if ("main".equals(imageType)) {
                b.setMainImageUrl(url);
            } else if ("logo".equals(imageType)) {
                b.setLogoImageUrl(url);
            } else if ("first".equals(imageType)) {
                b.setFirstLogoImageUrl(url);
            } else {
                b.setPopconImageUrl(url);
            }
            brandingRepository.save(b);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("url", url);
            payload.put("imageType", imageType);
            payload.put("originalFileName", sanitizeUploadOriginalName(file.getOriginalFilename()));
            payload.put("storedFileName", fileName);
            return ResponseEntity.ok(ApiResponse.ok(payload));
        } catch (IOException e) {
            return ResponseEntity.ok(ApiResponse.fail("파일 저장 실패: " + e.getMessage(), "IO_ERROR"));
        }
    }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> save(
            @RequestParam String compId,
            @RequestParam(required = false) String theme,
            @RequestParam(required = false) String brandHost,
            @RequestParam(required = false) String siteName) {
        Optional<OrgUnit> ouOpt = orgUnitRepository.findByCode(compId.trim());
        if (ouOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("업체를 찾을 수 없습니다.", "NOT_FOUND"));
        }
        OrgUnit ou = ouOpt.get();
        if (ou.getOrgLevel() != OrgLevel.HEADQUARTERS && ou.getOrgLevel() != OrgLevel.REGIONAL && ou.getOrgLevel() != OrgLevel.MASTER_DIST) {
            return ResponseEntity.ok(ApiResponse.fail("총본사, 본사 또는 총판만 브랜딩을 설정할 수 있습니다.", "FORBIDDEN"));
        }
        if (!isBrandingEditable(ou)) {
            return ResponseEntity.ok(ApiResponse.fail("브랜딩(배경/로고) 변경권한이 없습니다.", "FORBIDDEN"));
        }
        String themeVal = (theme != null && !theme.trim().isEmpty()) ? theme.trim().toUpperCase() : "DEFAULT";
        if (!themeVal.matches("DEFAULT|LIGHT|GRAY|BROWN|DARK|PASTEL_1|PASTEL_2|PASTEL_3|PASTEL_4|PASTEL_5")) {
            themeVal = "DEFAULT";
        }
        OrgBranding b = brandingRepository.findByOrgUnitId(ou.getId())
                .orElseGet(() -> {
                    OrgBranding nb = new OrgBranding();
                    nb.setOrgUnitId(ou.getId());
                    return nb;
                });
        b.setTheme(themeVal);
        if (brandHost != null) {
            b.setBrandHost(brandHost.isBlank() ? null : brandHost.trim());
        }
        if (siteName != null) {
            String s = siteName.trim();
            if (s.length() > 100) s = s.substring(0, 100);
            b.setSiteName(s.isBlank() ? null : s);
        }
        brandingRepository.save(b);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("theme", themeVal);
        out.put("brandHost", b.getBrandHost() != null ? b.getBrandHost() : "");
        out.put("siteName", b.getSiteName() != null ? b.getSiteName() : "");
        return ResponseEntity.ok(ApiResponse.ok(out));
    }

    @PostMapping("/delete-image")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteImage(
            @RequestParam String compId,
            @RequestParam String imageType) {
        Optional<OrgUnit> ouOpt = orgUnitRepository.findByCode(compId.trim());
        if (ouOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("업체를 찾을 수 없습니다.", "NOT_FOUND"));
        }
        OrgUnit ou = ouOpt.get();
        if (ou.getOrgLevel() != OrgLevel.HEADQUARTERS && ou.getOrgLevel() != OrgLevel.REGIONAL && ou.getOrgLevel() != OrgLevel.MASTER_DIST) {
            return ResponseEntity.ok(ApiResponse.fail("총본사, 본사 또는 총판만 브랜딩을 설정할 수 있습니다.", "FORBIDDEN"));
        }
        if (!isBrandingEditable(ou)) {
            return ResponseEntity.ok(ApiResponse.fail("브랜딩(배경/로고) 변경권한이 없습니다.", "FORBIDDEN"));
        }
        if (!"main".equals(imageType) && !"logo".equals(imageType) && !"first".equals(imageType) && !"popcon".equals(imageType)) {
            return ResponseEntity.ok(ApiResponse.fail("imageType은 main, logo, first 또는 popcon이어야 합니다.", "INVALID"));
        }
        OrgBranding b = brandingRepository.findByOrgUnitId(ou.getId())
                .orElseGet(() -> {
                    OrgBranding nb = new OrgBranding();
                    nb.setOrgUnitId(ou.getId());
                    return nb;
                });
        String oldUrl;
        if ("main".equals(imageType)) {
            oldUrl = b.getMainImageUrl();
            b.setMainImageUrl(null);
        } else if ("logo".equals(imageType)) {
            oldUrl = b.getLogoImageUrl();
            b.setLogoImageUrl(null);
        } else if ("first".equals(imageType)) {
            oldUrl = b.getFirstLogoImageUrl();
            b.setFirstLogoImageUrl(null);
        } else {
            oldUrl = b.getPopconImageUrl();
            b.setPopconImageUrl(null);
        }
        brandingRepository.save(b);
        deleteUploadedFileIfManaged(compId.trim(), oldUrl);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("imageType", imageType);
        out.put("deleted", true);
        return ResponseEntity.ok(ApiResponse.ok(out));
    }

    private static String getExtension(String filename) {
        if (filename == null || filename.isEmpty()) return null;
        int i = filename.lastIndexOf('.');
        return i > 0 ? filename.substring(i + 1) : null;
    }

    /** 업로드 응답·UI 표시용: 경로 제거·길이 제한 */
    private static String sanitizeUploadOriginalName(String name) {
        if (name == null) return "";
        String s = name.trim().replace('\\', '/');
        int slash = s.lastIndexOf('/');
        if (slash >= 0) {
            s = s.substring(slash + 1);
        }
        if (s.length() > 200) {
            s = s.substring(0, 200);
        }
        return s;
    }

    private void deleteUploadedFileIfManaged(String compId, String oldUrl) {
        if (oldUrl == null || oldUrl.isBlank() || compId == null || compId.isBlank()) return;
        String prefix = "/uploads/org/" + compId + "/";
        if (!oldUrl.startsWith(prefix)) return;
        String fileName = oldUrl.substring(prefix.length()).trim();
        if (fileName.isBlank() || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) return;
        try {
            Path filePath = Paths.get(System.getProperty("user.dir"), uploadDir, "org", compId, fileName).normalize();
            Files.deleteIfExists(filePath);
        } catch (Exception ignored) {
            // DB 값 제거가 우선이며 파일 삭제 실패는 무시한다.
        }
    }

    private boolean isBrandingEditable(OrgUnit ou) {
        if (ou == null) return false;
        if (ou != null && (ou.getOrgLevel() == OrgLevel.HEADQUARTERS
                || ou.getOrgLevel() == OrgLevel.REGIONAL
                || ou.getOrgLevel() == OrgLevel.MASTER_DIST)) {
            return true;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUser u && "ADMIN".equalsIgnoreCase(u.getRole())) {
            return true;
        }
        return merchantProfileRepository.findByOrgUnitId(ou.getId()).map(mp -> {
            String rs = mp.getRegionalSettings();
            if (rs == null || rs.isBlank()) return false;
            try {
                com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
                java.util.Map<String, Object> m = om.readValue(rs, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
                Object v = m.get("brandingEditAllowedYn");
                return v != null && "Y".equalsIgnoreCase(String.valueOf(v));
            } catch (Exception e) {
                return false;
            }
        }).orElse(false);
    }
}
