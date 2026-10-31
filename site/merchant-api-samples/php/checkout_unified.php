<?php
/**
 * ICOPAY unified inline checkout — PHP integration sample (auto PG routing).
 * buyer.email, buyer.phone, buyer.countryIso2, buyer.name are required (empty string = missing).
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
$pgVendor = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $orderNo = trim((string)($_POST['orderNo'] ?? ''));
    $amount = trim((string)($_POST['amount'] ?? ''));
    $currency = trim((string)($_POST['currency'] ?? 'USD'));
    $productName = trim((string)($_POST['productName'] ?? 'Sample product'));
    $email = trim((string)($_POST['email'] ?? ''));
    $phone = trim((string)($_POST['phone'] ?? ''));
    $countryIso2 = strtoupper(trim((string)($_POST['countryIso2'] ?? '')));
    $buyerName = trim((string)($_POST['name'] ?? ''));

    if ($orderNo === '' || $amount === '' || $email === '' || $phone === '' || strlen($countryIso2) !== 2 || $buyerName === '') {
        $error = 'orderNo, amount, email, phone, countryIso2 (2 letters), and name are required.';
    } else {
        $buyer = [
            'email' => $email,
            'phone' => $phone,
            'countryIso2' => $countryIso2,
            'name' => $buyerName,
        ];
        $prep = $api->prepareUnifiedCheckout($orderNo, $amount, $buyer, $currency, $productName);
        if (empty($prep['success']) || empty($prep['data']['sessionToken'])) {
            $error = (string)($prep['message'] ?? 'prepare failed');
        } else {
            $pgVendor = (string)($prep['data']['pgVendor'] ?? '');
            if (IcopayMerchantApi::isSandboxPrepare($prep)) {
                $done = $api->completeSandboxCheckout($orderNo, (string)($prep['data']['sessionToken'] ?? ''));
                if (empty($done['success'])) {
                    $error = (string)($done['message'] ?? 'sandbox complete failed');
                } else {
                    $embedHtml = '<p>ICOPAY sandbox simulated (no live acquirer). Status: <code>'
                        . htmlspecialchars((string)($done['data']['status'] ?? 'APPROVED'), ENT_QUOTES, 'UTF-8')
                        . '</code></p>';
                }
            } else {
                $embedHtml = $api->buildUnifiedEmbedHtml((string)$prep['data']['sessionToken']);
            }
        }
    }
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>ICOPAY Unified Checkout (PHP sample)</title>
  <style>body{font-family:system-ui,sans-serif;max-width:640px;margin:2rem auto;padding:0 1rem} .err{color:#b02a37}</style>
</head>
<body>
  <h1>Unified checkout (PHP sample)</h1>
  <p class="small text-muted">ICOPAY unified checkout. With the sandbox secret, this sample calls complete instead of opening a card window.</p>
  <?php if ($error !== ''): ?><p class="err"><?= htmlspecialchars($error, ENT_QUOTES, 'UTF-8') ?></p><?php endif; ?>

  <?php if ($embedHtml === ''): ?>
  <form method="post">
    <p><label>Order no. <input name="orderNo" required maxlength="64" value="<?= htmlspecialchars('ORD' . date('YmdHis'), ENT_QUOTES, 'UTF-8') ?>"></label></p>
    <p><label>Amount <input name="amount" type="number" step="0.01" min="0.01" required value="10000"></label></p>
    <p><label>Currency <input name="currency" value="USD" maxlength="3"></label></p>
    <p><label>Product name <input name="productName" value="Test product"></label></p>
    <fieldset>
      <legend>buyer (required)</legend>
      <p><label>Name <input name="name" required value="Taro Yamada"></label></p>
      <p><label>Email <input name="email" type="email" required value="buyer@example.com"></label></p>
      <p><label>Phone <input name="phone" required value="1012345678"></label></p>
      <p><label>Country ISO2 <input name="countryIso2" required maxlength="2" value="KR"></label></p>
    </fieldset>
    <button type="submit">Pay</button>
  </form>
  <?php else: ?>
  <p>Order no.: <strong><?= htmlspecialchars($orderNo, ENT_QUOTES, 'UTF-8') ?></strong>
    <?php if ($pgVendor !== ''): ?> · PG: <code><?= htmlspecialchars($pgVendor, ENT_QUOTES, 'UTF-8') ?></code><?php endif; ?>
  </p>
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
