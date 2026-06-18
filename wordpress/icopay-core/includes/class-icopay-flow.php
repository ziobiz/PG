<?php
/**
 * ICOPAY checkout flow constants.
 *
 * @package ICOPAY_Core
 */

defined( 'ABSPATH' ) || exit;

/**
 * Checkout integration flow.
 */
final class ICOPAY_Flow {

	const INLINE   = 'inline';
	const REDIRECT = 'redirect';

	const VENDOR_JPAY     = 'jpay';
	const VENDOR_CHILLPAY = 'chillpay';
	const VENDOR_UNIFIED  = 'unified';

	/**
	 * @param string $raw Raw value.
	 * @return string inline|redirect
	 */
	public static function normalize( $raw ) {
		$v = strtolower( trim( (string) $raw ) );
		return ( self::REDIRECT === $v ) ? self::REDIRECT : self::INLINE;
	}
}
