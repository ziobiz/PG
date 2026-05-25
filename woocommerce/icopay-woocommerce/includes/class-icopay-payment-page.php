<?php
/**
 * ICOPAY 결제(iframe) 페이지 — wc-api=icopay_pay.
 *
 * @package ICOPAY_WooCommerce
 */

defined( 'ABSPATH' ) || exit;

/**
 * Payment page handler.
 */
class ICOPAY_Payment_Page {

	/**
	 * Init.
	 */
	public static function init() {
		add_action( 'woocommerce_api_icopay_pay', array( __CLASS__, 'render' ) );
		add_action( 'wp_ajax_icopay_confirm_status', array( __CLASS__, 'ajax_confirm_status' ) );
		add_action( 'wp_ajax_nopriv_icopay_confirm_status', array( __CLASS__, 'ajax_confirm_status' ) );
	}

	/**
	 * Payment page URL.
	 *
	 * @param WC_Order $order Order.
	 * @return string
	 */
	public static function get_pay_url( WC_Order $order ) {
		return add_query_arg(
			array(
				'wc-api'   => 'icopay_pay',
				'order_id' => $order->get_id(),
				'key'      => $order->get_order_key(),
			),
			home_url( '/' )
		);
	}

	/**
	 * Render payment page with embed iframe.
	 */
	public static function render() {
		$order_id = isset( $_GET['order_id'] ) ? absint( $_GET['order_id'] ) : 0;
		$key      = isset( $_GET['key'] ) ? wc_clean( wp_unslash( $_GET['key'] ) ) : '';

		$order = wc_get_order( $order_id );
		if ( ! $order || ! hash_equals( $order->get_order_key(), $key ) ) {
			wp_die( esc_html__( 'Invalid payment link.', 'icopay-woocommerce' ), 403 );
		}

		if ( $order->is_paid() ) {
			wp_safe_redirect( $order->get_checkout_order_received_url() );
			exit;
		}

		$gateways = WC()->payment_gateways()->payment_gateways();
		/** @var WC_Gateway_ICOPAY|null $gateway */
		$gateway = isset( $gateways['icopay'] ) ? $gateways['icopay'] : null;
		if ( ! $gateway || 'icopay' !== $order->get_payment_method() ) {
			wp_die( esc_html__( 'Payment method not available.', 'icopay-woocommerce' ), 400 );
		}

		$session = $order->get_meta( ICOPAY_Order_Helper::META_SESSION, true );
		if ( ! is_string( $session ) || '' === trim( $session ) ) {
			wp_die( esc_html__( 'Payment session expired. Please try checkout again.', 'icopay-woocommerce' ), 410 );
		}

		$api     = ICOPAY_Api_Client::from_settings( $gateway->get_icopay_settings() );
		$lang    = $gateway->get_option( 'lang', 'auto' );
		$embed   = $api->build_embed_html( $session, 'icopay-wc-checkout', $lang );
		$origin  = $api->get_allowed_origin();
		$return  = $order->get_checkout_order_received_url();
		$order_no = $order->get_meta( ICOPAY_Order_Helper::META_ORDER_NO, true );

		wp_enqueue_style(
			'icopay-wc-checkout',
			ICOPAY_WC_PLUGIN_URL . 'assets/css/checkout.css',
			array(),
			ICOPAY_WC_VERSION
		);
		wp_enqueue_script(
			'icopay-wc-checkout',
			ICOPAY_WC_PLUGIN_URL . 'assets/js/checkout.js',
			array( 'jquery' ),
			ICOPAY_WC_VERSION,
			true
		);
		wp_localize_script(
			'icopay-wc-checkout',
			'icopayWcCheckout',
			array(
				'allowedOrigin' => $origin,
				'returnUrl'     => $return,
				'ajaxUrl'       => admin_url( 'admin-ajax.php' ),
				'nonce'         => wp_create_nonce( 'icopay_confirm_' . $order->get_id() ),
				'orderId'       => $order->get_id(),
				'orderKey'      => $order->get_order_key(),
				'orderNo'       => $order_no,
				'messages'      => array(
					'processing' => __( 'Confirming payment…', 'icopay-woocommerce' ),
					'failed'     => __( 'Payment was not completed. Please try again or contact support.', 'icopay-woocommerce' ),
				),
			)
		);

		$title = $gateway->get_option( 'title', 'ICOPAY' );
		include ICOPAY_WC_PLUGIN_DIR . 'templates/payment-page.php';
		exit;
	}

	/**
	 * AJAX: poll ICOPAY status after postMessage finished.
	 */
	public static function ajax_confirm_status() {
		$order_id = isset( $_POST['order_id'] ) ? absint( $_POST['order_id'] ) : 0;
		$key      = isset( $_POST['order_key'] ) ? wc_clean( wp_unslash( $_POST['order_key'] ) ) : '';
		$nonce    = isset( $_POST['nonce'] ) ? sanitize_text_field( wp_unslash( $_POST['nonce'] ) ) : '';

		if ( ! wp_verify_nonce( $nonce, 'icopay_confirm_' . $order_id ) ) {
			wp_send_json_error( array( 'message' => 'Invalid nonce' ), 403 );
		}

		$order = wc_get_order( $order_id );
		if ( ! $order || ! hash_equals( $order->get_order_key(), $key ) ) {
			wp_send_json_error( array( 'message' => 'Invalid order' ), 403 );
		}

		if ( $order->is_paid() ) {
			wp_send_json_success(
				array(
					'paid'       => true,
					'redirect'   => $order->get_checkout_order_received_url(),
				)
			);
		}

		$gateways = WC()->payment_gateways()->payment_gateways();
		/** @var WC_Gateway_ICOPAY|null $gateway */
		$gateway = isset( $gateways['icopay'] ) ? $gateways['icopay'] : null;
		if ( ! $gateway ) {
			wp_send_json_error( array( 'message' => 'Gateway missing' ), 500 );
		}

		$order_no = $order->get_meta( ICOPAY_Order_Helper::META_ORDER_NO, true );
		$api      = ICOPAY_Api_Client::from_settings( $gateway->get_icopay_settings() );
		$res      = $api->get_payment_status( $order_no );

		$paid = false;
		if ( ! empty( $res['success'] ) && ! empty( $res['data']['paymentStatus'] ) ) {
			$ps = strtoupper( (string) $res['data']['paymentStatus'] );
			if ( 'PAID' === $ps ) {
				ICOPAY_Order_Helper::mark_paid_from_payload(
					$order,
					array(
						'trnId'   => $res['data']['transactionId'] ?? '',
						'orderNo' => $order_no,
					)
				);
				$paid = true;
			}
		}

		if ( $paid || $order->is_paid() ) {
			wp_send_json_success(
				array(
					'paid'     => true,
					'redirect' => $order->get_checkout_order_received_url(),
				)
			);
		}

		wp_send_json_success(
			array(
				'paid'           => false,
				'paymentStatus'  => $res['data']['paymentStatus'] ?? 'PENDING',
			)
		);
	}
}
