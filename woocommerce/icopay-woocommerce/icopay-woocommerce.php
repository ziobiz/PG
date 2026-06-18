<?php
/**
 * Plugin Name: ICOPAY WooCommerce
 * Plugin URI: https://icopay.co.kr
 * Description: ICOPAY URL 인라인·리다이렉트 결제(ChillPay/JPAY)를 WooCommerce 결제 수단으로 연동합니다.
 * Version: 1.1.0
 * Author: ICOPAY
 * Requires at least: 6.0
 * Requires PHP: 7.4
 * WC requires at least: 7.0
 * WC tested up to: 9.6
 * Text Domain: icopay-woocommerce
 * License: GPL-2.0-or-later
 *
 * @package ICOPAY_WooCommerce
 */

defined( 'ABSPATH' ) || exit;

define( 'ICOPAY_WC_VERSION', '1.1.0' );
define( 'ICOPAY_WC_PLUGIN_FILE', __FILE__ );
define( 'ICOPAY_WC_PLUGIN_DIR', plugin_dir_path( __FILE__ ) );
define( 'ICOPAY_WC_PLUGIN_URL', plugin_dir_url( __FILE__ ) );

/**
 * Load icopay-core (bundled or sibling path).
 */
function icopay_wc_load_core() {
	static $loaded = false;
	if ( $loaded ) {
		return;
	}
	$candidates = array(
		ICOPAY_WC_PLUGIN_DIR . 'includes/icopay-core/icopay-core.php',
		dirname( ICOPAY_WC_PLUGIN_DIR ) . '/icopay-core/icopay-core.php',
		dirname( dirname( ICOPAY_WC_PLUGIN_DIR ) ) . '/wordpress/icopay-core/icopay-core.php',
	);
	foreach ( $candidates as $path ) {
		if ( is_readable( $path ) ) {
			require_once $path;
			if ( function_exists( 'icopay_core_bootstrap' ) ) {
				icopay_core_bootstrap();
			}
			$loaded = true;
			return;
		}
	}
}

/**
 * WooCommerce 미설치 시 안내.
 */
function icopay_wc_missing_wc_notice() {
	echo '<div class="error"><p><strong>ICOPAY WooCommerce</strong> requires WooCommerce to be installed and active.</p></div>';
}

/**
 * 플러그인 로드.
 */
function icopay_wc_init() {
	if ( ! class_exists( 'WooCommerce' ) ) {
		add_action( 'admin_notices', 'icopay_wc_missing_wc_notice' );
		return;
	}

	icopay_wc_load_core();

	require_once ICOPAY_WC_PLUGIN_DIR . 'includes/class-icopay-api-client.php';
	require_once ICOPAY_WC_PLUGIN_DIR . 'includes/class-icopay-order-helper.php';
	require_once ICOPAY_WC_PLUGIN_DIR . 'includes/class-icopay-webhook-handler.php';
	require_once ICOPAY_WC_PLUGIN_DIR . 'includes/class-icopay-payment-page.php';
	require_once ICOPAY_WC_PLUGIN_DIR . 'includes/class-wc-gateway-icopay.php';

	add_filter( 'woocommerce_payment_gateways', 'icopay_wc_register_gateway' );

	ICOPAY_Webhook_Handler::init();
	ICOPAY_Payment_Page::init();
}
add_action( 'plugins_loaded', 'icopay_wc_init', 11 );

/**
 * @param array $gateways Gateways.
 * @return array
 */
function icopay_wc_register_gateway( $gateways ) {
	$gateways[] = 'WC_Gateway_ICOPAY';
	return $gateways;
}

/**
 * HPOS(고성능 주문 저장) 호환 선언.
 */
add_action(
	'before_woocommerce_init',
	function () {
		if ( class_exists( \Automattic\WooCommerce\Utilities\FeaturesUtil::class ) ) {
			\Automattic\WooCommerce\Utilities\FeaturesUtil::declare_compatibility( 'custom_order_tables', __FILE__, true );
		}
	}
);
