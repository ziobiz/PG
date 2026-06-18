<?php
/**
 * Plugin Name: ICOPAY JPAY
 * Plugin URI: https://icopay.co.kr
 * Description: 일반 WordPress용 ICOPAY JPAY 결제(인라인·리다이렉트). WooCommerce 불필요.
 * Version: 1.0.0
 * Author: ICOPAY
 * Requires at least: 6.0
 * Requires PHP: 7.4
 * Text Domain: icopay-jpay
 * License: GPL-2.0-or-later
 *
 * @package ICOPAY_JPAY
 */

defined( 'ABSPATH' ) || exit;

define( 'ICOPAY_JPAY_VERSION', '1.0.0' );
define( 'ICOPAY_JPAY_PLUGIN_FILE', __FILE__ );
define( 'ICOPAY_JPAY_PLUGIN_DIR', plugin_dir_path( __FILE__ ) );
define( 'ICOPAY_JPAY_PLUGIN_URL', plugin_dir_url( __FILE__ ) );
define( 'ICOPAY_JPAY_OPTION_KEY', 'icopay_jpay_settings' );

/**
 * Load icopay-core.
 */
function icopay_jpay_load_core() {
	static $loaded = false;
	if ( $loaded ) {
		return;
	}
	$candidates = array(
		ICOPAY_JPAY_PLUGIN_DIR . 'includes/icopay-core/icopay-core.php',
		dirname( ICOPAY_JPAY_PLUGIN_DIR ) . '/icopay-core/icopay-core.php',
		dirname( dirname( ICOPAY_JPAY_PLUGIN_DIR ) ) . '/wordpress/icopay-core/icopay-core.php',
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

require_once ICOPAY_JPAY_PLUGIN_DIR . 'includes/class-icopay-jpay-settings.php';
require_once ICOPAY_JPAY_PLUGIN_DIR . 'includes/class-icopay-jpay-payment.php';
require_once ICOPAY_JPAY_PLUGIN_DIR . 'includes/class-icopay-jpay-webhook.php';
require_once ICOPAY_JPAY_PLUGIN_DIR . 'includes/class-icopay-jpay-shortcode.php';

/**
 * Bootstrap plugin.
 */
function icopay_jpay_init() {
	icopay_jpay_load_core();
	ICOPAY_JPAY_Settings::init();
	ICOPAY_JPAY_Payment::init();
	ICOPAY_JPAY_Webhook::init();
	ICOPAY_JPAY_Shortcode::init();
}
add_action( 'plugins_loaded', 'icopay_jpay_init' );

register_activation_hook(
	__FILE__,
	function () {
		require_once ICOPAY_JPAY_PLUGIN_DIR . 'includes/class-icopay-jpay-payment.php';
		if ( ! get_option( ICOPAY_JPAY_OPTION_KEY ) ) {
			update_option(
				ICOPAY_JPAY_OPTION_KEY,
				array(
					'comp_id'        => '',
					'api_base_url'   => 'https://api.icopay.co.kr',
					'broker_secret'  => '',
					'flow_mode'      => 'inline',
					'lang'           => 'auto',
					'webhook_secret' => '',
					'return_page_id' => 0,
				)
			);
		}
		ICOPAY_JPAY_Payment::register_query_var();
		flush_rewrite_rules();
	}
);

register_deactivation_hook(
	__FILE__,
	function () {
		flush_rewrite_rules();
	}
);
