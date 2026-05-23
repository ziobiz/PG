<?php
/**
 * ICOPAY 가맹점 인라인 결제 API 클라이언트 (PHP 7.4+).
 * prepare / status — ChillPay · JPAY 공통 패턴.
 */
final class IcopayMerchantApiClient
{
    private string $apiBaseUrl;
    private string $compId;
    private string $brokerSecret;
    private int $connectTimeoutSec;
    private int $readTimeoutSec;

    public function __construct(array $config)
    {
        $this->apiBaseUrl = rtrim((string)($config['apiBaseUrl'] ?? ''), '/');
        $this->compId = trim((string)($config['compId'] ?? ''));
        $this->brokerSecret = trim((string)($config['brokerSecret'] ?? ''));
        $this->connectTimeoutSec = (int)($config['connectTimeoutSec'] ?? 15);
        $this->readTimeoutSec = (int)($config['readTimeoutSec'] ?? 30);
    }

    public function getCompId(): string
    {
        return $this->compId;
    }

    public function getApiBaseUrl(): string
    {
        return $this->apiBaseUrl;
    }

    /** @return array{success:bool,data?:array,message?:string,errorCode?:string} */
    public function prepareChillpay(string $orderNo, $amount, string $currency = 'JPY', string $productName = ''): array
    {
        return $this->prepareInline('chillpay', $orderNo, $amount, $currency, $productName);
    }

    /** @return array{success:bool,data?:array,message?:string,errorCode?:string} */
    public function prepareJpay(string $orderNo, $amount, string $currency = 'USD', string $productName = ''): array
    {
        return $this->prepareInline('jpay', $orderNo, $amount, $currency, $productName);
    }

    /** @return array{success:bool,data?:array,message?:string,errorCode?:string} */
    public function statusChillpay(string $orderNo): array
    {
        return $this->statusInline('chillpay', $orderNo);
    }

    /** @return array{success:bool,data?:array,message?:string,errorCode?:string} */
    public function statusJpay(string $orderNo): array
    {
        return $this->statusInline('jpay', $orderNo);
    }

    private function prepareInline(string $vendor, string $orderNo, $amount, string $currency, string $productName): array
    {
        $path = '/api/middleware/v1/merchant/' . $vendor . '/inline-checkout/prepare';
        $body = [
            'compId' => $this->compId,
            'orderNo' => $orderNo,
            'amount' => $amount,
            'currency' => $currency,
            'productName' => $productName,
        ];
        return $this->requestJson('POST', $path, $body);
    }

    private function statusInline(string $vendor, string $orderNo): array
    {
        $qs = http_build_query([
            'compId' => $this->compId,
            'orderNo' => $orderNo,
        ]);
        $path = '/api/middleware/v1/merchant/' . $vendor . '/inline-checkout/status?' . $qs;
        return $this->requestJson('GET', $path, null);
    }

    /** @param array<string,mixed>|null $body */
    private function requestJson(string $method, string $path, ?array $body): array
    {
        if ($this->apiBaseUrl === '' || $this->compId === '') {
            return ['success' => false, 'message' => 'apiBaseUrl / compId 설정 필요', 'errorCode' => 'CONFIG'];
        }
        $url = $this->apiBaseUrl . $path;
        $headers = [
            'Accept: application/json',
            'X-Icopay-Merchant-Broker-Secret: ' . $this->brokerSecret,
        ];
        $ch = curl_init($url);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, $this->connectTimeoutSec);
        curl_setopt($ch, CURLOPT_TIMEOUT, $this->readTimeoutSec);
        curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);
        if ($method === 'POST') {
            $json = json_encode($body ?? [], JSON_UNESCAPED_UNICODE);
            $headers[] = 'Content-Type: application/json';
            curl_setopt($ch, CURLOPT_POSTFIELDS, $json);
        }
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        $raw = curl_exec($ch);
        $errno = curl_errno($ch);
        $err = curl_error($ch);
        $code = (int)curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        if ($errno !== 0) {
            return ['success' => false, 'message' => 'HTTP 오류: ' . $err, 'errorCode' => 'NETWORK'];
        }
        $decoded = json_decode((string)$raw, true);
        if (!is_array($decoded)) {
            return ['success' => false, 'message' => 'JSON 파싱 실패 (HTTP ' . $code . ')', 'errorCode' => 'PARSE'];
        }
        return $decoded;
    }

    public static function embedScriptHtml(string $apiBaseUrl, string $compId, string $sessionToken, string $vendor = 'chillpay'): string
    {
        $base = rtrim($apiBaseUrl, '/');
        $enc = rawurlencode($compId);
        $tok = htmlspecialchars($sessionToken, ENT_QUOTES, 'UTF-8');
        if (strtolower($vendor) === 'jpay') {
            $target = 'icopay-jpay-checkout';
            return '<div id="' . $target . '"></div>' . "\n"
                . '<script src="' . $base . '/v1/embed-jpay-pay/' . $enc . '"'
                . ' data-session-token="' . $tok . '"'
                . ' data-target="' . $target . '" async defer charset="utf-8"></script>';
        }
        $target = 'icopay-pay-checkout';
        return '<div id="' . $target . '"></div>' . "\n"
            . '<script src="' . $base . '/v1/embed-pay/' . $enc . '"'
            . ' data-session-token="' . $tok . '"'
            . ' data-target="' . $target . '" async defer charset="utf-8"></script>';
    }
}
