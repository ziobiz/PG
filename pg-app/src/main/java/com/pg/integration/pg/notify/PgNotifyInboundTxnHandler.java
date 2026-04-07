package com.pg.integration.pg.notify;

import com.pg.entity.PgNotifyInbound;

/**
 * PG 노티 수신({@link PgNotifyInbound}) 이후, 거래 적재·벤더별 후처리를 담당하는 확장 포인트.
 * <p>
 * 스프링이 등록한 구현체는 {@link PgNotifyInboundTxnDispatcher}가 {@link #order()} 오름차순으로 호출합니다.
 * 첫 번째로 {@code true}를 반환한 핸들러에서 체인이 종료됩니다.
 */
public interface PgNotifyInboundTxnHandler {

    /**
     * 낮을수록 먼저 시도(예: ChillPay = 0, 신규 PG = 10).
     */
    default int order() {
        return 100;
    }

    /**
     * 이 수신 건을 처리했는지 여부.
     *
     * @return {@code true} 이면 이후 핸들러를 호출하지 않음(이 핸들러가 처리 종료 또는 의도적 무시).
     *         {@code false} 이면 다음 PG 핸들러에게 넘김(벤더 스니핑 실패 등).
     */
    boolean tryRecord(PgNotifyInbound inbound, String notifyChannel);
}
