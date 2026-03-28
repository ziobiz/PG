package com.pg.service;

import com.pg.entity.HqApiConfig;
import com.pg.repository.HqApiConfigRepository;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 본사설정 서버관리: 호스트·JVM·DB·SSL(PEM)·Certbot·Nginx stub 요약
 */
@Service
public class HqServerManageService {

    private static final String PEM_BEGIN = "-----BEGIN CERTIFICATE-----";

    private final HqApiConfigRepository hqApiConfigRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.serverManage.uiAutoRefreshSeconds:120}")
    private int uiAutoRefreshSeconds;

    @Value("${app.serverManage.nginxStubStatusUrl:}")
    private String nginxStubStatusUrl;

    public HqServerManageService(HqApiConfigRepository hqApiConfigRepository, DataSource dataSource) {
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public int getUiAutoRefreshSeconds() {
        return uiAutoRefreshSeconds;
    }

    public Map<String, Object> buildSummary() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("uiAutoRefreshSeconds", uiAutoRefreshSeconds);
        root.put("nginxStubStatusUrlConfigured", nginxStubStatusUrl != null && !nginxStubStatusUrl.isBlank());

        Optional<HqApiConfig> cfgOpt = hqApiConfigRepository.findAll().stream().findFirst();
        String configuredPath = cfgOpt.map(HqApiConfig::getServerManageSslCertPath).orElse(null);
        String leDomain = cfgOpt.map(HqApiConfig::getServerManageSslLeDomain).orElse(null);

        root.put("serverManageSslCertPath", configuredPath != null ? configuredPath : "");
        root.put("serverManageSslLeDomain", leDomain != null ? leDomain : "");

        Path pemPath = resolvePemPath(configuredPath, leDomain);
        root.put("sslResolvedPath", pemPath != null ? pemPath.toString() : "");
        root.put("ssl", readSslInfo(pemPath));
        root.put("host", readHostInfo());
        root.put("jvm", readJvmInfo());
        root.put("certbot", readCertbotInfo());
        root.put("db", readDbInfo());
        root.put("nginxStub", readNginxStub());
        return root;
    }

    private Path resolvePemPath(String configured, String leDomain) {
        if (configured != null && !configured.isBlank()) {
            Path p = Paths.get(configured.trim());
            if (Files.isRegularFile(p)) {
                return p;
            }
        }
        String env = System.getenv("PG_SSL_CERT_PATH");
        if (env != null && !env.isBlank()) {
            Path p = Paths.get(env.trim());
            if (Files.isRegularFile(p)) {
                return p;
            }
        }
        if (leDomain != null && !leDomain.isBlank()) {
            Path p = Paths.get("/etc/letsencrypt/live", leDomain.trim(), "fullchain.pem");
            if (Files.isRegularFile(p)) {
                return p;
            }
        }
        return null;
    }

    private Map<String, Object> readSslInfo(Path pemPath) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (pemPath == null || !Files.isRegularFile(pemPath)) {
            m.put("status", "N/A");
            m.put("detail", "인증서 파일을 찾을 수 없습니다. 경로 또는 LE 도메인을 저장하세요.");
            return m;
        }
        try {
            String pem = Files.readString(pemPath, StandardCharsets.UTF_8);
            int b = pem.indexOf(PEM_BEGIN);
            int e = pem.indexOf("-----END CERTIFICATE-----");
            if (b < 0 || e < 0) {
                m.put("status", "ERROR");
                m.put("detail", "PEM 형식이 아닙니다.");
                return m;
            }
            String b64 = pem.substring(b + PEM_BEGIN.length(), e).replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(b64);
            X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(der));
            m.put("status", "OK");
            m.put("subjectDn", cert.getSubjectX500Principal().getName());
            m.put("issuerDn", cert.getIssuerX500Principal().getName());
            m.put("notBefore", cert.getNotBefore().toInstant().toString());
            m.put("notAfter", cert.getNotAfter().toInstant().toString());
            m.put("daysRemaining", daysBetween(Instant.now(), cert.getNotAfter().toInstant()));
        } catch (Exception ex) {
            m.put("status", "ERROR");
            m.put("detail", ex.getMessage() != null ? ex.getMessage() : "parse failed");
        }
        return m;
    }

    private static long daysBetween(Instant from, Instant to) {
        long secs = to.getEpochSecond() - from.getEpochSecond();
        return secs / 86400;
    }

    private Map<String, Object> readHostInfo() {
        Map<String, Object> m = new LinkedHashMap<>();
        try {
            SystemInfo si = new SystemInfo();
            OperatingSystem os = si.getOperatingSystem();
            HardwareAbstractionLayer hal = si.getHardware();
            m.put("hostname", os.getNetworkParams().getHostName());
            m.put("osFamily", os.getFamily());
            m.put("osVersion", os.getVersionInfo().getVersion());
            m.put("arch", System.getProperty("os.arch"));
            long total = hal.getMemory().getTotal();
            long avail = hal.getMemory().getAvailable();
            m.put("memoryTotalMb", total / (1024 * 1024));
            m.put("memoryAvailableMb", avail / (1024 * 1024));
        } catch (Throwable t) {
            m.put("hostname", safeHostname());
            m.put("error", t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName());
        }
        return m;
    }

    private static String safeHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "";
        }
    }

    private Map<String, Object> readJvmInfo() {
        Map<String, Object> m = new LinkedHashMap<>();
        Runtime rt = Runtime.getRuntime();
        m.put("javaVersion", System.getProperty("java.version"));
        m.put("javaVendor", System.getProperty("java.vendor"));
        m.put("maxMemoryMb", rt.maxMemory() / (1024 * 1024));
        m.put("totalMemoryMb", rt.totalMemory() / (1024 * 1024));
        m.put("freeMemoryMb", rt.freeMemory() / (1024 * 1024));
        try {
            var rbx = java.lang.management.ManagementFactory.getRuntimeMXBean();
            m.put("uptimeMs", rbx.getUptime());
        } catch (Exception ignored) {
            /* ignore */
        }
        return m;
    }

    private Map<String, Object> readCertbotInfo() {
        Map<String, Object> m = new LinkedHashMap<>();
        Path renewalDir = Paths.get("/etc/letsencrypt/renewal");
        List<String> files = new ArrayList<>();
        try {
            if (Files.isDirectory(renewalDir)) {
                try (var stream = Files.list(renewalDir)) {
                    stream.filter(p -> p.toString().endsWith(".conf"))
                            .map(p -> p.getFileName().toString())
                            .sorted()
                            .forEach(files::add);
                }
            }
        } catch (Exception ignored) {
            /* ignore */
        }
        m.put("renewalConfFiles", files);

        Map<String, String> timer = new LinkedHashMap<>();
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            timer.put("active", execShort("systemctl", "is-active", "certbot.timer"));
            timer.put("next", execShort("systemctl", "show", "certbot.timer", "-p", "NextElapseUSecRealtime", "--value"));
        } else {
            timer.put("active", "N/A");
            timer.put("next", "N/A");
        }
        m.put("certbotTimer", timer);
        return m;
    }

    private static String execShort(String... cmd) {
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (sb.length() > 0) {
                        sb.append(' ');
                    }
                    sb.append(line);
                }
            }
            p.waitFor();
            return sb.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private Map<String, Object> readDbInfo() {
        Map<String, Object> m = new LinkedHashMap<>();
        try {
            Integer n = jdbcTemplate.queryForObject(
                    "select count(*) from information_schema.tables where table_schema = current_schema()",
                    Integer.class);
            m.put("ok", true);
            m.put("tableCountInSchema", n != null ? n : 0);
        } catch (Exception ex) {
            m.put("ok", false);
            m.put("error", ex.getMessage() != null ? ex.getMessage() : "db check failed");
        }
        return m;
    }

    private Map<String, Object> readNginxStub() {
        Map<String, Object> m = new LinkedHashMap<>();
        if (nginxStubStatusUrl == null || nginxStubStatusUrl.isBlank()) {
            m.put("status", "SKIPPED");
            return m;
        }
        try {
            var client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(3))
                    .build();
            var req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(nginxStubStatusUrl.trim()))
                    .timeout(java.time.Duration.ofSeconds(5))
                    .GET()
                    .build();
            var resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            m.put("status", resp.statusCode() == 200 ? "OK" : "HTTP_" + resp.statusCode());
            String body = resp.body();
            if (body != null && body.length() > 500) {
                body = body.substring(0, 500) + "...";
            }
            m.put("bodyPreview", body != null ? body : "");
        } catch (Exception ex) {
            m.put("status", "ERROR");
            m.put("detail", ex.getMessage() != null ? ex.getMessage() : "request failed");
        }
        return m;
    }
}
