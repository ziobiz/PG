package com.pg.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * JPAY 가맹 포털 Playwright Export 스크립트 실행.
 */
@Component
public class JpayPortalExportRunner {

    private static final Logger log = LoggerFactory.getLogger(JpayPortalExportRunner.class);
    private static final int TIMEOUT_SEC = 300;

    public Path runExport(LocalDate from, LocalDate to, String username, String password) throws Exception {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException("JPAY 포털 로그인 ID·비밀번호를 전산설정에 등록하세요.");
        }
        Path script = resolveScriptPath();
        if (!Files.isRegularFile(script)) {
            throw new IllegalStateException("JPAY 포털 Export 스크립트를 찾을 수 없습니다: " + script);
        }
        Path outDir = Paths.get(System.getProperty("java.io.tmpdir"), "icopay-jpay-export");
        Files.createDirectories(outDir);
        Path outFile = outDir.resolve("jpay-orders-" + System.currentTimeMillis() + ".xlsx");

        List<String> cmd = new ArrayList<>();
        cmd.add(resolveNodeBinary());
        cmd.add(script.toAbsolutePath().toString());
        cmd.add("--from=" + from);
        cmd.add("--to=" + to);
        cmd.add("--out=" + outFile.toAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        pb.environment().put("JPAY_PORTAL_USER", username.trim());
        pb.environment().put("JPAY_PORTAL_PASSWORD", password);
        pb.environment().put("PLAYWRIGHT_BROWSERS_PATH", System.getenv().getOrDefault("PLAYWRIGHT_BROWSERS_PATH", ""));

        log.info("JPAY portal export start from={} to={}", from, to);
        Process proc = pb.start();
        StringBuilder logBuf = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                logBuf.append(line).append('\n');
                log.debug("jpay-export: {}", line);
            }
        }
        boolean finished = proc.waitFor(TIMEOUT_SEC, TimeUnit.SECONDS);
        if (!finished) {
            proc.destroyForcibly();
            throw new IllegalStateException("JPAY 포털 Export 시간 초과(" + TIMEOUT_SEC + "초)");
        }
        int code = proc.exitValue();
        if (code != 0) {
            throw new IllegalStateException("JPAY 포털 Export 실패(exit=" + code + "): " + truncate(logBuf.toString(), 800));
        }
        if (!Files.isRegularFile(outFile) || Files.size(outFile) < 100) {
            throw new IllegalStateException("JPAY Export 파일이 생성되지 않았습니다. " + truncate(logBuf.toString(), 400));
        }
        log.info("JPAY portal export done file={} bytes={}", outFile, Files.size(outFile));
        return outFile;
    }

    private static Path resolveScriptPath() {
        String env = System.getenv("PG_JPAY_EXPORT_SCRIPT");
        if (env != null && !env.isBlank()) {
            return Paths.get(env.trim());
        }
        String scriptsDir = System.getenv("PG_SCRIPTS_DIR");
        if (scriptsDir != null && !scriptsDir.isBlank()) {
            return Paths.get(scriptsDir.trim(), "jpay-portal-export.js");
        }
        Path cwd = Paths.get(System.getProperty("user.dir", "."));
        Path[] candidates = {
                cwd.resolve("scripts").resolve("jpay-portal-export.js"),
                cwd.resolve("..").resolve("scripts").resolve("jpay-portal-export.js"),
                cwd.getParent() != null ? cwd.getParent().resolve("scripts").resolve("jpay-portal-export.js") : null
        };
        for (Path p : candidates) {
            if (p != null && Files.isRegularFile(p)) {
                return p.normalize();
            }
        }
        return cwd.resolve("scripts").resolve("jpay-portal-export.js");
    }

    private static String resolveNodeBinary() {
        String env = System.getenv("PG_NODE_BIN");
        if (env != null && !env.isBlank()) {
            return env.trim();
        }
        return "node";
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }
}
