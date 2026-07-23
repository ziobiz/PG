package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.entity.AppUser;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgBranding;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgBrandingRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.AuthService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 운영 메뉴얼 — 총본사 브랜딩 + 조직별 audience + 정식 PDF(API).
 * 관리자 정적 호스트에 manuals 폴더가 없어도 /api 로 PDF를 받을 수 있다.
 */
@RestController
@RequestMapping(value = "/api/hq/platformManuals", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiHqPlatformManualsController {

    private static final Pattern SAFE_ID = Pattern.compile("^[a-z0-9-]{3,64}$");
    private static final Set<String> LANGS = Set.of("ko", "en", "ja", "zh", "th");

    /** id → audience (프론트 ITEMS 와 동일) — 정식 PDF 카탈로그 */
    private static final Map<String, String> MANUAL_AUDIENCE = Map.ofEntries(
            Map.entry("super-ops", "super"),
            Map.entry("hq-ops", "hqdist"),
            Map.entry("dist-ops", "hqdist"),
            Map.entry("hqdist-risk-intro", "hqdist"),
            Map.entry("merchant-chatbot", "merchant"),
            Map.entry("merchant-ops", "merchant"),
            Map.entry("merchant-url-user", "merchant"),
            Map.entry("merchant-split-user", "merchant"),
            Map.entry("merchant-subscribe-user", "merchant"),
            Map.entry("hqdist-ops", "hqdist"),
            Map.entry("merchant-user", "merchant")
    );

    private static final Map<String, String> LEGACY_ID_ALIAS = Map.of(
            "hqdist-ops", "dist-ops",
            "merchant-user", "merchant-ops"
    );

    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final OrgBrandingRepository orgBrandingRepository;
    private final AuthService authService;

    public ApiHqPlatformManualsController(OrgUnitRepository orgUnitRepository,
                                          MerchantProfileRepository merchantProfileRepository,
                                          OrgBrandingRepository orgBrandingRepository,
                                          AuthService authService) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.orgBrandingRepository = orgBrandingRepository;
        this.authService = authService;
    }

    @GetMapping("/brand")
    public ResponseEntity<ApiResponse<Map<String, Object>>> brand(Authentication authentication) {
        OrgUnit hq = resolveHeadquarters();
        Map<String, Object> m = new LinkedHashMap<>();
        if (hq == null) {
            m.put("compId", "");
            m.put("compNm", "ICOPAY");
            m.put("siteName", "ICOPAY");
            m.put("logoImageUrl", "");
            m.put("firstLogoImageUrl", "");
            m.put("urlPayImageUrl", "");
            m.put("popconImageUrl", "");
            m.put("manualLogoImageUrl", "");
            m.put("addr", "");
            m.put("compTel", "");
            m.put("email", "");
            m.put("homepage", "");
            m.put("copyright", "");
        } else {
            m.put("compId", hq.getCode() != null ? hq.getCode() : "");
            m.put("compNm", hq.getName() != null ? hq.getName() : "ICOPAY");

            Optional<OrgBranding> brandOpt = orgBrandingRepository.findByOrgUnitId(hq.getId());
            if (brandOpt.isPresent()) {
                OrgBranding b = brandOpt.get();
                String sidebarLogo = nz(b.getLogoImageUrl());
                String firstLogo = nz(b.getFirstLogoImageUrl());
                String urlPayLogo = nz(b.getUrlPayImageUrl());
                String popcon = nz(b.getPopconImageUrl());
                m.put("logoImageUrl", sidebarLogo);
                m.put("firstLogoImageUrl", firstLogo);
                m.put("urlPayImageUrl", urlPayLogo);
                m.put("popconImageUrl", popcon);
                /* 매뉴얼 헤더: PDF와 같은 전체 브랜드 마크 — 사이드바용 작은 로고보다 첫화면/URL결제 로고 우선 */
                m.put("manualLogoImageUrl", firstNonBlank(firstLogo, urlPayLogo, sidebarLogo));
                m.put("siteName", !nz(b.getSiteName()).isEmpty() ? b.getSiteName().trim() : m.get("compNm"));
            } else {
                m.put("logoImageUrl", "");
                m.put("firstLogoImageUrl", "");
                m.put("urlPayImageUrl", "");
                m.put("popconImageUrl", "");
                m.put("manualLogoImageUrl", "");
                m.put("siteName", m.get("compNm"));
            }

            Optional<MerchantProfile> mpOpt = merchantProfileRepository.findByOrgUnitId(hq.getId());
            if (mpOpt.isPresent()) {
                MerchantProfile mp = mpOpt.get();
                String addr = joinAddr(mp.getZipCode(), mp.getAddr(), mp.getAddrDetail(), mp.getAddrEtc());
                m.put("addr", addr);
                m.put("compTel", nz(mp.getCompTel()));
                m.put("email", nz(mp.getEmail()));
                String home = nz(mp.getHomepage());
                if (home.isEmpty()) {
                    home = nz(mp.getSiteUrl());
                }
                m.put("homepage", home);
                m.put("copyright", "");
            } else {
                m.put("addr", "");
                m.put("compTel", "");
                m.put("email", "");
                m.put("homepage", "");
                m.put("copyright", "");
            }
        }

        OrgLevel viewerLevel = resolveViewerOrgLevel(authentication);
        if (viewerLevel != null) {
            m.put("viewerOrgLevel", viewerLevel.name());
            m.put("viewerOrgLevelNm", viewerLevel.getNameKo());
            m.put("viewerOrgLevelCode", viewerLevel.getCode());
        } else {
            m.put("viewerOrgLevel", "HEADQUARTERS");
            m.put("viewerOrgLevelNm", OrgLevel.HEADQUARTERS.getNameKo());
            m.put("viewerOrgLevelCode", OrgLevel.HEADQUARTERS.getCode());
            viewerLevel = OrgLevel.HEADQUARTERS;
        }
        m.put("allowedAudiences", allowedAudiences(viewerLevel));
        return ResponseEntity.ok(ApiResponse.ok(m));
    }

    /**
     * 정식 매뉴얼 PDF. 인증·audience 검사 후 classpath PDF 바이트를 반환.
     */
    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<?> pdf(
            @RequestParam("id") String idRaw,
            @RequestParam(value = "lang", defaultValue = "ko") String langRaw,
            Authentication authentication) {
        ManualKey key = resolveManualKey(idRaw, langRaw);
        if (key == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.fail("알 수 없는 매뉴얼입니다."));
        }
        OrgLevel viewerLevel = resolveViewerOrgLevel(authentication);
        List<String> allowed = allowedAudiences(viewerLevel);
        if (!allowed.contains(key.audience())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.fail("이 조직 단계에서는 해당 매뉴얼을 열 수 없습니다."));
        }
        byte[] bytes;
        String resolvedLang;
        try {
            PdfBytes pdf = readManualPdf(key.id(), key.lang());
            bytes = pdf.bytes();
            resolvedLang = pdf.lang();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.fail("매뉴얼 PDF를 찾을 수 없습니다."));
        }
        if (bytes == null || bytes.length < 8 || !isPdfMagic(bytes)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.fail("매뉴얼 본문이 올바르지 않습니다."));
        }
        String filename = key.id() + "-" + resolvedLang + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .header("X-Manual-Id", key.id())
                .header("X-Manual-Lang", resolvedLang)
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
    }

    /**
     * PDF 표지에 들어간 브랜드 로고(PNG). 사이드바용 작은 로고 대신 표지형 마크.
     */
    @GetMapping(value = "/coverLogo", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<?> coverLogo(
            @RequestParam("id") String idRaw,
            Authentication authentication) {
        ManualKey key = resolveManualKey(idRaw, "ko");
        if (key == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.fail("알 수 없는 매뉴얼입니다."));
        }
        OrgLevel viewerLevel = resolveViewerOrgLevel(authentication);
        List<String> allowed = allowedAudiences(viewerLevel);
        if (!allowed.contains(key.audience())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.fail("이 조직 단계에서는 해당 매뉴얼을 열 수 없습니다."));
        }
        byte[] bytes;
        try {
            bytes = readClasspathBytes(
                    "static/manuals/pdf/" + key.id() + "/logo.png",
                    "manuals/pdf/" + key.id() + "/logo.png");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.fail("커버 로고를 찾을 수 없습니다."));
        }
        if (bytes == null || bytes.length < 24) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ApiResponse.fail("커버 로고를 찾을 수 없습니다."));
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(bytes);
    }

    /**
     * 레거시 HTML 스텁(있을 때만). 신규 UI는 /pdf 사용.
     */
    @GetMapping("/content")
    public ResponseEntity<ApiResponse<Map<String, Object>>> content(
            @RequestParam("id") String idRaw,
            @RequestParam(value = "lang", defaultValue = "ko") String langRaw,
            Authentication authentication) {
        ManualKey key = resolveManualKey(idRaw, langRaw);
        if (key == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.fail("알 수 없는 매뉴얼입니다."));
        }
        OrgLevel viewerLevel = resolveViewerOrgLevel(authentication);
        List<String> allowed = allowedAudiences(viewerLevel);
        if (!allowed.contains(key.audience())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.fail("이 조직 단계에서는 해당 매뉴얼을 열 수 없습니다."));
        }
        String html;
        try {
            html = readManualHtml(key.id(), key.lang());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("매뉴얼 파일을 찾을 수 없습니다. PDF API(/pdf)를 사용하세요."));
        }
        if (html == null || html.isBlank() || looksLikeAdminShell(html)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("매뉴얼 본문이 올바르지 않습니다."));
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", key.id());
        m.put("lang", key.lang());
        m.put("audience", key.audience());
        m.put("html", html);
        m.put("format", "html");
        return ResponseEntity.ok(ApiResponse.ok(m));
    }

    private record ManualKey(String id, String lang, String audience) {}

    private record PdfBytes(byte[] bytes, String lang) {}

    private static ManualKey resolveManualKey(String idRaw, String langRaw) {
        String id = idRaw == null ? "" : idRaw.trim().toLowerCase(Locale.ROOT);
        String lang = normalizeLang(langRaw);
        if (!SAFE_ID.matcher(id).matches() || !MANUAL_AUDIENCE.containsKey(id)) {
            return null;
        }
        if (LEGACY_ID_ALIAS.containsKey(id)) {
            id = LEGACY_ID_ALIAS.get(id);
        }
        String audience = MANUAL_AUDIENCE.get(id);
        if (audience == null) {
            return null;
        }
        return new ManualKey(id, lang, audience);
    }

    private static String normalizeLang(String langRaw) {
        String lang = langRaw == null ? "ko" : langRaw.trim().toLowerCase(Locale.ROOT);
        if ("jp".equals(lang) || "ja".equals(lang)) {
            return "ja";
        }
        if ("ch".equals(lang) || "zh".equals(lang)) {
            return "zh";
        }
        if (!LANGS.contains(lang)) {
            return "ko";
        }
        return lang;
    }

    private static PdfBytes readManualPdf(String id, String lang) throws IOException {
        String[] tryLangs = {lang, "en", "ko", "zh", "th", "ja"};
        IOException last = null;
        for (String lg : tryLangs) {
            try {
                byte[] bytes = readClasspathBytes(
                        "static/manuals/pdf/" + id + "/" + lg + ".pdf",
                        "manuals/pdf/" + id + "/" + lg + ".pdf");
                return new PdfBytes(bytes, lg);
            } catch (IOException e) {
                last = e;
            }
        }
        throw last != null ? last : new IOException("missing pdf " + id);
    }

    private static String readManualHtml(String id, String lang) throws IOException {
        byte[] bytes = readClasspathBytes(
                "static/manuals/generated/" + id + "-" + lang + ".html",
                "manuals/generated/" + id + "-" + lang + ".html");
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static byte[] readClasspathBytes(String primary, String fallback) throws IOException {
        Resource res = new ClassPathResource(primary);
        if (!res.exists()) {
            res = new ClassPathResource(fallback);
        }
        if (!res.exists()) {
            res = new FileSystemResource("../site/" + fallback);
        }
        if (!res.exists()) {
            res = new FileSystemResource("site/" + fallback);
        }
        if (!res.exists()) {
            throw new IOException("missing " + primary);
        }
        try (InputStream in = res.getInputStream()) {
            return in.readAllBytes();
        }
    }

    private static boolean isPdfMagic(byte[] bytes) {
        return bytes.length >= 5
                && bytes[0] == '%'
                && bytes[1] == 'P'
                && bytes[2] == 'D'
                && bytes[3] == 'F';
    }

    private static boolean looksLikeAdminShell(String html) {
        String h = html.toLowerCase(Locale.ROOT);
        return h.contains("id=\"side-nav-ul\"")
                || h.contains("id='side-nav-ul'")
                || h.contains("contentsmain")
                || h.contains("pg_admin_user")
                || h.contains("pg-app-shell");
    }

    /**
     * 총본사: 전체 / 본사·총판: hqdist+merchant / 지사·대리점·영업점·가맹: merchant 만.
     */
    static List<String> allowedAudiences(OrgLevel level) {
        List<String> out = new ArrayList<>();
        if (level == null || level == OrgLevel.HEADQUARTERS) {
            out.add("super");
            out.add("hqdist");
            out.add("merchant");
            return out;
        }
        int code = level.getCode();
        if (code <= OrgLevel.MASTER_DIST.getCode()) {
            out.add("hqdist");
            out.add("merchant");
            return out;
        }
        out.add("merchant");
        return out;
    }

    private OrgLevel resolveViewerOrgLevel(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUser user)) {
            return OrgLevel.HEADQUARTERS;
        }
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return OrgLevel.HEADQUARTERS;
        }
        return authService.resolveOrgUnitForLoginId(user.getUsername())
                .map(OrgUnit::getOrgLevel)
                .orElse(OrgLevel.HEADQUARTERS);
    }

    private OrgUnit resolveHeadquarters() {
        Optional<OrgUnit> byCode = orgUnitRepository.findByCode("0000000000");
        if (byCode.isPresent() && byCode.get().getOrgLevel() == OrgLevel.HEADQUARTERS) {
            return byCode.get();
        }
        List<OrgUnit> list = orgUnitRepository.findByOrgLevelOrderByCodeAsc(OrgLevel.HEADQUARTERS);
        return list.isEmpty() ? null : list.get(0);
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    private static String firstNonBlank(String... vals) {
        if (vals == null) {
            return "";
        }
        for (String v : vals) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
    }

    private static String joinAddr(String zip, String a1, String a2, String a3) {
        StringBuilder sb = new StringBuilder();
        if (zip != null && !zip.isBlank()) {
            sb.append('(').append(zip.trim()).append(") ");
        }
        if (a1 != null && !a1.isBlank()) {
            sb.append(a1.trim());
        }
        if (a2 != null && !a2.isBlank()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(a2.trim());
        }
        if (a3 != null && !a3.isBlank()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(a3.trim());
        }
        return sb.toString().trim();
    }
}
