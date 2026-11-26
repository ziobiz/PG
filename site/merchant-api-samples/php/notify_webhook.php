<?php
/**
 * ICOPAY → merchant server webhook receiver.
 *
 * When HQ/merchant 「웹훅 서명」 is Active, ICOPAY POSTs JSON with header:
 *   X-Icopay-Signature: v1={hmac_sha256_hex(raw_body, webhook_secret)}
 *
 * Configure ICOPAY_WEBHOOK_SIGN_SECRET (same secret shown once on rotate in admin).
 * If signing is OFF for the merchant, the header is absent — accept without verify.
 *
 * Typical sender: NOTI → merchant callbackUrl (same HMAC when signing Active).
 * Also used for ICOPAY-direct Background/inbound notify. Browser Result is separate.
 */
declare(strict_types=1);

const WEBHOOK_SECRET = getenv('ICOPAY_WEBHOOK_SIGN_SECRET') ?: '';

$raw = file_get_contents('php://input') ?: '';
$sigHeader = $_SERVER['HTTP_X_ICOPAY_SIGNATURE'] ?? '';

function icopay_verify_webhook_signature(string $rawBody, string $header, string $secret): bool
{
    if ($secret === '') {
        // Secret not configured locally — skip verify (dev only). Production must set secret when signing is ON.
        return true;
    }
    if ($header === '' || !preg_match('/^v1=([0-9a-fA-F]+)$/', trim($header), $m)) {
        return false;
    }
    $expected = hash_hmac('sha256', $rawBody, $secret);
    return hash_equals(strtolower($expected), strtolower($m[1]));
}

$signingExpected = ($WEBHOOK_SECRET !== '');
if ($signingExpected && !icopay_verify_webhook_signature($raw, $sigHeader, $WEBHOOK_SECRET)) {
    http_response_code(401);
    header('Content-Type: text/plain; charset=UTF-8');
    echo 'INVALID_SIGNATURE';
    exit;
}

http_response_code(200);
header('Content-Type: text/plain; charset=UTF-8');

$logLine = date('c') . ' sig=' . ($sigHeader !== '' ? 'yes' : 'no')
    . ' len=' . strlen($raw) . ' body=' . $raw . PHP_EOL;
@file_put_contents(__DIR__ . '/notify_webhook.log', $logLine, FILE_APPEND | LOCK_EX);

// TODO: json_decode($raw, true) → orderNo + paymentStatus → idempotent DB update

echo 'OK';
