<?php
/**
 * Payment session + return handling.
 *
 * @package ICOPAY_JPAY
 */

defined( 'ABSPATH' ) || exit;

/**
 * Payment flow handler.
 */
class ICOPAY_JPAY_Payment {

	const TRANSIENT_PREFIX = 'icopay_jpay_';
	const QUERY_ACTION     = 'icopay_jpay';

	/**
	 * Init hooks.
	 */
	public static function init() {
		add_action( 'init', array( __CLASS__, 'register_query_var' ) );
		add_action( 'template_redirect', array( __CLASS__, 'route' ) );
	}

	/**
	 * Register query var.
	 */
	public static function register_query_var() {
		add_rewrite_tag( '%icopay_jpay%', '([^&]+)' );
		add_rewrite_rule( '^icopay-jpay/([^/]+)/?', 'index.php?icopay_jpay=$matches[1]', 'top' );
	}

	/**
	 * Route pay / return / inline pages.
	 */
	public static function route() {
		$action = get_query_var( 'icopay_jpay' );
		if ( ! $action ) {
			return;
		}
		if ( 'pay' === $action ) {
			self::render_inline_pay();
		} elseif ( 'return' === $action ) {
			self::handle_return();
		} elseif ( 'start' === $action && isset( $_SERVER['REQUEST_METHOD'] ) && 'POST' === $_SERVER['REQUEST_METHOD'] ) {
			self::start_checkout();
		}
	}

	/**
	 * Start checkout from form POST.
	 */
	public static function start_checkout() {
		if ( ! isset( $_POST['icopay_jpay_nonce'] ) || ! wp_verify_nonce( sanitize_text_field( wp_unslash( $_POST['icopay_jpay_nonce'] ) ), 'icopay_jpay_start' ) ) {
			wp_die( esc_html__( 'Invalid request.', 'icopay-jpay' ), 403 );
		}

		$amount   = isset( $_POST['amount'] ) ? sanitize_text_field( wp_unslash( $_POST['amount'] ) ) : '';
		$currency = isset( $_POST['currency'] ) ? sanitize_text_field( wp_unslash( $_POST['currency'] ) ) : 'USD';
		$product  = isset( $_POST['product'] ) ? sanitize_text_field( wp_unslash( $_POST['product'] ) ) : '';
		$order_no = isset( $_POST['order_no'] ) ? sanitize_text_field( wp_unslash( $_POST['order_no'] ) ) : '';

		if ( '' === $amount || ! is_numeric( $amount ) ) {
			wp_die( esc_html__( 'Invalid amount.', 'icopay-jpay' ), 400 );
		}
		if ( '' === $order_no ) {
			$order_no = self::generate_order_no();
		}
		$order_no = substr( preg_replace( '/[^A-Za-z0-9_-]/', '', $order_no ), 0, 20 );
		if ( '' === $order_no ) {
			$order_no = self::generate_order_no();
		}

		$settings = ICOPAY_JPAY_Settings::get_settings();
		$flow     = ICOPAY_Flow::normalize( $settings['flow_mode'] );
		$client   = ICOPAY_JPAY_Settings::api_client();
		if ( ! $client ) {
			wp_die( esc_html__( 'ICOPAY core not loaded.', 'icopay-jpay' ), 500 );
		}

		$lang = $settings['lang'];
		if ( ICOPAY_Flow::REDIRECT === $flow ) {
			$return = self::return_url( $order_no );
			$cancel = self::cancel_url();
			$prep   = $client->prepare_redirect_checkout( $order_no, $amount, $currency, $product, $return, $cancel, $lang );
			if ( empty( $prep['success'] ) || empty( $prep['data']['payUrl'] ) ) {
				$msg = ! empty( $prep['message'] ) ? (string) $prep['message'] : __( 'Prepare failed.', 'icopay-jpay' );
				wp_die( esc_html( $msg ), 502 );
			}
			self::store_session( $order_no, $prep['data'], $flow, $amount, $currency, $product );
			wp_safe_redirect( esc_url_raw( (string) $prep['data']['payUrl'] ) );
			exit;
		}

		$prep = $client->prepare_inline_checkout( $order_no, $amount, $currency, $product, $lang );
		if ( empty( $prep['success'] ) || empty( $prep['data']['sessionToken'] ) ) {
			$msg = ! empty( $prep['message'] ) ? (string) $prep['message'] : __( 'Prepare failed.', 'icopay-jpay' );
			wp_die( esc_html( $msg ), 502 );
		}
		self::store_session( $order_no, $prep['data'], $flow, $amount, $currency, $product );
		wp_safe_redirect( self::inline_pay_url( $order_no ) );
		exit;
	}

	/**
	 * Inline payment page with embed.
	 */
	public static function render_inline_pay() {
		$order_no = isset( $_GET['order_no'] ) ? sanitize_text_field( wp_unslash( $_GET['order_no'] ) ) : '';
		$session  = self::get_session( $order_no );
		if ( ! $session || empty( $session['sessionToken'] ) ) {
			wp_die( esc_html__( 'Session expired.', 'icopay-jpay' ), 410 );
		}

		$client = ICOPAY_JPAY_Settings::api_client();
		$lang   = ICOPAY_JPAY_Settings::get_settings()['lang'];
		$embed  = $client->build_embed_html( $session['sessionToken'], 'icopay-jpay-checkout', $lang );
		$origin = $client->get_allowed_origin();

		status_header( 200 );
		nocache_headers();
		?><!DOCTYPE html>
		<html <?php language_attributes(); ?>>
		<head>
			<meta charset="<?php bloginfo( 'charset' ); ?>">
			<meta name="viewport" content="width=device-width, initial-scale=1">
			<title><?php esc_html_e( 'ICOPAY Payment', 'icopay-jpay' ); ?></title>
			<style>.icopay-jpay-wrap{max-width:480px;margin:2rem auto;padding:1rem;}</style>
		</head>
		<body>
		<div class="icopay-jpay-wrap">
			<h1><?php echo esc_html( $session['product'] ?? __( 'Payment', 'icopay-jpay' ) ); ?></h1>
			<?php echo $embed; // phpcs:ignore WordPress.Security.EscapeOutput.OutputNotEscaped -- trusted API HTML ?>
		</div>
		<script>
		window.addEventListener('message', function (ev) {
			if (!ev.data || ev.data.type !== 'ICOPAY_INLINE_CHECKOUT') return;
			if (ev.data.phase !== 'finished') return;
			var allowed = <?php echo wp_json_encode( $origin ); ?>;
			if (allowed && ev.origin !== allowed) return;
			if (ev.data.success) {
				window.location.href = <?php echo wp_json_encode( self::return_url( $order_no ) . '&icopay_status=paid' ); ?>;
			}
		});
		</script>
		</body>
		</html>
		<?php
		exit;
	}

	/**
	 * Redirect return handler.
	 */
	public static function handle_return() {
		$order_no = isset( $_GET['order_no'] ) ? sanitize_text_field( wp_unslash( $_GET['order_no'] ) ) : '';
		$status   = isset( $_GET['icopay_status'] ) ? sanitize_text_field( wp_unslash( $_GET['icopay_status'] ) ) : '';
		$session  = self::get_session( $order_no );

		$settings = ICOPAY_JPAY_Settings::get_settings();
		$page_id  = (int) $settings['return_page_id'];
		$dest     = $page_id > 0 ? get_permalink( $page_id ) : home_url( '/' );

		if ( ( 'paid' === strtolower( $status ) || '' === $status ) && $session ) {
			$client = ICOPAY_JPAY_Settings::api_client();
			$flow   = isset( $session['flow'] ) ? ICOPAY_Flow::normalize( $session['flow'] ) : ICOPAY_Flow::INLINE;
			$res    = $client->get_payment_status( $order_no, $flow );
			if ( ! empty( $res['success'] ) && ! empty( $res['data']['paymentStatus'] ) && 'PAID' === strtoupper( (string) $res['data']['paymentStatus'] ) ) {
				$session['paid'] = true;
				self::store_session( $order_no, $session, $flow, $session['amount'] ?? '', $session['currency'] ?? '', $session['product'] ?? '' );
				$status = 'paid';
			}
		}

		$dest = add_query_arg(
			array(
				'icopay_order_no' => $order_no,
				'icopay_status'   => $status,
			),
			$dest
		);
		wp_safe_redirect( $dest );
		exit;
	}

	/**
	 * @param string $order_no Order number.
	 * @return string
	 */
	public static function return_url( $order_no ) {
		return add_query_arg(
			array(
				'order_no' => rawurlencode( $order_no ),
			),
			home_url( '/icopay-jpay/return/', 'https' )
		);
	}

	/**
	 * @return string
	 */
	public static function cancel_url() {
		return home_url( '/', 'https' );
	}

	/**
	 * @param string $order_no Order number.
	 * @return string
	 */
	public static function inline_pay_url( $order_no ) {
		return add_query_arg( 'order_no', rawurlencode( $order_no ), home_url( '/icopay-jpay/pay/' ) );
	}

	/**
	 * @return string
	 */
	public static function start_url() {
		return home_url( '/icopay-jpay/start/' );
	}

	/**
	 * @return string
	 */
	public static function generate_order_no() {
		return substr( 'WP' . wp_generate_password( 10, false, false ), 0, 20 );
	}

	/**
	 * @param string $order_no Order number.
	 * @param array  $data     Session data.
	 * @param string $flow     Flow.
	 * @param string $amount   Amount.
	 * @param string $currency Currency.
	 * @param string $product  Product.
	 */
	private static function store_session( $order_no, array $data, $flow, $amount, $currency, $product ) {
		$payload = array_merge(
			$data,
			array(
				'flow'     => $flow,
				'amount'   => $amount,
				'currency' => $currency,
				'product'  => $product,
			)
		);
		set_transient( self::TRANSIENT_PREFIX . $order_no, $payload, 6 * HOUR_IN_SECONDS );
	}

	/**
	 * @param string $order_no Order number.
	 * @return array|null
	 */
	public static function get_session( $order_no ) {
		$order_no = trim( (string) $order_no );
		if ( '' === $order_no ) {
			return null;
		}
		$data = get_transient( self::TRANSIENT_PREFIX . $order_no );
		return is_array( $data ) ? $data : null;
	}
}
