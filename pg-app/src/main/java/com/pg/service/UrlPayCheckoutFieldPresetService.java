package com.pg.service;

import com.pg.entity.HqApiConfig;
import com.pg.entity.UrlPayCheckoutFieldPreset;
import com.pg.repository.HqApiConfigRepository;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.UrlPayCheckoutFieldPresetRepository;
import com.pg.urlpay.UrlPayFollowHqYnUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 결제창 구매자 입력 필드 프리셋(기본형·N형) 관리.
 */
@Service
public class UrlPayCheckoutFieldPresetService {

    private static final String DEFAULT_NAME = "기본형";
    private static final Pattern NUMBERED_NAME = Pattern.compile("^(\\d+)형$");

    private final UrlPayCheckoutFieldPresetRepository repository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final HqApiConfigRepository hqApiConfigRepository;

    public UrlPayCheckoutFieldPresetService(UrlPayCheckoutFieldPresetRepository repository,
                                            MerchantProfileRepository merchantProfileRepository,
                                            HqApiConfigRepository hqApiConfigRepository) {
        this.repository = repository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.hqApiConfigRepository = hqApiConfigRepository;
    }

    @Transactional
    public UrlPayCheckoutFieldPreset ensureDefault() {
        Optional<UrlPayCheckoutFieldPreset> existing = repository.findFirstByIsDefaultYnIgnoreCase("Y");
        if (existing.isPresent()) {
            return existing.get();
        }
        Optional<UrlPayCheckoutFieldPreset> byName = repository.findByPresetName(DEFAULT_NAME);
        if (byName.isPresent()) {
            UrlPayCheckoutFieldPreset p = byName.get();
            p.setIsDefaultYn("Y");
            p.setSortNo(0);
            return repository.save(p);
        }
        UrlPayCheckoutFieldPreset p = new UrlPayCheckoutFieldPreset();
        p.setPresetName(DEFAULT_NAME);
        p.setSortNo(0);
        p.setIsDefaultYn("Y");
        HqApiConfig hq = hqApiConfigRepository.findAll().stream().findFirst().orElse(null);
        if (hq != null) {
            p.setBuyerEmailUseYn(UrlPayFollowHqYnUtil.normalizeHqDefault(hq.getUrlPayBuyerEmailUseDefaultYn(), "Y"));
            p.setBuyerCountryUseYn(UrlPayFollowHqYnUtil.normalizeHqDefault(hq.getUrlPayBuyerCountryUseDefaultYn(), "Y"));
            p.setBuyerPhoneUseYn(UrlPayFollowHqYnUtil.normalizeHqDefault(hq.getUrlPayBuyerPhoneUseDefaultYn(), "Y"));
            p.setShippingAddressUseYn(UrlPayFollowHqYnUtil.normalizeHqDefault(hq.getUrlPayShippingAddressUseDefaultYn(), "N"));
        } else {
            p.setBuyerEmailUseYn("Y");
            p.setBuyerCountryUseYn("Y");
            p.setBuyerPhoneUseYn("Y");
            p.setShippingAddressUseYn("N");
        }
        return repository.save(p);
    }

    @Transactional
    public List<UrlPayCheckoutFieldPreset> listAll() {
        ensureDefault();
        return repository.findAllByOrderBySortNoAscIdAsc();
    }

    @Transactional
    public UrlPayCheckoutFieldPreset getDefault() {
        return ensureDefault();
    }

    @Transactional(readOnly = true)
    public Optional<UrlPayCheckoutFieldPreset> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public long countMerchantsUsing(Long presetId) {
        if (presetId == null) {
            return 0L;
        }
        return merchantProfileRepository.countByUrlPayCheckoutFieldPresetId(presetId);
    }

    @Transactional
    public UrlPayCheckoutFieldPreset createNext() {
        ensureDefault();
        int next = nextNumberedName();
        UrlPayCheckoutFieldPreset def = getDefault();
        UrlPayCheckoutFieldPreset n = new UrlPayCheckoutFieldPreset();
        n.setPresetName(next + "형");
        n.setSortNo(next);
        n.setIsDefaultYn("N");
        n.setBuyerEmailUseYn(def.getBuyerEmailUseYn());
        n.setBuyerCountryUseYn(def.getBuyerCountryUseYn());
        n.setBuyerPhoneUseYn(def.getBuyerPhoneUseYn());
        n.setShippingAddressUseYn(def.getShippingAddressUseYn());
        return repository.save(n);
    }

    @Transactional
    public UrlPayCheckoutFieldPreset update(Long id, Map<String, Object> body) {
        if (id == null) {
            throw new IllegalArgumentException("프리셋 id가 필요합니다.");
        }
        UrlPayCheckoutFieldPreset p = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("프리셋을 찾을 수 없습니다."));
        if (body != null) {
            if (body.containsKey("buyerEmailUseYn")) {
                p.setBuyerEmailUseYn(str(body.get("buyerEmailUseYn")));
            }
            if (body.containsKey("buyerCountryUseYn")) {
                p.setBuyerCountryUseYn(str(body.get("buyerCountryUseYn")));
            }
            if (body.containsKey("buyerPhoneUseYn")) {
                p.setBuyerPhoneUseYn(str(body.get("buyerPhoneUseYn")));
            }
            if (body.containsKey("shippingAddressUseYn")) {
                p.setShippingAddressUseYn(str(body.get("shippingAddressUseYn")));
            }
            if (body.containsKey("presetName") && !p.isDefault()) {
                String rename = str(body.get("presetName"));
                if (rename != null && !rename.isBlank()) {
                    String name = rename.trim();
                    if (name.length() > 40) {
                        throw new IllegalArgumentException("프리셋명은 40자 이하여야 합니다.");
                    }
                    if (DEFAULT_NAME.equals(name)) {
                        throw new IllegalArgumentException("「기본형」은 예약된 이름입니다.");
                    }
                    Optional<UrlPayCheckoutFieldPreset> clash = repository.findByPresetName(name);
                    if (clash.isPresent() && !clash.get().getId().equals(p.getId())) {
                        throw new IllegalArgumentException("이미 존재하는 프리셋명입니다.");
                    }
                    p.setPresetName(name);
                }
            }
        }
        UrlPayCheckoutFieldPreset saved = repository.save(p);
        if (saved.isDefault()) {
            syncDefaultToHqConfig(saved);
        }
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("프리셋 id가 필요합니다.");
        }
        UrlPayCheckoutFieldPreset p = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("프리셋을 찾을 수 없습니다."));
        if (p.isDefault()) {
            throw new IllegalArgumentException("기본형은 삭제할 수 없습니다.");
        }
        long used = countMerchantsUsing(id);
        if (used > 0) {
            throw new IllegalArgumentException("가맹점이 사용 중인 프리셋은 삭제할 수 없습니다. (사용 " + used + "건)");
        }
        repository.delete(p);
    }

    /** 본사 결제창 표시 기본값 저장 시 기본형 프리셋과 동기화 */
    @Transactional
    public void syncDefaultFromHqYn(String emailYn, String countryYn, String phoneYn, String shippingYn) {
        UrlPayCheckoutFieldPreset def = ensureDefault();
        def.setBuyerEmailUseYn(UrlPayFollowHqYnUtil.normalizeHqDefault(emailYn, "Y"));
        def.setBuyerCountryUseYn(UrlPayFollowHqYnUtil.normalizeHqDefault(countryYn, "Y"));
        def.setBuyerPhoneUseYn(UrlPayFollowHqYnUtil.normalizeHqDefault(phoneYn, "Y"));
        def.setShippingAddressUseYn(UrlPayFollowHqYnUtil.normalizeHqDefault(shippingYn, "N"));
        repository.save(def);
    }

    public Map<String, Object> toMap(UrlPayCheckoutFieldPreset p) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (p == null) {
            return m;
        }
        m.put("id", p.getId());
        m.put("presetName", p.getPresetName());
        m.put("sortNo", p.getSortNo());
        m.put("isDefaultYn", p.isDefault() ? "Y" : "N");
        m.put("buyerEmailUseYn", p.getBuyerEmailUseYn());
        m.put("buyerCountryUseYn", p.getBuyerCountryUseYn());
        m.put("buyerPhoneUseYn", p.getBuyerPhoneUseYn());
        m.put("shippingAddressUseYn", p.getShippingAddressUseYn());
        m.put("merchantCount", countMerchantsUsing(p.getId()));
        return m;
    }

    private void syncDefaultToHqConfig(UrlPayCheckoutFieldPreset def) {
        HqApiConfig hq = hqApiConfigRepository.findAll().stream().findFirst().orElse(null);
        if (hq == null) {
            return;
        }
        hq.setUrlPayBuyerEmailUseDefaultYn(def.getBuyerEmailUseYn());
        hq.setUrlPayBuyerCountryUseDefaultYn(def.getBuyerCountryUseYn());
        hq.setUrlPayBuyerPhoneUseDefaultYn(def.getBuyerPhoneUseYn());
        hq.setUrlPayShippingAddressUseDefaultYn(def.getShippingAddressUseYn());
        hq.setJpayCheckoutFieldMode(com.pg.urlpay.CheckoutBuyerContactUtil.toLegacyCheckoutFieldMode(
                def.getBuyerEmailUseYn(), def.getBuyerCountryUseYn(),
                def.getBuyerPhoneUseYn(), def.getShippingAddressUseYn()));
        hqApiConfigRepository.save(hq);
    }

    private int nextNumberedName() {
        int max = 0;
        for (UrlPayCheckoutFieldPreset p : repository.findAll()) {
            if (p.getPresetName() == null) {
                continue;
            }
            Matcher m = NUMBERED_NAME.matcher(p.getPresetName().trim());
            if (m.matches()) {
                try {
                    max = Math.max(max, Integer.parseInt(m.group(1)));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        int candidate = max + 1;
        while (repository.existsByPresetName(candidate + "형")) {
            candidate++;
        }
        return candidate;
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}
