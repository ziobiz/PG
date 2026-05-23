<?php
/**
 * JPAY 인라인 결제 페이지 예 (PHP).
 */
declare(strict_types=1);

$configPath = __DIR__ . '/config.php';
if (!is_file($configPath)) {
    http_response_code(500);
    echo 'config.php 가 없습니다. config.sample.php 를 복사하세요.';
    exit;
}
$config = require $configPath;
require_once __DIR__ . '/IcopayMerchantApiClient.php';

$client = new IcopayMerchantApiClient($config);
$error = '';
$embedHtml = '';
$orderNo = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $orderNo = 'J' . date('ymdHis') . substr((string)random_int(100, 999), 0, 3);
    $amount = (float)($_POST['amount'] ?? 0);
    $item = trim((string)($_POST['item'] ?? 'Sample product'));
    $currency = trim((string)($_POST['currency'] ?? 'USD'));
    if ($amount <= 0) {
        $error = '금액을 입력하세요.';
    } else {
        $res = $client->prepareJpay($orderNo, $amount, $currency, $item);
        if (!empty($res['success']) && !empty($res['data']['sessionToken'])) {
            $embedHtml = IcopayMerchantApiClient::embedScriptHtml(
                $client->getApiBaseUrl(),
                $client->getCompId(),
                (string)$res['data']['sessionToken'],
                'jpay'
            );
        } else {
            $error = (string)($res['message'] ?? 'prepare 실패');
        }
    }
}
?>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>JPAY checkout (PHP sample)</title>
  <style>
    body { font-family: system-ui, sans-serif; max-width: 640px; margin: 2rem auto; padding: 0 1rem; }
    .err { color: #b02a37; margin-bottom: 1rem; }
    label { display: block; margin-top: 0.75rem; }
    input, button { padding: 0.4rem 0.6rem; }
  </style>
</head>
<body>
  <h1>ICOPAY JPAY 결제 (PHP 샘플)</h1>
  <p>compId: <code><?= htmlspecialchars($client->getCompId(), ENT_QUOTES, 'UTF-8') ?></code></p>
  <?php if ($error !== ''): ?><p class="err"><?= htmlspecialchars($error, ENT_QUOTES, 'UTF-8') ?></p><?php endif; ?>

  <?php if ($embedHtml === ''): ?>
  <form method="post">
    <label>상품명 <input type="text" name="item" value="Test item" required></label>
    <label>금액 <input type="number" name="amount" step="0.01" min="1" value="100" required></label>
    <label>통화 <input type="text" name="currency" value="USD" maxlength="3"></label>
    <p><button type="submit">결제창 열기 (prepare)</button></p>
  </form>
  <?php else: ?>
  <p>주문번호: <strong><?= htmlspecialchars($orderNo, ENT_QUOTES, 'UTF-8') ?></strong></p>
  <?= $embedHtml ?>
  <script>
    window.onIcopayJpayCheckout = function (detail) {
      console.log('JPAY checkout', detail);
    };
    window.addEventListener('message', function (ev) {
      if (!ev.data || ev.data.type !== 'ICOPAY_INLINE_CHECKOUT') return;
      console.log('ICOPAY checkout event', ev.data.detail);
    });
  </script>
  <?php endif; ?>
</body>
</html>
