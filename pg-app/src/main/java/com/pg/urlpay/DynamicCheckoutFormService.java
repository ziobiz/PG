package com.pg.urlpay;

import com.pg.repository.HqApiConfigRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 본사 다이나믹 결제방식(Y/N) — ICOPAY 카드 입력창 UX.
 * 기기별 적용(PC=일반, 모바일·태블릿=다이나믹)은 클라이언트에서 판정.
 */
@Service
public class DynamicCheckoutFormService {

    private final HqApiConfigRepository hqApiConfigRepository;

    public DynamicCheckoutFormService(HqApiConfigRepository hqApiConfigRepository) {
        this.hqApiConfigRepository = hqApiConfigRepository;
    }

    public String resolveHqYn() {
        return hqApiConfigRepository.findAll().stream().findFirst()
                .map(c -> normalizeYn(c.getDynamicCheckoutFormYn()))
                .orElse("Y");
    }

    public void putIntoMap(Map<String, Object> data) {
        if (data == null) {
            return;
        }
        String yn = resolveHqYn();
        data.put("dynamicCheckoutFormYn", yn);
        data.put("hqDynamicCheckoutFormYn", yn);
    }

    private static String normalizeYn(String raw) {
        return raw != null && "N".equalsIgnoreCase(raw.trim()) ? "N" : "Y";
    }
}
