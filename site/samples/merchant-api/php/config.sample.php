<?php
/**
 * ICOPAY 가맹점 API 설정 — config.php 로 복사 후 값을 채우세요.
 * brokerSecret 은 소스·Git 에 올리지 말고 서버 환경변수/별도 파일로 관리하세요.
 */
return [
    'apiBaseUrl'    => 'https://api.icopay.co.kr',
    'compId'        => 'M000123',
    'brokerSecret'  => 'YOUR_BROKER_SECRET',
    'connectTimeoutSec' => 15,
    'readTimeoutSec'    => 30,
];
