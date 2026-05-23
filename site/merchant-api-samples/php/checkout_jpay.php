<?php
/**
 * JPAY 인라인 결제 — PHP 연동 예제.
 */
declare(strict_types=1);

$configPath = __DIR__ . '/icopay_config.php';
if (!is_file($configPath)) {
    http_response_code(500);
    exit('Copy icopay_config.example.php to icopay_config.php and configure.');
}
$config = require $configPath;
require_once __DIR__ . '/IcopayMerchantApi.php';

$api = IcopayMerchantApi::fromConfig($config);
$apiBase = rtrim((string)($config['api_base_url'] ?? ''), '/');
$error = '';
$embedHtml = '';
$orderNo = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $orderNo = trim((string)($_POST['orderNo'] ?? ''));
    $amount = trim((string)($_POST['amount'] ?? ''));
    $currency = trim((string)($_POST['currency'] ?? 'USD'));
    $productName = trim((string)($_POST['productName'] ?? 'Product'));

    if ($orderNo === '' || $amount === '') {
        $error = 'orderNo and amount are required.';
    } else {
        $prep = $api->prepareInlineCheckout(IcopayMerchantApi::VENDOR_JPAY, $orderNo, $amount, $currency, $productName);
        if (empty($prep['success']) || empty($prep['data']['sessionToken'])) {
            $error = (string)($prep['message'] ?? 'prepare failed');
        } else {
            $embedHtml = $api->buildEmbedHtml(IcopayMerchantApi::VENDOR_JPAY, (string)$prep['data']['sessionToken']);
        }
    }
}
?>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>ICOPAY JPAY Checkout (PHP sample)</title>
  <style>body{font-family:system-ui,sans-serif;max-width:640px;margin:2rem auto;padding:0 1rem} .err{color:#b02a37}</style>
</head>
<body>
  <h1>JPAY 결제 (PHP 예제)</h1>
  <?php if ($error !== ''): ?><p class="err"><?= htmlspecialchars($error, ENT_QUOTES, 'UTF-8') ?></p><?php endif; ?>

  <?php if ($embedHtml === ''): ?>
  <form method="post">
    <p><label>주문번호 <input name="orderNo" required maxlength="64" value="<?= htmlspecialchars('JORD' . date('YmdHis'), ENT_QUOTES, 'UTF-8') ?>"></label></p>
    <p><label>금액 <input name="amount" type="number" step="0.01" min="0.01" required value="100"></label></p>
    <p><label>통화 <input name="currency" value="USD" maxlength="3"></label></p>
    <p><label>상품명 <input name="productName" value="Test product"></label></p>
    <button type="submit">결제하기</button>
  </form>
  <?php else: ?>
  <p>주문번호: <strong><?= htmlspecialchars($orderNo, ENT_QUOTES, 'UTF-8') ?></strong></p>
  <?= $embedHtml ?>
  <script src="<?= htmlspecialchars($apiBase, ENT_QUOTES, 'UTF-8') ?>/merchant-api-samples/common/icopay-checkout.js"></script>
  <script>
    IcopayCheckout.onMessage(function (detail) {
      if (detail.phase === 'finished' && detail.success) {
        window.location.href = 'order_complete.php?orderNo=' + encodeURIComponent(detail.orderNo || '');
      }
    }, <?= json_encode($apiBase, JSON_UNESCAPED_SLASHES) ?>);
  </script>
  <?php endif; ?>
</body>
</html>
