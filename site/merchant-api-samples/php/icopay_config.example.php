<?php
/**
 * Copy icopay_config.example.php to icopay_config.php and fill in values.
 * Keep icopay_config.php outside the web document root when possible.
 */
return [
    'api_base_url'    => 'https://icopay.co.kr',
    'comp_id'         => 'M000123',
    'broker_secret'   => 'YOUR_BROKER_SECRET',
    /**
     * unified — PG-agnostic unified prepare (recommended)
     * chillpay | jpay — legacy per-PG inline-checkout
     */
    'default_integration' => 'unified',
];
