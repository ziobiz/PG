package com.pg.service;

import com.pg.entity.PgTrnsctn;
import com.pg.integration.pg.PgVendor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * KSNET 서버 승인 후 NOTI {@code /noti/ksnet} 로 JSON 미러 — 노티거래내역·가맹 릴레이.
 * (결제창형은 KSNET→가맹 직접 노티가 없으므로 ICOPAY가 미러한다.)
 */
@Service
public class KsnetNotiMirrorService {

    private static final Logger log = LoggerFactory.getLogger(KsnetNotiMirrorService.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.noti.public-base:https://noti.icopay.net}")
    private String notiPublicBase;

    public void scheduleAfterCommit(PgTrnsctn txn, boolean paid) {
        if (txn == null || !PgVendor.isKsnetFamily(txn.getVan())) {
            return;
        }
        final Map<String, Object> body = buildBody(txn, paid);
        Runnable job = () -> post(body);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    job.run();
                }
            });
        } else {
            job.run();
        }
    }

    Map<String, Object> buildBody(PgTrnsctn txn, boolean paid) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("authyn", paid ? "O" : "X");
        body.put("trno", nz(txn.getChillTransactionId(), txn.getTrnId()));
        body.put("authno", nz(txn.getApprovalNo(), ""));
        body.put("ordno", nz(txn.getOrderNo(), ""));
        body.put("orderNo", nz(txn.getOrderNo(), ""));
        body.put("amt", txn.getAmtKrw() != null
                ? txn.getAmtKrw().stripTrailingZeros().toPlainString() : "");
        body.put("currency", nz(txn.getCurType(), "USD"));
        body.put("resultcd", paid ? "0000" : "9999");
        body.put("Comp-Id", nz(txn.getMerchantId(), ""));
        body.put("pgKind", "ksnet");
        body.put("icopay_source", "ksnet_approve_mirror");
        return body;
    }

    private void post(Map<String, Object> body) {
        String base = notiPublicBase != null ? notiPublicBase.trim().replaceAll("/+$", "")
                : "https://noti.icopay.net";
        String url = base + "/noti/ksnet";
        try {
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            String comp = String.valueOf(body.getOrDefault("Comp-Id", ""));
            if (!comp.isBlank()) {
                h.set("X-Icopay-Comp-Id", comp);
                h.set("X-Comp-Id", comp);
            }
            String order = String.valueOf(body.getOrDefault("orderNo", ""));
            if (!order.isBlank()) {
                h.set("X-Icopay-Order-No", order);
            }
            ResponseEntity<String> res = restTemplate.postForEntity(url, new HttpEntity<>(body, h), String.class);
            log.info("KSNET NOTI mirror order={} status={}", order, res.getStatusCode().value());
        } catch (Exception e) {
            log.warn("KSNET NOTI mirror failed: {}", e.getMessage());
        }
    }

    private static String nz(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        return b != null ? b : "";
    }
}
