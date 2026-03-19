package com.pg.service;

import com.pg.entity.HqNotifyEnvConfig;
import com.pg.entity.PgTrnsctn;
import com.pg.repository.PgTrnsctnRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제내역 그리드 후속조치 (NOTI 환경설정의 자동무효·이메일무효·자동환불·강제환불 플래그와 연동).
 */
@Service
public class PayListActionService {

    public enum PayFollowAction {
        AUTO_VOID,
        EMAIL_VOID,
        AUTO_REFUND,
        FORCE_REFUND
    }

    private final HqNotifyEnvService hqNotifyEnvService;
    private final PgTrnsctnRepository trnsctnRepository;

    public PayListActionService(HqNotifyEnvService hqNotifyEnvService, PgTrnsctnRepository trnsctnRepository) {
        this.hqNotifyEnvService = hqNotifyEnvService;
        this.trnsctnRepository = trnsctnRepository;
    }

    @Transactional
    public void apply(String trnId, String actionRaw) {
        if (trnId == null || trnId.isBlank()) {
            throw new IllegalArgumentException("거래번호(trnId)가 필요합니다.");
        }
        if (actionRaw == null || actionRaw.isBlank()) {
            throw new IllegalArgumentException("action이 필요합니다.");
        }
        PayFollowAction action;
        try {
            action = PayFollowAction.valueOf(actionRaw.trim().toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원하지 않는 action입니다: " + actionRaw);
        }
        HqNotifyEnvConfig env = hqNotifyEnvService.getOrCreate();
        switch (action) {
            case AUTO_VOID -> requireYn(env.getAutoVoidYn(), "자동무효");
            case EMAIL_VOID -> requireYn(env.getEmailVoidYn(), "이메일무효");
            case AUTO_REFUND -> requireYn(env.getAutoRefundYn(), "자동환불");
            case FORCE_REFUND -> requireYn(env.getForceRefundYn(), "강제환불");
        }
        PgTrnsctn t = trnsctnRepository.findById(trnId.trim())
                .orElseThrow(() -> new IllegalArgumentException("거래를 찾을 수 없습니다."));
        t.setStatus(switch (action) {
            case AUTO_VOID -> "40";
            case EMAIL_VOID -> "41";
            case AUTO_REFUND -> "42";
            case FORCE_REFUND -> "31";
        });
        trnsctnRepository.save(t);
    }

    private static void requireYn(String yn, String label) {
        if (!"Y".equalsIgnoreCase(yn)) {
            throw new IllegalStateException("본사설정 > 전산노티·결제환경에서 [" + label + "] 사용이 꺼져 있습니다.");
        }
    }
}
