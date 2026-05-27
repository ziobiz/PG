package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.entity.MerchantJpaySubscription;
import com.pg.repository.MerchantJpaySubscriptionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 본사·결제관리 — JPAY API 구독 마스터 목록. */
@RestController
@RequestMapping(value = "/api/hq", produces = "application/json")
public class ApiHqJpaySubscriptionController {

    private final MerchantJpaySubscriptionRepository subscriptionRepository;

    public ApiHqJpaySubscriptionController(MerchantJpaySubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @GetMapping("/jpaySubscriptions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> list(
            @RequestParam(required = false) String compId) {
        List<MerchantJpaySubscription> rows;
        if (compId != null && !compId.isBlank()) {
            rows = subscriptionRepository.findTop500ByOrderByCreatedAtDesc().stream()
                    .filter(s -> compId.trim().equalsIgnoreCase(s.getCompCode()))
                    .limit(200)
                    .toList();
        } else {
            rows = subscriptionRepository.findTop500ByOrderByCreatedAtDesc();
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (MerchantJpaySubscription s : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("compCode", s.getCompCode());
            m.put("checkoutOrderNo", s.getCheckoutOrderNo());
            m.put("pgCd", s.getPgCd());
            m.put("status", s.getStatus());
            m.put("periodCount", s.getPeriodCount());
            m.put("paymentTransactionId", s.getPaymentTransactionId());
            m.put("lastNotifyAt", s.getLastNotifyAt() != null ? s.getLastNotifyAt().toString() : null);
            m.put("cancelledAt", s.getCancelledAt() != null ? s.getCancelledAt().toString() : null);
            m.put("createdAt", s.getCreatedAt() != null ? s.getCreatedAt().toString() : null);
            list.add(m);
        }
        return ResponseEntity.ok(ApiResponse.ok(list));
    }
}
