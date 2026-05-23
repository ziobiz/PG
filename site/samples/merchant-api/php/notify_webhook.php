<?php
/**
 * ICOPAY → 가맹점 통보(웹훅) 수신 스텁 (PHP).
 * 본사가 등록한 merchantNotifyUrls 에 이 URL 을 HTTPS 로 노출하세요.
 * 실제 본문 형식·서명 검증은 본사 배포 문서를 따릅니다.
 */
declare(strict_types=1);

$raw = file_get_contents('php://input') ?: '';
$headers = function_exists('getallheaders') ? getallheaders() : [];

/* TODO: IP 화이트리스트·서명 검증·멱등(orderNo+상태) 처리 */
$logLine = date('c') . ' NOTIFY ' . $raw . "\n";
@file_put_contents(sys_get_temp_dir() . '/icopay_notify.log', $logLine, FILE_APPEND);

http_response_code(200);
header('Content-Type: text/plain; charset=UTF-8');
echo 'OK';
