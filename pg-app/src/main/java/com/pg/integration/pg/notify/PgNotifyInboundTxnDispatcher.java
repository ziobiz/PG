package com.pg.integration.pg.notify;

import com.pg.entity.PgNotifyInbound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * 등록된 {@link PgNotifyInboundTxnHandler} 에게 노티→거래 후처리를 순서대로 위임합니다.
 */
@Component
public class PgNotifyInboundTxnDispatcher {

    private static final Logger log = LoggerFactory.getLogger(PgNotifyInboundTxnDispatcher.class);

    private final List<PgNotifyInboundTxnHandler> handlers;

    public PgNotifyInboundTxnDispatcher(List<PgNotifyInboundTxnHandler> handlers) {
        this.handlers = handlers == null ? List.of() : handlers.stream()
                .sorted(Comparator.comparingInt(PgNotifyInboundTxnHandler::order))
                .toList();
    }

    /**
     * 수신 로그 저장 직후 호출 — 한 핸들러가 {@code true}를 반환할 때까지 시도합니다.
     */
    public void dispatch(PgNotifyInbound inbound, String notifyChannel) {
        if (inbound == null) {
            return;
        }
        for (PgNotifyInboundTxnHandler h : handlers) {
            try {
                if (h.tryRecord(inbound, notifyChannel)) {
                    return;
                }
            } catch (Exception e) {
                log.warn("PgNotifyInboundTxnHandler 실패 (다음 핸들러 시도): {} — {}",
                        h.getClass().getSimpleName(), e.getMessage());
            }
        }
        if (!handlers.isEmpty()) {
            log.debug("노티 inbound id={} 에 대해 어떤 핸들러도 처리하지 않음 (channel={})",
                    inbound.getId(), notifyChannel);
        }
    }
}
