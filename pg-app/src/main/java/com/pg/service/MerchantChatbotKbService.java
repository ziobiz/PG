package com.pg.service;

import com.pg.chatbot.ChatbotCatalogPolicy;
import com.pg.chatbot.ChatbotMerchantVertical;
import com.pg.chatbot.ChatbotOperationMode;
import com.pg.chatbot.ChatbotOrderSheetUiResolver;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgUnit;
import com.pg.repository.MerchantProfileRepository;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 가맹 챗봇용 고객 안내(회사·연락처·소개): 등록정보 기반 기본값과 LLM 초안.
 */
@Service
public class MerchantChatbotKbService {

    /** 공개 챗봇 첫 진입 시 상단 버블 — DB 미설정 시 */
    public static final String DEFAULT_CHATBOT_WELCOME_HINT_KO =
            "원하시는 상품을 위에서 고르시거나, 결제·상품에 대해 물어보세요.";

    private static final int MAX_INTRO = 4000;
    private static final int MAX_PRODUCT = 4000;
    private static final int MAX_WELCOME_HINT = 600;
    private static final int MAX_SINGLE = 600;
    private static final int MAX_VERTICAL_NOTES = 2000;

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
        mp.setChatbotKbWelcomeHint(null);
    }

    /** 카탈로그 API 등: 고객 화면에 내려줄 최종 첫 진입 안내 문구 */
    public String effectiveWelcomeHintForPublic(Long merchantOrgUnitId) {
        if (merchantOrgUnitId == null) {
            return DEFAULT_CHATBOT_WELCOME_HINT_KO;
        }
        return merchantProfileRepository.findByOrgUnitId(merchantOrgUnitId)
                .map(this::effectiveWelcomeHintForPublic)
                .orElse(DEFAULT_CHATBOT_WELCOME_HINT_KO);
    }

    public String effectiveWelcomeHintForPublic(MerchantProfile mp) {
        if (mp == null || mp.getChatbotKbWelcomeHint() == null || mp.getChatbotKbWelcomeHint().isBlank()) {
            return DEFAULT_CHATBOT_WELCOME_HINT_KO;
        }
        return mp.getChatbotKbWelcomeHint().trim();
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
        m.put("chatbotKbWelcomeHint", nz(mp.getChatbotKbWelcomeHint()));
        ChatbotOperationMode op = ChatbotOperationMode.resolveStored(mp.getChatbotOperationMode());
        m.put("chatbotOperationMode", op.getCode());
        m.put("chatbotOperationModeLabelKo", op.getLabelKo());
        ChatbotMerchantVertical mv = ChatbotMerchantVertical.resolveStored(mp.getChatbotMerchantVertical());
        m.put("chatbotMerchantVertical", mv.getCode());
        m.put("chatbotMerchantVerticalLabelKo", mv.getLabelKo());
        m.put("chatbotMerchantVerticalNotes", nz(mp.getChatbotMerchantVerticalNotes()));
        m.put("chatbotOrderSheetUiJson", nz(mp.getChatbotOrderSheetUiJson()));
        int slotM = mp.getChatbotReservationSlotMinutes() != null ? mp.getChatbotReservationSlotMinutes() : 60;
        m.put("chatbotReservationSlotMinutes", String.valueOf(Math.max(15, Math.min(24 * 60, slotM))));
        m.put("chatbotReservationZoneId", nz(mp.getChatbotReservationZoneId()));
        return m;
    }

    /** 공개 카탈로그·챗봇 화면용 가맹점 업체성격(LLM·클라이언트 힌트). */
    public Map<String, Object> publicMerchantVerticalMeta(Long merchantOrgUnitId) {
        LinkedHashMap<String, Object> m = new LinkedHashMap<>();
        if (merchantOrgUnitId == null) {
            return m;
        }
        Optional<MerchantProfile> p = merchantProfileRepository.findByOrgUnitId(merchantOrgUnitId);
        if (p.isEmpty()) {
            return m;
        }
        MerchantProfile mp = p.get();
        ChatbotMerchantVertical v = ChatbotMerchantVertical.resolveStored(mp.getChatbotMerchantVertical());
        m.put("chatbotMerchantVertical", v.getCode());
        m.put("chatbotMerchantVerticalLabelKo", v.getLabelKo());
        m.put("chatbotMerchantVerticalCollectHintKo", v.getOrderCollectHintKo());
        m.put("chatbotMerchantVerticalNotes", nz(mp.getChatbotMerchantVerticalNotes()));
        m.put("chatbotReservationOrderPrecheckKo", ChatbotMerchantVertical.sharedReservationOrderPrecheckKo());
        m.put("chatbotOrderSheetUi", ChatbotOrderSheetUiResolver.resolvePublicUi(mp));
        return m;
    }

    /** 공개 카탈로그·챗봇 화면용 예약 슬롯·타임존(기본값 포함). */
    public Map<String, Object> publicReservationMeta(Long merchantOrgUnitId) {
        Map<String, Object> m = new LinkedHashMap<>();
        int slot = 60;
        String zone = "Asia/Seoul";
        if (merchantOrgUnitId != null) {
            Optional<MerchantProfile> p = merchantProfileRepository.findByOrgUnitId(merchantOrgUnitId);
            if (p.isPresent()) {
                MerchantProfile mp = p.get();
                if (mp.getChatbotReservationSlotMinutes() != null) {
                    slot = Math.max(15, Math.min(24 * 60, mp.getChatbotReservationSlotMinutes()));
                }
                if (mp.getChatbotReservationZoneId() != null && !mp.getChatbotReservationZoneId().isBlank()) {
                    zone = mp.getChatbotReservationZoneId().trim();
                }
            }
        }
        m.put("chatbotReservationSlotMinutes", slot);
        m.put("chatbotReservationZoneId", zone);
        return m;
    }

    /**
     * 예약 슬롯(분)·타임존 저장. {@code slotParam}·{@code zoneParam} 이 null 이면 변경 없음(구 클라이언트).
     */
    public void applyReservationSettings(MerchantProfile mp, String slotParam, String zoneParam) {
        if (slotParam != null) {
            String t = slotParam.trim();
            if (t.isEmpty()) {
                mp.setChatbotReservationSlotMinutes(60);
            } else {
                try {
                    int v = Integer.parseInt(t);
                    mp.setChatbotReservationSlotMinutes(Math.max(15, Math.min(24 * 60, v)));
                } catch (NumberFormatException ignored) {
                    mp.setChatbotReservationSlotMinutes(60);
                }
            }
        }
        if (zoneParam != null) {
            String z = zoneParam.trim();
            if (z.isEmpty()) {
                mp.setChatbotReservationZoneId("Asia/Seoul");
            } else {
                try {
                    ZoneId.of(z);
                    mp.setChatbotReservationZoneId(z.length() > 64 ? z.substring(0, 64) : z);
                } catch (Exception ex) {
                    mp.setChatbotReservationZoneId("Asia/Seoul");
                }
            }
        }
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
        sb.append("- 고객 첫 진입 상단 안내(버블 문구): ").append(effectiveWelcomeHintForPublic(mp)).append('\n');
        appendLine(sb, "회사소개", kb.get("chatbotKbIntro"));
        appendLine(sb, "판매상품 안내", kb.get("chatbotKbProductDesc"));
        ChatbotOperationMode mode = ChatbotOperationMode.resolveStored(mp.getChatbotOperationMode());
        sb.append("- 운영방식: ").append(mode.getLabelKo()).append(" (코드 ").append(mode.getCode()).append(")\n");
        sb.append("- 챗봇 응대 규칙(운영방식, 고객 언어로 반영):\n");
        for (String ln : mode.getLlmDirectiveKo().split("\\R")) {
            if (!ln.isBlank()) {
                sb.append("  • ").append(ln.trim()).append('\n');
            }
        }
        ChatbotMerchantVertical mv = ChatbotMerchantVertical.resolveStored(mp.getChatbotMerchantVertical());
        sb.append("\n- 가맹점 업체성격: ").append(mv.getLabelKo()).append(" (코드 ").append(mv.getCode()).append(")\n");
        sb.append("- 주문·예약 대화에서 우선 확인할 정보(업체성격 기준):\n");
        for (String ln : mv.getOrderCollectHintKo().split("\\R")) {
            if (!ln.isBlank()) {
                sb.append("  • ").append(ln.trim()).append('\n');
            }
        }
        String vn = mp.getChatbotMerchantVerticalNotes();
        if (vn != null && !vn.isBlank()) {
            sb.append("- 본사·총판 「업체성격 보조 메모」(반드시 반영. 불법·과도한 개인정보 요구 금지): ")
                    .append(vn.trim()).append('\n');
        }
        int sm = mp.getChatbotReservationSlotMinutes() != null ? mp.getChatbotReservationSlotMinutes() : 60;
        sm = Math.max(15, Math.min(24 * 60, sm));
        String zid = mp.getChatbotReservationZoneId() != null && !mp.getChatbotReservationZoneId().isBlank()
                ? mp.getChatbotReservationZoneId().trim() : "Asia/Seoul";
        sb.append("- 예약 시 동일 상품에 대해 기본 ").append(sm).append("분 단위로 겹치는 시간대는 잡히지 않습니다. (고객 화면 주문 시 검증)\n");
        sb.append("- 예약 일시는 타임존 ").append(zid).append(" 기준으로 해석합니다.\n");
        if (mp.getChatbotCommerceHoldYn() != null && "Y".equalsIgnoreCase(mp.getChatbotCommerceHoldYn().trim())) {
            sb.append("\n[중요 — 상업 기능 운영 보류]\n");
            sb.append("현재 이 가맹점은 챗봇에서 주문·예약 접수·결제 페이지 안내가 일시 중지되어 있습니다.\n");
            sb.append("- 연락처·영업 시간·위치·회사 소개 등 일반 문의는 사실 범위에서 답변할 수 있습니다.\n");
            sb.append("- 시스템이 제공하는 상품 목록(이름·가격 등)은 「참고용」으로만 설명할 수 있습니다. 결제 URL 을 만들거나 예약을 접수하지 마세요.\n");
            sb.append("- 고객이 구매나 예약을 원하면 접수 불가임을 알리고, 전화나 이메일 등 「기본 안내」 연락처로 개별 안내를 받도록 하세요.\n");
        } else {
            sb.append("- 주문 흐름: 고객이 챗봇에서 상품을 고른 뒤 주문서(성명·전화·예약일시·인원 등, 이메일·주소·LINE Notify 토큰은 선택)를 제출하고, 결제 페이지로 이동하거나(동시에) 이메일·LINE Notify로 결제 링크를 받을 수 있습니다.\n");
        }
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

    /**
     * 챗봇 운영방식 저장. {@code rawCode} 가 null 이면 무시(구 API 호환).
     * 빈 문자열이면 DB 를 비워 기본값({@link ChatbotOperationMode#SALE_PREPAID})으로 간주합니다.
     */
    public void applyOperationMode(MerchantProfile mp, String rawCode) {
        if (rawCode == null) {
            return;
        }
        String t = rawCode.trim();
        if (t.isEmpty()) {
            mp.setChatbotOperationMode(null);
            return;
        }
        mp.setChatbotOperationMode(ChatbotOperationMode.fromCodeStrict(t).getCode());
    }

    /**
     * 가맹점 업체성격 저장. {@code rawCode}·{@code notesParam} 이 모두 null 이면 무시(구 API 호환).
     * 코드 빈 문자열이면 GENERAL_SALE 로 저장합니다.
     */
    public void applyMerchantVertical(MerchantProfile mp, String rawCode, String notesParam) {
        if (rawCode == null && notesParam == null) {
            return;
        }
        if (rawCode != null) {
            String t = rawCode.trim();
            if (t.isEmpty()) {
                mp.setChatbotMerchantVertical(ChatbotMerchantVertical.GENERAL_SALE.getCode());
            } else {
                mp.setChatbotMerchantVertical(ChatbotMerchantVertical.fromCodeStrict(t).getCode());
            }
        }
        if (notesParam != null) {
            String clamped = clampText(notesParam, MAX_VERTICAL_NOTES);
            mp.setChatbotMerchantVerticalNotes(trimOrNull(clamped));
        }
    }

    /**
     * 챗봇 주문·예약 시트 UI JSON. {@code null} 이면 변경 없음, 빈 문자열이면 컬럼 비움.
     */
    public void applyOrderSheetUiJson(MerchantProfile mp, String raw) {
        if (raw == null) {
            return;
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            mp.setChatbotOrderSheetUiJson(null);
            return;
        }
        ChatbotOrderSheetUiResolver.validateMerchantJsonOrThrow(t);
        mp.setChatbotOrderSheetUiJson(ChatbotOrderSheetUiResolver.clampStoredJson(t));
    }

    /** 본사·총판·본사등: 산하 가맹 상품 유형 교집합 마스크. */
    public void applyCatalogListingGrant(MerchantProfile mp, String rawCsv) {
        if (rawCsv == null) {
            return;
        }
        String t = rawCsv.trim();
        if (t.isEmpty()) {
            mp.setChatbotCatalogListingGrant(null);
            return;
        }
        LinkedHashSet<String> parsed = ChatbotCatalogPolicy.parseListingCsvOrNull(t);
        if (parsed == null) {
            throw new IllegalArgumentException(
                    "산하 허용 상품 유형이 올바르지 않습니다. 예: SALE,RESERVATION_TIME,RESERVATION_PLACE");
        }
        mp.setChatbotCatalogListingGrant(ChatbotCatalogPolicy.joinListingCsv(parsed));
    }

    /** 가맹: 실제 활성 유형 서브셋. */
    public void applyCatalogListingEnabled(MerchantProfile mp, String rawCsv) {
        if (rawCsv == null) {
            return;
        }
        String t = rawCsv.trim();
        if (t.isEmpty()) {
            mp.setChatbotCatalogListingEnabled(null);
            return;
        }
        LinkedHashSet<String> parsed = ChatbotCatalogPolicy.parseListingCsvOrNull(t);
        if (parsed == null) {
            throw new IllegalArgumentException(
                    "상품 유형 코드가 올바르지 않습니다. 예: SALE,RESERVATION_TIME,RESERVATION_PLACE");
        }
        mp.setChatbotCatalogListingEnabled(ChatbotCatalogPolicy.joinListingCsv(parsed));
    }

    public void applyCatalogMaxProductImages(MerchantProfile mp, Integer maxSlots) {
        if (maxSlots == null) {
            return;
        }
        int v = maxSlots;
        if (v <= 0) {
            mp.setChatbotMaxProductImagesGrant(null);
            return;
        }
        Integer clamped = ChatbotCatalogPolicy.clampImageGrant(v);
        if (clamped == null) {
            mp.setChatbotMaxProductImagesGrant(null);
            return;
        }
        mp.setChatbotMaxProductImagesGrant(clamped);
    }

    /**
     * 첫 진입 상단 안내 저장. {@code raw} 가 null 이면 무시(구 클라이언트).
     * 빈 문자열이면 DB 를 비워 기본 문구를 씁니다.
     */
    public void applyWelcomeHint(MerchantProfile mp, String raw) {
        if (raw == null) {
            return;
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            mp.setChatbotKbWelcomeHint(null);
            return;
        }
        String clamped = clampText(t, MAX_WELCOME_HINT);
        mp.setChatbotKbWelcomeHint(clamped != null && !clamped.isBlank() ? clamped.trim() : null);
    }

    public String suggestWelcomeDraft(OrgUnit ou, MerchantProfile mp) throws Exception {
        Map<String, Object> cfg = hqChatbotAiSettingsService.rawConfigForServerUse();
        Map<String, String> eff = effectiveKbForDisplay(ou, mp);
        String base = opt(cfg.get("ai_system_prompt_chatbot"));
        String sys = (base.isBlank() ? "당신은 결제 가맹점의 고객 응대용 챗봇을 돕는 카피라이터입니다." : base)
                + "\n\n아래는 해당 가맹점 정보입니다. 추측하지 말고 맥락만 사용하세요.\n"
                + "- 업체명: " + eff.get("chatbotKbCompanyNm") + "\n"
                + "- 회사소개(있을 때): " + nz(mp.getChatbotKbIntro()) + "\n"
                + "- 운영방식 코드: " + eff.get("chatbotOperationMode") + "\n";

        String task = "모바일 챗봇 **첫 화면 상단**에 잠깐 보이는 **한두 문장 안내**를 한국어로 작성하세요. "
                + "고객이 위쪽 상품 목록에서 고르거나, 결제·상품 문의를 할 수 있다는 느낌으로 짧고 친절하게. "
                + "가격·약속은 쓰지 마세요. 따옴표·제목 없이 본문 한 덩어리만 출력하세요.";

        List<Map<String, String>> msgs = List.of(Map.of("role", "user", "content", task));
        String out = chatbotLlmCompletionService.completeChat(cfg, sys, msgs);
        return clampText(out, MAX_WELCOME_HINT).trim();
    }

    public String suggestIntroDraft(OrgUnit ou, MerchantProfile mp) throws Exception {
        return runSuggest(ou, mp, true);
    }

    public String suggestProductDraft(OrgUnit ou, MerchantProfile mp) throws Exception {
        return runSuggest(ou, mp, false);
    }

    /** @param intro true=회사소개, false=판매상품 안내 */
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
