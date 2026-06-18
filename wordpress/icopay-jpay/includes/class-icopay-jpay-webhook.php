<?php
/**
 * Webhook REST endpoint.
 *
 * @package ICOPAY_JPAY
 */

defined( 'ABSPATH' ) || exit;

/**
 * Webhook handler.
 */
class ICOPAY_JPAY_Webhook {

	/**
	 * Init.
	 */
	public static function init() {
		add_action( 'rest_api_init', array( __CLASS__, 'register_routes' ) );
	}

	/**
	 * Register route.
	 */
	public static function register_routes() {
		register_rest_route(
			'icopay-jpay/v1',
			'/webhook',
			array(
				'methods'             => 'POST',
				'callback'            => array( __CLASS__, 'handle' ),
				'permission_callback' => '__return_true',
			)
		);
	}

	/**
	 * @return string
	 */
	public static function get_webhook_url() {
		return rest_url( 'icopay-jpay/v1/webhook' );
	}

	/**
	 * @param WP_REST_Request $request Request.
	 * @return WP_REST_Response
	 */
	public static function handle( WP_REST_Request $request ) {
		$raw  = $request->get_body();
		$data = json_decode( (string) $raw, true );
		if ( ! is_array( $data ) ) {
			return new WP_REST_Response( 'Invalid JSON', 400 );
		}

		$settings = ICOPAY_JPAY_Settings::get_settings();
		if ( ! self::verify_signature( $settings, $raw, $request ) ) {
			return new WP_REST_Response( 'Invalid signature', 401 );
		}

		$order_no = ! empty( $data['orderNo'] ) ? sanitize_text_field( (string) $data['orderNo'] ) : '';
		if ( '' === $order_no ) {
			return new WP_REST_Response( 'Missing orderNo', 422 );
		}

		$comp_id = ! empty( $data['compId'] ) ? trim( (string) $data['compId'] ) : '';
		if ( '' !== $comp_id && strcasecmp( $comp_id, $settings['comp_id'] ) !== 0 ) {
			return new WP_REST_Response( 'compId mismatch', 403 );
		}

		$session = ICOPAY_JPAY_Payment::get_session( $order_no );
		if ( ! $session ) {
			$session = array( 'orderNo' => $order_no );
		}

		$payment_status = isset( $data['paymentStatus'] ) ? (string) $data['paymentStatus'] : '';
		$status         = isset( $data['status'] ) ? (string) $data['status'] : '';
		if ( in_array( $status, array( '00', '10', 'PAID' ), true ) || 'PAID' === strtoupper( $payment_status ) ) {
			$session['paid']    = true;
			$session['webhook'] = $data;
			$session['orderNo'] = $order_no;
			set_transient( ICOPAY_JPAY_Payment::TRANSIENT_PREFIX . $order_no, $session, 6 * HOUR_IN_SECONDS );
		}

		return new WP_REST_Response( 'OK', 200 );
	}

	/**
	 * @param array           $settings Settings.
	 * @param string          $raw      Body.
	 * @param WP_REST_Request $request  Request.
	 * @return bool
	 */
	private static function verify_signature( array $settings, $raw, WP_REST_Request $request ) {
		$secret = trim( (string) ( $settings['webhook_secret'] ?? '' ) );
		if ( '' === $secret ) {
			return true;
		}
		$header = $request->get_header( 'x-icopay-signature' );
		if ( ! $header ) {
			return false;
		}
		$expected = 'v1=' . hash_hmac( 'sha256', (string) $raw, $secret );
		return hash_equals( $expected, trim( (string) $header ) );
	}
}
