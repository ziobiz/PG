<?php
/**
 * ICOPAY → merchant webhook receiver stub — POST to merchantNotifyUrls registered by HQ.
 * Field layout follows HQ/PG notify mapping. Require idempotent handling, HTTPS, HTTP 200.
 */
declare(strict_types=1);

http_response_code(200);
header('Content-Type: text/plain; charset=UTF-8');

$raw = file_get_contents('php://input') ?: '';
$logLine = date('c') . ' len=' . strlen($raw) . ' body=' . $raw . PHP_EOL;
@file_put_contents(__DIR__ . '/notify_webhook.log', $logLine, FILE_APPEND | LOCK_EX);

// TODO: parse JSON/XML → orderNo + approval status → update merchant DB (idempotent)

echo 'OK';
