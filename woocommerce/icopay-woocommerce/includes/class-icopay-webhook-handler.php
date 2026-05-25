<?php
/**
 * ICOPAY 결제 통보(웹훅) REST 엔드포인트.
 *
 * @package ICOPAY_WooCommerce
 */

defined( 'ABSPATH' ) || exit;

/**
 * Webhook handler.
 */
class ICOPAY_Webhook_Handler {

	/**
	 * Init hooks.
	 */
	public static function init() {
		add_action( 'rest_api_init', array( __CLASS__, 'register_routes' ) );
	}

	/**
	 * Register REST route.
	 */
	public static function register_routes() {
		register_rest_route(
			'icopay/v1',
			'/webhook',
			array(
				'methods'             => 'POST',
				'callback'            => array( __CLASS__, 'handle_webhook' ),
				'permission_callback' => '__return_true',
			)
		);
	}

	/**
	 * Public webhook URL for HQ merchantNotifyUrls registration.
	 *
	 * @return string
	 */
	public static function get_webhook_url() {
		return rest_url( 'icopay/v1/webhook' );
	}

	/**
	 * @param WP_REST_Request $request Request.
	 * @return WP_REST_Response
	 */
	public static function handle_webhook( WP_REST_Request $request ) {
		$raw  = $request->get_body();
		$data = json_decode( (string) $raw, true );
		if ( ! is_array( $data ) ) {
			return new WP_REST_Response( 'Invalid JSON', 400 );
		}

		$gateways = WC()->payment_gateways()->payment_gateways();
		/** @var WC_Gateway_ICOPAY|null $gateway */
		$gateway = isset( $gateways['icopay'] ) ? $gateways['icopay'] : null;
		if ( ! $gateway || 'yes' !== $gateway->enabled ) {
			return new WP_REST_Response( 'Gateway disabled', 503 );
		}

		if ( ! self::verify_signature( $gateway, $raw, $request ) ) {
			return new WP_REST_Response( 'Invalid signature', 401 );
		}

		$order_no = '';
		if ( ! empty( $data['orderNo'] ) ) {
			$order_no = sanitize_text_field( (string) $data['orderNo'] );
		}
		if ( '' === $order_no ) {
			return new WP_REST_Response( 'Missing orderNo', 422 );
		}

		$order = ICOPAY_Order_Helper::find_order_by_icopay_order_no( $order_no );
		if ( ! $order ) {
			return new WP_REST_Response( 'Order not found', 404 );
		}

		$comp_id = ! empty( $data['compId'] ) ? trim( (string) $data['compId'] ) : '';
		if ( '' !== $comp_id && strcasecmp( $comp_id, $gateway->get_option( 'comp_id' ) ) !== 0 ) {
			return new WP_REST_Response( 'compId mismatch', 403 );
		}

		$status = isset( $data['status'] ) ? (string) $data['status'] : '';
		$payment_status = isset( $data['paymentStatus'] ) ? (string) $data['paymentStatus'] : '';
		if ( '' === $payment_status && ! empty( $data['chillPaymentStatus'] ) ) {
			$payment_status = (string) $data['chillPaymentStatus'];
		}

		if ( ICOPAY_Order_Helper::is_paid_status( $status ) || 'PAID' === strtoupper( $payment_status ) ) {
			ICOPAY_Order_Helper::mark_paid_from_payload( $order, $data );
		} elseif ( in_array( $status, array( '02', '20', '30' ), true ) || 'FAILED' === strtoupper( $payment_status ) ) {
			if ( ! $order->is_paid() && 'failed' !== $order->get_status() ) {
				$order->update_status( 'failed', __( 'ICOPAY payment failed.', 'icopay-woocommerce' ) );
			}
		} elseif ( in_array( $status, array( '01' ), true ) || 'CANCELLED' === strtoupper( $payment_status ) ) {
			if ( ! $order->is_paid() && 'cancelled' !== $order->get_status() ) {
				$order->update_status( 'cancelled', __( 'ICOPAY payment cancelled.', 'icopay-woocommerce' ) );
			}
		}

		return new WP_REST_Response( 'OK', 200 );
	}

	/**
	 * @param WC_Gateway_ICOPAY $gateway Gateway.
	 * @param string            $raw     Raw body.
	 * @param WP_REST_Request   $request Request.
	 * @return bool
	 */
	private static function verify_signature( WC_Gateway_ICOPAY $gateway, $raw, WP_REST_Request $request ) {
		$secret = trim( (string) $gateway->get_option( 'webhook_secret' ) );
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
