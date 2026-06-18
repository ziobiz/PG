<?php
/**
 * ICOPAY Core loader — shared by WooCommerce / general WP plugins.
 *
 * @package ICOPAY_Core
 */

defined( 'ABSPATH' ) || exit;

if ( ! function_exists( 'icopay_core_bootstrap' ) ) {
	/**
	 * Load core classes once.
	 */
	function icopay_core_bootstrap() {
		static $loaded = false;
		if ( $loaded ) {
			return;
		}
		require_once __DIR__ . '/includes/class-icopay-flow.php';
		require_once __DIR__ . '/includes/class-icopay-api-client.php';
		$loaded = true;
	}
}
