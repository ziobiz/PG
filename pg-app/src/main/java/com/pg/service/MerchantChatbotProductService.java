package com.pg.service;

import com.pg.entity.MerchantChatbotProduct;
import com.pg.entity.MerchantProfile;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.MerchantChatbotProductRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.util.ChatbotProductPricingUtil;
import com.pg.repository.OrgBrandingRepository;
import com.pg.repository.OrgUnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MerchantChatbotProductService {

    private final MerchantChatbotProductRepository productRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final OrgBrandingRepository orgBrandingRepository;
    private final OrgServiceUseService orgServiceUseService;

    public MerchantChatbotProductService(MerchantChatbotProductRepository productRepository,
                                        OrgUnitRepository orgUnitRepository,
                                        MerchantProfileRepository merchantProfileRepository,
                                        OrgBrandingRepository orgBrandingRepository,
                                        OrgServiceUseService orgServiceUseService) {
        this.productRepository = productRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.orgBrandingRepository = orgBrandingRepository;
        this.orgServiceUseService = orgServiceUseService;
    }

    public long countProductsForMerchant(Long merchantOrgUnitId) {
        if (merchantOrgUnitId == null) {
            return 0;
        }
        return productRepository.countByOrgUnitId(merchantOrgUnitId);
    }

    /** 가맹이 판매 활성( use_yn=Y )으로 둔 상품 건수 */
    @Transactional(readOnly = true)
    public long countSaleActiveProductsForMerchant(Long merchantOrgUnitId) {
        if (merchantOrgUnitId == null) {
            return 0;
        }
        return productRepository.countByOrgUnitIdAndUseYn(merchantOrgUnitId, "Y");
    }

    /**
     * 플랜 기준 총 등록(보관) 가능 건수: 판매 활성 상한 + {@link ChatbotProductPricingUtil#CHATBOT_PRODUCT_REGISTER_EXTRA_SLOTS}.
     * 플랜 미적용(0)이면 0.
     */
    @Transactional(readOnly = true)
    public int getEffectiveRegistrationCap(Long orgUnitId) {
        int sale = getEffectiveChatbotProductSlotCap(orgUnitId);
        return registrationCapFromSaleCap(sale);
    }

    private static int registrationCapFromSaleCap(int saleCap) {
        return saleCap > 0 ? saleCap + ChatbotProductPricingUtil.CHATBOT_PRODUCT_REGISTER_EXTRA_SLOTS : 0;
    }

    /**
     * 공개 챗봇 UI: 가맹점명 + 상단 로고(가맹 직접 URL 우선, 없으면 상위 본사·총판·총본사 브랜딩 순).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> resolveChatbotPublicUi(Long merchantOrgUnitId) {
        Map<String, Object> meta = new LinkedHashMap<>();
        if (merchantOrgUnitId == null) {
            return meta;
        }
        OrgUnit merchant = orgUnitRepository.findById(merchantOrgUnitId).orElse(null);
        if (merchant == null) {
            return meta;
        }
        meta.put("merchantName", merchant.getName() != null ? merchant.getName() : "");
        meta.put("compId", merchant.getCode() != null ? merchant.getCode() : "");
        String logo = "";
        Optional<MerchantProfile> mpOpt = merchantProfileRepository.findByOrgUnitId(merchantOrgUnitId);
        if (mpOpt.isPresent()) {
            String own = mpOpt.get().getChatbotHeaderLogoUrl();
            if (own != null && !own.isBlank()) {
                logo = own.trim();
            }
        }
        if (logo.isEmpty()) {
            logo = resolveInheritedBrandingLogoUrl(merchantOrgUnitId).orElse("");
        }
        meta.put("headerLogoUrl", logo);
        boolean chatbotAdminConfigured = mpOpt
                .map(mp -> mp.getChatbotAdminUserId() != null && mp.getChatbotAdminUserId() > 0)
                .orElse(false);
        meta.put("chatbotAdminConfigured", chatbotAdminConfigured);
        return meta;
    }

    private Optional<String> resolveInheritedBrandingLogoUrl(Long merchantOrgUnitId) {
        Long cur = merchantOrgUnitId;
        Set<Long> seen = new HashSet<>();
        while (cur != null && seen.add(cur)) {
            Optional<OrgUnit> opt = orgUnitRepository.findById(cur);
            if (opt.isEmpty()) {
                break;
            }
            OrgUnit u = opt.get();
            if (u.getOrgLevel() == OrgLevel.MASTER_DIST || u.getOrgLevel() == OrgLevel.REGIONAL
                    || u.getOrgLevel() == OrgLevel.HEADQUARTERS) {
                Optional<String> fromBrand = orgBrandingRepository.findByOrgUnitId(u.getId()).flatMap(b -> {
                    if (b.getFirstLogoImageUrl() != null && !b.getFirstLogoImageUrl().isBlank()) {
                        return Optional.of(b.getFirstLogoImageUrl().trim());
                    }
                    if (b.getLogoImageUrl() != null && !b.getLogoImageUrl().isBlank()) {
                        return Optional.of(b.getLogoImageUrl().trim());
                    }
                    if (b.getUrlPayImageUrl() != null && !b.getUrlPayImageUrl().isBlank()) {
                        return Optional.of(b.getUrlPayImageUrl().trim());
                    }
                    return Optional.empty();
                });
                if (fromBrand.isPresent()) {
                    return fromBrand;
                }
            }
            cur = u.getParentId();
        }
        return Optional.empty();
    }

    @Transactional(readOnly = true)
    public Optional<OrgUnit> requireMerchantOrgByCode(String compId) {
        if (compId == null || compId.isBlank()) {
            return Optional.empty();
        }
        return orgUnitRepository.findByCode(compId.trim())
                .filter(ou -> ou.getOrgLevel() == OrgLevel.MERCHANT);
    }

    /** 챗봇결제 사용(Y)·서비스 활성 가맹점만 공개 API 허용 */
    @Transactional(readOnly = true)
    public boolean isChatbotPaymentOpenForMerchant(Long orgUnitId) {
        if (orgUnitId == null) {
            return false;
        }
        if (!orgServiceUseService.isOrgServiceActive(orgUnitId)) {
            return false;
        }
        return merchantProfileRepository.findByOrgUnitId(orgUnitId)
                .map(mp -> "Y".equalsIgnoreCase(mp.getChatbotPaymentUseYn() != null ? mp.getChatbotPaymentUseYn().trim() : ""))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAllForOrg(Long orgUnitId) {
        return productRepository.findByOrgUnitIdOrderBySortOrderAscIdAsc(orgUnitId).stream()
                .map(this::toMap)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listPublicCatalog(Long orgUnitId) {
        return productRepository
                .findByOrgUnitIdAndUseYnAndHqCatalogBlockYnOrderBySortOrderAscIdAsc(orgUnitId, "Y", "N")
                .stream()
                .map(this::toPublicMap)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 상위 조직 상품관리: 산하 가맹점들의 상품을 한 목록으로 (가맹코드·명 포함).
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listProductsForMerchantOrgIds(List<Long> merchantOrgUnitIds) {
        if (merchantOrgUnitIds == null || merchantOrgUnitIds.isEmpty()) {
            return List.of();
        }
        List<MerchantChatbotProduct> rows = productRepository
                .findByOrgUnitIdInOrderByOrgUnitIdAscSortOrderAscIdAsc(merchantOrgUnitIds);
        if (rows.isEmpty()) {
            return List.of();
        }
        Set<Long> ouIds = rows.stream().map(MerchantChatbotProduct::getOrgUnitId).collect(Collectors.toSet());
        Map<Long, OrgUnit> ouById = orgUnitRepository.findAllById(ouIds).stream()
                .collect(Collectors.toMap(OrgUnit::getId, o -> o, (a, b) -> a));
        List<Map<String, Object>> out = new ArrayList<>(rows.size());
        for (MerchantChatbotProduct p : rows) {
            Map<String, Object> m = toMap(p);
            OrgUnit ou = ouById.get(p.getOrgUnitId());
            m.put("compId", ou != null && ou.getCode() != null ? ou.getCode() : "");
            m.put("merchantName", ou != null && ou.getName() != null ? ou.getName() : "");
            out.add(m);
        }
        return out;
    }

    private Map<String, Object> toMap(MerchantChatbotProduct p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("productCode", p.getProductCode() != null ? p.getProductCode() : "");
        m.put("title", p.getTitle() != null ? p.getTitle() : "");
        m.put("description", p.getDescription() != null ? p.getDescription() : "");
        m.put("amount", p.getAmount() != null ? p.getAmount().stripTrailingZeros().toPlainString() : "0");
        m.put("currencyCode", p.getCurrencyCode() != null ? p.getCurrencyCode() : "KRW");
        m.put("imageUrl", p.getImageUrl() != null ? p.getImageUrl() : "");
        m.put("sortOrder", p.getSortOrder() != null ? p.getSortOrder() : 0);
        m.put("useYn", yn(p.getUseYn()));
        m.put("hqCatalogBlockYn", yn(p.getHqCatalogBlockYn()));
        m.put("listingType", normalizeListingTypeStored(p.getListingType()));
        return m;
    }

    private Map<String, Object> toPublicMap(MerchantChatbotProduct p) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (p.getProductCode() != null && !p.getProductCode().isBlank()) {
            m.put("productCode", p.getProductCode().trim());
        }
        m.put("title", p.getTitle() != null ? p.getTitle() : "");
        m.put("description", p.getDescription() != null ? p.getDescription() : "");
        m.put("amount", p.getAmount() != null ? p.getAmount().stripTrailingZeros().toPlainString() : "0");
        m.put("currencyCode", p.getCurrencyCode() != null ? p.getCurrencyCode().trim().toUpperCase(Locale.ROOT) : "KRW");
        m.put("imageUrl", p.getImageUrl() != null ? p.getImageUrl() : "");
        m.put("listingType", normalizeListingTypeStored(p.getListingType()));
        return m;
    }

    @Transactional
    public Map<String, Object> saveRow(Long orgUnitId, Map<String, Object> body) {
        return saveRow(orgUnitId, body, false);
    }

    /**
     * @param allowHqCatalogFields 가맹이 아닌 관리 계정만 true — {@code hqCatalogBlockYn} 갱신 허용
     */
    @Transactional
    public Map<String, Object> saveRow(Long orgUnitId, Map<String, Object> body, boolean allowHqCatalogFields) {
        MerchantChatbotProduct p;
        Object idObj = body != null ? body.get("id") : null;
        Long id = null;
        if (idObj instanceof Number) {
            id = ((Number) idObj).longValue();
        } else if (idObj != null) {
            try {
                id = Long.parseLong(String.valueOf(idObj).trim());
            } catch (NumberFormatException ignored) {
                id = null;
            }
        }
        boolean isNew = id == null || id <= 0;
        int saleCap = resolveChatbotProductSlotCap(orgUnitId);
        int regCap = registrationCapFromSaleCap(saleCap);
        int extra = ChatbotProductPricingUtil.CHATBOT_PRODUCT_REGISTER_EXTRA_SLOTS;

        String previousUseYn = "N";
        if (!isNew) {
            p = productRepository.findById(id).filter(x -> orgUnitId.equals(x.getOrgUnitId()))
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
            previousUseYn = yn(p.getUseYn());
        } else {
            if (saleCap > 0) {
                long cnt = productRepository.countByOrgUnitId(orgUnitId);
                if (cnt >= regCap) {
                    throw new IllegalArgumentException(
                            "등록 가능한 상품은 최대 " + regCap + "건입니다.(판매 활성 최대 " + saleCap
                                    + "건 + 미판매 보관 " + extra + "건) 상품을 삭제하거나 플랜을 올린 뒤 등록하세요.");
                }
            }
            p = new MerchantChatbotProduct();
            p.setOrgUnitId(orgUnitId);
        }
        if (body != null) {
            String code = str(body.get("productCode"));
            p.setProductCode(code != null && code.length() > 64 ? code.substring(0, 64) : code);
            String title = str(body.get("title"));
            if (title == null || title.isBlank()) {
                throw new IllegalArgumentException("상품명은 필수입니다.");
            }
            p.setTitle(title.length() > 200 ? title.substring(0, 200) : title);
            String desc = str(body.get("description"));
            if (desc != null && desc.length() > 8000) {
                desc = desc.substring(0, 8000);
            }
            p.setDescription(desc);
            p.setAmount(parseAmount(body.get("amount")));
            String cur = str(body.get("currencyCode"));
            if (cur == null || cur.isBlank()) {
                cur = "KRW";
            }
            cur = cur.trim().toUpperCase(Locale.ROOT);
            p.setCurrencyCode(cur.length() > 10 ? cur.substring(0, 10) : cur);
            String img = str(body.get("imageUrl"));
            if (img != null && img.length() > 512) {
                img = img.substring(0, 512);
            }
            p.setImageUrl(img);
            Integer so = parseIntObj(body.get("sortOrder"));
            p.setSortOrder(so != null ? so : 0);
            p.setUseYn(yn(str(body.get("useYn"))));
            if (allowHqCatalogFields) {
                p.setHqCatalogBlockYn(yn(str(body.get("hqCatalogBlockYn"))));
            }
            if (body.containsKey("listingType")) {
                p.setListingType(normalizeListingType(body.get("listingType")));
            }
        }

        if (saleCap > 0) {
            String newYn = yn(p.getUseYn());
            long activeDb = productRepository.countByOrgUnitIdAndUseYn(orgUnitId, "Y");
            long minusOld = (!isNew && "Y".equals(previousUseYn)) ? 1L : 0L;
            long plusNew = "Y".equals(newYn) ? 1L : 0L;
            long projectedActive = activeDb - minusOld + plusNew;
            if (projectedActive > saleCap) {
                throw new IllegalArgumentException(
                        "판매 활성(고객 챗봇·카탈로그에 노출 가능) 상품은 플랜 기준 최대 " + saleCap
                                + "개까지입니다. 다른 상품의 판매 활성을 끄거나(사용=N) 플랜을 변경하세요."
                                + " 미판매로 등록만 해 두는 상품은 총 " + regCap + "건까지 등록할 수 있습니다.");
            }
        }

        return toMap(productRepository.save(p));
    }

    /**
     * 챗봇결제 Y 가맹의 등록 건수 상한. 미설정·0 이하면 제한 없음.
     */
    private int resolveChatbotProductSlotCap(Long orgUnitId) {
        return getEffectiveChatbotProductSlotCap(orgUnitId);
    }

    /** 플랜 기준 「판매 활성(use_yn=Y)」 동시 허용 상한. 0이면 무제한(플랜 미설정 또는 챗봇 미사용). 총 등록 상한은 {@link #getEffectiveRegistrationCap(Long)}. */
    @Transactional(readOnly = true)
    public int getEffectiveChatbotProductSlotCap(Long orgUnitId) {
        if (orgUnitId == null) {
            return 0;
        }
        Optional<MerchantProfile> mp = merchantProfileRepository.findByOrgUnitId(orgUnitId);
        if (mp.isEmpty()) {
            return 0;
        }
        MerchantProfile m = mp.get();
        if (!"Y".equalsIgnoreCase(m.getChatbotPaymentUseYn() != null ? m.getChatbotPaymentUseYn().trim() : "")) {
            return 0;
        }
        Integer slot = m.getChatbotProductSlotLimit();
        if (slot == null || slot <= 0) {
            return 0;
        }
        return ChatbotProductPricingUtil.isAllowedSlot(slot) ? slot : 0;
    }

    @Transactional
    public void deleteRow(Long orgUnitId, Long productId) {
        if (productId == null || productId <= 0) {
            throw new IllegalArgumentException("상품 ID가 필요합니다.");
        }
        MerchantChatbotProduct p = productRepository.findById(productId)
                .filter(x -> orgUnitId.equals(x.getOrgUnitId()))
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
        productRepository.delete(p);
    }

    private static String normalizeListingTypeStored(String stored) {
        if (stored != null && "RESERVATION".equalsIgnoreCase(stored.trim())) {
            return "RESERVATION";
        }
        return "SALE";
    }

    private static String normalizeListingType(Object raw) {
        String s = str(raw);
        if (s == null) {
            return "SALE";
        }
        return switch (s.trim().toUpperCase(Locale.ROOT)) {
            case "RESERVATION", "RESERVE", "BOOKING" -> "RESERVATION";
            default -> "SALE";
        };
    }

    private static String str(Object o) {
        if (o == null) {
            return null;
        }
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : s;
    }

    private static String yn(String v) {
        return "Y".equalsIgnoreCase(v != null ? v.trim() : "") ? "Y" : "N";
    }

    private static Integer parseIntObj(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal parseAmount(Object o) {
        if (o == null) {
            throw new IllegalArgumentException("금액을 입력하세요.");
        }
        BigDecimal a;
        try {
            a = new BigDecimal(String.valueOf(o).trim().replace(",", ""));
        } catch (Exception e) {
            throw new IllegalArgumentException("금액 형식이 올바르지 않습니다.");
        }
        if (a.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("금액은 0보다 커야 합니다.");
        }
        return a.setScale(4, RoundingMode.HALF_UP);
    }
}
