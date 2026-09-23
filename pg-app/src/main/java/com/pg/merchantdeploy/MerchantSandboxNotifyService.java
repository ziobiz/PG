package com.pg.merchantdeploy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pg.entity.MerchantNotifyUrl;
import com.pg.entity.MerchantSandboxNotifyLog;
import com.pg.entity.MerchantSandboxTxn;
import com.pg.repository.MerchantNotifyUrlRepository;
import com.pg.repository.MerchantSandboxNotifyLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 샌드박스 결제 확정 후 ICOPAY → 가맹 SANDBOX 결제통보 URL 직접 POST (NOTI 미경유).
 */
@Service
public class MerchantSandboxNotifyService {

    private static final Logger log = LoggerFactory.getLogger(MerchantSandboxNotifyService.class);
    private static final ObjectMapper OM = new ObjectMapper();

    private final MerchantNotifyUrlRepository merchantNotifyUrlRepository;
    private final MerchantSandboxNotifyLogRepository notifyLogRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public MerchantSandboxNotifyService(MerchantNotifyUrlRepository merchantNotifyUrlRepository,
                                        MerchantSandboxNotifyLogRepository notifyLogRepository) {
        this.merchantNotifyUrlRepository = merchantNotifyUrlRepository;
        this.notifyLogRepository = notifyLogRepository;
    }

    public void notifyApproved(MerchantSandboxTxn txn) {
        if (txn == null || txn.getOrgUnitId() == null) {
            return;
        }
        Map<String, Object> payload = basePayload(txn);
        postOne(txn, MerchantNotifyUrl.URL_TYPE_BACKGROUND_SANDBOX, payload);
        postOne(txn, MerchantNotifyUrl.URL_TYPE_RESULT_SANDBOX, payload);
    }

    public Map<String, Object> resend(Long logId) {
        MerchantSandboxNotifyLog prev = notifyLogRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("통보 이력을 찾을 수 없습니다."));
        Map<String, Object> payload;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = OM.readValue(
                    prev.getPayloadBody() != null ? prev.getPayloadBody() : "{}", Map.class);
            payload = parsed != null ? parsed : new LinkedHashMap<>();
        } catch (Exception e) {
            payload = new LinkedHashMap<>();
            payload.put("orderNo", prev.getOrderNo());
            payload.put("sandbox", true);
        }
        payload.put("resend", true);
        return postToUrl(prev.getOrgUnitId(), prev.getCompId(), prev.getSandboxTxnId(),
                prev.getOrderNo(), prev.getUrlType(), prev.getTargetUrl(), payload);
    }

    private void postOne(MerchantSandboxTxn txn, String urlType, Map<String, Object> base) {
        Optional<MerchantNotifyUrl> row = merchantNotifyUrlRepository
                .findByOrgUnitIdAndUrlType(txn.getOrgUnitId(), urlType)
                .filter(r -> "Y".equalsIgnoreCase(r.getUseYn())
                        && r.getNotiUrl() != null && !r.getNotiUrl().isBlank());
        if (row.isEmpty()) {
            return;
        }
        Map<String, Object> body = new LinkedHashMap<>(base);
        body.put("merchantNotifyTarget", urlType);
        postToUrl(txn.getOrgUnitId(), txn.getCompId(), txn.getId(), txn.getOrderNo(),
                urlType, row.get().getNotiUrl().trim(), body);
    }

    private Map<String, Object> postToUrl(Long orgUnitId, String compId, Long txnId, String orderNo,
                                          String urlType, String url, Map<String, Object> payload) {
        Map<String, Object> out = new LinkedHashMap<>();
        String bodyJson;
        try {
            bodyJson = OM.writeValueAsString(payload);
        } catch (Exception e) {
            out.put("success", false);
            out.put("message", e.getMessage());
            return out;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Icopay-Sandbox", "true");
        int attempts = 0;
        Integer httpStatus = null;
        String err = null;
        boolean ok = false;
        for (int i = 0; i < 3; i++) {
            attempts++;
            try {
                ResponseEntity<String> resp = restTemplate.postForEntity(
                        url, new HttpEntity<>(bodyJson, headers), String.class);
                httpStatus = resp.getStatusCode().value();
                if (resp.getStatusCode().is2xxSuccessful()) {
                    ok = true;
                    break;
                }
                err = "HTTP " + httpStatus;
            } catch (Exception e) {
                err = e.getMessage();
                log.warn("샌드박스 통보 실패 urlType={} url={}: {}", urlType, url, e.getMessage());
            }
            try {
                Thread.sleep(i == 0 ? 0 : (i == 1 ? 250L : 900L));
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        MerchantSandboxNotifyLog logRow = new MerchantSandboxNotifyLog();
        logRow.setOrgUnitId(orgUnitId);
        logRow.setCompId(compId != null ? compId : "");
        logRow.setSandboxTxnId(txnId);
        logRow.setOrderNo(orderNo);
        logRow.setUrlType(urlType);
        logRow.setTargetUrl(url);
        logRow.setResultStatus(ok ? "OK" : "FAIL");
        logRow.setHttpStatus(httpStatus);
        logRow.setRetryCnt(attempts);
        logRow.setErrorMessage(err);
        logRow.setPayloadBody(bodyJson);
        notifyLogRepository.save(logRow);
        out.put("success", ok);
        out.put("logId", logRow.getId());
        out.put("httpStatus", httpStatus);
        out.put("message", ok ? "OK" : (err != null ? err : "FAIL"));
        return out;
    }

    private static Map<String, Object> basePayload(MerchantSandboxTxn txn) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("sandbox", true);
        m.put("pgVendor", "ICOPAY");
        m.put("compId", txn.getCompId());
        m.put("orderNo", txn.getOrderNo());
        m.put("amount", txn.getAmount());
        m.put("currency", txn.getCurrency());
        m.put("productName", txn.getProductName());
        m.put("status", txn.getStatus());
        m.put("event", "pg.payment.sandbox");
        return m;
    }
}
