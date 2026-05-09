package com.pg.service;

import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgUnit;
import com.pg.repository.MerchantProfileRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 가맹 챗봇용 고객 안내(회사·연락처·소개): 등록정보 기반 기본값과 LLM 초안.
 */
@Service
public class MerchantChatbotKbService {

    private static final int MAX_INTRO = 4000;
    private static final int MAX_PRODUCT = 4000;
    private static final int MAX_SINGLE = 600;

    private final MerchantProfileRepository merchantProfileRepository;
    private final HqChatbotAiSettingsService hqChatbotAiSettingsService;
    private final ChatbotLlmCompletionService chatbotLlmCompletionService;

    public MerchantChatbotKbService(MerchantProfileRepository merchantProfileRepository,
                                    HqChatbotAiSettingsService hqChatbotAiSettingsService,
                                    ChatbotLlmCompletionService chatbotLlmCompletionService) {
        this.merchantProfileRepository = merchantProfileRepository;
        this.hqChatbotAiSettingsService = hqChatbotAiSettingsService;
        this.chatbotLlmCompletionService = chatbotLlmCompletionService;
    }

    /** 공개 챗봇 system 프롬프트에 붙일 가맹 안내 텍스트(프로필 없으면 빈 문자열). */
    public String publicChatKnowledgeAppendix(OrgUnit ou) {
        if (ou == null) {
            return "";
        }
        return merchantProfileRepository.findByOrgUnitId(ou.getId())
                .map(mp -> formatKnowledgeBlockForPublicChat(ou, mp))
                .orElse("");
    }

    /** 신규 가맹 등록 후 1~5번 컬럼에 등록폼 값을 복사하고, 6·7은 비움. */
    public void seedFromRegistration(MerchantProfile mp, OrgUnit ou) {
        if (mp == null || ou == null) {
            return;
        }
        mp.setChatbotKbCompanyNm(trimOrNull(ou.getName()));
        mp.setChatbotKbAddr(trimOrNull(buildAddrLine(mp)));
        mp.setChatbotKbTel(trimOrNull(firstNonBlank(mp.getCompTel(), mp.getContactTel())));
        mp.setChatbotKbEmail(trimOrNull(mp.getEmail()));
        mp.setChatbotKbContactNm(trimOrNull(firstNonBlank(mp.getSettleName(), mp.getCeoNm())));
        mp.setChatbotKbIntro(null);
        mp.setChatbotKbProductDesc(null);
    }

    public Map<String, String> effectiveKbForDisplay(OrgUnit ou, MerchantProfile mp) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("chatbotKbCompanyNm", effective(mp.getChatbotKbCompanyNm(), ou.getName()));
        m.put("chatbotKbAddr", effective(mp.getChatbotKbAddr(), buildAddrLine(mp)));
        m.put("chatbotKbTel", effective(mp.getChatbotKbTel(), firstNonBlank(mp.getCompTel(), mp.getContactTel())));
        m.put("chatbotKbEmail", effective(mp.getChatbotKbEmail(), mp.getEmail()));
        m.put("chatbotKbContactNm", effective(mp.getChatbotKbContactNm(), firstNonBlank(mp.getSettleName(), mp.getCeoNm())));
        m.put("chatbotKbIntro", nz(mp.getChatbotKbIntro()));
        m.put("chatbotKbProductDesc", nz(mp.getChatbotKbProductDesc()));
        return m;
    }

    /** 공개 챗봇 LLM system 블록에 넣는 요약(비어 있으면 빈 문자열). */
    public String formatKnowledgeBlockForPublicChat(OrgUnit ou, MerchantProfile mp) {
        Map<String, String> kb = effectiveKbForDisplay(ou, mp);
        StringBuilder sb = new StringBuilder();
        appendLine(sb, "회사이름", kb.get("chatbotKbCompanyNm"));
        appendLine(sb, "회사주소", kb.get("chatbotKbAddr"));
        appendLine(sb, "전화", kb.get("chatbotKbTel"));
        appendLine(sb, "이메일", kb.get("chatbotKbEmail"));
        appendLine(sb, "담당자", kb.get("chatbotKbContactNm"));
        appendLine(sb, "회사소개", kb.get("chatbotKbIntro"));
        appendLine(sb, "판매상품 안내", kb.get("chatbotKbProductDesc"));
        return sb.toString().trim();
    }

    private static void appendLine(StringBuilder sb, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        sb.append("- ").append(label).append(": ").append(value.trim()).append('\n');
    }

    /**
     * 사용자가 입력한 값을 프로필에 반영합니다. 빈 문자열은 해당 컬럼을 null 로 두어
     * {@link #effectiveKbForDisplay} 가 등록정보로 대체표시 하게 합니다(1~5).
     * 6·7은 빈 문자열이면 null 처리(비움).
     */
    public void applyUserInput(MerchantProfile mp,
                               String companyNm, String addr, String tel, String email, String contactNm,
                               String intro, String productDesc) {
        mp.setChatbotKbCompanyNm(emptyToNull(trimToNull(companyNm), true));
        mp.setChatbotKbAddr(emptyToNull(trimToNull(addr), true));
        mp.setChatbotKbTel(emptyToNull(trimToNull(tel), true));
        mp.setChatbotKbEmail(emptyToNull(trimToNull(email), true));
        mp.setChatbotKbContactNm(emptyToNull(trimToNull(contactNm), true));
        String in = clampText(intro, MAX_INTRO);
        String pd = clampText(productDesc, MAX_PRODUCT);
        mp.setChatbotKbIntro(in == null || in.isBlank() ? null : in.trim());
        mp.setChatbotKbProductDesc(pd == null || pd.isBlank() ? null : pd.trim());
    }

    public String suggestIntroDraft(OrgUnit ou, MerchantProfile mp) throws Exception {
        return runSuggest(ou, mp, true);
    }

    public String suggestProductDraft(OrgUnit ou, MerchantProfile mp) throws Exception {
        return runSuggest(ou, mp, false);
    }

    private String runSuggest(OrgUnit ou, MerchantProfile mp, boolean intro) throws Exception {
        Map<String, Object> cfg = hqChatbotAiSettingsService.rawConfigForServerUse();
        Map<String, String> eff = effectiveKbForDisplay(ou, mp);
        String base = opt(cfg.get("ai_system_prompt_chatbot"));
        String sys = (base.isBlank() ? "당신은 결제 가맹점의 고객 응대용 챗봇을 돕는 카피라이터입니다." : base)
                + "\n\n아래는 해당 가맹점의 사실 정보입니다. 추측하지 말고 제공된 사업 맥락만 사용하세요.\n"
                + "- 업체명: " + eff.get("chatbotKbCompanyNm") + "\n"
                + "- 주소: " + eff.get("chatbotKbAddr") + "\n"
                + "- 전화: " + eff.get("chatbotKbTel") + "\n"
                + "- 이메일: " + eff.get("chatbotKbEmail") + "\n"
                + "- 담당자: " + eff.get("chatbotKbContactNm") + "\n"
                + "- 사업자등록 업태/종목: " + nz(mp.getBizType()) + " / " + nz(mp.getIndustry()) + "\n"
                + "- 취급/상품 키워드(등록): " + nz(mp.getProduct()) + "\n"
                + "- 사이트 개요(등록): " + nz(mp.getSiteSummary()) + "\n";

        String task = intro
                ? "고객이 문의할 때 쓸 **회사 소개** 초안을 한국어로 작성하세요. 2~5문장, 정중하고 간결하게. 회사명·업종 맥락을 반영하세요. 다른 설명·제목 없이 본문만 출력하세요."
                : "**판매 상품·서비스 안내**(개요) 초안을 한국어로 작성하세요. 문단 또는 짧은 글머리 기호 형태. 구체적인 가격·약속은 피하고 개요 중심. 다른 설명 없이 본문만 출력하세요.";

        List<Map<String, String>> msgs = List.of(Map.of("role", "user", "content", task));
        String out = chatbotLlmCompletionService.completeChat(cfg, sys, msgs);
        return clampText(out, intro ? MAX_INTRO : MAX_PRODUCT).trim();
    }

    private static String nz(String s) {
        return s == null ? "" : s.trim();
    }

    private static String trimOrNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * @param nullable true: "" -> null 로 저장하여 effective 가 등록정보를 쓰게 함
     */
    private static String emptyToNull(String s, boolean nullable) {
        if (s == null) {
            return null;
        }
        if (nullable && s.isEmpty()) {
            return null;
        }
        if (s.length() > MAX_SINGLE) {
            return s.substring(0, MAX_SINGLE);
        }
        return s;
    }

    private static String effective(String stored, String fallback) {
        if (stored != null && !stored.isBlank()) {
            return stored.trim();
        }
        return fallback != null ? fallback.trim() : "";
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return "";
    }

    private static String buildAddrLine(MerchantProfile mp) {
        StringBuilder sb = new StringBuilder();
        if (mp.getZipCode() != null && !mp.getZipCode().isBlank()) {
            sb.append('(').append(mp.getZipCode().trim()).append(") ");
        }
        if (mp.getAddr() != null && !mp.getAddr().isBlank()) {
            sb.append(mp.getAddr().trim());
        }
        if (mp.getAddrDetail() != null && !mp.getAddrDetail().isBlank()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(mp.getAddrDetail().trim());
        }
        if (mp.getAddrEtc() != null && !mp.getAddrEtc().isBlank()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(mp.getAddrEtc().trim());
        }
        return sb.toString().trim();
    }

    private static String clampText(String s, int max) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }

    private static String opt(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }
}
