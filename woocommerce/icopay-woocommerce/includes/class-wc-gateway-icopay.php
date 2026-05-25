<?php
/**
 * WooCommerce ICOPAY payment gateway.
 *
 * @package ICOPAY_WooCommerce
 */

defined( 'ABSPATH' ) || exit;

/**
 * WC_Gateway_ICOPAY
 */
class WC_Gateway_ICOPAY extends WC_Payment_Gateway {

	/**
	 * Constructor.
	 */
	public function __construct() {
		$this->id                 = 'icopay';
		$this->icon               = '';
		$this->has_fields         = false;
		$this->method_title       = __( 'ICOPAY (URL Inline)', 'icopay-woocommerce' );
		$this->method_description = __( 'ICOPAY URL 인라인 결제(ChillPay/JPAY iframe). 본사에서 발급한 compId·브로커 시크릿을 입력하세요.', 'icopay-woocommerce' );
		$this->supports           = array( 'products' );

		$this->init_form_fields();
		$this->init_settings();

		$this->title       = $this->get_option( 'title' );
		$this->description = $this->get_option( 'description' );
		$this->enabled     = $this->get_option( 'enabled' );

		add_action( 'woocommerce_update_options_payment_gateways_' . $this->id, array( $this, 'process_admin_options' ) );
		add_action( 'woocommerce_thankyou_' . $this->id, array( $this, 'thankyou_page' ) );
	}

	/**
	 * Admin settings fields.
	 */
	public function init_form_fields() {
		$webhook_url = ICOPAY_Webhook_Handler::get_webhook_url();

		$this->form_fields = array(
			'enabled'        => array(
				'title'   => __( 'Enable/Disable', 'icopay-woocommerce' ),
				'type'    => 'checkbox',
				'label'   => __( 'Enable ICOPAY payment', 'icopay-woocommerce' ),
				'default' => 'no',
			),
			'title'          => array(
				'title'       => __( 'Title', 'icopay-woocommerce' ),
				'type'        => 'text',
				'description' => __( 'Checkout payment method title.', 'icopay-woocommerce' ),
				'default'     => __( 'Credit card (ICOPAY)', 'icopay-woocommerce' ),
				'desc_tip'    => true,
			),
			'description'    => array(
				'title'       => __( 'Description', 'icopay-woocommerce' ),
				'type'        => 'textarea',
				'description' => __( 'Shown on checkout.', 'icopay-woocommerce' ),
				'default'     => __( 'Pay securely via ICOPAY inline checkout.', 'icopay-woocommerce' ),
				'desc_tip'    => true,
			),
			'vendor'         => array(
				'title'       => __( 'PG vendor', 'icopay-woocommerce' ),
				'type'        => 'select',
				'description' => __( 'ChillPay (default) or JPAY inline checkout.', 'icopay-woocommerce' ),
				'default'     => 'chillpay',
				'options'     => array(
					'chillpay' => 'ChillPay',
					'jpay'     => 'JPAY',
				),
				'desc_tip'    => true,
			),
			'comp_id'        => array(
				'title'       => __( 'compId (업체코드)', 'icopay-woocommerce' ),
				'type'        => 'text',
				'description' => __( 'ICOPAY merchant company code from HQ.', 'icopay-woocommerce' ),
				'default'     => '',
				'desc_tip'    => true,
			),
			'api_base_url'   => array(
				'title'       => __( 'API Base URL', 'icopay-woocommerce' ),
				'type'        => 'text',
				'description' => __( 'Example: https://api.icopay.co.kr', 'icopay-woocommerce' ),
				'default'     => 'https://api.icopay.co.kr',
				'desc_tip'    => true,
			),
			'broker_secret'  => array(
				'title'       => __( 'Broker secret', 'icopay-woocommerce' ),
				'type'        => 'password',
				'description' => __( 'X-Icopay-Merchant-Broker-Secret from 「가맹점 API 생성」. Server only — never expose in browser.', 'icopay-woocommerce' ),
				'default'     => '',
			),
			'webhook_secret' => array(
				'title'       => __( 'Webhook sign secret (optional)', 'icopay-woocommerce' ),
				'type'        => 'password',
				'description' => __( 'If HQ configured HMAC on merchantNotifyUrls, enter the same secret for X-Icopay-Signature verification.', 'icopay-woocommerce' ),
				'default'     => '',
			),
			'lang'           => array(
				'title'       => __( 'Checkout language', 'icopay-woocommerce' ),
				'type'        => 'select',
				'description' => __( 'Payment UI language sent to prepare API.', 'icopay-woocommerce' ),
				'default'     => 'auto',
				'options'     => array(
					'auto' => __( 'Auto (site locale)', 'icopay-woocommerce' ),
					'KOR'  => 'KOR',
					'ENG'  => 'ENG',
					'JPN'  => 'JPN',
					'CHN'  => 'CHN',
					'THA'  => 'THA',
				),
				'desc_tip'    => true,
			),
			'webhook_info'   => array(
				'title'       => __( 'Webhook URL (register at HQ)', 'icopay-woocommerce' ),
				'type'        => 'title',
				'description' => sprintf(
					/* translators: %s: webhook REST URL */
					__( 'Register this URL in ICOPAY HQ as merchant notify URL (MIDDLEWARE or BACKGROUND/RESULT): %s', 'icopay-woocommerce' ),
					'<code style="word-break:break-all;">' . esc_html( $webhook_url ) . '</code>'
				),
			),
		);
	}

	/**
	 * Settings for API client.
	 *
	 * @return array
	 */
	public function get_icopay_settings() {
		return array(
			'comp_id'        => $this->get_option( 'comp_id' ),
			'api_base_url'   => $this->get_option( 'api_base_url' ),
			'broker_secret'  => $this->get_option( 'broker_secret' ),
			'vendor'         => $this->get_option( 'vendor', 'chillpay' ),
		);
	}

	/**
	 * Validate required credentials before save.
	 */
	public function process_admin_options() {
		$ok = parent::process_admin_options();
		if ( 'yes' === $this->get_option( 'enabled' ) ) {
			if ( '' === trim( (string) $this->get_option( 'comp_id' ) ) ) {
				WC_Admin_Settings::add_error( __( 'compId is required when ICOPAY is enabled.', 'icopay-woocommerce' ) );
			}
			if ( '' === trim( (string) $this->get_option( 'broker_secret' ) ) ) {
				WC_Admin_Settings::add_error( __( 'Broker secret is required when ICOPAY is enabled.', 'icopay-woocommerce' ) );
			}
		}
		return $ok;
	}

	/**
	 * @param int $order_id Order ID.
	 * @return array
	 */
	public function process_payment( $order_id ) {
		$order = wc_get_order( $order_id );
		if ( ! $order ) {
			wc_add_notice( __( 'Order not found.', 'icopay-woocommerce' ), 'error' );
			return array( 'result' => 'fail' );
		}

		$comp_id = trim( (string) $this->get_option( 'comp_id' ) );
		$secret  = trim( (string) $this->get_option( 'broker_secret' ) );
		if ( '' === $comp_id || '' === $secret ) {
			wc_add_notice( __( 'ICOPAY is not configured. Contact the store administrator.', 'icopay-woocommerce' ), 'error' );
			return array( 'result' => 'fail' );
		}

		$order_no = ICOPAY_Order_Helper::get_or_create_order_no( $order );
		$amount   = ICOPAY_Order_Helper::amount_plain( $order );
		$currency = $order->get_currency();
		$product  = ICOPAY_Order_Helper::product_name_from_order( $order );
		$lang     = $this->get_option( 'lang', 'auto' );

		$api  = ICOPAY_Api_Client::from_settings( $this->get_icopay_settings() );
		$prep = $api->prepare_inline_checkout( $order_no, $amount, $currency, $product, $lang );

		if ( empty( $prep['success'] ) || empty( $prep['data']['sessionToken'] ) ) {
			$msg = ! empty( $prep['message'] ) ? (string) $prep['message'] : __( 'Unable to start ICOPAY payment session.', 'icopay-woocommerce' );
			wc_add_notice( $msg, 'error' );
			return array( 'result' => 'fail' );
		}

		$order->update_meta_data( ICOPAY_Order_Helper::META_SESSION, sanitize_text_field( (string) $prep['data']['sessionToken'] ) );
		$order->update_status( 'pending', __( 'Awaiting ICOPAY payment.', 'icopay-woocommerce' ) );
		$order->save();

		WC()->cart->empty_cart();

		return array(
			'result'   => 'success',
			'redirect' => ICOPAY_Payment_Page::get_pay_url( $order ),
		);
	}

	/**
	 * Thank you page note when webhook is delayed.
	 *
	 * @param int $order_id Order ID.
	 */
	public function thankyou_page( $order_id ) {
		$order = wc_get_order( $order_id );
		if ( ! $order || ! $order->is_paid() ) {
			echo '<p class="icopay-wc-pending">' . esc_html__( 'Your payment is being confirmed. You will receive an email when the order is complete.', 'icopay-woocommerce' ) . '</p>';
		}
	}
}
