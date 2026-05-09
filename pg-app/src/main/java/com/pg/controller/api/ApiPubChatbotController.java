package com.pg.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.api.ApiResponse;
import com.pg.service.ChatbotLlmCompletionService;
import com.pg.service.HqChatbotAiSettingsService;
import com.pg.service.MerchantChatbotProductService;
import com.pg.service.MerchantChatbotKbService;
import com.pg.entity.OrgUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 공개 챗봇 — 카탈로그 조회·LLM 대화 (가맹점 챗봇결제 사용 시에만).
 */
@RestController
@RequestMapping(value = "/api/pub/chatbot", produces = MediaType.APPLICATION_JSON_VALUE)
public class ApiPubChatbotController {

    private static final long GUEST_IMAGE_MAX_BYTES = 2 * 1024 * 1024;
    /** 고객 채팅 이미지 — LLM용 마커 ([CHATBOT_GUEST_IMAGE:url=...])와 동일 규칙 */
    public static final String GUEST_IMAGE_MARKER = "[CHATBOT_GUEST_IMAGE:url=";

    /** 첫 고객 메시지 기준 언어 힌트(한글·히라가나·가타카나) */
    private static final Pattern HAS_HANGUL = Pattern.compile("[\\uAC00-\\uD7AF\\u1100-\\u11FF\\u3130-\\u318F]");
    private static final Pattern HAS_KANA = Pattern.compile("[\\u3040-\\u309F\\u30A0-\\u30FF\\u31F0-\\u31FF]");

    private final MerchantChatbotProductService productService;
    private final HqChatbotAiSettingsService hqChatbotAiSettingsService;
    private final ChatbotLlmCompletionService chatbotLlmCompletionService;
    private final MerchantChatbotKbService merchantChatbotKbService;
    private final ObjectMapper objectMapper;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public ApiPubChatbotController(MerchantChatbotProductService productService,
                                   HqChatbotAiSettingsService hqChatbotAiSettingsService,
                                   ChatbotLlmCompletionService chatbotLlmCompletionService,
                                   MerchantChatbotKbService merchantChatbotKbService,
                                   ObjectMapper objectMapper) {
        this.productService = productService;
        this.hqChatbotAiSettingsService = hqChatbotAiSettingsService;
        this.chatbotLlmCompletionService = chatbotLlmCompletionService;
        this.merchantChatbotKbService = merchantChatbotKbService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/catalog")
    public ResponseEntity<ApiResponse<Map<String, Object>>> catalog(@RequestParam String compId) {
        Optional<OrgUnit> ou = productService.requireMerchantOrgByCode(compId);
        if (ou.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점 코드를 확인하세요.", "NOT_FOUND"));
        }
        if (!productService.isChatbotPaymentOpenForMerchant(ou.get().getId())) {
            return ResponseEntity.ok(ApiResponse.fail("챗봇 결제가 비활성입니다.", "FORBIDDEN"));
        }
        List<Map<String, Object>> items = productService.listPublicCatalog(ou.get().getId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("compId", ou.get().getCode());
        data.put("items", items);
        data.putAll(productService.resolveChatbotPublicUi(ou.get().getId()));
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /** 일반 고객 채팅용 이미지 업로드(인증 없음). 챗봇결제 활성 가맹만 — URL은 대화 텍스트에 포함해 LLM이 참고(비전 미지원). */
    @PostMapping(value = "/guestImageUpload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> guestImageUpload(
            @RequestParam String compId,
            @RequestParam("file") MultipartFile file) {
        String cid0 = str(compId);
        if (cid0 == null) {
            return ResponseEntity.ok(ApiResponse.fail("compId가 필요합니다.", "INVALID"));
        }
        Optional<OrgUnit> ou = productService.requireMerchantOrgByCode(cid0);
        if (ou.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점 코드를 확인하세요.", "NOT_FOUND"));
        }
        if (!productService.isChatbotPaymentOpenForMerchant(ou.get().getId())) {
            return ResponseEntity.ok(ApiResponse.fail("챗봇 결제가 비활성입니다.", "FORBIDDEN"));
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("파일을 선택하세요.", "EMPTY"));
        }
        if (file.getSize() > GUEST_IMAGE_MAX_BYTES) {
            return ResponseEntity.ok(ApiResponse.fail("이미지는 2MB 이하여야 합니다.", "SIZE_EXCEEDED"));
        }
        String ext = guestExt(file.getOriginalFilename());
        if (ext == null) {
            return ResponseEntity.ok(ApiResponse.fail("PNG 또는 JPG만 가능합니다.", "INVALID_TYPE"));
        }
        String cid = ou.get().getCode().trim();
        try {
            Path basePath = Paths.get(System.getProperty("user.dir"), uploadDir, "chatbot", cid).normalize();
            Files.createDirectories(basePath);
            String fileName = "guest_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12)
                    + "." + ext.toLowerCase(Locale.ROOT);
            Path targetPath = basePath.resolve(fileName);
            Files.copy(file.getInputStream(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            String url = "/uploads/chatbot/" + cid + "/" + fileName;
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("url", url);
            return ResponseEntity.ok(ApiResponse.ok(payload));
        } catch (IOException e) {
            return ResponseEntity.ok(ApiResponse.fail("파일 저장 실패: " + e.getMessage(), "IO_ERROR"));
        }
    }

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<Map<String, Object>>> chat(@RequestBody Map<String, Object> body) {
        String compId = str(body.get("compId"));
        if (compId == null) {
            return ResponseEntity.ok(ApiResponse.fail("compId가 필요합니다.", "INVALID"));
        }
        Optional<OrgUnit> ou = productService.requireMerchantOrgByCode(compId);
        if (ou.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점 코드를 확인하세요.", "NOT_FOUND"));
        }
        if (!productService.isChatbotPaymentOpenForMerchant(ou.get().getId())) {
            return ResponseEntity.ok(ApiResponse.fail("챗봇 결제가 비활성입니다.", "FORBIDDEN"));
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawMsgs = body.get("messages") instanceof List<?> lm ? (List<Map<String, Object>>) lm : List.of();
        List<Map<String, String>> messages = new ArrayList<>();
        for (Map<String, Object> m : rawMsgs) {
            if (m == null) {
                continue;
            }
            String role = str(m.get("role"));
            String content = str(m.get("content"));
            if (role == null || content == null) {
                continue;
            }
            messages.add(Map.of("role", role, "content", content));
        }
        if (messages.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("messages가 비어 있습니다.", "INVALID"));
        }

        Map<String, Object> aiCfg = hqChatbotAiSettingsService.rawConfigForServerUse();
        String basePrompt = optStr(aiCfg.get("ai_system_prompt_chatbot"));
        String catalogInstr = optStr(aiCfg.get("ai_prompt_chatbot_catalog"));
        List<Map<String, Object>> catalog = productService.listPublicCatalog(ou.get().getId());
        String catalogJson;
        try {
            catalogJson = objectMapper.writeValueAsString(catalog);
        } catch (Exception e) {
            catalogJson = "[]";
        }
        StringBuilder sys = new StringBuilder();
        if (!basePrompt.isEmpty()) {
            sys.append(basePrompt).append("\n\n");
        }
        if (!catalogInstr.isEmpty()) {
            sys.append(catalogInstr).append("\n\n");
        }
        sys.append("고객 챗봇에 노출되는 상품(JSON, 가맹 사용=Y 및 본사 판매금지 아님):\n").append(catalogJson);
        long merchantOuId = ou.get().getId();
        long publicProductCount = catalog.size();
        int slotCapEff = productService.getEffectiveChatbotProductSlotCap(merchantOuId);
        int regCapEff = productService.getEffectiveRegistrationCap(merchantOuId);
        sys.append("\n\n[필수 규칙 — 상품 범위]\n")
                .append("각 상품에는 listingType이 있습니다: SALE=일반 판매, RESERVATION=예약 성격 안내입니다. 예약 안내 문구 유지 및 일반판매와 안내 차별.\n")
                .append("위 JSON 배열만 이 가맹점의 결제 가능한 등록 상품입니다. 목록에 없는 상품명·가격·재고·옵션을 지어내거나 추측하지 마세요.\n")
                .append("고객이 목록에 없는 품목을 원하면 목록 안의 상품으로만 안내 가능함을 알리고, 과장·허위 없이 안내하세요.\n")
                .append("Follow the customer message language when applying these rules;\n")
                .append("keep names/amounts/currency strictly consistent with the JSON items.\n");
        if (slotCapEff > 0 && regCapEff > 0) {
            sys.append("(플랜) 판매 활성(동시 노출 가능) 최대 ").append(slotCapEff)
                    .append("종, 등록 보관 총 ").append(regCapEff)
                    .append("종까지 가능합니다. 현재 고객 노출 카탈로그는 ").append(publicProductCount)
                    .append("개입니다.(본사 판매금지 등 제외 시 더 적을 수 있음)\n");
        }
        sys.append("\n결제 링크 안내: 고객이 상품을 고르면 동일 사이트의 URL 결제(pay)로 안내할 수 있습니다. compId는 ")
                .append(ou.get().getCode()).append(" 입니다.");
        String kb = merchantChatbotKbService.publicChatKnowledgeAppendix(ou.get());
        if (kb != null && !kb.isBlank()) {
            sys.append("\n\n가맹점 기본 안내(고객 문의 시 사실로 답할 수 있는 범위):\n").append(kb);
        }

        try {
            String reply = chatbotLlmCompletionService.completeChat(aiCfg, sys.toString(), messages);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("reply", reply);
            return ResponseEntity.ok(ApiResponse.ok(data));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "LLM 호출 실패";
            return ResponseEntity.ok(ApiResponse.fail(msg, "LLM_ERROR"));
        }
    }

    private static String str(Object o) {
        if (o == null) {
            return null;
        }
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : s;
    }

    private static boolean hasGuestImageInMessages(List<Map<String, String>> messages) {
        if (messages == null) {
            return false;
        }
        for (Map<String, String> m : messages) {
            if (m == null) {
                continue;
            }
            String c = m.get("content");
            if (c != null && c.contains(GUEST_IMAGE_MARKER)) {
                return true;
            }
        }
        return false;
    }

    private static String guestExt(String name) {
        if (name == null) {
            return null;
        }
        int i = name.lastIndexOf('.');
        if (i < 0 || i >= name.length() - 1) {
            return null;
        }
        String ext = name.substring(i + 1).trim().toLowerCase(Locale.ROOT);
        if ("png".equals(ext) || "jpg".equals(ext) || "jpeg".equals(ext)) {
            return ext.equalsIgnoreCase("jpeg") ? "jpg" : ext;
        }
        return null;
    }

    private static String optStr(Object o) {
        if (o == null) {
            return "";
        }
        return String.valueOf(o).trim();
    }

    /**
     * 대화 목록에서 <strong>가장 마지막</strong> {@code role=user} 메시지(이번 질문) 언어에 맞춰 답하도록 지시합니다.
     * 대화 도중 다른 언어로 바꾸면 같은 언어로 이어 받습니다.
     */
    private static String customerLanguageDirective(List<Map<String, String>> messages) {
        String latestUserText = "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            Map<String, String> m = messages.get(i);
            if (m == null) {
                continue;
            }
            String role = m.get("role");
            String content = m.get("content");
            if (role == null || content == null) {
                continue;
            }
            if ("user".equalsIgnoreCase(role.trim())) {
                latestUserText = content.trim();
                break;
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[언어 규칙] 이번 답변은 반드시 **고객이 방금 작성한 마지막 사용자 메시지**에 쓰인 언어와 동일해야 합니다. ");
        sb.append("대화 중간에 고객이 다른 언어로 질문하면, 그 메시지의 언어로만 답하세요. 앞쪽 메시지 언어에 고정되지 마세요.");
        sb.append(' ');
        if (!latestUserText.isEmpty()) {
            boolean ko = HAS_HANGUL.matcher(latestUserText).find();
            boolean ja = HAS_KANA.matcher(latestUserText).find();
            if (ko && !ja) {
                sb.append("(감지) 방금 사용자 메시지에 한글(한국어)이 포함되어 있습니다. 이번 응답은 한국어만 사용하세요.");
                return sb.toString();
            }
            if (ja && !ko) {
                sb.append("(検知) 直近のユーザーメッセージにはひらがな・カタカナ等が含まれます。この応答は必ず自然な日本語のみで書いてください。");
                return sb.toString();
            }
            if (ko && ja) {
                sb.append("방금 메시지에 한글과 일본어 표기가 섞여 있습니다. 주된 표기 언어에 맞춰 답하세요.");
                return sb.toString();
            }
        }
        sb.append("그 밖의 언어(영어·태국어 등)도 고객이 그 언어로 쓴 것과 같은 언어로만 답합니다.");
        return sb.toString();
    }
}
