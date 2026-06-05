<?php
declare(strict_types=1);
$orderNo = trim((string)($_GET['orderNo'] ?? ''));
?>
<!DOCTYPE html>
<html lang="en"><head><meta charset="UTF-8"><title>Order complete</title></head>
<body>
  <h1>Payment complete (sample)</h1>
  <p>Order no.: <?= htmlspecialchars($orderNo, ENT_QUOTES, 'UTF-8') ?></p>
  <p>In production, redirect to your merchant order status or receipt page.</p>
</body></html>
