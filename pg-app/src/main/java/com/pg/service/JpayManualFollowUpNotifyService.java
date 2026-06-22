package com.pg.service;

import com.pg.entity.MerchantNotifyUrl;
import com.pg.entity.MerchantPgBinding;
import com.pg.entity.OrgUnit;
import com.pg.entity.PgAgency;
import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import com.pg.repository.MerchantNotifyUrlRepository;
import com.pg.repository.MerchantPgBindingRepository;
import com.pg.repository.OrgUnitRepository;
import com.pg.repository.PgAgencyRepository;
import com.pg.util.JpaySignatureUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * JPAY 관리자 포털에서 무효·환불 승인 후 ICOPAY 결제내역 「수동무효」「수동환불」 실행 시
 * 가맹 등록 {@link MerchantNotifyUrl#URL_TYPE_JPAY_NOTIFY}(노티미들웨어) 로 JPAY 형식 노티를 POST 합니다.
 */
@Service
public class JpayManualFollowUpNotifyService {

    private static final Logger log = LoggerFactory.getLogger(JpayManualFollowUpNotifyService.class);
    private static final DateTimeFormatter JPAY_DT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.ROOT);
    public static final String ICOPAY_MANUAL_FOLLOWUP_FLAG = "_icopay_manual_followup";

    private final OrgUnitRepository orgUnitRepository;
    private final MerchantPgBindingRepository merchantPgBindingRepository;
    private final PgAgencyRepository pgAgencyRepository;
    private final MerchantNotifyUrlRepository merchantNotifyUrlRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public JpayManualFollowUpNotifyService(OrgUnitRepository orgUnitRepository,
                                           MerchantPgBindingRepository merchantPgBindingRepository,
                                           PgAgencyRepository pgAgencyRepository,
                                           MerchantNotifyUrlRepository merchantNotifyUrlRepository) {
        this.orgUnitRepository = orgUnitRepository;
        this.merchantPgBindingRepository = merchantPgBindingRepository;
        this.pgAgencyRepository = pgAgencyRepository;
        this.merchantNotifyUrlRepository = merchantNotifyUrlRepository;
    }

    /**
     * @param action {@link PayListActionService.PayFollowAction#MANUAL_VOID} 또는 {@link PayListActionService.PayFollowAction#MANUAL_REFUND}
     */
    public void sendAfterManualFollowUp(PgTrnsctn t, PayListActionService.PayFollowAction action, String actor) {
        if (t == null || action == null) {
            return;
        }
        if (!PgVendor.isJpayFamily(t.getVan())) {
            return;
        }
        String merchantCode = t.getMerchantId();
        if (merchantCode == null || merchantCode.isBlank()) {
            log.warn("JPAY 수동후속 노티 생략 — merchantId 없음 trnId={}", t.getTrnId());
            return;
        }
        Optional<OrgUnit> ouOpt = orgUnitRepository.findByCodeIgnoreCase(merchantCode.trim());
        if (ouOpt.isEmpty()) {
            log.warn("JPAY 수동후속 노티 생략 — 조직 없음 merchantId={} trnId={}", merchantCode, t.getTrnId());
            return;
        }
        Long orgUnitId = ouOpt.get().getId();
        Optional<MerchantNotifyUrl> notifyRow = merchantNotifyUrlRepository.findByOrgUnitIdAndUrlType(
                orgUnitId, MerchantNotifyUrl.URL_TYPE_JPAY_NOTIFY);
        if (notifyRow.isEmpty() || !yn(notifyRow.get().getUseYn())) {
            log.warn("JPAY 수동후속 노티 생략 — JPAY_NOTIFY URL 미설정 trnId={} merchantId={}", t.getTrnId(), merchantCode);
            return;
        }
        String targetUrl = notifyRow.get().getNotiUrl() != null ? notifyRow.get().getNotiUrl().trim() : "";
        if (targetUrl.isBlank()) {
            log.warn("JPAY 수동후속 노티 생략 — JPAY_NOTIFY URL 비어 있음 trnId={}", t.getTrnId());
            return;
        }
        Optional<MerchantPgBinding> bindOpt = findOperationalJpayBinding(orgUnitId);
        if (bindOpt.isEmpty()) {
            log.warn("JPAY 수동후속 노티 생략 — JPAY 바인딩 없음 trnId={}", t.getTrnId());
            return;
        }
        MerchantPgBinding bind = bindOpt.get();
        String pgCd = bind.getPgCd() != null ? bind.getPgCd().trim() : "";
        Optional<PgAgency> agOpt = pgAgencyRepository.findByPgCd(pgCd);
        if (agOpt.isEmpty()) {
            log.warn("JPAY 수동후속 노티 생략 — PG 대행사 없음 trnId={} pgCd={}", t.getTrnId(), pgCd);
            return;
        }
        PgAgency ag = agOpt.get();
        String memberid = bind.getMid() != null ? bind.getMid().trim() : "";
        String apiKey = ag.getApiKey() != null ? ag.getApiKey().trim() : "";
        if (memberid.isBlank()) {
            memberid = ag.getMerchantMid() != null ? ag.getMerchantMid().trim() : "";
        }
        if (memberid.isBlank() || apiKey.isBlank()) {
            log.warn("JPAY 수동후속 노티 생략 — MID·API Key 없음 trnId={}", t.getTrnId());
            return;
        }
        String orderid = t.getOrderNo() != null ? t.getOrderNo().trim() : "";
        if (orderid.isBlank()) {
            log.warn("JPAY 수동후속 노티 생략 — orderNo 없음 trnId={}", t.getTrnId());
            return;
        }
        String txnId = firstNonBlank(t.getChillTransactionId(), t.getApprovalNo(), t.getPayNo());
        if (txnId.isBlank()) {
            log.warn("JPAY 수동후속 노티 생략 — transaction_id 없음 trnId={}", t.getTrnId());
            return;
        }
        boolean refund = action == PayListActionService.PayFollowAction.MANUAL_REFUND;
        String returnCode = refund ? "09" : "08";
        String manualFollowup = refund ? "refund" : "void";
        String amount = formatAmount(t.getAmtKrw());

        Map<String, String> signFields = new LinkedHashMap<>();
        signFields.put("memberid", memberid);
        signFields.put("orderid", orderid);
        signFields.put("amount", amount);
        signFields.put("true_amount", amount);
        String currency = t.getCurType() != null && !t.getCurType().isBlank()
                ? t.getCurType().trim().toUpperCase(Locale.ROOT) : "USD";
        if (currency.length() > 3) {
            currency = currency.substring(0, 3);
        }
        signFields.put("currency", currency);
        signFields.put("transaction_id", txnId);
        signFields.put("returncode", returnCode);
        String datetime = JPAY_DT.format(LocalDateTime.now());
        signFields.put("datetime", datetime);
        signFields.put("attach", "");

        String sign = JpaySignatureUtil.signRequestParams(signFields, apiKey);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        signFields.forEach(form::add);
        form.add("sign", sign);
        form.add("_middleware_manualfollowup", manualFollowup);
        form.add(ICOPAY_MANUAL_FOLLOWUP_FLAG, "Y");
        form.add("icopaycompid", merchantCode.trim());
        if (actor != null && !actor.isBlank()) {
            form.add("icopay_manual_actor", actor.trim());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(targetUrl, entity, String.class);
            if (resp.getStatusCode().is2xxSuccessful()) {
                log.info("JPAY 수동후속 노티 전송 완료 trnId={} action={} url={} http={}",
                        t.getTrnId(), action, maskUrl(targetUrl), resp.getStatusCode().value());
            } else {
                log.warn("JPAY 수동후속 노티 비성공 HTTP trnId={} action={} http={}",
                        t.getTrnId(), action, resp.getStatusCode().value());
            }
        } catch (Exception e) {
            log.warn("JPAY 수동후속 노티 전송 실패 trnId={} action={}: {}", t.getTrnId(), action, e.getMessage());
        }
    }

    private Optional<MerchantPgBinding> findOperationalJpayBinding(Long orgUnitId) {
        List<MerchantPgBinding> list = merchantPgBindingRepository.findByOrgUnitIdOrderBySortOrderAsc(orgUnitId);
        return list.stream()
                .filter(b -> b.getPgCd() != null && PgVendor.isJpayFamily(b.getPgCd()))
                .filter(b -> "Y".equalsIgnoreCase(String.valueOf(b.getOperationalYn()).trim()))
                .findFirst();
    }

    private static String formatAmount(BigDecimal amt) {
        if (amt == null || amt.compareTo(BigDecimal.ZERO) <= 0) {
            return "0.00";
        }
        return amt.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static String firstNonBlank(String... vals) {
        if (vals == null) {
            return "";
        }
        for (String v : vals) {
            if (v != null && !v.isBlank()) {
                return v.trim();
            }
        }
        return "";
    }

    private static boolean yn(String v) {
        return v == null || !"N".equalsIgnoreCase(v.trim());
    }

    private static String maskUrl(String url) {
        if (url == null || url.length() < 24) {
            return "***";
        }
        return url.substring(0, Math.min(32, url.length())) + "...";
    }
}
