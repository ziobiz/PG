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
import com.pg.util.LinkPreviewOgSupport;
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
    private static final long OG_IMAGE_MAX_BYTES = 1 * 1024 * 1024;

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
            return ResponseEntity.ok(ApiResponse.ok(emptyBranding(null)));
        }
        return orgUnitRepository.findByCode(compId.trim())
                .filter(ou -> ou.getOrgLevel() == OrgLevel.HEADQUARTERS
                        || ou.getOrgLevel() == OrgLevel.REGIONAL
                        || ou.getOrgLevel() == OrgLevel.MASTER_DIST)
                .map(ou -> {
                    OrgBranding b = brandingRepository.findByOrgUnitId(ou.getId()).orElse(null);
                    return toBrandingMap(compId, ou, b);
                })
                .map(ApiResponse::ok)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.ok(emptyBranding(null))));
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
        if (!"main".equals(imageType) && !"logo".equals(imageType) && !"first".equals(imageType) && !"popcon".equals(imageType)
                && !"urlPay".equals(imageType) && !"og".equals(imageType)) {
            return ResponseEntity.ok(ApiResponse.fail("imageType은 main, logo, first, popcon, urlPay 또는 og이어야 합니다.", "INVALID"));
        }
        long maxBytes = "main".equals(imageType)
                ? MAIN_IMAGE_MAX_BYTES
                : ("popcon".equals(imageType)
                    ? POPCON_IMAGE_MAX_BYTES
                    : ("first".equals(imageType) ? FIRST_LOGO_IMAGE_MAX_BYTES
                        : ("og".equals(imageType) ? OG_IMAGE_MAX_BYTES : LOGO_IMAGE_MAX_BYTES)));
        if (file.getSize() > maxBytes) {
            String sizeMsg = "메인이미지는 5MB 이하여야 합니다.";
            if ("logo".equals(imageType)) sizeMsg = "로고이미지는 1MB 이하여야 합니다.";
            if ("first".equals(imageType)) sizeMsg = "첫화면 로고이미지는 1MB 이하여야 합니다.";
            if ("popcon".equals(imageType)) sizeMsg = "파비콘 이미지는 1MB 이하여야 합니다.";
            if ("urlPay".equals(imageType)) sizeMsg = "URL결제 이미지는 1MB 이하여야 합니다.";
            if ("og".equals(imageType)) sizeMsg = "링크 미리보기 이미지는 1MB 이하여야 합니다.";
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
            } else if ("urlPay".equals(imageType)) {
                b.setUrlPayImageUrl(url);
            } else if ("og".equals(imageType)) {
                b.setOgImageUrl(url);
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
            @RequestParam(required = false) String siteName,
            @RequestParam(required = false) String ogMode,
            @RequestParam(required = false) String ogTitleKo,
            @RequestParam(required = false) String ogTitleEn,
            @RequestParam(required = false) String ogTitleJp,
            @RequestParam(required = false) String ogTitleCh,
            @RequestParam(required = false) String ogTitleTh,
            @RequestParam(required = false) String ogDescKo,
            @RequestParam(required = false) String ogDescEn,
            @RequestParam(required = false) String ogDescJp,
            @RequestParam(required = false) String ogDescCh,
            @RequestParam(required = false) String ogDescTh) {
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
        applyOgSave(b, ou.getOrgLevel(), ogMode, ogTitleKo, ogTitleEn, ogTitleJp, ogTitleCh, ogTitleTh,
                ogDescKo, ogDescEn, ogDescJp, ogDescCh, ogDescTh);
        brandingRepository.save(b);
        Map<String, Object> out = toBrandingMap(compId, ou, b);
        out.put("success", true);
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
        if (!"main".equals(imageType) && !"logo".equals(imageType) && !"first".equals(imageType) && !"popcon".equals(imageType)
                && !"urlPay".equals(imageType) && !"og".equals(imageType)) {
            return ResponseEntity.ok(ApiResponse.fail("imageType은 main, logo, first, popcon, urlPay 또는 og이어야 합니다.", "INVALID"));
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
        } else if ("urlPay".equals(imageType)) {
            oldUrl = b.getUrlPayImageUrl();
            b.setUrlPayImageUrl(null);
        } else if ("og".equals(imageType)) {
            oldUrl = b.getOgImageUrl();
            b.setOgImageUrl(null);
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

    private static Map<String, Object> emptyBranding(OrgLevel level) {
        Map<String, Object> empty = new LinkedHashMap<>();
        empty.put("mainImageUrl", "");
        empty.put("logoImageUrl", "");
        empty.put("firstLogoImageUrl", "");
        empty.put("popconImageUrl", "");
        empty.put("urlPayImageUrl", "");
        empty.put("theme", "DEFAULT");
        empty.put("brandHost", "");
        empty.put("siteName", "");
        putOgOnMap(empty, null, level);
        return empty;
    }

    private static Map<String, Object> toBrandingMap(String compId, OrgUnit ou, OrgBranding b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("compId", compId);
        m.put("mainImageUrl", b != null && b.getMainImageUrl() != null ? b.getMainImageUrl() : "");
        m.put("logoImageUrl", b != null && b.getLogoImageUrl() != null ? b.getLogoImageUrl() : "");
        m.put("firstLogoImageUrl", b != null && b.getFirstLogoImageUrl() != null ? b.getFirstLogoImageUrl() : "");
        m.put("popconImageUrl", b != null && b.getPopconImageUrl() != null ? b.getPopconImageUrl() : "");
        m.put("urlPayImageUrl", b != null && b.getUrlPayImageUrl() != null ? b.getUrlPayImageUrl() : "");
        m.put("theme", b != null && b.getTheme() != null ? b.getTheme() : "DEFAULT");
        m.put("brandHost", b != null && b.getBrandHost() != null ? b.getBrandHost() : "");
        m.put("siteName", b != null && b.getSiteName() != null ? b.getSiteName() : "");
        putOgOnMap(m, b, ou != null ? ou.getOrgLevel() : null);
        return m;
    }

    private static void putOgOnMap(Map<String, Object> m, OrgBranding b, OrgLevel level) {
        m.put("ogMode", LinkPreviewOgSupport.normalizeMode(b != null ? b.getOgMode() : null, level));
        Map<String, String> titles = LinkPreviewOgSupport.parseLangMap(b != null ? b.getOgTitleJson() : null);
        Map<String, String> descs = LinkPreviewOgSupport.parseLangMap(b != null ? b.getOgDescJson() : null);
        m.put("ogTitleKo", titles.getOrDefault("KO", ""));
        m.put("ogTitleEn", titles.getOrDefault("EN", ""));
        m.put("ogTitleJp", titles.getOrDefault("JP", ""));
        m.put("ogTitleCh", titles.getOrDefault("CH", ""));
        m.put("ogTitleTh", titles.getOrDefault("TH", ""));
        m.put("ogDescKo", descs.getOrDefault("KO", ""));
        m.put("ogDescEn", descs.getOrDefault("EN", ""));
        m.put("ogDescJp", descs.getOrDefault("JP", ""));
        m.put("ogDescCh", descs.getOrDefault("CH", ""));
        m.put("ogDescTh", descs.getOrDefault("TH", ""));
        m.put("ogImageUrl", b != null && b.getOgImageUrl() != null ? b.getOgImageUrl() : "");
    }

    private static void applyOgSave(OrgBranding b, OrgLevel level, String ogMode,
                                   String ogTitleKo, String ogTitleEn, String ogTitleJp, String ogTitleCh, String ogTitleTh,
                                   String ogDescKo, String ogDescEn, String ogDescJp, String ogDescCh, String ogDescTh) {
        if (ogMode != null || ogTitleKo != null || ogTitleEn != null || ogTitleJp != null || ogTitleCh != null || ogTitleTh != null
                || ogDescKo != null || ogDescEn != null || ogDescJp != null || ogDescCh != null || ogDescTh != null) {
            b.setOgMode(LinkPreviewOgSupport.normalizeMode(ogMode, level));
            Map<String, String> titles = LinkPreviewOgSupport.emptyLangMap();
            titles.put("KO", LinkPreviewOgSupport.clip(ogTitleKo, 200));
            titles.put("EN", LinkPreviewOgSupport.clip(ogTitleEn, 200));
            titles.put("JP", LinkPreviewOgSupport.clip(ogTitleJp, 200));
            titles.put("CH", LinkPreviewOgSupport.clip(ogTitleCh, 200));
            titles.put("TH", LinkPreviewOgSupport.clip(ogTitleTh, 200));
            Map<String, String> descs = LinkPreviewOgSupport.emptyLangMap();
            descs.put("KO", LinkPreviewOgSupport.clip(ogDescKo, 500));
            descs.put("EN", LinkPreviewOgSupport.clip(ogDescEn, 500));
            descs.put("JP", LinkPreviewOgSupport.clip(ogDescJp, 500));
            descs.put("CH", LinkPreviewOgSupport.clip(ogDescCh, 500));
            descs.put("TH", LinkPreviewOgSupport.clip(ogDescTh, 500));
            b.setOgTitleJson(LinkPreviewOgSupport.toJson(titles));
            b.setOgDescJson(LinkPreviewOgSupport.toJson(descs));
        } else if (level == OrgLevel.HEADQUARTERS && (b.getOgMode() == null || b.getOgMode().isBlank())) {
            b.setOgMode(LinkPreviewOgSupport.MODE_CUSTOM);
        }
    }
}
