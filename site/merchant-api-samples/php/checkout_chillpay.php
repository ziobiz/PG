<?php
/**
 * ChillPay inline checkout — PHP integration sample.
 * 1) Create order (merchant DB) 2) prepare 3) ICOPAY payment iframe embed 4) complete via postMessage
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
    $currency = trim((string)($_POST['currency'] ?? 'JPY'));
    $productName = trim((string)($_POST['productName'] ?? 'Sample product'));

    if ($orderNo === '' || $amount === '') {
        $error = 'orderNo and amount are required.';
    } else {
        // TODO: persist order (PENDING) in merchant DB
        $prep = $api->prepareInlineCheckout(IcopayMerchantApi::VENDOR_CHILLPAY, $orderNo, $amount, $currency, $productName);
        if (empty($prep['success']) || empty($prep['data']['sessionToken'])) {
            $error = (string)($prep['message'] ?? 'prepare failed');
        } else {
            $embedHtml = $api->buildEmbedHtml(IcopayMerchantApi::VENDOR_CHILLPAY, (string)$prep['data']['sessionToken']);
        }
    }
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>ICOPAY ChillPay Checkout (PHP sample)</title>
  <style>body{font-family:system-ui,sans-serif;max-width:640px;margin:2rem auto;padding:0 1rem} .err{color:#b02a37}</style>
</head>
<body>
  <h1>ChillPay checkout (PHP sample)</h1>
  <?php if ($error !== ''): ?><p class="err"><?= htmlspecialchars($error, ENT_QUOTES, 'UTF-8') ?></p><?php endif; ?>

  <?php if ($embedHtml === ''): ?>
  <form method="post">
    <p><label>Order no. <input name="orderNo" required maxlength="20" value="<?= htmlspecialchars('ORD' . date('YmdHis'), ENT_QUOTES, 'UTF-8') ?>"></label></p>
    <p><label>Amount <input name="amount" type="number" step="0.01" min="0.01" required value="10000"></label></p>
    <p><label>Currency <input name="currency" value="JPY" maxlength="3"></label></p>
    <p><label>Product name <input name="productName" value="Test product"></label></p>
    <button type="submit">Pay</button>
  </form>
  <?php else: ?>
  <p>Order no.: <strong><?= htmlspecialchars($orderNo, ENT_QUOTES, 'UTF-8') ?></strong></p>
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
