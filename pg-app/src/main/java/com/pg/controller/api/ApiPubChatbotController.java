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

    /** 고객 메시지 언어 힌트 */
    private static final Pattern HAS_HANGUL = Pattern.compile("[\\uAC00-\\uD7AF\\u1100-\\u11FF\\u3130-\\u318F]");
    private static final Pattern HAS_KANA = Pattern.compile("[\\u3040-\\u309F\\u30A0-\\u30FF\\u31F0-\\u31FF]");
    private static final Pattern HAS_THAI = Pattern.compile("[\\u0E00-\\u0E7F]");

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
        data.putAll(merchantChatbotKbService.publicMerchantVerticalMeta(ou.get().getId()));
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
        final String outputLanguageLock = customerLanguageDirective(messages);
        StringBuilder sys = new StringBuilder();
        sys.append(outputLanguageLock).append("\n\n");
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
        sys.append("고객 챗봇에 노출되는 항목(JSON, 가맹 사용=Y 및 본사 판매금지 아님):\n").append(catalogJson);
        sys.append("\n각 상품의 promotionShelfYn: Y이면 고객 화면 상단 「프로모션」 영역에 표시되는 대표 상품이며, N이면 상단에는 안 나오고 채팅·안내용 목록에만 포함될 수 있습니다.\n");
        long merchantOuId = ou.get().getId();
        long publicProductCount = catalog.size();
        int slotCapEff = productService.getEffectiveChatbotProductSlotCap(merchantOuId);
        int regCapEff = productService.getEffectiveRegistrationCap(merchantOuId);
        sys.append("\n\n[필수 규칙 — 항목 범위]\n")
                .append("각 항목에는 listingType이 있습니다: SALE=일반 판매, RESERVATION_TIME=날짜·시간 단위 예약(미팅·서비스 슬롯 등), ")
                .append("RESERVATION_PLACE=숙박·장소 예약(호텔·펜션·객실 등: 체크인 일시·체크아웃 날짜 중심 안내). ")
                .append("RESERVATION_PLACE는 고객이 체크인과 퇴실일을 정하는 숙박형이며, RESERVATION_TIME과 안내 톤을 구분하세요.\n")
                .append("각 항목에는 itemNature(항목 성격)가 있을 수 있습니다: GOODS/FOOD/ANIMAL/SERVICE/SERVICE_PERSON(사람 서비스) 등.\n")
                .append("- itemNature=SERVICE_PERSON(사람 서비스)이면, 사람을 물건/상품처럼 표현하면 안 됩니다. '상품 구매' 같은 표현 금지.\n")
                .append("  대신 '서비스 예약/선택', '파트너/스태프/아티스트 선택', '예약 대상'처럼 존중 표현을 사용하고, 이름은 title 그대로 부르세요.\n")
                .append("항목 사진을 보여 줄 때는 해당 JSON 행의 imageUrl(또는 imageUrls) 값을 그대로 사용하세요. 비어 있으면 사진이 없다고 안내합니다. ")
                .append("고객 화면에서 사진과 함께 반드시 누구/무엇인지 알 수 있게 하세요: 마크다운 이미지는 `![표시 텍스트](imageUrl)` 한 줄로만 쓰고, 표시 텍스트(alt)에는 JSON의 title과 description 요지(이름·한 줄 소개)를 함께 넣으세요. ")
                .append("alt를 비우거나 `![` 와 `](` 를 줄바꿈으로 끊어 쓰지 마세요(화면에 `[` `](` 잡글자가 남습니다). ")
                .append("`[이미지 URL: imageUrl]` 형식을 쓸 경우에도, 그 직전 문장에 동일 항목의 title·요약을 반드시 문장으로 적으세요(사진만 단독 금지). 임의 URL 금지.\n")
                .append("위 JSON 배열만 이 가맹점의 결제 가능한 등록 항목입니다. 목록에 없는 이름·가격·옵션을 지어내거나 추측하지 마세요.\n")
                .append("고객이 목록에 없는 항목을 원하면 목록 안의 항목으로만 안내 가능함을 알리고, 과장·허위 없이 안내하세요.\n")
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
                .append("- 고객 채팅 화면에서는 긴 pay.html 주소를 같은 메시지에 반복 나열하지 마세요. ")
                .append("pay.html URL의 쿼리에서 & 앞에 백슬래시(\\)를 넣지 마세요(예: \\& 형태 금지). 반드시 원문 & 만 사용하세요. ")
                .append("이스케이프된 주소는 브라우저에서 HTTP 400 오류가 납니다. ")
                .append("urlPayPrefillUrl 은 해당 상품 안내에 한 번만 정확히 포함하면 됩니다. ")
                .append("클라이언트가 listingType·payLinkVerbKo 에 맞춰 「예약하기/구매하기/숙박 예약하기」 형태의 버튼으로 바꿔 보여 주므로, ")
                .append("본문에서는 '아래 버튼으로 결제(또는 예약)를 진행해 주세요'처럼 짧게 안내해도 됩니다.\n")
                .append("- 링크 버튼 문구(내부 참고): payLinkVerbKo / payLinkVerbJa 및 listingType을 참고해 예약·구매 표현을 고객 언어에 맞게 선택하세요.\n")
                .append("- 금액·통화 설명은 아래 JSON(urlPayCheckoutFacts)의 checkoutCurrencyCode·urlPayPricingMode·")
                .append("urlPayDisplayFxActive·urlPaySettlementCurrencyCode·urlPayCustomerCurrencyHintKo 를 근거로 하세요. ")
                .append("표시 통화와 실제 청구 통화가 다를 수 있음을 고객 언어로 명확히 안내하세요.\n");
        sys.append("\nURL 결제·통화 사실(urlPayCheckoutFacts):\n").append(urlPayFactsJson);
        String kb = merchantChatbotKbService.publicChatKnowledgeAppendix(ou.get());
        if (kb != null && !kb.isBlank()) {
            sys.append("\n\n가맹점 기본 안내(고객 문의 시 사실로 답할 수 있는 범위):\n").append(kb);
        }
        Map<String, Object> vertMeta = merchantChatbotKbService.publicMerchantVerticalMeta(ou.get().getId());
        try {
            sys.append("\n\n[가맹점 업체성격·수집 힌트(JSON)]\n")
                    .append(objectMapper.writeValueAsString(vertMeta))
                    .append("""

                            [필수 — 업체성격·예약 정보(대화 단계)]
                            - 위 JSON의 chatbotMerchantVertical*, chatbotMerchantVerticalCollectHintKo, chatbotMerchantVerticalNotes 를 반드시 참고하세요.
                            - 고객이 결제·주문서(화면)로 가기 전에, 업체성격에 맞는 방문·예약 정보(일시·타임존, 방문 인원, 이용 시간(분), 룸·좌석·동행 등)를 고객 언어로 **먼저** 확인하세요. 빠진 항목이 있으면 질문으로 채운 뒤 안내하세요.
                            - 상품 listingType 이 RESERVATION_TIME 또는 RESERVATION_PLACE 이면 예약 시작 일시는 주문서에서 서버가 검증합니다. 인원·이용 시간 등은 주문서 요청사항에 적히도록 고객에게 안내하고, 대화에서도 요약해 일치하는지 확인하세요.
                            - JSON·가맹 기본 안내에 없는 조건·가격·옵션을 지어내지 마세요.
                            """.stripIndent());
        } catch (Exception ignored) {
            /* JSON 직렬화 실패 시에도 나머지 규칙은 유지 */
        }
        sys.append("\n\n[필수 규칙 — 가맹점 운영방식]\n")
                .append("위 기본 안내에 포함된 「운영방식」및 「챗봇 응대 규칙(운영방식)」을 반드시 따르세요. ")
                .append("선불/후불·판매/예약 안내가 해당 규칙과 모순되지 않게 고객 언어로 통일하고, ")
                .append("결제 링크 안내는 규칙상 허용되는 경우에만 제안하세요.\n");
        sys.append("\n\n[필수 규칙 — 주문·예약]\n")
                .append("고객이 실제 결제·예약을 진행하려면 챗봇 화면의 상품 카드에서 「주문·결제 진행」을 통해 ")
                .append("이름·이메일·전화·주소를 입력하고(RESERVATION_TIME이면 예약 일시·방문 인원, RESERVATION_PLACE이면 체크인 일시·가능하면 체크아웃 날짜·인원), ")
                .append("업체성격에 따라 이용 시간(분)·룸·요청사항 등은 주문서 필드 또는 요청사항에 적도록 안내합니다. ")
                .append("서버가 안내하는 결제 페이지로 이동해야 합니다. ")
                .append("결제가 완료되어야 주문이 접수(확정)됩니다. ")
                .append("예약 상품(RESERVATION_TIME·RESERVATION_PLACE)은 동일 상품·겹치는 숙박·시간 구간이 막혀 있을 수 있음을 안내하세요. ")
                .append("카탈로그 JSON의 chatbotReservationSlotMinutes·chatbotReservationZoneId(최상위와 동일 키가 있으면 예약 해석 기준)를 참고하세요.\n");
        sys.append("\n[필수 — 예약 상품 사전 수집(대화 → 주문서 일치)]\n")
                .append("아래 JSON의 chatbotReservationOrderPrecheckKo 는 예약형 상품에 공통으로 적용되는 「주문서와 동일한 필수 확인」입니다. ")
                .append("클럽·VIP·음식점·마사지·코스메틱·서비스업 등 업체성격(chatbotMerchantVertical*) 블록은 그에 더해 ")
                .append("반드시 지켜야 할 질문 순서와 톤을 정합니다. ")
                .append("예약자 실명·전화·이메일·일시·인원이 대화에 없는데 결제 URL만 주거나 주문서로 재촉하지 마세요.\n");

        try {
            String reply = chatbotLlmCompletionService.completeChat(aiCfg, sys.toString(), messages);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("reply", sanitizeChatbotPayReplyMarkdown(reply));
            return ResponseEntity.ok(ApiResponse.ok(data));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "LLM 호출 실패";
            return ResponseEntity.ok(ApiResponse.fail(msg, "LLM_ERROR"));
        }
    }

    /**
     * LLM 이 빈 alt·줄바꿈으로 깨뜨린 마크다운 이미지 껍데기([ 와 ]( 단독 줄 등)를 제거해
     * 고객 화면에 잡글자가 남지 않게 합니다.
     */
    private static String sanitizeChatbotPayReplyMarkdown(String reply) {
        if (reply == null || reply.isBlank()) {
            return reply;
        }
        String s = reply.replace("\r\n", "\n");
        s = s.replaceAll("(?m)(^|\\n)[ \\t]*\\[[ \\t]*\\n[ \\t]*\\][ \\t]*\\([ \\t]*(?=\\n)", "$1");
        s = s.replaceAll("(?m)(^|\\n)[ \\t]*\\[\\s*\\][ \\t]*\\([ \\t]*(?=\\n)", "$1");
        s = s.replaceAll("(?m)(^|\\n)[ \\t]*\\[\\s*(?=\\n|$)", "$1");
        s = s.replaceAll("(?m)(^|\\n)[ \\t]*\\]\\(\\s*(?=\\n|$)", "$1");
        /* Markdown/LaTeX-style \\& in pay.html links breaks query parsing (Tomcat may return 400). */
        s = s.replace("\\&", "&");
        s = s.replace("%5C%26", "%26");
        return s;
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
     * 대화 도중 다른 언어로 바꾸면 그 턴의 언어로만 답합니다(영어 질문→영어, 일본어 질문→일본어).
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
        sb.append("[CRITICAL — OUTPUT LANGUAGE]\n");
        sb.append("Your entire reply (every sentence) MUST be written in the same language as the customer's ")
                .append("**latest** user message below. If they switch language mid-thread, follow the latest turn only. ");
        sb.append("Do not answer in Korean when they wrote in English or Japanese, and do not answer in English ")
                .append("when they wrote in Japanese, unless the latest message itself mixes those scripts.\n");
        sb.append("[언어 규칙] 이번 턴 답변 전체는 **방금** 고객이 보낸 마지막 사용자 메시지의 언어와 동일해야 합니다. ");
        sb.append("이전 대화가 한국어여도, 마지막 메시지가 영어/일본어면 그 언어로만 답하세요.\n");
        if (!latestUserText.isEmpty()) {
            boolean ko = HAS_HANGUL.matcher(latestUserText).find();
            boolean jaKana = HAS_KANA.matcher(latestUserText).find();
            boolean th = HAS_THAI.matcher(latestUserText).find();
            int lat = latinLetterCount(latestUserText);
            int han = hanIdeographCount(latestUserText);
            if (ko && !jaKana) {
                sb.append("(감지) 마지막 사용자 메시지에 한글이 있습니다. **응답 전체를 한국어로만** 작성하세요. ");
                sb.append("영어·일본어 문장으로 본문을 쓰지 마세요(JSON에 있는 고유명사·금액 표기는 예외).");
                return sb.toString();
            }
            if (jaKana && !ko) {
                sb.append("(検知) 直近のユーザーメッセージにひらがな・カタカナ等が含まれます。**応答の本文はすべて自然な日本語のみ**で書いてください。 ");
                sb.append("韓国語や英語の文で本文を書かないでください(商品名・金額など固有名・表記は除く)。");
                return sb.toString();
            }
            if (ko && jaKana) {
                sb.append("마지막 메시지에 한글과 일본어 가나가 함께 있습니다. **주된 표기에 맞춰 한 가지 언어로만** 본문을 작성하세요.");
                return sb.toString();
            }
            if (th && lat < 3) {
                sb.append("(Detected) Thai script in the latest user message. Reply **entirely in Thai** ")
                        .append("for the body text; do not use Korean or Japanese for explanations.");
                return sb.toString();
            }
            if (han >= 2 && !ko && !jaKana && lat < 3) {
                sb.append("(Detection) The latest message is mostly Han characters without Hangul or Japanese kana. ")
                        .append("Reply in **one** language only: use **natural Japanese** if the wording or catalog ")
                        .append("context is clearly Japanese; otherwise use **Chinese** (Simplified unless the user ")
                        .append("uses Traditional forms). Do not answer in Korean for the body.");
                return sb.toString();
            }
            if (lat >= 2) {
                sb.append("(Detected) The latest user message is primarily Latin script (e.g. English). ")
                        .append("Write the **entire reply in that language (English)**. ")
                        .append("Do not switch to Korean or Japanese because earlier turns used them; ")
                        .append("do not add Korean/Japanese paragraphs unless the latest user message itself mixes them.");
                return sb.toString();
            }
        }
        sb.append("For any other language, write the full reply only in the language the customer used in their ")
                .append("latest message (match register and script).");
        return sb.toString();
    }

    private static int hanIdeographCount(String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                n++;
            }
        }
        return n;
    }

    private static int latinLetterCount(String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
                n++;
            }
        }
        return n;
    }
}
