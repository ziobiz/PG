<?php
/**
 * ICOPAY 가맹점 인라인 결제 API 클라이언트.
 *
 * @package ICOPAY_WooCommerce
 */

defined( 'ABSPATH' ) || exit;

/**
 * ICOPAY API client (inline-checkout prepare / status).
 */
class ICOPAY_Api_Client {

	const HEADER_BROKER_SECRET = 'X-Icopay-Merchant-Broker-Secret';
	const VENDOR_CHILLPAY      = 'chillpay';
	const VENDOR_JPAY          = 'jpay';

	/** @var string */
	private $api_base;

	/** @var string */
	private $comp_id;

	/** @var string */
	private $broker_secret;

	/** @var string */
	private $vendor;

	/**
	 * @param string $api_base       API base URL.
	 * @param string $comp_id        compId.
	 * @param string $broker_secret  Broker secret.
	 * @param string $vendor         chillpay|jpay.
	 */
	public function __construct( $api_base, $comp_id, $broker_secret, $vendor = self::VENDOR_CHILLPAY ) {
		$this->api_base      = untrailingslashit( trim( (string) $api_base ) );
		$this->comp_id       = trim( (string) $comp_id );
		$this->broker_secret = trim( (string) $broker_secret );
		$this->vendor        = self::normalize_vendor( $vendor );
	}

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

	/**
	 * @param string $vendor Vendor key.
	 * @return string
	 */
	public static function normalize_vendor( $vendor ) {
		$v = strtolower( trim( (string) $vendor ) );
		return ( self::VENDOR_JPAY === $v ) ? self::VENDOR_JPAY : self::VENDOR_CHILLPAY;
	}

	/**
	 * @param string $order_no     Order number (≤20).
	 * @param string $amount       Amount plain string.
	 * @param string $currency     Currency code.
	 * @param string $product_name Product label.
	 * @param string $lang         KOR|ENG|JPN|CHN|THA or empty.
	 * @return array{success:bool,data?:array,message?:string,errorCode?:string}
	 */
	public function prepare_inline_checkout( $order_no, $amount, $currency, $product_name, $lang = '' ) {
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
		$lang_norm = self::normalize_lang( $lang );
		if ( '' !== $lang_norm ) {
			$body['lang'] = $lang_norm;
		}
		return $this->post_json( $this->prepare_path(), $body );
	}

	/**
	 * @param string $order_no ICOPAY orderNo.
	 * @return array{success:bool,data?:array,message?:string,errorCode?:string}
	 */
	public function get_payment_status( $order_no ) {
		$path = $this->status_path();
		$url  = add_query_arg(
			array(
				'compId'  => $this->comp_id,
				'orderNo' => $order_no,
			),
			$this->api_base . $path
		);
		return $this->get_json( $url );
	}

	/**
	 * @param string $session_token Session token from prepare.
	 * @param string $target_id     DOM target id.
	 * @param string $lang          Optional lang code.
	 * @return string HTML embed snippet.
	 */
	public function build_embed_html( $session_token, $target_id = '', $lang = '' ) {
		$is_jpay   = ( self::VENDOR_JPAY === $this->vendor );
		$embed     = $is_jpay ? '/v1/embed-jpay-pay/' : '/v1/embed-pay/';
		$default   = $is_jpay ? 'icopay-jpay-checkout' : 'icopay-pay-checkout';
		$target    = '' !== $target_id ? $target_id : $default;
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
	 * @return string Allowed postMessage origin (API host).
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
			'KO'      => 'KOR',
			'KR'      => 'KOR',
			'KOREAN'  => 'KOR',
			'EN'      => 'ENG',
			'ENGLISH' => 'ENG',
			'JA'      => 'JPN',
			'JP'      => 'JPN',
			'JPY'     => 'JPN',
			'JAPANESE'=> 'JPN',
			'ZH'      => 'CHN',
			'CN'      => 'CHN',
			'CH'      => 'CHN',
			'CHINESE' => 'CHN',
			'TH'      => 'THA',
			'THAI'    => 'THA',
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
		$locale = determine_locale();
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
	 * @param string $path  API path.
	 * @param array  $body  JSON body.
	 * @return array
	 */
	private function post_json( $path, array $body ) {
		$url = $this->api_base . $path;
		$json = wp_json_encode( $body );
		if ( false === $json ) {
			return array(
				'success'   => false,
				'message'   => 'JSON encode failed',
				'errorCode' => 'LOCAL_ERROR',
			);
		}

		$response = wp_remote_post(
			$url,
			array(
				'timeout' => 45,
				'headers' => array(
					'Content-Type'               => 'application/json',
					'Accept'                     => 'application/json',
					self::HEADER_BROKER_SECRET     => $this->broker_secret,
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
					'Accept'                 => 'application/json',
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
		$raw  = wp_remote_retrieve_body( $response );
		$data = json_decode( (string) $raw, true );
		if ( ! is_array( $data ) ) {
			return array(
				'success'   => false,
				'message'   => 'Invalid JSON (HTTP ' . $code . ')',
				'errorCode' => 'PARSE_ERROR',
			);
		}
		return $data;
	}

	/**
	 * @return string
	 */
	private function prepare_path() {
		if ( self::VENDOR_JPAY === $this->vendor ) {
			return '/api/middleware/v1/merchant/jpay/inline-checkout/prepare';
		}
		return '/api/middleware/v1/merchant/chillpay/inline-checkout/prepare';
	}

	/**
	 * @return string
	 */
	private function status_path() {
		if ( self::VENDOR_JPAY === $this->vendor ) {
			return '/api/middleware/v1/merchant/jpay/inline-checkout/status';
		}
		return '/api/middleware/v1/merchant/chillpay/inline-checkout/status';
	}
}
