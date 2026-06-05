<?php
/**
 * ICOPAY merchant inline checkout API client (PHP 7.4+).
 * Flow: prepare → embed script → postMessage / webhook for completion.
 */
final class IcopayMerchantApi
{
    public const VENDOR_CHILLPAY = 'chillpay';
    public const VENDOR_JPAY = 'jpay';
    public const HEADER_BROKER_SECRET = 'X-Icopay-Merchant-Broker-Secret';

    private string $apiBase;
    private string $compId;
    private string $brokerSecret;

    public function __construct(string $apiBase, string $compId, string $brokerSecret)
    {
        $this->apiBase = rtrim(trim($apiBase), '/');
        $this->compId = trim($compId);
        $this->brokerSecret = trim($brokerSecret);
    }

    public static function fromConfig(array $cfg): self
    {
        return new self(
            (string)($cfg['api_base_url'] ?? ''),
            (string)($cfg['comp_id'] ?? ''),
            (string)($cfg['broker_secret'] ?? '')
        );
    }

    /** Map page language to KOR|ENG|JPN|CHN|THA (html lang / Accept-Language). */
    public static function detectPageLang(): string
    {
        return self::normalizeLang($_SERVER['HTTP_ACCEPT_LANGUAGE'] ?? '');
    }

    public static function normalizeLang(?string $raw): string
    {
        if ($raw === null || trim($raw) === '') {
            return '';
        }
        $u = strtoupper(trim($raw));
        if (in_array($u, ['KOR', 'ENG', 'JPN', 'CHN', 'THA'], true)) {
            return $u;
        }
        if (in_array($u, ['KO', 'KR', 'KOREAN'], true)) {
            return 'KOR';
        }
        if (in_array($u, ['EN', 'ENGLISH'], true)) {
            return 'ENG';
        }
        if (in_array($u, ['JA', 'JP', 'JPY', 'JAPANESE'], true)) {
            return 'JPN';
        }
        if (in_array($u, ['ZH', 'CN', 'CH', 'CHINESE'], true)) {
            return 'CHN';
        }
        if (in_array($u, ['TH', 'THAI'], true)) {
            return 'THA';
        }
        $tag = strtolower(trim(explode(',', $raw)[0] ?? ''));
        if (strpos($tag, 'ko') === 0) {
            return 'KOR';
        }
        if (strpos($tag, 'ja') === 0) {
            return 'JPN';
        }
        if (strpos($tag, 'zh') === 0) {
            return 'CHN';
        }
        if (strpos($tag, 'th') === 0) {
            return 'THA';
        }
        if (strpos($tag, 'en') === 0) {
            return 'ENG';
        }
        return '';
    }

    /** @return array{success:bool,data?:array,message?:string,errorCode?:string} */
    public function prepareInlineCheckout(
        string $vendor,
        string $orderNo,
        $amount,
        string $currency = '',
        string $productName = '',
        string $lang = ''
    ): array {
        $path = $this->preparePath($vendor);
        $body = [
            'compId' => $this->compId,
            'orderNo' => $orderNo,
            'amount' => $amount,
        ];
        if ($currency !== '') {
            $body['currency'] = strtoupper($currency);
        }
        if ($productName !== '') {
            $body['productName'] = $productName;
        }
        $langNorm = self::normalizeLang($lang !== '' ? $lang : self::detectPageLang());
        if ($langNorm !== '') {
            $body['lang'] = $langNorm;
        }
        return $this->postJson($path, $body);
    }

    /**
     * PG-agnostic unified prepare — buyer (email, phone, countryIso2) required.
     *
     * @param array{email:string,phone:string,countryIso2:string,...} $buyer
     * @return array{success:bool,data?:array,message?:string,errorCode?:string}
     */
    public function prepareUnifiedCheckout(
        string $orderNo,
        $amount,
        array $buyer,
        string $currency = '',
        string $productName = '',
        string $lang = ''
    ): array {
        $body = [
            'compId' => $this->compId,
            'orderNo' => $orderNo,
            'amount' => $amount,
            'buyer' => $buyer,
        ];
        if ($currency !== '') {
            $body['currency'] = strtoupper($currency);
        }
        if ($productName !== '') {
            $body['productName'] = $productName;
        }
        $langNorm = self::normalizeLang($lang !== '' ? $lang : self::detectPageLang());
        if ($langNorm !== '') {
            $body['lang'] = $langNorm;
        }
        return $this->postJson('/api/middleware/v1/merchant/checkout/prepare', $body);
    }

    /** @return array{success:bool,data?:array,message?:string,errorCode?:string} */
    public function getUnifiedPaymentStatus(string $orderNo): array
    {
        $qs = http_build_query([
            'compId' => $this->compId,
            'orderNo' => $orderNo,
        ]);
        return $this->getJson('/api/middleware/v1/merchant/checkout/status?' . $qs);
    }

    public function buildUnifiedEmbedHtml(string $sessionToken, string $targetId = '', string $lang = ''): string
    {
        $target = $targetId !== '' ? $targetId : 'icopay-checkout';
        $compEnc = rawurlencode($this->compId);
        $tokEnc = htmlspecialchars($sessionToken, ENT_QUOTES, 'UTF-8');
        $src = htmlspecialchars($this->apiBase . '/v1/embed-checkout/' . $compEnc, ENT_QUOTES, 'UTF-8');
        $targetEsc = htmlspecialchars($target, ENT_QUOTES, 'UTF-8');
        $langNorm = self::normalizeLang($lang !== '' ? $lang : self::detectPageLang());
        $langAttr = $langNorm !== ''
            ? ' data-lang="' . htmlspecialchars($langNorm, ENT_QUOTES, 'UTF-8') . '"'
            : '';
        return '<div id="' . $targetEsc . '"></div>' . "\n"
            . '<script src="' . $src . '"'
            . ' data-session-token="' . $tokEnc . '"'
            . ' data-target="' . $targetEsc . '"'
            . $langAttr
            . ' async defer charset="utf-8"></script>';
    }

    /** @return array{success:bool,data?:array,message?:string,errorCode?:string} */
    public function getPaymentStatus(string $vendor, string $orderNo): array
    {
        $path = $this->statusPath($vendor);
        $qs = http_build_query([
            'compId' => $this->compId,
            'orderNo' => $orderNo,
        ]);
        return $this->getJson($path . '?' . $qs);
    }

    public function buildEmbedHtml(string $vendor, string $sessionToken, string $targetId = '', string $lang = ''): string
    {
        $v = strtolower(trim($vendor));
        $isJpay = ($v === self::VENDOR_JPAY);
        $embedPath = $isJpay ? '/v1/embed-jpay-pay/' : '/v1/embed-pay/';
        $defaultTarget = $isJpay ? 'icopay-jpay-checkout' : 'icopay-pay-checkout';
        $target = $targetId !== '' ? $targetId : $defaultTarget;
        $compEnc = rawurlencode($this->compId);
        $tokEnc = htmlspecialchars($sessionToken, ENT_QUOTES, 'UTF-8');
        $src = htmlspecialchars($this->apiBase . $embedPath . $compEnc, ENT_QUOTES, 'UTF-8');
        $targetEsc = htmlspecialchars($target, ENT_QUOTES, 'UTF-8');
        $langNorm = self::normalizeLang($lang !== '' ? $lang : self::detectPageLang());
        $langAttr = $langNorm !== ''
            ? ' data-lang="' . htmlspecialchars($langNorm, ENT_QUOTES, 'UTF-8') . '"'
            : '';
        return '<div id="' . $targetEsc . '"></div>' . "\n"
            . '<script src="' . $src . '"'
            . ' data-session-token="' . $tokEnc . '"'
            . ' data-target="' . $targetEsc . '"'
            . $langAttr
            . ' async defer charset="utf-8"></script>';
    }

    private function preparePath(string $vendor): string
    {
        return strtolower(trim($vendor)) === self::VENDOR_JPAY
            ? '/api/middleware/v1/merchant/jpay/inline-checkout/prepare'
            : '/api/middleware/v1/merchant/chillpay/inline-checkout/prepare';
    }

    private function statusPath(string $vendor): string
    {
        return strtolower(trim($vendor)) === self::VENDOR_JPAY
            ? '/api/middleware/v1/merchant/jpay/inline-checkout/status'
            : '/api/middleware/v1/merchant/chillpay/inline-checkout/status';
    }

    /** @return array{success:bool,data?:array,message?:string,errorCode?:string} */
    private function postJson(string $path, array $body): array
    {
        $url = $this->apiBase . $path;
        $json = json_encode($body, JSON_UNESCAPED_UNICODE);
        if ($json === false) {
            return ['success' => false, 'message' => 'JSON encode failed', 'errorCode' => 'LOCAL_ERROR'];
        }
        $ch = curl_init($url);
        curl_setopt_array($ch, [
            CURLOPT_POST => true,
            CURLOPT_POSTFIELDS => $json,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_TIMEOUT => 30,
            CURLOPT_HTTPHEADER => [
                'Content-Type: application/json',
                'Accept: application/json',
                self::HEADER_BROKER_SECRET . ': ' . $this->brokerSecret,
            ],
        ]);
        $raw = curl_exec($ch);
        $errno = curl_errno($ch);
        $err = curl_error($ch);
        curl_close($ch);
        if ($errno !== 0) {
            return ['success' => false, 'message' => 'HTTP error: ' . $err, 'errorCode' => 'NETWORK_ERROR'];
        }
        $decoded = json_decode((string)$raw, true);
        if (!is_array($decoded)) {
            return ['success' => false, 'message' => 'Invalid JSON response', 'errorCode' => 'PARSE_ERROR'];
        }
        return $decoded;
    }

    /** @return array{success:bool,data?:array,message?:string,errorCode?:string} */
    private function getJson(string $pathWithQuery): array
    {
        $url = $this->apiBase . $pathWithQuery;
        $ch = curl_init($url);
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_TIMEOUT => 30,
            CURLOPT_HTTPHEADER => [
                'Accept: application/json',
                self::HEADER_BROKER_SECRET . ': ' . $this->brokerSecret,
            ],
        ]);
        $raw = curl_exec($ch);
        $errno = curl_errno($ch);
        $err = curl_error($ch);
        curl_close($ch);
        if ($errno !== 0) {
            return ['success' => false, 'message' => 'HTTP error: ' . $err, 'errorCode' => 'NETWORK_ERROR'];
        }
        $decoded = json_decode((string)$raw, true);
        if (!is_array($decoded)) {
            return ['success' => false, 'message' => 'Invalid JSON response', 'errorCode' => 'PARSE_ERROR'];
        }
        return $decoded;
    }
}
