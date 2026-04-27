package com.pg.service;

import com.pg.entity.HqApiConfig;
import com.pg.entity.ServerUsageDaily;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.ServerUsageDailyRepository;
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
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.nio.file.FileStore;
import java.net.URI;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 본사설정 서버운영관리: 호스트·JVM·DB·SSL(PEM)·Certbot·Nginx stub 요약
 */
@Service
public class HqServerManageService {

    private static final String PEM_BEGIN = "-----BEGIN CERTIFICATE-----";

    /** NOTI advanced_system_monitor 와 유사한 임계치 (단일화) */
    private static final double SYS_MEM_PCT_DANGER = 90;
    private static final double SYS_MEM_PCT_WARN = 70;
    private static final double DISK_PCT_DANGER = 90;
    private static final double DISK_PCT_WARN = 75;
    private static final double HEAP_PCT_WARN = 80;
    private static final double HEAP_PCT_DANGER = 92;
    private static final long HEAP_FRAC_MIN_TOTAL_BYTES = 64L * 1024 * 1024;
    private static final double LOAD_MULT = 2;
    private static final int SSL_DAYS_WARN = 30;
    private static final int SSL_DAYS_DANGER = 14;

    /* Admin UI i18n keys (site/js/pg-ui-i18n.js STATIC) — health rows & alerts */
    private static final String HK_L_SYS_MEM = "hqSrv.health.lbl.sysMem";
    private static final String HK_L_JVM_HEAP = "hqSrv.health.lbl.jvmHeap";
    private static final String HK_L_LOAD_AVG = "hqSrv.health.lbl.loadAvg";
    private static final String HK_L_DISK = "hqSrv.health.lbl.diskAppPath";
    private static final String HK_L_SSL = "hqSrv.health.lbl.sslCert";
    private static final String HK_L_CONTRACT_DISK = "hqSrv.health.lbl.contractDisk";
    private static final String HK_L_CONTRACT_TRAFFIC = "hqSrv.health.lbl.contractTraffic";
    private static final String HK_L_DB_TABLES = "hqSrv.health.lbl.dbTables";
    private static final String HK_C_SYS_MEM = "hqSrv.health.criteria.sysMem";
    private static final String HK_C_JVM_HEAP = "hqSrv.health.criteria.jvmHeap";
    private static final String HK_C_LOAD = "hqSrv.health.criteria.loadAvg";
    private static final String HK_C_DISK = "hqSrv.health.criteria.disk";
    private static final String HK_C_SSL = "hqSrv.health.criteria.ssl";
    private static final String HK_C_CONTRACT_DISK = "hqSrv.health.criteria.contractDisk";
    private static final String HK_C_CONTRACT_TRAFFIC = "hqSrv.health.criteria.contractTraffic";
    private static final String HK_C_DB_TABLES = "hqSrv.health.criteria.dbTables";
    private static final String HK_V_SYS_MEM = "hqSrv.health.value.sysMem";
    private static final String HK_V_JVM_HEAP = "hqSrv.health.value.jvmHeap";
    private static final String HK_V_LOAD = "hqSrv.health.value.loadAvg";
    private static final String HK_V_DISK_PCT = "hqSrv.health.value.diskPct";
    private static final String HK_V_SSL_DAYS = "hqSrv.health.value.sslDays";
    private static final String HK_V_SSL_NA = "hqSrv.health.value.sslNa";
    private static final String HK_V_SSL_ERR = "hqSrv.health.value.sslErr";
    private static final String HK_V_CONTRACT_DISK = "hqSrv.health.value.contractDisk";
    private static final String HK_V_CONTRACT_TRAFFIC = "hqSrv.health.value.contractTraffic";
    private static final String HK_V_CONTRACT_TRAFFIC_SUGGEST = "hqSrv.health.value.contractTrafficSuggested";
    private static final String HK_V_CONTRACT_TRAFFIC_EMPTY = "hqSrv.health.value.contractTrafficEmpty";
    private static final String HK_V_DB_TABLES = "hqSrv.health.value.dbTables";
    private static final String HK_V_DB_FAIL = "hqSrv.health.value.dbFail";
    private static final String HK_V_DASH = "hqSrv.health.value.dash";
    private static final String HK_ALERT_HOSTING_EXPIRED = "hqSrv.alert.hostingContractExpired";
    private static final String HK_ALERT_HOSTING_SOON = "hqSrv.alert.hostingContractEndingSoon";
    private static final String HK_ALERT_SYS_MEM = "hqSrv.alert.systemMemoryHigh";
    private static final String HK_ALERT_JVM_HEAP = "hqSrv.alert.jvmHeapHigh";
    private static final String HK_ALERT_LOAD = "hqSrv.alert.loadAverageHigh";
    private static final String HK_ALERT_DISK = "hqSrv.alert.diskUsageHigh";
    private static final String HK_ALERT_CONTRACT_DISK = "hqSrv.alert.contractDiskHigh";
    private static final String HK_ALERT_CONTRACT_TRAFFIC = "hqSrv.alert.contractTrafficHigh";
    private static final String HK_ALERT_SSL_DANGER = "hqSrv.alert.sslExpiresCritical";
    private static final String HK_ALERT_SSL_WARN = "hqSrv.alert.sslExpiresSoon";
    private static final String HK_ALERT_SSL_READ = "hqSrv.alert.sslReadFailed";
    private static final String HK_ALERT_DB_META = "hqSrv.alert.dbMetaFailed";

    private final HqApiConfigRepository hqApiConfigRepository;
    private final ServerUsageDailyRepository serverUsageDailyRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.serverManage.uiAutoRefreshSeconds:120}")
    private int uiAutoRefreshSeconds;

    @Value("${app.serverManage.nginxStubStatusUrl:}")
    private String nginxStubStatusUrl;

    public HqServerManageService(HqApiConfigRepository hqApiConfigRepository,
                                 ServerUsageDailyRepository serverUsageDailyRepository,
                                 DataSource dataSource) {
        this.hqApiConfigRepository = hqApiConfigRepository;
        this.serverUsageDailyRepository = serverUsageDailyRepository;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public int getUiAutoRefreshSeconds() {
        return uiAutoRefreshSeconds;
    }

    public Map<String, Object> buildSummary() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("generatedAt", Instant.now().toString());
        root.put("nginxStubStatusUrlConfigured", nginxStubStatusUrl != null && !nginxStubStatusUrl.isBlank());

        Optional<HqApiConfig> cfgOpt = hqApiConfigRepository.findAll().stream().findFirst();
        HqApiConfig cfgRow = cfgOpt.orElse(null);
        Integer suggestedTrafficMb = computeSuggestedTrafficUsedMb(cfgRow);
        root.put("serverManageSuggestedTrafficUsedMb", suggestedTrafficMb);
        int refreshEff = resolveDashboardRefreshSeconds(cfgOpt);
        root.put("uiAutoRefreshSeconds", refreshEff);
        root.put("serverManageUiRefreshSec", cfgOpt.map(HqApiConfig::getServerManageUiRefreshSec).orElse(null));

        String configuredPath = cfgOpt.map(HqApiConfig::getServerManageSslCertPath).orElse(null);
        String leDomain = cfgOpt.map(HqApiConfig::getServerManageSslLeDomain).orElse(null);

        root.put("serverManageSslCertPath", configuredPath != null ? configuredPath : "");
        root.put("serverManageSslLeDomain", leDomain != null ? leDomain : "");

        Path pemPath = resolvePemPath(configuredPath, leDomain);
        root.put("sslResolvedPath", pemPath != null ? pemPath.toString() : "");
        Map<String, Object> host = readHostInfo();
        Map<String, Object> jvm = readJvmInfo();
        Map<String, Object> disk = readDiskInfo();
        Map<String, Object> ssl = readSslInfo(pemPath);
        root.put("ssl", ssl);
        root.put("host", host);
        root.put("jvm", jvm);
        root.put("disk", disk);
        Map<String, Object> db = readDbInfo();
        root.put("health", buildHealth(host, jvm, disk, ssl, cfgOpt, db, suggestedTrafficMb));
        root.put("certbot", readCertbotInfo());
        root.put("db", db);
        root.put("nginxStub", readNginxStub());
        root.put("sslOpsGuide", buildSslOpsGuide());
        putServerManageContractFields(root, cfgOpt);
        return root;
    }

    /** DB 저장값이 유효하면 사용, 아니면 yml 기본값(15~3600초로 클램프) */
    private int resolveDashboardRefreshSeconds(Optional<HqApiConfig> cfgOpt) {
        Integer stored = cfgOpt.map(HqApiConfig::getServerManageUiRefreshSec).orElse(null);
        if (stored != null && stored >= 15 && stored <= 3600) {
            return stored;
        }
        int d = uiAutoRefreshSeconds;
        if (d < 15) {
            d = 15;
        }
        if (d > 3600) {
            d = 3600;
        }
        return d;
    }

    /** 운영 안내 — UI는 pg-ui-i18n 키로 번역 */
    private Map<String, Object> buildSslOpsGuide() {
        Map<String, Object> g = new LinkedHashMap<>();
        g.put("dnsK", "hqSrv.sslOps.dns");
        g.put("leSanK", "hqSrv.sslOps.leSan");
        g.put("cloudflareK", "hqSrv.sslOps.cloudflare");
        return g;
    }

    /**
     * 도메인구성설정 화면 연동: PEM의 SAN과 전사·조직 URL에 적힌 호스트명을 대조합니다.
     */
    public Map<String, Object> buildSslDomainLinkage(String publicAdminSiteUrl, String publicApiBaseUrl,
                                                     List<Map<String, Object>> orgDomainRows) {
        Map<String, Object> out = new LinkedHashMap<>();
        Optional<HqApiConfig> cfgOpt = hqApiConfigRepository.findAll().stream().findFirst();
        String configuredPath = cfgOpt.map(HqApiConfig::getServerManageSslCertPath).orElse(null);
        String leDomain = cfgOpt.map(HqApiConfig::getServerManageSslLeDomain).orElse(null);
        Path pemPath = resolvePemPath(configuredPath, leDomain);
        Map<String, Object> ssl = readSslInfo(pemPath);
        out.put("sslStatus", ssl.get("status"));
        out.put("sslDetail", ssl.getOrDefault("detail", ""));
        out.put("notAfter", ssl.getOrDefault("notAfter", ""));
        out.put("daysRemaining", ssl.get("daysRemaining"));
        out.put("leLiveCertName", ssl.getOrDefault("leLiveCertName", ""));
        @SuppressWarnings("unchecked")
        List<String> san = ssl.get("sanDnsNames") instanceof List ? (List<String>) ssl.get("sanDnsNames") : List.of();
        out.put("sanDnsNames", san);

        Set<String> sanLower = san.stream()
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(TreeSet::new));

        List<Map<String, Object>> configuredRows = new ArrayList<>();
        List<Map<String, Object>> missing = new ArrayList<>();
        addLinkageRow(configuredRows, missing, sanLower, hostFromUrl(publicAdminSiteUrl), "GLOBAL_ADMIN", "");
        addLinkageRow(configuredRows, missing, sanLower, hostFromUrl(publicApiBaseUrl), "GLOBAL_API", "");
        if (orgDomainRows != null) {
            for (Map<String, Object> row : orgDomainRows) {
                String orgName = row.get("name") != null ? String.valueOf(row.get("name")) : "";
                addLinkageRow(configuredRows, missing, sanLower, hostFromUrl(strObj(row.get("orgDomainAdminUrl"))),
                        "ORG_ADMIN", orgName);
                addLinkageRow(configuredRows, missing, sanLower, hostFromUrl(strObj(row.get("orgDomainApiUrl"))),
                        "ORG_API", orgName);
            }
        }
        out.put("configuredHostRows", configuredRows);
        out.put("hostsMissingFromCert", missing);

        Set<String> referredLower = new TreeSet<>();
        for (Map<String, Object> e : configuredRows) {
            Object h = e.get("hostname");
            if (h != null && !String.valueOf(h).isBlank()) {
                referredLower.add(String.valueOf(h).toLowerCase(Locale.ROOT));
            }
        }
        List<String> sanOnly = new ArrayList<>();
        for (String s : san) {
            if (!referredLower.contains(s.toLowerCase(Locale.ROOT))) {
                sanOnly.add(s);
            }
        }
        out.put("sanWithoutConfiguredUrl", sanOnly);
        out.put("linkageHint", "도메인구성설정 URL의 호스트명이 인증서 SAN에 없으면 HTTPS 경고가 납니다. SAN에만 있고 여기 미기재인 호스트는 운영용으로 쓰는지 검토하세요.");
        return out;
    }

    private static String strObj(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static void addLinkageRow(List<Map<String, Object>> configuredRows, List<Map<String, Object>> missing,
                                      Set<String> sanLower, String host, String sourceKind, String orgLabel) {
        if (host == null || host.isBlank()) {
            return;
        }
        String hn = host.trim();
        String hLower = hn.toLowerCase(Locale.ROOT);
        boolean ok = sanLower.contains(hLower);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("hostname", hn);
        row.put("sourceKind", sourceKind);
        if (orgLabel != null && !orgLabel.isBlank()) {
            row.put("orgLabel", orgLabel.trim());
        }
        row.put("inCertificate", ok);
        configuredRows.add(row);
        if (!ok) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("hostname", hn);
            m.put("sourceKind", sourceKind);
            if (orgLabel != null && !orgLabel.isBlank()) {
                m.put("orgLabel", orgLabel.trim());
            }
            missing.add(m);
        }
    }

    static String hostFromUrl(String url) {
        if (url == null) {
            return null;
        }
        String t = url.trim();
        if (t.isEmpty()) {
            return null;
        }
        try {
            if (!t.contains("://")) {
                t = "https://" + t;
            }
            URI u = new URI(t);
            String h = u.getHost();
            return h != null ? h : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void putServerManageContractFields(Map<String, Object> root, Optional<HqApiConfig> cfgOpt) {
        HqApiConfig c = cfgOpt.orElse(null);
        root.put("serverManageContractDiskMb", c != null ? c.getServerManageContractDiskMb() : null);
        root.put("serverManageContractTrafficMb", c != null ? c.getServerManageContractTrafficMb() : null);
        root.put("serverManageContractStart", c != null && c.getServerManageContractStart() != null
                ? c.getServerManageContractStart().toString() : "");
        root.put("serverManageContractEnd", c != null && c.getServerManageContractEnd() != null
                ? c.getServerManageContractEnd().toString() : "");
        root.put("serverManageTrafficUsedMb", c != null ? c.getServerManageTrafficUsedMb() : null);
        Map<String, Object> contract = new LinkedHashMap<>();
        contract.put("diskMb", c != null ? c.getServerManageContractDiskMb() : null);
        contract.put("trafficMb", c != null ? c.getServerManageContractTrafficMb() : null);
        contract.put("trafficUsedMb", c != null ? c.getServerManageTrafficUsedMb() : null);
        contract.put("periodStart", c != null && c.getServerManageContractStart() != null
                ? c.getServerManageContractStart().toString() : "");
        contract.put("periodEnd", c != null && c.getServerManageContractEnd() != null
                ? c.getServerManageContractEnd().toString() : "");
        root.put("serverManageContract", contract);
    }

    /**
     * 약정 시작일~min(약정 종료일, 오늘) 구간의 일별 수집 트래픽 합(MB, 내림).
     * 호스팅사 패널의 “기간 누적”과 측정 방식이 다를 수 있어 참고·폼 자동 채움용.
     */
    private Integer computeSuggestedTrafficUsedMb(HqApiConfig c) {
        if (c == null || c.getServerManageContractStart() == null) {
            return null;
        }
        LocalDate from = c.getServerManageContractStart();
        LocalDate end = c.getServerManageContractEnd();
        LocalDate today = LocalDate.now();
        LocalDate to = (end == null || end.isAfter(today)) ? today : end;
        if (from.isAfter(to)) {
            return null;
        }
        long sumBytes = 0L;
        for (ServerUsageDaily d : serverUsageDailyRepository.findByUsageDateBetweenOrderByUsageDateAsc(from, to)) {
            sumBytes += d.getTrafficBytes();
        }
        if (sumBytes <= 0L) {
            return null;
        }
        long mb = sumBytes / (1024L * 1024L);
        if (mb < 1L) {
            mb = 1L;
        }
        return (int) Math.min(Integer.MAX_VALUE, mb);
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
        m.put("sanDnsNames", List.of());
        m.put("leLiveCertName", pemPath != null && pemPath.getParent() != null
                ? pemPath.getParent().getFileName().toString() : "");
        if (pemPath == null || !Files.isRegularFile(pemPath)) {
            m.put("status", "N/A");
            m.put("detail", "인증서 파일을 찾을 수 없습니다. 경로 또는 LE live 폴더명(예: api.icopay.co.kr)을 저장하세요.");
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
            m.put("fingerprintSha256", sha256Hex(cert.getEncoded()));
            m.put("sanDnsNames", extractSanDnsNames(cert));
            m.put("leLiveCertName", pemPath.getParent().getFileName().toString());
        } catch (Exception ex) {
            m.put("status", "ERROR");
            m.put("detail", ex.getMessage() != null ? ex.getMessage() : "parse failed");
        }
        return m;
    }

    /** 리프 인증서의 Subject Alternative Name (dNSName) 목록 */
    private static List<String> extractSanDnsNames(X509Certificate cert) {
        List<String> out = new ArrayList<>();
        try {
            Collection<List<?>> sans = cert.getSubjectAlternativeNames();
            if (sans == null) {
                return out;
            }
            for (List<?> san : sans) {
                if (san == null || san.size() < 2) {
                    continue;
                }
                Object o0 = san.get(0);
                Object o1 = san.get(1);
                if (o0 instanceof Integer it && it == 2 && o1 instanceof String dns) {
                    out.add(dns);
                }
            }
        } catch (Exception ignored) {
            /* ignore */
        }
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
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
            MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
            MemoryUsage heap = mem.getHeapMemoryUsage();
            long heapUsed = heap.getUsed();
            long heapMax = heap.getMax() > 0 ? heap.getMax() : heap.getCommitted();
            m.put("heapUsedMb", heapUsed / (1024 * 1024));
            m.put("heapMaxMb", heapMax > 0 ? heapMax / (1024 * 1024) : 0);
            double heapPct = heapMax > 0 ? Math.round((heapUsed * 1000.0 / heapMax)) / 10.0 : 0;
            m.put("heapUsedPct", heapPct);
        } catch (Exception ignored) {
            m.put("heapUsedMb", 0);
            m.put("heapMaxMb", 0);
            m.put("heapUsedPct", 0.0);
        }
        int cpuN = Math.max(1, rt.availableProcessors());
        m.put("cpuCount", cpuN);
        double loadAvg = -1;
        try {
            loadAvg = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        } catch (Exception ignored) {
            /* ignore */
        }
        m.put("systemLoadAverage", loadAvg >= 0 ? Math.round(loadAvg * 100.0) / 100.0 : null);
        try {
            var rbx = ManagementFactory.getRuntimeMXBean();
            m.put("uptimeMs", rbx.getUptime());
        } catch (Exception ignored) {
            /* ignore */
        }
        return m;
    }

    private Map<String, Object> readDiskInfo() {
        Map<String, Object> m = new LinkedHashMap<>();
        try {
            Path base = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
            FileStore store = Files.getFileStore(base);
            long total = store.getTotalSpace();
            long usable = store.getUsableSpace();
            long used = total - usable;
            double pct = total > 0 ? Math.round((used * 1000.0 / total)) / 10.0 : 0;
            m.put("ok", true);
            m.put("pathRoot", base.toString());
            m.put("fileSystem", store.name());
            m.put("totalBytes", total);
            m.put("usableBytes", usable);
            m.put("usedBytes", used);
            m.put("usedPct", pct);
        } catch (Exception ex) {
            m.put("ok", false);
            m.put("error", ex.getMessage() != null ? ex.getMessage() : "disk");
        }
        return m;
    }

    private Map<String, Object> buildHealth(Map<String, Object> host, Map<String, Object> jvm, Map<String, Object> disk, Map<String, Object> ssl,
                                            Optional<HqApiConfig> cfgOpt, Map<String, Object> db,
                                            Integer suggestedTrafficUsedMb) {
        Map<String, Object> h = new LinkedHashMap<>();
        List<Object> alerts = new ArrayList<>();
        List<Map<String, Object>> rows = new ArrayList<>();

        cfgOpt.flatMap(x -> Optional.ofNullable(x.getServerManageContractEnd())).ifPresent(end -> {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), end);
            if (days < 0) {
                alerts.add(alertTuple(HK_ALERT_HOSTING_EXPIRED, end));
            } else if (days <= 14) {
                alerts.add(alertTuple(HK_ALERT_HOSTING_SOON, days));
            }
        });

        long totalMb = toLong(host.get("memoryTotalMb"));
        long availMb = toLong(host.get("memoryAvailableMb"));
        double sysMemPct = totalMb > 0 ? Math.round(((totalMb - availMb) * 1000.0 / totalMb)) / 10.0 : 0;
        String sysStatus = sysMemPct >= SYS_MEM_PCT_DANGER ? "danger" : sysMemPct >= SYS_MEM_PCT_WARN ? "warn" : "ok";
        if (sysMemPct >= SYS_MEM_PCT_DANGER) {
            alerts.add(alertTuple(HK_ALERT_SYS_MEM, (int) SYS_MEM_PCT_DANGER));
        }
        rows.add(rowMetric("sys_mem", sysStatus, HK_L_SYS_MEM, List.of(),
                HK_C_SYS_MEM, List.of((int) SYS_MEM_PCT_WARN, (int) SYS_MEM_PCT_DANGER),
                HK_V_SYS_MEM, List.of(sysMemPct + "%", availMb, totalMb)));

        double heapPct = jvm.get("heapUsedPct") instanceof Number ? ((Number) jvm.get("heapUsedPct")).doubleValue() : 0;
        long heapMaxB = toLong(jvm.get("heapMaxMb")) * 1024L * 1024L;
        boolean heapDanger = heapMaxB >= HEAP_FRAC_MIN_TOTAL_BYTES && heapPct >= HEAP_PCT_DANGER;
        String heapStatus = heapDanger ? "danger" : heapPct >= HEAP_PCT_WARN ? "warn" : "ok";
        if (heapDanger) {
            alerts.add(alertTuple(HK_ALERT_JVM_HEAP, (int) HEAP_PCT_DANGER));
        }
        rows.add(rowMetric("jvm_heap", heapStatus, HK_L_JVM_HEAP, List.of(),
                HK_C_JVM_HEAP, List.of((int) HEAP_PCT_WARN, (int) HEAP_PCT_DANGER, (int) (HEAP_FRAC_MIN_TOTAL_BYTES / (1024 * 1024))),
                HK_V_JVM_HEAP, List.of(jvm.get("heapUsedMb"), jvm.get("heapMaxMb"), heapPct)));

        Double load = jvm.get("systemLoadAverage") instanceof Number ? ((Number) jvm.get("systemLoadAverage")).doubleValue() : null;
        int cpuN = jvm.get("cpuCount") instanceof Number ? ((Number) jvm.get("cpuCount")).intValue() : 1;
        boolean loadDanger = load != null && load >= 0 && load > cpuN * LOAD_MULT;
        String loadStatus = load == null || load < 0 ? "ok" : loadDanger ? "danger" : load > cpuN ? "warn" : "ok";
        if (loadDanger) {
            alerts.add(alertTuple(HK_ALERT_LOAD, (int) LOAD_MULT));
        }
        boolean loadDash = load == null || load < 0;
        rows.add(rowMetric("load_avg", loadStatus, HK_L_LOAD_AVG, List.of(),
                HK_C_LOAD, List.of(cpuN, (int) LOAD_MULT, cpuN * LOAD_MULT),
                loadDash ? HK_V_DASH : HK_V_LOAD, loadDash ? List.of() : List.of(loadStr(load))));

        String diskStatus = "ok";
        String diskValKey = HK_V_DASH;
        List<Object> diskValArgs = List.of();
        if (Boolean.TRUE.equals(disk.get("ok"))) {
            double dPct = disk.get("usedPct") instanceof Number ? ((Number) disk.get("usedPct")).doubleValue() : 0;
            diskValKey = HK_V_DISK_PCT;
            diskValArgs = List.of(dPct + "%");
            diskStatus = dPct >= DISK_PCT_DANGER ? "danger" : dPct >= DISK_PCT_WARN ? "warn" : "ok";
            if (dPct >= DISK_PCT_DANGER) {
                alerts.add(alertTuple(HK_ALERT_DISK, (int) DISK_PCT_DANGER));
            }
        } else {
            diskStatus = "warn";
        }
        rows.add(rowMetric("disk", diskStatus, HK_L_DISK, List.of(),
                HK_C_DISK, List.of((int) DISK_PCT_WARN, (int) DISK_PCT_DANGER),
                diskValKey, diskValArgs));

        String sslStatus = "ok";
        String sslValKey = HK_V_DASH;
        List<Object> sslValArgs = List.of();
        if ("OK".equals(ssl.get("status"))) {
            long days = ssl.get("daysRemaining") instanceof Number ? ((Number) ssl.get("daysRemaining")).longValue() : 999;
            sslValKey = HK_V_SSL_DAYS;
            sslValArgs = List.of(days);
            sslStatus = days < SSL_DAYS_DANGER ? "danger" : days < SSL_DAYS_WARN ? "warn" : "ok";
            if (days < SSL_DAYS_DANGER) {
                alerts.add(alertTuple(HK_ALERT_SSL_DANGER, SSL_DAYS_DANGER));
            } else if (days < SSL_DAYS_WARN) {
                alerts.add(alertTuple(HK_ALERT_SSL_WARN, SSL_DAYS_WARN));
            }
        } else if ("N/A".equals(ssl.get("status"))) {
            sslStatus = "warn";
            sslValKey = HK_V_SSL_NA;
            sslValArgs = List.of();
        } else if ("ERROR".equals(ssl.get("status"))) {
            sslStatus = "danger";
            sslValKey = HK_V_SSL_ERR;
            sslValArgs = List.of(String.valueOf(ssl.getOrDefault("detail", "")));
            alerts.add(alertTuple(HK_ALERT_SSL_READ));
        }
        rows.add(rowMetric("ssl", sslStatus, HK_L_SSL, List.of(),
                HK_C_SSL, List.of(SSL_DAYS_WARN, SSL_DAYS_DANGER),
                sslValKey, sslValArgs));

        HqApiConfig cfg = cfgOpt.orElse(null);
        Integer contractDiskMb = cfg != null ? cfg.getServerManageContractDiskMb() : null;
        if (contractDiskMb != null && contractDiskMb > 0 && Boolean.TRUE.equals(disk.get("ok"))) {
            long usedBytes = disk.get("usedBytes") instanceof Number ? ((Number) disk.get("usedBytes")).longValue() : 0L;
            double usedMb = usedBytes / (1024.0 * 1024.0);
            double pctContract = Math.round((usedMb * 1000.0 / contractDiskMb)) / 10.0;
            String cDiskStatus = pctContract >= DISK_PCT_DANGER ? "danger" : pctContract >= DISK_PCT_WARN ? "warn" : "ok";
            String quotaDiskGb = formatGbFromMb(contractDiskMb);
            if (pctContract >= DISK_PCT_DANGER) {
                alerts.add(alertTuple(HK_ALERT_CONTRACT_DISK, quotaDiskGb, (int) DISK_PCT_DANGER));
            }
            rows.add(rowMetric("contract_disk", cDiskStatus, HK_L_CONTRACT_DISK, List.of(),
                    HK_C_CONTRACT_DISK, List.of(quotaDiskGb, (int) DISK_PCT_WARN, (int) DISK_PCT_DANGER, contractPeriodSuffix(cfg)),
                    HK_V_CONTRACT_DISK, List.of(quotaDiskGb, formatGbFromMb(usedMb), pctContract)));
        }

        Integer contractTrafficMb = cfg != null ? cfg.getServerManageContractTrafficMb() : null;
        Integer trafficUsedMb = cfg != null ? cfg.getServerManageTrafficUsedMb() : null;
        if (contractTrafficMb != null && contractTrafficMb > 0) {
            String quotaTrGb = formatGbFromMb(contractTrafficMb);
            List<Object> trafficCritArgs = new ArrayList<>();
            trafficCritArgs.add(quotaTrGb);
            trafficCritArgs.add((int) DISK_PCT_WARN);
            trafficCritArgs.add((int) DISK_PCT_DANGER);
            trafficCritArgs.add(contractPeriodSuffix(cfg));
            if (trafficUsedMb == null) {
                if (suggestedTrafficUsedMb != null && suggestedTrafficUsedMb > 0) {
                    rows.add(rowMetric("contract_traffic", "warn", HK_L_CONTRACT_TRAFFIC, List.of(),
                            HK_C_CONTRACT_TRAFFIC, trafficCritArgs,
                            HK_V_CONTRACT_TRAFFIC_SUGGEST, List.of(formatGbFromMb(suggestedTrafficUsedMb))));
                } else {
                    rows.add(rowMetric("contract_traffic", "warn", HK_L_CONTRACT_TRAFFIC, List.of(),
                            HK_C_CONTRACT_TRAFFIC, trafficCritArgs,
                            HK_V_CONTRACT_TRAFFIC_EMPTY, List.of()));
                }
            } else {
                double pctT = Math.round((trafficUsedMb * 1000.0 / contractTrafficMb)) / 10.0;
                String tStatus = pctT >= DISK_PCT_DANGER ? "danger" : pctT >= DISK_PCT_WARN ? "warn" : "ok";
                if (pctT >= DISK_PCT_DANGER) {
                    alerts.add(alertTuple(HK_ALERT_CONTRACT_TRAFFIC, quotaTrGb, (int) DISK_PCT_DANGER));
                }
                rows.add(rowMetric("contract_traffic", tStatus, HK_L_CONTRACT_TRAFFIC, List.of(),
                        HK_C_CONTRACT_TRAFFIC, trafficCritArgs,
                        HK_V_CONTRACT_TRAFFIC, List.of(formatGbFromMb(trafficUsedMb), quotaTrGb, pctT)));
            }
        }

        if (db != null) {
            boolean dbOk = Boolean.TRUE.equals(db.get("ok"));
            Object tc = db.get("tableCountInSchema");
            if (!dbOk) {
                alerts.add(alertTuple(HK_ALERT_DB_META, String.valueOf(db.getOrDefault("error", ""))));
                rows.add(rowMetric("db_tables", "danger", HK_L_DB_TABLES, List.of(),
                        HK_C_DB_TABLES, List.of(),
                        HK_V_DB_FAIL, List.of(String.valueOf(db.getOrDefault("error", "")))));
            } else if (tc != null) {
                rows.add(rowMetric("db_tables", "ok", HK_L_DB_TABLES, List.of(),
                        HK_C_DB_TABLES, List.of(),
                        HK_V_DB_TABLES, List.of(tc)));
            } else {
                rows.add(rowMetric("db_tables", "ok", HK_L_DB_TABLES, List.of(),
                        HK_C_DB_TABLES, List.of(),
                        HK_V_DASH, List.of()));
            }
        }

        h.put("alerts", alerts);
        h.put("rows", rows);
        h.put("worstStatus", worstOf(rows));
        return h;
    }

    private static String loadStr(Double load) {
        return load == null ? "" : String.valueOf(load);
    }

    private static Map<String, Object> alertTuple(String key, Object... args) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("k", key);
        m.put("a", args.length == 0 ? List.of() : Arrays.asList(args));
        return m;
    }

    /** Locale-neutral suffix for criteria templates (dates only; leading " · ") */
    private static String contractPeriodSuffix(HqApiConfig cfg) {
        if (cfg == null) {
            return "";
        }
        LocalDate s = cfg.getServerManageContractStart();
        LocalDate e = cfg.getServerManageContractEnd();
        if (s == null && e == null) {
            return "";
        }
        String a = s != null ? s.toString() : "?";
        String b = e != null ? e.toString() : "?";
        return " · " + a + " ~ " + b;
    }

    /** 표시용 MB → GB (저장은 MB 유지) */
    private static String formatGbFromMb(double mbVal) {
        if (mbVal <= 0) {
            return "0 GB";
        }
        return String.format(Locale.ROOT, "%.2f GB", mbVal / 1024.0);
    }

    private static String worstOf(List<Map<String, Object>> rows) {
        int w = 0;
        for (Map<String, Object> r : rows) {
            String s = String.valueOf(r.getOrDefault("status", "ok"));
            if ("danger".equals(s)) {
                return "danger";
            }
            if ("warn".equals(s)) {
                w = 1;
            }
        }
        return w == 1 ? "warn" : "ok";
    }

    private static Map<String, Object> rowMetric(String id, String status,
            String labelKey, List<Object> labelArgs,
            String criteriaKey, List<Object> criteriaArgs,
            String valueKey, List<Object> valueArgs) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("status", status);
        m.put("lK", labelKey);
        m.put("lA", labelArgs == null ? List.of() : labelArgs);
        m.put("cK", criteriaKey);
        m.put("cA", criteriaArgs == null ? List.of() : criteriaArgs);
        m.put("vK", valueKey);
        m.put("vA", valueArgs == null ? List.of() : valueArgs);
        return m;
    }

    private static long toLong(Object o) {
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        return 0;
    }

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(data);
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) {
                sb.append(String.format(Locale.ROOT, "%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
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
