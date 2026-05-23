<?php
/**
 * ICOPAY → 가맹점 통보(웹훅) 수신 스텁 — 본사가 등록한 merchantNotifyUrls 로 POST 됩니다.
 * 실제 필드는 본사·PG별 노티 매핑에 따릅니다. 멱등 처리·HTTPS·200 응답 필수.
 */
declare(strict_types=1);

http_response_code(200);
header('Content-Type: text/plain; charset=UTF-8');

$raw = file_get_contents('php://input') ?: '';
$logLine = date('c') . ' len=' . strlen($raw) . ' body=' . $raw . PHP_EOL;
@file_put_contents(__DIR__ . '/notify_webhook.log', $logLine, FILE_APPEND | LOCK_EX);

// TODO: JSON/XML 파싱 → orderNo·승인여부 확인 → 가맹 DB 주문 상태 갱신(멱등)

echo 'OK';
