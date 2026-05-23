<?php
/**
 * icopay_config.example.php → icopay_config.php 로 복사 후 값을 채우세요.
 * icopay_config.php 는 웹에서 직접 접근되지 않도록 document root 밖에 두는 것을 권장합니다.
 */
return [
    'api_base_url'    => 'https://icopay.co.kr',
    'comp_id'         => 'M000123',
    'broker_secret'   => 'YOUR_BROKER_SECRET',
    /** chillpay | jpay — 가맹 계약 PG */
    'default_vendor'  => 'chillpay',
];
