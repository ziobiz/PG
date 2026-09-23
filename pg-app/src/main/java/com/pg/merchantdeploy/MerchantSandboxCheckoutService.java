package com.pg.merchantdeploy;

import com.pg.entity.MerchantProfile;
import com.pg.entity.MerchantSandboxTxn;
import com.pg.entity.OrgLevel;
import com.pg.entity.OrgUnit;
import com.pg.repository.MerchantProfileRepository;
import com.pg.repository.MerchantSandboxTxnRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.service.OrgServiceUseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 가맹↔ICOPAY 샌드박스 통신 테스트 — 실PG·NOTI 미호출.
 */
@Service
public class MerchantSandboxCheckoutService {

    private static final SecureRandom RND = new SecureRandom();

    private final OrgUnitRepository orgUnitRepository;
    private final MerchantProfileRepository merchantProfileRepository;
    private final MerchantSandboxTxnRepository sandboxTxnRepository;
    private final MerchantSandboxNotifyService sandboxNotifyService;
    private final OrgServiceUseService orgServiceUseService;

    public MerchantSandboxCheckoutService(OrgUnitRepository orgUnitRepository,
                                          MerchantProfileRepository merchantProfileRepository,
                                          MerchantSandboxTxnRepository sandboxTxnRepository,
                                          MerchantSandboxNotifyService sandboxNotifyService,
                                          OrgServiceUseService orgServiceUseService) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantProfileRepository = merchantProfileRepository;
        this.sandboxTxnRepository = sandboxTxnRepository;
        this.sandboxNotifyService = sandboxNotifyService;
        this.orgServiceUseService = orgServiceUseService;
    }

    @Transactional
    public Map<String, Object> prepare(Long orgUnitId, Map<String, Object> body) {
        Map<String, Object> gate = gate(orgUnitId);
        if (gate != null) {
            return gate;
        }
        OrgUnit ou = orgUnitRepository.findById(orgUnitId).orElse(null);
        if (ou == null || ou.getOrgLevel() != OrgLevel.MERCHANT) {
            return fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND");
        }
        String orderNo = str(body.get("orderNo"));
        if (orderNo.isBlank()) {
            return fail("orderNo가 필요합니다.", "INVALID_ORDER_NO");
        }
        BigDecimal amount = parseAmount(body.get("amount"));
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return fail("유효한 amount가 필요합니다.", "INVALID_AMOUNT");
        }
        String currency = str(body.get("currency"));
        if (currency.isBlank()) {
            currency = "KRW";
        }
        Optional<MerchantSandboxTxn> existing = sandboxTxnRepository.findByOrgUnitIdAndOrderNo(orgUnitId, orderNo);
        if (existing.isPresent() && MerchantSandboxTxn.ST_APPROVED.equals(existing.get().getStatus())) {
            return fail("이미 승인된 샌드박스 주문번호입니다.", "DUPLICATE_ORDER");
        }
        MerchantSandboxTxn txn = existing.orElseGet(MerchantSandboxTxn::new);
        txn.setOrgUnitId(orgUnitId);
        txn.setCompId(ou.getCode());
        txn.setOrderNo(orderNo);
        txn.setAmount(amount);
        txn.setCurrency(currency.toUpperCase(Locale.ROOT));
        txn.setProductName(str(body.get("productName")));
        if (txn.getProductName().isBlank()) {
            txn.setProductName(str(body.get("item")));
        }
        txn.setStatus(MerchantSandboxTxn.ST_PENDING);
        txn.setSessionToken("sb_" + HexFormat.of().formatHex(randomBytes(24)));
        txn.setCompletedAt(null);
        sandboxTxnRepository.save(txn);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sandbox", true);
        data.put("pgVendor", MerchantApiResponseMapper.MERCHANT_FACING_BRAND);
        data.put("compId", ou.getCode());
        data.put("orderNo", orderNo);
        data.put("amount", amount);
        data.put("currency", txn.getCurrency());
        data.put("productName", txn.getProductName());
        data.put("sessionToken", txn.getSessionToken());
        data.put("status", txn.getStatus());
        data.put("message", "샌드박스 prepare 성공. complete API로 승인을 완료하세요.");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("data", data);
        return out;
    }

    @Transactional
    public Map<String, Object> complete(Long orgUnitId, Map<String, Object> body) {
        Map<String, Object> gate = gate(orgUnitId);
        if (gate != null) {
            return gate;
        }
        String orderNo = str(body.get("orderNo"));
        String session = str(body.get("sessionToken"));
        Optional<MerchantSandboxTxn> opt;
        if (!session.isBlank()) {
            opt = sandboxTxnRepository.findBySessionToken(session)
                    .filter(t -> orgUnitId.equals(t.getOrgUnitId()));
        } else {
            opt = sandboxTxnRepository.findByOrgUnitIdAndOrderNo(orgUnitId, orderNo);
        }
        if (opt.isEmpty()) {
            return fail("샌드박스 주문을 찾을 수 없습니다.", "NOT_FOUND");
        }
        MerchantSandboxTxn txn = opt.get();
        if (MerchantSandboxTxn.ST_APPROVED.equals(txn.getStatus())) {
            return okStatus(txn);
        }
        boolean failWanted = "Y".equalsIgnoreCase(str(body.get("forceFail")))
                || "FAIL".equalsIgnoreCase(str(body.get("result")));
        if (failWanted) {
            txn.setStatus(MerchantSandboxTxn.ST_FAILED);
            txn.setCompletedAt(LocalDateTime.now());
            sandboxTxnRepository.save(txn);
            return okStatus(txn);
        }
        txn.setStatus(MerchantSandboxTxn.ST_APPROVED);
        txn.setCompletedAt(LocalDateTime.now());
        sandboxTxnRepository.save(txn);
        try {
            sandboxNotifyService.notifyApproved(txn);
        } catch (Exception e) {
            // 통보 실패해도 승인 상태는 유지
        }
        return okStatus(txn);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> orderStatus(Long orgUnitId, String orderNo) {
        Map<String, Object> gate = gate(orgUnitId);
        if (gate != null) {
            return gate;
        }
        Optional<MerchantSandboxTxn> opt = sandboxTxnRepository.findByOrgUnitIdAndOrderNo(orgUnitId, orderNo);
        if (opt.isEmpty()) {
            return fail("샌드박스 주문을 찾을 수 없습니다.", "NOT_FOUND");
        }
        return okStatus(opt.get());
    }

    private Map<String, Object> gate(Long orgUnitId) {
        if (orgUnitId == null) {
            return fail("가맹점을 찾을 수 없습니다.", "NOT_FOUND");
        }
        if (!orgServiceUseService.isOrgServiceActive(orgUnitId)) {
            return fail(OrgServiceUseService.MSG_ORG_SERVICE_DISABLED, "ORG_DISABLED");
        }
        Optional<MerchantProfile> mp = merchantProfileRepository.findByOrgUnitId(orgUnitId);
        if (mp.isEmpty() || !"Y".equalsIgnoreCase(mp.get().getSandboxUseYn())) {
            return fail("샌드박스가 비활성입니다.", "SANDBOX_DISABLED");
        }
        return null;
    }

    private static Map<String, Object> okStatus(MerchantSandboxTxn txn) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sandbox", true);
        data.put("pgVendor", MerchantApiResponseMapper.MERCHANT_FACING_BRAND);
        data.put("compId", txn.getCompId());
        data.put("orderNo", txn.getOrderNo());
        data.put("amount", txn.getAmount());
        data.put("currency", txn.getCurrency());
        data.put("status", txn.getStatus());
        data.put("sessionToken", txn.getSessionToken());
        data.put("completedAt", txn.getCompletedAt() != null ? txn.getCompletedAt().toString() : null);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("data", data);
        return out;
    }

    private static Map<String, Object> fail(String message, String code) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", false);
        out.put("message", message);
        out.put("errorCode", code);
        out.put("code", code);
        return out;
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString().trim();
    }

    private static BigDecimal parseAmount(Object o) {
        if (o == null) {
            return null;
        }
        try {
            return new BigDecimal(o.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        RND.nextBytes(b);
        return b;
    }
}
