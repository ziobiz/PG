package com.pg.crypto;

import com.pg.entity.CryptoPayoutQueue;

/**
 * AUTO 출금 실행 게이트웨이.
 * 미설정·비활성이면 SENT 를 찍지 않는다(스텁 해시 금지).
 */
public interface CryptoPayoutWalletGateway {

    /** 외부 지갑 API URL 등이 준비됐는지. */
    boolean isConfigured();

    /** 운영 플래그 + 설정이 모두 켜져 실제 송금을 시도해도 되는지. */
    boolean isExecuteEnabled();

    /** 미실행 사유(관리자·로그용). */
    String disabledReason();

    Result send(CryptoPayoutQueue queue);

    record Result(boolean success, String txHash, String message, boolean retryable) {
        public static Result ok(String txHash) {
            return new Result(true, txHash, null, false);
        }

        public static Result fail(String message, boolean retryable) {
            return new Result(false, null, message, retryable);
        }
    }
}
