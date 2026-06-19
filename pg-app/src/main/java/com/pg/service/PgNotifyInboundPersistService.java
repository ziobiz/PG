package com.pg.service;



import com.pg.entity.PgNotifyInbound;

import com.pg.integration.pg.notify.PgNotifyInboundTxnDispatcher;

import com.pg.repository.PgNotifyInboundRepository;

import com.pg.util.PgNotifyInboundSanitizer;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Propagation;

import org.springframework.transaction.annotation.Transactional;



/**

 * 노티 수신 로그({@code tb_pg_notify_inbound}) 저장과 {@code pg_trnsctn} 후처리를

 * <strong>서로 다른 트랜잭션</strong>으로 분리합니다.

 */

@Service

public class PgNotifyInboundPersistService {



    private static final Logger log = LoggerFactory.getLogger(PgNotifyInboundPersistService.class);



    private final PgNotifyInboundRepository inboundRepository;

    private final PgNotifyInboundTxnDispatcher pgNotifyInboundTxnDispatcher;



    public PgNotifyInboundPersistService(PgNotifyInboundRepository inboundRepository,

                                         PgNotifyInboundTxnDispatcher pgNotifyInboundTxnDispatcher) {

        this.inboundRepository = inboundRepository;

        this.pgNotifyInboundTxnDispatcher = pgNotifyInboundTxnDispatcher;

    }



    @Transactional(propagation = Propagation.REQUIRES_NEW)

    public PgNotifyInbound saveInbound(PgNotifyInbound in) {

        PgNotifyInboundSanitizer.sanitize(in);

        try {

            return inboundRepository.save(in);

        } catch (Exception first) {

            log.warn("노티 inbound 1차 저장 실패 merchantId(len)={} status={}: {}",

                    in.getMerchantId() != null ? in.getMerchantId().length() : 0,

                    in.getProcessStatus(),

                    first.getMessage());

            PgNotifyInboundSanitizer.applyLegacyDbLimits(in);

            PgNotifyInboundSanitizer.sanitize(in);

            return inboundRepository.save(in);

        }

    }



    /**

     * @return true 이면 후처리(dispatch) 실패

     */

    @Transactional(propagation = Propagation.REQUIRES_NEW)

    public boolean dispatchTxnUpdates(long inboundId, String channelType) {

        PgNotifyInbound in = inboundRepository.findById(inboundId).orElse(null);

        if (in == null) {

            log.warn("노티 후처리 대상 inbound 없음 id={}", inboundId);

            return true;

        }

        try {

            pgNotifyInboundTxnDispatcher.dispatch(in, channelType);

            return false;

        } catch (Exception e) {

            log.warn("노티→결제내역(pg_trnsctn) 후처리 실패 inboundId={}: {}", inboundId, e.getMessage());

            return true;

        }

    }

}


