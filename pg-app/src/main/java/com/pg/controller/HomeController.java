package com.pg.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Controller
public class HomeController {

    /** 루트(/) 리다이렉트는 RootRedirectFilter 가 처리 (환영 페이지 406 방지) */

    @GetMapping("/main")
    public String main() {
        return "redirect:/index.html";
    }

    @GetMapping("/transactions")
    public String transactions() {
        return "transactions";
    }

    @GetMapping("/merchants")
    public String merchantsRedirect() {
        return "redirect:/org/6";
    }

    @GetMapping("/pay/{compId}")
    public String payByComp(@PathVariable("compId") String compId, HttpServletRequest request) {
        return redirectWithMergedQuery("/pay.html", compId, request);
    }

    /** 챗봇·임베드용 공개 진입 URL (결제 화면은 동일 셸, 구분은 쿼리로 확장 가능) */
    @GetMapping("/chatbot-pay/{compId}")
    public String chatbotPayByComp(@PathVariable("compId") String compId, HttpServletRequest request) {
        return redirectWithMergedQuery("/chatbot-pay.html", compId, request);
    }

    @GetMapping("/jpay-pay/{compId}")
    public String jpayPayByComp(@PathVariable("compId") String compId, HttpServletRequest request) {
        return redirectWithMergedQuery("/jpay-pay.html", compId, request);
    }

    @GetMapping("/pay-repay/{compId}")
    public String payRepayByComp(@PathVariable("compId") String compId, HttpServletRequest request) {
        String enc = URLEncoder.encode(compId != null ? compId : "", StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder("redirect:/pay.html?variant=repay&m=").append(enc);
        if (request != null) {
            for (Map.Entry<String, String[]> e : request.getParameterMap().entrySet()) {
                String key = e.getKey();
                if (key == null) {
                    continue;
                }
                String kl = key.trim();
                if (kl.isEmpty() || "m".equalsIgnoreCase(kl) || "compId".equalsIgnoreCase(kl)
                        || "merchant".equalsIgnoreCase(kl) || "variant".equalsIgnoreCase(kl)) {
                    continue;
                }
                if (e.getValue() == null) {
                    continue;
                }
                for (String val : e.getValue()) {
                    if (val == null) {
                        continue;
                    }
                    sb.append('&').append(URLEncoder.encode(kl, StandardCharsets.UTF_8))
                            .append('=').append(URLEncoder.encode(val, StandardCharsets.UTF_8));
                }
            }
        }
        return sb.toString();
    }

    /**
     * {@code m}/{@code compId}/{@code merchant} 는 클라이언트가 넘긴 값보다 경로의 업체코드를 우선합니다.
     */
    private static String redirectWithMergedQuery(String htmlPath, String compId, HttpServletRequest request) {
        String enc = URLEncoder.encode(compId != null ? compId : "", StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder("redirect:").append(htmlPath).append("?m=").append(enc);
        if (request != null) {
            for (Map.Entry<String, String[]> e : request.getParameterMap().entrySet()) {
                String key = e.getKey();
                if (key == null) {
                    continue;
                }
                String kl = key.trim();
                if (kl.isEmpty() || "m".equalsIgnoreCase(kl) || "compId".equalsIgnoreCase(kl)
                        || "merchant".equalsIgnoreCase(kl)) {
                    continue;
                }
                if (e.getValue() == null) {
                    continue;
                }
                for (String val : e.getValue()) {
                    if (val == null) {
                        continue;
                    }
                    sb.append('&').append(URLEncoder.encode(kl, StandardCharsets.UTF_8))
                            .append('=').append(URLEncoder.encode(val, StandardCharsets.UTF_8));
                }
            }
        }
        return sb.toString();
    }
}
