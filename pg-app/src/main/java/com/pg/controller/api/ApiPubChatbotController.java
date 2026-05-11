package com.pg.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.api.ApiResponse;
import com.pg.service.ChatbotLlmCompletionService;
import com.pg.service.HqChatbotAiSettingsService;
import com.pg.service.MerchantChatbotOrderService;
import com.pg.service.MerchantChatbotProductService;
import com.pg.service.MerchantChatbotKbService;
import com.pg.entity.OrgUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
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
    private final MerchantChatbotOrderService merchantChatbotOrderService;
    private final ObjectMapper objectMapper;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public ApiPubChatbotController(MerchantChatbotProductService productService,
                                   HqChatbotAiSettingsService hqChatbotAiSettingsService,
                                   ChatbotLlmCompletionService chatbotLlmCompletionService,
                                   MerchantChatbotKbService merchantChatbotKbService,
                                   MerchantChatbotOrderService merchantChatbotOrderService,
                                   ObjectMapper objectMapper) {
        this.productService = productService;
        this.hqChatbotAiSettingsService = hqChatbotAiSettingsService;
        this.chatbotLlmCompletionService = chatbotLlmCompletionService;
        this.merchantChatbotKbService = merchantChatbotKbService;
        this.merchantChatbotOrderService = merchantChatbotOrderService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/catalog")
    public ResponseEntity<ApiResponse<Map<String, Object>>> catalog(@RequestParam String compId,
                                                                   HttpServletRequest request) {
        Optional<OrgUnit> ou = productService.requireMerchantOrgByCode(compId);
        if (ou.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.fail("가맹점 코드를 확인하세요.", "NOT_FOUND"));
        }
        if (!productService.isChatbotPaymentOpenForMerchant(ou.get().getId())) {
            return ResponseEntity.ok(ApiResponse.fail("챗봇 결제가 비활성입니다.", "FORBIDDEN"));
        }
        boolean commerceHold = productService.isMerchantChatbotCommerceHold(ou.get().getId());
        /* 운영 보류여도 상품·로고·안내는 유지하고, 결제 프리필 URL 만 생략(주문 API 는 별도 차단). */
        List<Map<String, Object>> items = productService.listPublicCatalog(ou.get().getId());
        if (!commerceHold) {
            productService.enrichPublicCatalogItemsWithPayUrls(items, ou.get().getCode(), request);
        }
        List<Map<String, Object>> promotionItems = new java.util.ArrayList<>();
        for (Map<String, Object> row : items) {
            if (row != null && "Y".equalsIgnoreCase(String.valueOf(row.getOrDefault("promotionShelfYn", "N")))) {
                promotionItems.add(row);
            }
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("compId", ou.get().getCode());
        data.put("chatbotCommerceHold", commerceHold);
        data.put("items", items);
        data.put("promotionItems", promotionItems);
        data.put("publicPaySiteBase", productService.resolvePublicCustomerSiteBase(request));
        data.putAll(productService.resolveChatbotPublicUi(ou.get().getId()));
        data.put("chatbotWelcomeHint", merchantChatbotKbService.effectiveWelcomeHintForPublic(ou.get().getId()));
        data.putAll(merchantChatbotKbService.publicReservationMeta(ou.get().getId()));
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    /** 고객 주문서 제출 → 결제 URL(OrderNo 고정) 반환. */
    @PostMapping("/order-intent")
    public ResponseEntity<ApiResponse<Map<String, Object>>> orderIntent(@RequestBody Map<String, Object> body,
                                                                        HttpServletRequest request) {
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
        if (!productService.isChatbotCommercialFeaturesOpen(ou.get().getId())) {
            return ResponseEntity.ok(ApiResponse.fail(
                    "운영 보류 중입니다. 상품 주문·예약·결제는 접수되지 않습니다.", "COMMERCE_HOLD"));
        }
        try {
            Map<String, Object> data = merchantChatbotOrderService.createPublicOrderIntent(compId, body, request);
            return ResponseEntity.ok(ApiResponse.ok(data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.fail(e.getMessage(), "VALIDATION"));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "주문 처리 실패";
            return ResponseEntity.ok(ApiResponse.fail(msg, "ORDER_ERROR"));
        }
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
    public ResponseEntity<ApiResponse<Map<String, Object>>> chat(@RequestBody Map<String, Object> body,
                                                                HttpServletRequest request) {
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
        boolean commerceHold = productService.isMerchantChatbotCommerceHold(ou.get().getId());
        List<Map<String, Object>> catalog = productService.listPublicCatalog(ou.get().getId());
        if (!commerceHold) {
            productService.enrichPublicCatalogItemsWithPayUrls(catalog, ou.get().getCode(), request);
        }
        Map<String, Object> urlPayFacts = commerceHold ? Map.of() : productService.urlPayFactsForChatbotLlm(ou.get().getId());
        String catalogJson;
        String urlPayFactsJson;
        try {
            catalogJson = objectMapper.writeValueAsString(catalog);
            urlPayFactsJson = objectMapper.writeValueAsString(urlPayFacts);
        } catch (Exception e) {
            catalogJson = "[]";
            urlPayFactsJson = "{}";
        }
        StringBuilder sys = new StringBuilder();
        if (!basePrompt.isEmpty()) {
            sys.append(basePrompt).append("\n\n");
        }
        if (!catalogInstr.isEmpty()) {
            sys.append(catalogInstr).append("\n\n");
        }
        if (commerceHold) {
            sys.append("""
                    [중요 — 상업 기능 운영 보류]
                    이 가맹점 챗봇은 현재 「운영 보류」 상태입니다. 아래 상품 JSON은 안내용(노출) 정보일 수 있으나, 결제·주문·예약 접수는 할 수 없습니다.
                    - 각 상품에 urlPayPrefillUrl 이 없거나 비어 있으면 결제 페이지로 안내하지 마세요. 임의 URL 을 만들지 마세요.
                    - 일반 문의·안내(회사 정보·연락처·위치 등)는 아래 「가맹점 기본 안내」 범위에서 답할 수 있습니다.
                    - JSON 에 있는 상품명·가격은 참고용이며, 구매·예약 진행은 불가임을 고객에게 알리고 「기본 안내」 연락처로 안내하세요.
                    
                    """.stripIndent()).append('\n');
        }
        sys.append("고객 챗봇에 노출되는 상품(JSON, 가맹 사용=Y 및 본사 판매금지 아님):\n").append(catalogJson);
        sys.append("\n각 상품의 promotionShelfYn: Y이면 고객 화면 상단 「프로모션」 영역에 표시되는 대표 상품이며, N이면 상단에는 안 나오고 채팅·안내용 목록에만 포함될 수 있습니다.\n");
        long merchantOuId = ou.get().getId();
        long publicProductCount = catalog.size();
        int slotCapEff = productService.getEffectiveChatbotProductSlotCap(merchantOuId);
        int regCapEff = productService.getEffectiveRegistrationCap(merchantOuId);
        sys.append("\n\n[필수 규칙 — 상품 범위]\n")
                .append("각 상품에는 listingType이 있습니다: SALE=일반 판매, RESERVATION_TIME=날짜·시간 단위 예약(미팅·서비스 슬롯 등), ")
                .append("RESERVATION_PLACE=숙박·장소 예약(호텔·펜션·객실 등: 체크인 일시·체크아웃 날짜 중심 안내). ")
                .append("RESERVATION_PLACE는 고객이 체크인과 퇴실일을 정하는 숙박형이며, RESERVATION_TIME과 안내 톤을 구분하세요.\n")
                .append("상품 사진을 보여 줄 때는 해당 JSON 행의 imageUrl 값을 그대로 사용하세요. 비어 있으면 사진이 없다고 안내합니다. ")
                .append("고객 화면에서 사진이 보이도록 할 때는 본문에 ![상품](imageUrl) 또는 한 줄에 [이미지 URL: imageUrl] 형식으로 포함하세요(임의 URL 금지).\n")
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
        sys.append("\n\n[필수 규칙 — 결제 링크·통화]\n")
                .append("- 고객에게 줄 결제 URL은 반드시 각 상품 객체의 urlPayPrefillUrl 값만 사용하세요. ")
                .append("placeholder 도메인(example.com)·추측 URL·임의 경로를 만들지 마세요.\n")
                .append("- 링크 버튼 문구: payLinkVerbKo / payLinkVerbJa 및 listingType을 참고해 예약·구매 표현을 고객 언어에 맞게 선택하세요.\n")
                .append("- 금액·통화 설명은 아래 JSON(urlPayCheckoutFacts)의 checkoutCurrencyCode·urlPayPricingMode·")
                .append("urlPayDisplayFxActive·urlPaySettlementCurrencyCode·urlPayCustomerCurrencyHintKo 를 근거로 하세요. ")
                .append("표시 통화와 실제 청구 통화가 다를 수 있음을 고객 언어로 명확히 안내하세요.\n");
        sys.append("\nURL 결제·통화 사실(urlPayCheckoutFacts):\n").append(urlPayFactsJson);
        String kb = merchantChatbotKbService.publicChatKnowledgeAppendix(ou.get());
        if (kb != null && !kb.isBlank()) {
            sys.append("\n\n가맹점 기본 안내(고객 문의 시 사실로 답할 수 있는 범위):\n").append(kb);
        }
        sys.append("\n\n[필수 규칙 — 가맹점 운영방식]\n")
                .append("위 기본 안내에 포함된 「운영방식」및 「챗봇 응대 규칙(운영방식)」을 반드시 따르세요. ")
                .append("선불/후불·판매/예약 안내가 해당 규칙과 모순되지 않게 고객 언어로 통일하고, ")
                .append("결제 링크 안내는 규칙상 허용되는 경우에만 제안하세요.\n");
        sys.append("\n\n[필수 규칙 — 주문·예약]\n")
                .append("고객이 실제 결제·예약을 진행하려면 챗봇 화면의 상품 카드에서 「주문·결제 진행」을 통해 ")
                .append("이름·이메일·전화·주소를 입력하고(RESERVATION_TIME이면 예약 일시, RESERVATION_PLACE이면 체크인 일시와 가능하면 체크아웃 날짜), ")
                .append("서버가 안내하는 결제 페이지로 이동해야 합니다. ")
                .append("결제가 완료되어야 주문이 접수(확정)됩니다. ")
                .append("예약 상품(RESERVATION_TIME·RESERVATION_PLACE)은 동일 상품·겹치는 숙박·시간 구간이 막혀 있을 수 있음을 안내하세요. ")
                .append("카탈로그 JSON의 chatbotReservationSlotMinutes·chatbotReservationZoneId(최상위와 동일 키가 있으면 예약 해석 기준)를 참고하세요.\n");

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
