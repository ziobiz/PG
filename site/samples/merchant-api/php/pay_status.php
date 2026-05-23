<?php
/**
 * 주문 결제 상태 조회 예 (PHP) — ChillPay / JPAY
 * 사용: pay_status.php?vendor=chillpay&orderNo=ORD-001
 */
declare(strict_types=1);

header('Content-Type: application/json; charset=UTF-8');

$configPath = __DIR__ . '/config.php';
if (!is_file($configPath)) {
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'config.php missing'], JSON_UNESCAPED_UNICODE);
    exit;
}
$config = require $configPath;
require_once __DIR__ . '/IcopayMerchantApiClient.php';

$client = new IcopayMerchantApiClient($config);
$vendor = strtolower(trim((string)($_GET['vendor'] ?? 'chillpay')));
$orderNo = trim((string)($_GET['orderNo'] ?? ''));

if ($orderNo === '') {
    echo json_encode(['success' => false, 'message' => 'orderNo required'], JSON_UNESCAPED_UNICODE);
    exit;
}

$res = ($vendor === 'jpay') ? $client->statusJpay($orderNo) : $client->statusChillpay($orderNo);
echo json_encode($res, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT);
