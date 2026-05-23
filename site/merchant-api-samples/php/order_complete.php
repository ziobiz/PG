<?php
declare(strict_types=1);
$orderNo = trim((string)($_GET['orderNo'] ?? ''));
?>
<!DOCTYPE html>
<html lang="ko"><head><meta charset="UTF-8"><title>주문 완료</title></head>
<body>
  <h1>결제 완료 (예제)</h1>
  <p>주문번호: <?= htmlspecialchars($orderNo, ENT_QUOTES, 'UTF-8') ?></p>
  <p>실제 연동에서는 가맹 DB 상태·영수증 페이지로 연결하세요.</p>
</body></html>
