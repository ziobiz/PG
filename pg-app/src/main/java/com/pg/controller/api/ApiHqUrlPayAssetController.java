package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.util.FaviconImageUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 본사 URL 결제 폼용 정적 자산 업로드 (결제구문설정 JSON에 경로 저장).
 */
@RestController
@RequestMapping(value = "/api/hq/url-pay", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiHqUrlPayAssetController {

    private static final long FAVICON_MAX_BYTES = 1 * 1024 * 1024;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @PostMapping(value = "/favicon-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadFavicon(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("파일을 선택하세요.", "EMPTY"));
        }
        if (file.getSize() > FAVICON_MAX_BYTES) {
            return ResponseEntity.ok(ApiResponse.fail("파비콘 이미지는 1MB 이하여야 합니다.", "SIZE_EXCEEDED"));
        }
        String ext = getExtension(file.getOriginalFilename());
        if (ext == null || (!ext.equalsIgnoreCase("png") && !ext.equalsIgnoreCase("jpg") && !ext.equalsIgnoreCase("jpeg"))) {
            return ResponseEntity.ok(ApiResponse.fail("PNG 또는 JPG 파일만 업로드 가능합니다.", "INVALID_TYPE"));
        }
        try {
            Path basePath = Paths.get(System.getProperty("user.dir"), uploadDir, "hq", "url-pay").normalize();
            Files.createDirectories(basePath);
            String fileName = "favicon_" + UUID.randomUUID().toString().substring(0, 8) + ".png";
            Path targetPath = basePath.resolve(fileName);
            FaviconImageUtil.saveMultipartAsFaviconPng32(file, targetPath);
            String url = "/uploads/hq/url-pay/" + fileName;
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("url", url);
            payload.put("storedFileName", fileName);
            payload.put("originalFileName", sanitizeUploadOriginalName(file.getOriginalFilename()));
            return ResponseEntity.ok(ApiResponse.ok(payload));
        } catch (IOException e) {
            return ResponseEntity.ok(ApiResponse.fail("파일 저장 실패: " + e.getMessage(), "IO_ERROR"));
        }
    }

    private static String getExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        int i = filename.lastIndexOf('.');
        return i > 0 ? filename.substring(i + 1) : null;
    }

    private static String sanitizeUploadOriginalName(String name) {
        if (name == null) {
            return "";
        }
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
}
