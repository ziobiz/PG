package com.pg.config;

import com.pg.service.LinkPreviewService;
import com.pg.util.LinkPreviewOgSupport;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * GET/HEAD {@code /}, {@code /login.html}, {@code /index.html} 에 크롤러용 Open Graph 메타를 심어 응답한다.
 * {@code /} 는 302 없이 로그인 HTML을 200으로 내려 LINE·WhatsApp이 본문을 읽게 한다.
 */
public class OgHtmlInjectionFilter extends OncePerRequestFilter {

    private final LinkPreviewService linkPreviewService;
    private volatile String loginHtml;
    private volatile String indexHtml;

    public OgHtmlInjectionFilter(LinkPreviewService linkPreviewService) {
        this.linkPreviewService = linkPreviewService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
            return true;
        }
        String path = pathWithinApp(request);
        return !("/".equals(path) || "/login.html".equals(path) || "/index.html".equals(path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = pathWithinApp(request);
        boolean login = "/".equals(path) || "/login.html".equals(path);
        String html = login ? loadLogin() : loadIndex();
        if (html == null || html.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        LinkPreviewService.Preview preview;
        try {
            preview = linkPreviewService.resolve(request);
        } catch (Exception e) {
            filterChain.doFilter(request, response);
            return;
        }
        String block = LinkPreviewOgSupport.buildOgBlock(
                preview.title(), preview.description(), preview.imageAbsUrl(), preview.pageUrl());
        String out = LinkPreviewOgSupport.injectOgBlock(html, block, preview.title());
        byte[] bytes = out.getBytes(StandardCharsets.UTF_8);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/html;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setContentLength(bytes.length);
        if (!"HEAD".equalsIgnoreCase(request.getMethod())) {
            response.getOutputStream().write(bytes);
        }
    }

    private static String pathWithinApp(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath() == null ? "" : request.getContextPath();
        if (uri == null) {
            return "/";
        }
        if (!ctx.isEmpty() && uri.startsWith(ctx)) {
            uri = uri.substring(ctx.length());
        }
        if (uri.isEmpty()) {
            return "/";
        }
        return uri;
    }

    private String loadLogin() {
        if (loginHtml == null) {
            loginHtml = loadNamed("login.html");
        }
        return loginHtml;
    }

    private String loadIndex() {
        if (indexHtml == null) {
            indexHtml = loadNamed("index.html");
        }
        return indexHtml;
    }

    private static String loadNamed(String name) {
        try {
            ClassPathResource cp = new ClassPathResource("static/" + name);
            if (cp.exists()) {
                try (var in = cp.getInputStream()) {
                    return new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        } catch (Exception ignored) {
            // fall through to files
        }
        String userDir = System.getProperty("user.dir", ".");
        Path[] candidates = new Path[] {
                Paths.get(userDir, "..", "site", name),
                Paths.get(userDir, "site", name)
        };
        for (Path p : candidates) {
            try {
                if (Files.isRegularFile(p)) {
                    return Files.readString(p, StandardCharsets.UTF_8);
                }
            } catch (Exception ignored) {
                // next
            }
        }
        return null;
    }
}
