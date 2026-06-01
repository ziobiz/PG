<?php
/**
 * ICOPAY 통합 인라인 checkout — PHP 연동 예제 (PG 자동 분기).
 * buyer(email·phone·countryIso2) 필수.
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
    $productName = trim((string)($_POST['productName'] ?? '상품'));
    $email = trim((string)($_POST['email'] ?? ''));
    $phone = trim((string)($_POST['phone'] ?? ''));
    $countryIso2 = strtoupper(trim((string)($_POST['countryIso2'] ?? '')));

    if ($orderNo === '' || $amount === '' || $email === '' || $phone === '' || strlen($countryIso2) !== 2) {
        $error = 'orderNo, amount, email, phone, countryIso2(2자) are required.';
    } else {
        $buyer = [
            'email' => $email,
            'phone' => $phone,
            'countryIso2' => $countryIso2,
        ];
        $prep = $api->prepareUnifiedCheckout($orderNo, $amount, $buyer, $currency, $productName);
        if (empty($prep['success']) || empty($prep['data']['sessionToken'])) {
            $error = (string)($prep['message'] ?? 'prepare failed');
        } else {
            $pgVendor = (string)($prep['data']['pgVendor'] ?? '');
            $embedHtml = $api->buildUnifiedEmbedHtml((string)$prep['data']['sessionToken']);
        }
    }
}
?>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>ICOPAY Unified Checkout (PHP sample)</title>
  <style>body{font-family:system-ui,sans-serif;max-width:640px;margin:2rem auto;padding:0 1rem} .err{color:#b02a37}</style>
</head>
<body>
  <h1>통합 checkout (PHP 예제)</h1>
  <p class="small text-muted">운영 PG에 따라 ChillPay/JPAY 결제창이 자동으로 열립니다.</p>
  <?php if ($error !== ''): ?><p class="err"><?= htmlspecialchars($error, ENT_QUOTES, 'UTF-8') ?></p><?php endif; ?>

  <?php if ($embedHtml === ''): ?>
  <form method="post">
    <p><label>주문번호 <input name="orderNo" required maxlength="64" value="<?= htmlspecialchars('ORD' . date('YmdHis'), ENT_QUOTES, 'UTF-8') ?>"></label></p>
    <p><label>금액 <input name="amount" type="number" step="0.01" min="0.01" required value="10000"></label></p>
    <p><label>통화 <input name="currency" value="USD" maxlength="3"></label></p>
    <p><label>상품명 <input name="productName" value="테스트 상품"></label></p>
    <fieldset>
      <legend>buyer (필수)</legend>
      <p><label>이메일 <input name="email" type="email" required value="buyer@example.com"></label></p>
      <p><label>전화 <input name="phone" required value="1012345678"></label></p>
      <p><label>국가 ISO2 <input name="countryIso2" required maxlength="2" value="KR"></label></p>
    </fieldset>
    <button type="submit">결제하기</button>
  </form>
  <?php else: ?>
  <p>주문번호: <strong><?= htmlspecialchars($orderNo, ENT_QUOTES, 'UTF-8') ?></strong>
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
