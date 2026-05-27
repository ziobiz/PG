package com.pg.service;

import com.pg.chatbot.ChatbotListingType;
import com.pg.chatbot.ChatbotReservationCollectMode;
import com.pg.entity.MerchantChatbotOrder;
import com.pg.entity.MerchantChatbotProduct;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgTrnsctn;
import com.pg.repository.MerchantChatbotOrderRepository;
import com.pg.repository.MerchantChatbotProductRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.util.ChillPayDirectCreditUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 챗봇 고객 주문 접수(주문자·상품·예약) 및 결제 승인 후 주문 확정.
 */
@Service
public class MerchantChatbotOrderService {

    private static final Logger log = LoggerFactory.getLogger(MerchantChatbotOrderService.class);
    private static final String PAID_STATUS = "10";

    private final MerchantChatbotOrderRepository orderRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final MerchantChatbotProductRepository productRepository;
    private final MerchantChatbotProductService productService;
    private final ChatbotPayLinkDeliveryService chatbotPayLinkDeliveryService;

    public MerchantChatbotOrderService(MerchantChatbotOrderRepository orderRepository,
                                       OrgUnitRepository orgUnitRepository,
                                       MerchantProfileRepository merchantProfileRepository,
                                       MerchantChatbotProductRepository productRepository,
                                       MerchantChatbotProductService productService,
                                       ChatbotPayLinkDeliveryService chatbotPayLinkDeliveryService) {
        this.orderRepository = orderRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.productRepository = productRepository;
        this.productService = productService;
        this.chatbotPayLinkDeliveryService = chatbotPayLinkDeliveryService;
    }

    /**
     * ChillPay 거래가 승인(10)으로 적재된 직후·노티 후 호출 — 대기 주문을 확정하고 PG 거래번호를 연결합니다.
     */
    @Transactional
    public void tryConfirmOrderAfterPaidTxn(PgTrnsctn t) {
        if (t == null || t.getOrderNo() == null || t.getOrderNo().isBlank()) {
            return;
        }
        if (!PAID_STATUS.equals(trim(t.getStatus()))) {
            return;
        }
        String orderNo = ChillPayDirectCreditUtil.normalizeOrderNo(t.getOrderNo());
        String merchantCode = trim(t.getMerchantId());
        if (merchantCode.isEmpty()) {
            return;
        }
        Optional<MerchantChatbotOrder> opt = orderRepository.findByCheckoutOrderNo(orderNo);
        if (opt.isEmpty()) {
            return;
        }
        MerchantChatbotOrder o = opt.get();
        if (!orgCodeMatches(merchantCode, o.getOrgUnitId())) {
            log.debug("주문 확정 스킵: 가맹코드 불일치 orderNo={} mid={}", orderNo, merchantCode);
            return;
        }
        if (MerchantChatbotOrder.STATUS_CONFIRMED.equals(o.getStatus())) {
            if (t.getTrnId() != null && !t.getTrnId().isBlank()
                    && (o.getPgTrnId() == null || o.getPgTrnId().isBlank())) {
                o.setPgTrnId(trimToMax(t.getTrnId(), 20));
                orderRepository.save(o);
            }
            return;
        }
        if (MerchantChatbotOrder.STATUS_PENDING.equals(o.getStatus())) {
            o.setStatus(MerchantChatbotOrder.STATUS_CONFIRMED);
            if (t.getTrnId() != null && !t.getTrnId().isBlank()) {
                o.setPgTrnId(trimToMax(t.getTrnId(), 20));
            }
            orderRepository.save(o);
        }
    }

    private boolean orgCodeMatches(String merchantCode, Long orgUnitId) {
        if (orgUnitId == null) {
            return false;
        }
        return orgUnitRepository.findById(orgUnitId)
                .map(ou -> merchantCode.equalsIgnoreCase(trim(ou.getCode())))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listOrderRowsForOrg(Long orgUnitId) {
        if (orgUnitId == null) {
            return List.of();
        }
        List<MerchantChatbotOrder> rows = orderRepository.findTop200ByOrgUnitIdOrderByCreatedAtDesc(orgUnitId);
        List<Map<String, Object>> out = new java.util.ArrayList<>();
        for (MerchantChatbotOrder o : rows) {
            out.add(toAdminRow(o));
        }
        return out;
    }

    private static Map<String, Object> toAdminRow(MerchantChatbotOrder o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", o.getId());
        m.put("status", o.getStatus());
        m.put("productTitle", nz(o.getProductTitle()));
        m.put("productId", o.getProductId());
        m.put("amount", o.getAmount() != null ? o.getAmount().stripTrailingZeros().toPlainString() : "0");
        m.put("currencyCode", nz(o.getCurrencyCode()));
        m.put("listingTypeSnapshot", nz(o.getListingTypeSnapshot()));
        m.put("reservationCollectSnapshot", nz(o.getReservationCollectSnapshot()));
        m.put("productLineTotalAmount", o.getProductLineTotalAmount() != null
                ? o.getProductLineTotalAmount().stripTrailingZeros().toPlainString() : "");
        m.put("balanceDueAmount", o.getBalanceDueAmount() != null
                ? o.getBalanceDueAmount().stripTrailingZeros().toPlainString() : "");
        m.put("ordererName", nz(o.getOrdererName()));
        m.put("ordererEmail", nz(o.getOrdererEmail()));
        m.put("ordererPhone", nz(o.getOrdererPhone()));
        m.put("ordererAddr", nz(o.getOrdererAddr()));
        m.put("reservationStart", o.getReservationStart() != null ? o.getReservationStart().toString() : "");
        m.put("reservationEnd", o.getReservationEnd() != null ? o.getReservationEnd().toString() : "");
        m.put("checkoutOrderNo", nz(o.getCheckoutOrderNo()));
        m.put("pgTrnId", nz(o.getPgTrnId()));
        m.put("orderMemo", nz(o.getOrderMemo()));
        m.put("createdAt", o.getCreatedAt() != null ? o.getCreatedAt().toString() : "");
        m.put("updatedAt", o.getUpdatedAt() != null ? o.getUpdatedAt().toString() : "");
        return m;
    }

    /**
     * 공개 챗봇에서 주문·예약 정보를 받고 결제 URL(동일 OrderNo 프리필)을 반환합니다.
     */
    @Transactional
    public Map<String, Object> createPublicOrderIntent(String compId,
                                                      Map<String, Object> body,
                                                      HttpServletRequest request) {
        String cid = trim(compId);
        if (cid.isEmpty()) {
            throw new IllegalArgumentException("가맹점 코드가 필요합니다.");
        }
        OrgUnit ou = orgUnitRepository.findByCode(cid)
                .orElseThrow(() -> new IllegalArgumentException("가맹점 코드를 확인하세요."));
        if (!productService.isChatbotPaymentOpenForMerchant(ou.getId())) {
            throw new IllegalArgumentException("챗봇 결제가 비활성입니다.");
        }
        if (!productService.isChatbotCommercialFeaturesOpen(ou.getId())) {
            throw new IllegalArgumentException("운영 보류 중입니다. 상품 주문·예약은 접수되지 않습니다.");
        }
        MerchantProfile mp = merchantProfileRepository.findByOrgUnitId(ou.getId())
                .orElseThrow(() -> new IllegalArgumentException("가맹 프로필을 찾을 수 없습니다."));

        String ordererName = clamp(trim(str(body, "ordererName")), 100);
        String ordererEmail = clamp(trim(str(body, "ordererEmail")), 120);
        String ordererPhone = clamp(trim(str(body, "ordererPhone")), 50);
        String ordererAddr = clamp(trim(str(body, "ordererAddr")), 600);
        if (ordererPhone.length() < 6) {
            throw new IllegalArgumentException("연락처(전화)를 입력하세요.");
        }
        if (!ordererEmail.isEmpty() && !ordererEmail.contains("@")) {
            throw new IllegalArgumentException("유효한 이메일을 입력하세요.");
        }
        boolean sendPayLinkEmail = truthy(body.get("sendPayLinkEmail"));
        String lineNotifyTokenUser = clamp(trim(str(body, "lineNotifyToken")), 256);
        if (sendPayLinkEmail && ordererEmail.isEmpty()) {
            throw new IllegalArgumentException("결제 링크를 이메일로 받으려면 이메일을 입력하세요.");
        }

        Long productId = parseLongObj(body.get("productId"));
        MerchantChatbotProduct product = null;
        String listingType = ChatbotListingType.SALE.getCode();
        String title;
        BigDecimal amountBd;
        String currency;
        ChatbotReservationCollectMode collectModeSnapshot = ChatbotReservationCollectMode.FULL;

        if (productId != null && productId > 0) {
            product = productRepository.findById(productId)
                    .filter(p -> ou.getId().equals(p.getOrgUnitId()))
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
            if (!"Y".equalsIgnoreCase(trim(product.getUseYn()))) {
                throw new IllegalArgumentException("판매 중인 상품이 아닙니다.");
            }
            if ("Y".equalsIgnoreCase(trim(product.getHqCatalogBlockYn()))) {
                throw new IllegalArgumentException("해당 상품은 노출되지 않습니다.");
            }
            listingType = normalizeListing(product.getListingType());
            title = product.getTitle() != null ? product.getTitle().trim() : "";
            currency = product.getCurrencyCode() != null ? product.getCurrencyCode().trim().toUpperCase(Locale.ROOT) : "KRW";
            ChatbotReservationCollectMode collect =
                    ChatbotReservationCollectMode.resolve(product.getReservationCollectMode());
            BigDecimal lineTotal = product.getAmount() != null ? product.getAmount() : BigDecimal.ZERO;
            amountBd = lineTotal;
            if (needsReservationListing(listingType) && collect == ChatbotReservationCollectMode.DEPOSIT) {
                BigDecimal dep = product.getDepositAmount();
                if (dep == null || dep.compareTo(BigDecimal.ZERO) <= 0 || dep.compareTo(lineTotal) >= 0) {
                    throw new IllegalArgumentException("이 상품의 예약금 설정을 확인하세요.");
                }
                amountBd = dep;
            }
            collectModeSnapshot = collect;
        } else {
            title = clamp(trim(str(body, "itemTitle")), 200);
            if (title.isEmpty()) {
                throw new IllegalArgumentException("상품명이 필요합니다.");
            }
            amountBd = parseAmount(str(body, "amount"));
            if (amountBd == null || amountBd.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("금액을 확인하세요.");
            }
            currency = trim(str(body, "currencyCode"));
            if (currency.isEmpty()) {
                currency = "KRW";
            }
            currency = currency.toUpperCase(Locale.ROOT);
        }

        if (ordererName.isEmpty() || ordererName.trim().length() < 2) {
            throw new IllegalArgumentException(needsReservationListing(listingType)
                    ? "예약자·방문 대표 성명을 2자 이상 입력하세요."
                    : "성명을 2자 이상 입력하세요.");
        }

        int slotMinutes = resolveSlotMinutes(mp, product);
        ZoneId zone = resolveZone(mp);

        Instant resStart = null;
        Instant resEnd = null;
        if (needsReservationListing(listingType)) {
            boolean placeStay = ChatbotListingType.RESERVATION_PLACE.getCode().equalsIgnoreCase(listingType);
            String local = trim(str(body, "reservationLocal"));
            if (local.isEmpty()) {
                throw new IllegalArgumentException(placeStay ? "체크인 일시를 선택하세요." : "예약 일시를 선택하세요.");
            }
            try {
                LocalDateTime ldt = LocalDateTime.parse(local);
                resStart = ldt.atZone(zone).toInstant();
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException(
                        placeStay ? "체크인 일시 형식이 올바르지 않습니다." : "예약 일시 형식이 올바르지 않습니다.");
            }
            if (placeStay) {
                String checkoutRaw = trim(str(body, "reservationCheckoutLocal"));
                if (!checkoutRaw.isEmpty()) {
                    LocalDate checkoutDate;
                    try {
                        checkoutDate = LocalDate.parse(checkoutRaw);
                    } catch (DateTimeParseException ex) {
                        throw new IllegalArgumentException("체크아웃 날짜는 YYYY-MM-DD 형식이어야 합니다.");
                    }
                    LocalDate checkinDate = resStart.atZone(zone).toLocalDate();
                    if (!checkoutDate.isAfter(checkinDate)) {
                        throw new IllegalArgumentException("체크아웃 날짜는 체크인 다음 날 이후여야 합니다.");
                    }
                    /* 숙박 구간 끝: 퇴실일 정오(예약 충돌 검사용 기본값). 실제 퇴실 시각은 가맹 운영에 맞게 요청사항 등으로 조정 */
                    resEnd = checkoutDate.atTime(12, 0).atZone(zone).toInstant();
                    if (!resEnd.isAfter(resStart)) {
                        throw new IllegalArgumentException("숙박 기간이 올바르지 않습니다. 체크인·체크아웃을 확인하세요.");
                    }
                } else {
                    resEnd = resStart.plusSeconds(slotMinutes * 60L);
                }
            } else {
                resEnd = resStart.plusSeconds(slotMinutes * 60L);
            }
            long overlap = orderRepository.countReservationOverlap(ou.getId(), product != null ? product.getId() : -1L,
                    resStart, resEnd, null);
            if (product != null && overlap > 0) {
                throw new IllegalArgumentException("해당 시간대는 이미 예약이 있습니다. 다른 시간을 선택하세요.");
            }
            /* productId 없는 자유 주문은 예약 겹침 검사 생략 */
        }

        Integer guestCount = firstPositiveInt(body.get("guestCount"), body.get("partySize"));
        Integer serviceDurationMinutes = parsePositiveInt(body.get("serviceDurationMinutes"));
        if (needsReservationListing(listingType)) {
            if (guestCount == null || guestCount < 1 || guestCount > 999) {
                throw new IllegalArgumentException("예약·방문 인원(1~999명)을 입력하세요.");
            }
        }
        if (serviceDurationMinutes != null
                && (serviceDurationMinutes < 5 || serviceDurationMinutes > 24 * 60 * 2)) {
            throw new IllegalArgumentException("이용 시간(분)은 5~2880 범위로 입력하세요.");
        }

        String checkoutNo = allocateCheckoutOrderNo(ou.getCode());
        MerchantChatbotOrder order = new MerchantChatbotOrder();
        order.setOrgUnitId(ou.getId());
        order.setProductId(product != null ? product.getId() : null);
        order.setProductTitle(title);
        order.setAmount(amountBd);
        order.setCurrencyCode(currency);
        order.setListingTypeSnapshot(listingType);
        order.setReservationCollectSnapshot(collectModeSnapshot.getCode());
        if (product != null) {
            BigDecimal full = product.getAmount() != null ? product.getAmount() : BigDecimal.ZERO;
            order.setProductLineTotalAmount(full.setScale(4, RoundingMode.HALF_UP));
            if (collectModeSnapshot == ChatbotReservationCollectMode.DEPOSIT) {
                order.setBalanceDueAmount(full.subtract(amountBd).setScale(4, RoundingMode.HALF_UP));
            } else {
                order.setBalanceDueAmount(null);
            }
        } else {
            order.setProductLineTotalAmount(null);
            order.setBalanceDueAmount(null);
        }
        order.setOrdererName(ordererName.isEmpty() ? null : ordererName);
        order.setOrdererEmail(ordererEmail.isEmpty() ? null : ordererEmail);
        order.setOrdererPhone(ordererPhone);
        order.setOrdererAddr(ordererAddr.isEmpty() ? null : ordererAddr);
        order.setReservationStart(resStart);
        order.setReservationEnd(resEnd);
        order.setStatus(MerchantChatbotOrder.STATUS_PENDING);
        order.setCheckoutOrderNo(checkoutNo);
        String userMemo = trim(str(body, "orderMemo"));
        String combinedMemo = combineOrderMemoWithStructuredLines(userMemo, guestCount, serviceDurationMinutes);
        order.setOrderMemo(combinedMemo.isEmpty() ? null : clamp(combinedMemo, 4000));
        orderRepository.save(order);

        String payUrl = buildPayPrefillUrl(request, ou.getId(), ou.getCode(), title, amountBd.toPlainString(), currency, checkoutNo);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("orderId", order.getId());
        data.put("checkoutOrderNo", checkoutNo);
        data.put("payUrl", payUrl);
        chatbotPayLinkDeliveryService.deliverIfRequested(data, sendPayLinkEmail, ordererEmail, lineNotifyTokenUser,
                ou.getCode(), checkoutNo, title, amountBd, currency, payUrl);
        return data;
    }

    private String buildPayPrefillUrl(HttpServletRequest request, Long orgUnitId, String compCode, String itemTitle,
                                      String amountPlain, String currencyIso, String orderNo) {
        boolean repayMode = productService.isMerchantUrlPayCheckoutRepay(orgUnitId);
        String base = trimSlash(productService.resolvePublicCustomerSiteBase(request));
        StringBuilder q = new StringBuilder();
        q.append("m=").append(urlEncode(compCode));
        q.append("&entry=chatbot");
        if (repayMode) {
            q.append("&variant=repay");
        }
        q.append("&orderNo=").append(urlEncode(orderNo));
        if (!itemTitle.isEmpty()) {
            String t = itemTitle.length() > 500 ? itemTitle.substring(0, 500) : itemTitle;
            q.append("&item=").append(urlEncode(t));
        }
        if (!amountPlain.isEmpty()) {
            String a = amountPlain.length() > 40 ? amountPlain.substring(0, 40) : amountPlain;
            q.append("&amount=").append(urlEncode(a));
        }
        if (!currencyIso.isEmpty()) {
            q.append("&currency=").append(urlEncode(currencyIso));
        }
        String path = "/pay.html?" + q;
        if (base.isEmpty()) {
            return path;
        }
        return base + path;
    }

    private static String urlEncode(String raw) {
        return URLEncoder.encode(raw != null ? raw : "", StandardCharsets.UTF_8);
    }

    private static String trimSlash(String u) {
        if (u == null) {
            return "";
        }
        return u.trim().replaceAll("/+$", "");
    }

    private String allocateCheckoutOrderNo(String compCode) {
        String c = compCode != null ? compCode.replaceAll("[^0-9A-Za-z]", "") : "";
        if (c.length() > 6) {
            c = c.substring(0, 6);
        }
        for (int i = 0; i < 12; i++) {
            String raw = "P" + c + System.currentTimeMillis();
            if (raw.length() > 20) {
                raw = raw.substring(0, 20);
            }
            String normalized = ChillPayDirectCreditUtil.normalizeOrderNo(raw);
            if (!orderRepository.existsByCheckoutOrderNo(normalized)) {
                return normalized;
            }
            try {
                Thread.sleep(2L);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new IllegalStateException("주문번호 생성에 실패했습니다. 잠시 후 다시 시도하세요.");
    }

    private static int resolveSlotMinutes(MerchantProfile mp, MerchantChatbotProduct product) {
        if (product != null && product.getReservationSlotMinutes() != null && product.getReservationSlotMinutes() > 0) {
            return clampSlot(product.getReservationSlotMinutes());
        }
        int def = mp.getChatbotReservationSlotMinutes() != null ? mp.getChatbotReservationSlotMinutes() : 60;
        return clampSlot(def);
    }

    private static int clampSlot(int m) {
        if (m < 15) {
            return 15;
        }
        if (m > 24 * 60) {
            return 24 * 60;
        }
        return m;
    }

    private static ZoneId resolveZone(MerchantProfile mp) {
        String z = mp.getChatbotReservationZoneId() != null ? mp.getChatbotReservationZoneId().trim() : "";
        if (z.isEmpty()) {
            return ZoneId.of("Asia/Seoul");
        }
        try {
            return ZoneId.of(z);
        } catch (Exception e) {
            return ZoneId.of("Asia/Seoul");
        }
    }

    private static String normalizeListing(String lt) {
        return ChatbotListingType.fromCode(lt != null ? lt : "")
                .map(ChatbotListingType::getCode)
                .orElse(ChatbotListingType.SALE.getCode());
    }

    private static boolean needsReservationListing(String listingCode) {
        return ChatbotListingType.fromCode(listingCode).map(ChatbotListingType::needsReservationWindow).orElse(false);
    }

    private static BigDecimal parseAmount(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(s.trim().replace(",", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private static Long parseLongObj(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String str(Map<String, Object> body, String key) {
        if (body == null) {
            return null;
        }
        Object v = body.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static boolean truthy(Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof Boolean b) {
            return b;
        }
        String s = String.valueOf(o).trim();
        return "1".equals(s) || "true".equalsIgnoreCase(s) || "y".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s);
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String clamp(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }

    private static String trimToMax(String s, int max) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }

    private static Integer parsePositiveInt(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            int v = n.intValue();
            return v > 0 ? v : null;
        }
        try {
            int v = Integer.parseInt(String.valueOf(o).trim());
            return v > 0 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer firstPositiveInt(Object a, Object b) {
        Integer p = parsePositiveInt(a);
        if (p != null) {
            return p;
        }
        return parsePositiveInt(b);
    }

    /**
     * 예약·업체성격 확장 필드(인원·이용시간)를 메모 상단에 고정 형식으로 남겨 가맹 백오피스에서 식별하기 쉽게 합니다.
     */
    private static String combineOrderMemoWithStructuredLines(String userMemo,
                                                                Integer guestCount,
                                                                Integer serviceDurationMinutes) {
        StringBuilder struct = new StringBuilder();
        if (guestCount != null && guestCount > 0) {
            struct.append("[주문·예약] 방문·이용 인원: ").append(guestCount).append("명\n");
        }
        if (serviceDurationMinutes != null && serviceDurationMinutes > 0) {
            struct.append("[주문·예약] 이용 시간(분): ").append(serviceDurationMinutes).append('\n');
        }
        String prefix = struct.toString().trim();
        String u = userMemo == null ? "" : userMemo.trim();
        if (prefix.isEmpty()) {
            return u;
        }
        if (u.isEmpty()) {
            return prefix;
        }
        return prefix + "\n— 요청사항 —\n" + u;
    }
}
