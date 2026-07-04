<?php
/**
 * ICOPAY merchant API client — inline + redirect (JPAY / ChillPay / unified).
 *
 * @package ICOPAY_Core
 */

defined( 'ABSPATH' ) || exit;

require_once __DIR__ . '/class-icopay-flow.php';

/**
 * Server-side ICOPAY checkout API.
 */
class ICOPAY_Core_Api_Client {

	const HEADER_BROKER_SECRET = 'X-Icopay-Merchant-Broker-Secret';

	/** @var string */
	private $api_base;

	/** @var string */
	private $comp_id;

	/** @var string */
	private $broker_secret;

	/** @var string jpay|chillpay|unified */
	private $vendor;

	/**
	 * @param string $api_base      API base URL.
	 * @param string $comp_id       compId.
	 * @param string $broker_secret Broker secret.
	 * @param string $vendor        jpay|chillpay|unified.
	 */
	public function __construct( $api_base, $comp_id, $broker_secret, $vendor = ICOPAY_Flow::VENDOR_JPAY ) {
		$this->api_base      = untrailingslashit( trim( (string) $api_base ) );
		$this->comp_id       = trim( (string) $comp_id );
		$this->broker_secret = trim( (string) $broker_secret );
		$this->vendor        = self::normalize_vendor( $vendor );
	}

	/**
	 * @param string $vendor Vendor key.
	 * @return string
	 */
	public static function normalize_vendor( $vendor ) {
		$v = strtolower( trim( (string) $vendor ) );
		if ( ICOPAY_Flow::VENDOR_UNIFIED === $v ) {
			return ICOPAY_Flow::VENDOR_UNIFIED;
		}
		if ( ICOPAY_Flow::VENDOR_JPAY === $v ) {
			return ICOPAY_Flow::VENDOR_JPAY;
		}
		return ICOPAY_Flow::VENDOR_CHILLPAY;
	}

	/**
	 * Prepare checkout session.
	 *
	 * @param string $flow         inline|redirect.
	 * @param string $order_no     Order number.
	 * @param string $amount       Amount.
	 * @param string $currency     Currency.
	 * @param string $product_name Product label.
	 * @param array  $opts         lang, buyerPrefill (redirect: returnUrl/cancelUrl 미사용 — NOTI Result 경유).
	 * @return array
	 */
	public function prepare_checkout( $flow, $order_no, $amount, $currency, $product_name, array $opts = array() ) {
		$body = array(
			'compId'  => $this->comp_id,
			'orderNo' => $order_no,
			'amount'  => (string) $amount,
		);
		if ( '' !== trim( (string) $currency ) ) {
			$body['currency'] = strtoupper( trim( (string) $currency ) );
		}
		if ( '' !== trim( (string) $product_name ) ) {
			$body['productName'] = mb_substr( (string) $product_name, 0, 500 );
		}
		$lang = isset( $opts['lang'] ) ? self::normalize_lang( $opts['lang'] ) : '';
		if ( '' !== $lang ) {
			$body['lang'] = $lang;
		}
		if ( ! empty( $opts['buyerPrefill'] ) && is_array( $opts['buyerPrefill'] ) ) {
			$body['buyerPrefill'] = $opts['buyerPrefill'];
		}
		return $this->post_json( $this->prepare_path( $flow ), $body );
	}

	/** Backward-compatible inline prepare. */
	public function prepare_inline_checkout( $order_no, $amount, $currency, $product_name, $lang = '' ) {
		return $this->prepare_checkout( ICOPAY_Flow::INLINE, $order_no, $amount, $currency, $product_name, array( 'lang' => $lang ) );
	}

	/** Redirect prepare (browser return via NOTI Result — do not send merchant returnUrl). */
	public function prepare_redirect_checkout( $order_no, $amount, $currency, $product_name, $return_url = '', $cancel_url = '', $lang = '' ) {
		return $this->prepare_checkout(
			ICOPAY_Flow::REDIRECT,
			$order_no,
			$amount,
			$currency,
			$product_name,
			array(
				'lang' => $lang,
			)
		);
	}

	/**
	 * @param string $order_no Order number.
	 * @param string $flow     inline|redirect (default inline).
	 * @return array
	 */
	public function get_payment_status( $order_no, $flow = ICOPAY_Flow::INLINE ) {
		$url = add_query_arg(
			array(
				'compId'  => $this->comp_id,
				'orderNo' => $order_no,
			),
			$this->api_base . $this->status_path( $flow )
		);
		return $this->get_json( $url );
	}

	/**
	 * @param string $session_token Session token.
	 * @param string $target_id     DOM target.
	 * @param string $lang          Lang code.
	 * @return string HTML embed snippet (inline only).
	 */
	public function build_embed_html( $session_token, $target_id = '', $lang = '' ) {
		$embed   = '/v1/embed-checkout/';
		$default = 'icopay-checkout';
		$target  = '' !== $target_id ? $target_id : $default;
		$src       = esc_url( $this->api_base . $embed . rawurlencode( $this->comp_id ) );
		$tok       = esc_attr( $session_token );
		$target_e  = esc_attr( $target );
		$lang_norm = self::normalize_lang( $lang );
		$lang_attr = '' !== $lang_norm ? ' data-lang="' . esc_attr( $lang_norm ) . '"' : '';

		return '<div id="' . $target_e . '" class="icopay-embed-target"></div>' . "\n"
			. '<script src="' . $src . '"'
			. ' data-session-token="' . $tok . '"'
			. ' data-target="' . $target_e . '"'
			. $lang_attr
			. ' async defer charset="utf-8"></script>';
	}

	/**
	 * @return string postMessage origin.
	 */
	public function get_allowed_origin() {
		$parts = wp_parse_url( $this->api_base );
		if ( empty( $parts['scheme'] ) || empty( $parts['host'] ) ) {
			return '';
		}
		$port = isset( $parts['port'] ) ? ':' . $parts['port'] : '';
		return $parts['scheme'] . '://' . $parts['host'] . $port;
	}

	/**
	 * @param string|null $raw Raw lang.
	 * @return string
	 */
	public static function normalize_lang( $raw ) {
		if ( null === $raw || '' === trim( (string) $raw ) || 'auto' === strtolower( trim( (string) $raw ) ) ) {
			return self::detect_page_lang();
		}
		$u = strtoupper( trim( (string) $raw ) );
		$map = array(
			'KO'       => 'KOR',
			'KR'       => 'KOR',
			'KOREAN'   => 'KOR',
			'EN'       => 'ENG',
			'ENGLISH'  => 'ENG',
			'JA'       => 'JPN',
			'JP'       => 'JPN',
			'JPY'      => 'JPN',
			'JAPANESE' => 'JPN',
			'ZH'       => 'CHN',
			'CN'       => 'CHN',
			'CH'       => 'CHN',
			'CHINESE'  => 'CHN',
			'TH'       => 'THA',
			'THAI'     => 'THA',
		);
		if ( isset( $map[ $u ] ) ) {
			return $map[ $u ];
		}
		if ( in_array( $u, array( 'KOR', 'ENG', 'JPN', 'CHN', 'THA' ), true ) ) {
			return $u;
		}
		return '';
	}

	/**
	 * @return string
	 */
	public static function detect_page_lang() {
		$locale = function_exists( 'determine_locale' ) ? determine_locale() : get_locale();
		if ( 0 === strpos( $locale, 'ko' ) ) {
			return 'KOR';
		}
		if ( 0 === strpos( $locale, 'ja' ) ) {
			return 'JPN';
		}
		if ( 0 === strpos( $locale, 'zh' ) ) {
			return 'CHN';
		}
		if ( 0 === strpos( $locale, 'th' ) ) {
			return 'THA';
		}
		return 'ENG';
	}

	/**
	 * @param string $flow Flow mode.
	 * @return string
	 */
	private function prepare_path( $flow ) {
		$redirect = ( ICOPAY_Flow::REDIRECT === ICOPAY_Flow::normalize( $flow ) );
		return $redirect
			? '/api/middleware/v1/merchant/checkout/redirect/prepare'
			: '/api/middleware/v1/merchant/checkout/prepare';
	}

	/**
	 * @param string $flow Flow mode.
	 * @return string
	 */
	private function status_path( $flow ) {
		$redirect = ( ICOPAY_Flow::REDIRECT === ICOPAY_Flow::normalize( $flow ) );
		return $redirect
			? '/api/middleware/v1/merchant/checkout/redirect/status'
			: '/api/middleware/v1/merchant/checkout/status';
	}

	/**
	 * @param string $path API path.
	 * @param array  $body JSON body.
	 * @return array
	 */
	private function post_json( $path, array $body ) {
		$json = wp_json_encode( $body );
		if ( false === $json ) {
			return array(
				'success'   => false,
				'message'   => 'JSON encode failed',
				'errorCode' => 'LOCAL_ERROR',
			);
		}

		$response = wp_remote_post(
			$this->api_base . $path,
			array(
				'timeout' => 45,
				'headers' => array(
					'Content-Type'             => 'application/json',
					'Accept'                   => 'application/json',
					self::HEADER_BROKER_SECRET => $this->broker_secret,
				),
				'body'    => $json,
			)
		);
		return $this->parse_response( $response );
	}

	/**
	 * @param string $url Full URL.
	 * @return array
	 */
	private function get_json( $url ) {
		$response = wp_remote_get(
			$url,
			array(
				'timeout' => 30,
				'headers' => array(
					'Accept'                   => 'application/json',
					self::HEADER_BROKER_SECRET => $this->broker_secret,
				),
			)
		);
		return $this->parse_response( $response );
	}

	/**
	 * @param array|\WP_Error $response HTTP response.
	 * @return array
	 */
	private function parse_response( $response ) {
		if ( is_wp_error( $response ) ) {
			return array(
				'success'   => false,
				'message'   => $response->get_error_message(),
				'errorCode' => 'NETWORK_ERROR',
			);
		}
		$code = wp_remote_retrieve_response_code( $response );
		$data = json_decode( (string) wp_remote_retrieve_body( $response ), true );
		if ( ! is_array( $data ) ) {
			return array(
				'success'   => false,
				'message'   => 'Invalid JSON (HTTP ' . $code . ')',
				'errorCode' => 'PARSE_ERROR',
			);
		}
		return $data;
	}
}
