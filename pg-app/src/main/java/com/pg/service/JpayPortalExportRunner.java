package com.pg.service;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * JPAY 포털 Playwright export 스크립트({@code scripts/jpay-portal-export.js}) 실행.
 * JAR에 포함된 스크립트를 pg-app/scripts 로 자동 추출하며, 서버에 Node·Playwright가 설치되어 있어야 합니다.
 */
@Component
public class JpayPortalExportRunner {

    private static final String SCRIPT_NAME = "jpay-portal-export.js";
    private static final String PACKAGE_JSON = "package.json";
    private static final String CLASSPATH_SCRIPTS_PREFIX = "scripts/";

    /**
     * @return 생성된 xlsx 경로
     */
    public Path runExport(String portalUser, String portalPassword, String fromYmd, String toYmd) throws IOException, InterruptedException {
        Path scriptsDir = resolveScriptsDirectory();
        Path script = scriptsDir.resolve(SCRIPT_NAME);
        if (!Files.isRegularFile(script)) {
            throw new IOException(
                    "JPAY 포털 Export 스크립트를 찾을 수 없습니다: " + script.toAbsolutePath()
                            + ". JAR 재배포 후 재시도하거나 PG_SCRIPTS_DIR 환경변수를 설정하세요.");
        }
        ensurePlaywrightReady(scriptsDir);
        refreshBundledScript(scriptsDir);

        Path out = Files.createTempFile("jpay-portal-export-", ".xlsx");
        List<String> command = buildCommand(script, out, fromYmd, toYmd);
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(scriptsDir.toFile());
        pb.environment().put("JPAY_PORTAL_USER", portalUser != null ? portalUser : "");
        pb.environment().put("JPAY_PORTAL_PASSWORD", portalPassword != null ? portalPassword : "");
        if (useXvfbHeaded()) {
            pb.environment().put("JPAY_EXPORT_HEADED", "1");
        }
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String log = new String(p.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        boolean finished = p.waitFor(540, TimeUnit.SECONDS);
        if (!finished) {
            p.destroyForcibly();
            throw new IOException("JPAY 포털 export 타임아웃(540초). 로그: " + truncateForUser(log));
        }
        if (p.exitValue() != 0) {
            throw new IOException("JPAY 포털 export 실패(exit " + p.exitValue() + "): " + truncateForUser(log));
        }
        if (!Files.isRegularFile(out) || Files.size(out) < 100) {
            throw new IOException("JPAY export 파일이 비어 있습니다. 로그: " + truncate(log, 800));
        }
        return out;
    }

    private static List<String> buildCommand(Path script, Path out, String fromYmd, String toYmd) {
        List<String> cmd = new ArrayList<>();
        if (useXvfbHeaded()) {
            cmd.add("xvfb-run");
            cmd.add("-a");
        }
        cmd.add("node");
        cmd.add(script.toAbsolutePath().toString());
        cmd.add("--from=" + fromYmd);
        cmd.add("--to=" + toYmd);
        cmd.add("--out=" + out.toAbsolutePath());
        return cmd;
    }

    /** Linux 서버: DISPLAY 없을 때 xvfb-run + headed Chromium (headless shell 보다 안정적) */
    private static boolean useXvfbHeaded() {
        String force = System.getenv("JPAY_EXPORT_XVFB");
        if ("0".equals(force) || "false".equalsIgnoreCase(force)) {
            return false;
        }
        if ("1".equals(force) || "true".equalsIgnoreCase(force)) {
            return findExecutable("xvfb-run") != null;
        }
        String display = System.getenv("DISPLAY");
        if (display != null && !display.isBlank()) {
            return false;
        }
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("linux")) {
            return false;
        }
        return findExecutable("xvfb-run") != null;
    }

    private static Path findExecutable(String name) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null || pathEnv.isBlank()) {
            return null;
        }
        for (String dir : pathEnv.split(java.io.File.pathSeparator)) {
            if (dir.isBlank()) {
                continue;
            }
            Path candidate = Paths.get(dir.trim(), name);
            if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /** JAR에 포함된 export 스크립트를 매 실행 시 갱신 */
    private static void refreshBundledScript(Path scriptsDir) throws IOException {
        Files.createDirectories(scriptsDir);
        copyClasspathResource(CLASSPATH_SCRIPTS_PREFIX + SCRIPT_NAME, scriptsDir.resolve(SCRIPT_NAME));
    }

    private static void copyClasspathResource(String classpathResource, Path target) throws IOException {
        ClassLoader cl = JpayPortalExportRunner.class.getClassLoader();
        try (InputStream in = cl.getResourceAsStream(classpathResource)) {
            if (in == null) {
                return;
            }
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void ensurePlaywrightReady(Path scriptsDir) throws IOException {
        Path playwright = scriptsDir.resolve("node_modules").resolve("playwright");
        if (Files.isDirectory(playwright)) {
            return;
        }
        throw new IOException(
                "Playwright가 설치되어 있지 않습니다. 서버에서 한 번 실행하세요: "
                        + "cd " + scriptsDir.toAbsolutePath()
                        + " && npm install && npx playwright install chromium"
                        + " (Node.js 18+ 필요)");
    }

    private static Path resolveScriptsDirectory() throws IOException {
        String envScript = System.getenv("PG_JPAY_EXPORT_SCRIPT");
        if (envScript != null && !envScript.isBlank()) {
            Path p = Paths.get(envScript.trim());
            if (Files.isRegularFile(p)) {
                return p.getParent() != null ? p.getParent() : Paths.get(".");
            }
        }
        String envDir = System.getenv("PG_SCRIPTS_DIR");
        if (envDir != null && !envDir.isBlank()) {
            Path dir = Paths.get(envDir.trim());
            ensureBundledScripts(dir);
            return dir;
        }

        for (Path base : candidateAppBaseDirs()) {
            Path dir = base.resolve("scripts");
            ensureBundledScripts(dir);
            Path script = dir.resolve(SCRIPT_NAME);
            if (Files.isRegularFile(script)) {
                return dir;
            }
        }

        Path fallback = Paths.get(System.getProperty("user.dir", ".")).resolve("scripts");
        ensureBundledScripts(fallback);
        return fallback;
    }

    private static List<Path> candidateAppBaseDirs() {
        Set<Path> out = new LinkedHashSet<>();
        Path userDir = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        out.add(userDir);
        out.add(userDir.getParent());
        Path jarBase = resolveJarAppBaseDir();
        if (jarBase != null) {
            out.add(jarBase);
        }
        return new ArrayList<>(out);
    }

    /** JAR가 build/libs/*.jar 이면 pg-app 루트(build 의 상위) 반환 */
    private static Path resolveJarAppBaseDir() {
        try {
            URL loc = JpayPortalExportRunner.class.getProtectionDomain().getCodeSource().getLocation();
            if (loc == null) {
                return null;
            }
            Path jarPath = Paths.get(loc.toURI()).toAbsolutePath().normalize();
            if (!Files.isRegularFile(jarPath)) {
                return null;
            }
            Path libsDir = jarPath.getParent();
            if (libsDir == null) {
                return null;
            }
            if ("libs".equals(String.valueOf(libsDir.getFileName()))) {
                Path appRoot = libsDir.getParent();
                if (appRoot != null) {
                    return appRoot;
                }
            }
            return libsDir;
        } catch (Exception ignored) {
            return null;
        }
    }

    /** classpath scripts/* 를 대상 디렉터리에 복사(없을 때만) */
    private static void ensureBundledScripts(Path scriptsDir) throws IOException {
        Files.createDirectories(scriptsDir);
        copyClasspathResourceIfMissing(CLASSPATH_SCRIPTS_PREFIX + SCRIPT_NAME, scriptsDir.resolve(SCRIPT_NAME));
        copyClasspathResourceIfMissing(CLASSPATH_SCRIPTS_PREFIX + PACKAGE_JSON, scriptsDir.resolve(PACKAGE_JSON));
    }

    private static void copyClasspathResourceIfMissing(String classpathResource, Path target) throws IOException {
        if (Files.isRegularFile(target)) {
            return;
        }
        ClassLoader cl = JpayPortalExportRunner.class.getClassLoader();
        try (InputStream in = cl.getResourceAsStream(classpathResource)) {
            if (in == null) {
                return;
            }
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }

    /** Playwright Chromium launch 로그(수백 줄)는 사용자 메시지에서 제외 */
    private static String truncateForUser(String log) {
        if (log == null || log.isBlank()) {
            return "";
        }
        String[] lines = log.split("\\R");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String t = line.trim();
            if (t.isEmpty()) {
                continue;
            }
            if (t.startsWith("<launching>") || t.startsWith("--") || t.contains("playwright/builds/cft")) {
                continue;
            }
            if (t.startsWith("[jpay-export]")) {
                // 단계 로그는 사용자에게 유용
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(t);
            if (sb.length() >= 600) {
                break;
            }
        }
        String out = sb.toString().trim();
        if (out.isEmpty()) {
            return truncate(log, 600);
        }
        return out.length() <= 600 ? out : out.substring(0, 600) + "…";
    }
}
