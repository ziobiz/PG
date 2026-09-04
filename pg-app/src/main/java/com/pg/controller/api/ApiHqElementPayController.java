package com.pg.controller.api;

import com.pg.api.ApiResponse;
import com.pg.service.ElementPayPaymentService;
import com.pg.service.ElementPayPendingReconcileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 본사 — ElementPay getMethods 진단·cardServiceAlias 안내·대기건 getStatus 재동기화.
 */
@RestController
@RequestMapping(value = "/api/hq", produces = "application/json")
public class ApiHqElementPayController {

    private final ElementPayPaymentService elementPayPaymentService;
    private final ElementPayPendingReconcileService elementPayPendingReconcileService;

    public ApiHqElementPayController(ElementPayPaymentService elementPayPaymentService,
                                     ElementPayPendingReconcileService elementPayPendingReconcileService) {
        this.elementPayPaymentService = elementPayPaymentService;
        this.elementPayPendingReconcileService = elementPayPendingReconcileService;
    }

    @PostMapping("/pgApiMng/elementpayMethods")
    public ResponseEntity<ApiResponse<Map<String, Object>>> elementpayMethods(@RequestBody Map<String, Object> body) {
        Long id = null;
        if (body != null && body.get("id") != null) {
            try {
                id = Long.valueOf(String.valueOf(body.get("id")).trim());
            } catch (NumberFormatException ignored) {
                id = null;
            }
        }
        Map<String, Object> result = elementPayPaymentService.listPaymentMethods(id);
        boolean ok = Boolean.TRUE.equals(result.get("success"));
        if (!ok) {
            return ResponseEntity.ok(ApiResponse.fail(
                    String.valueOf(result.getOrDefault("message", "ElementPay methods failed")),
                    String.valueOf(result.getOrDefault("errorCode", "ELEMENTPAY_METHODS_FAILED"))));
        }
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * ElementPay 요청(08) 대기 건을 getStatus 로 강제 동기화.
     * body: {@code paymentIds} / {@code orderNos} / {@code runBatch}=true / {@code forceFinalizeReject}=true
     */
    @PostMapping("/pgApiMng/elementpayReconcilePending")
    public ResponseEntity<ApiResponse<Map<String, Object>>> elementpayReconcilePending(
            @RequestBody(required = false) Map<String, Object> body) {
        boolean runBatch = body != null && Boolean.TRUE.equals(body.get("runBatch"));
        boolean force = body != null && Boolean.TRUE.equals(body.get("forceFinalizeReject"));
        List<String> paymentIds = stringList(body != null ? body.get("paymentIds") : null);
        List<String> orderNos = stringList(body != null ? body.get("orderNos") : null);
        Map<String, Object> result;
        if (!paymentIds.isEmpty() || !orderNos.isEmpty()) {
            result = elementPayPendingReconcileService.reconcileByKeys(paymentIds, orderNos, force);
        } else if (runBatch || body == null || body.isEmpty()) {
            result = elementPayPendingReconcileService.reconcileBatch();
        } else {
            return ResponseEntity.ok(ApiResponse.fail(
                    "paymentIds, orderNos, or runBatch=true required",
                    "ELEMENTPAY_RECONCILE_PARAM"));
        }
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    private static List<String> stringList(Object raw) {
        List<String> out = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (o != null && !String.valueOf(o).isBlank()) {
                    out.add(String.valueOf(o).trim());
                }
            }
        } else if (raw instanceof String s && !s.isBlank()) {
            for (String part : s.split("[,\\s]+")) {
                if (!part.isBlank()) {
                    out.add(part.trim());
                }
            }
        }
        return out;
    }
}
