<?php
/**
 * ICOPAY WooCommerce API client — extends icopay-core.
 *
 * @package ICOPAY_WooCommerce
 */

defined( 'ABSPATH' ) || exit;

/**
 * WooCommerce wrapper around ICOPAY_Core_Api_Client.
 */
class ICOPAY_Api_Client extends ICOPAY_Core_Api_Client {

	const VENDOR_CHILLPAY = ICOPAY_Flow::VENDOR_CHILLPAY;
	const VENDOR_JPAY     = ICOPAY_Flow::VENDOR_JPAY;

	/**
	 * @param array $settings Gateway settings.
	 * @return self
	 */
	public static function from_settings( array $settings ) {
		return new self(
			$settings['api_base_url'] ?? '',
			$settings['comp_id'] ?? '',
			$settings['broker_secret'] ?? '',
			$settings['vendor'] ?? self::VENDOR_CHILLPAY
		);
	}
}
