<?php
/**
 * Admin settings.
 *
 * @package ICOPAY_JPAY
 */

defined( 'ABSPATH' ) || exit;

/**
 * Settings page.
 */
class ICOPAY_JPAY_Settings {

	/**
	 * Init hooks.
	 */
	public static function init() {
		add_action( 'admin_menu', array( __CLASS__, 'register_menu' ) );
		add_action( 'admin_init', array( __CLASS__, 'register_settings' ) );
	}

	/**
	 * @return array
	 */
	public static function get_settings() {
		$defaults = array(
			'comp_id'        => '',
			'api_base_url'   => 'https://api.icopay.co.kr',
			'broker_secret'  => '',
			'flow_mode'      => 'inline',
			'lang'           => 'auto',
			'webhook_secret' => '',
			'return_page_id' => 0,
		);
		$saved = get_option( ICOPAY_JPAY_OPTION_KEY, array() );
		if ( ! is_array( $saved ) ) {
			$saved = array();
		}
		return array_merge( $defaults, $saved );
	}

	/**
	 * @return ICOPAY_Core_Api_Client|null
	 */
	public static function api_client() {
		icopay_jpay_load_core();
		if ( ! class_exists( 'ICOPAY_Core_Api_Client' ) ) {
			return null;
		}
		$s = self::get_settings();
		return new ICOPAY_Core_Api_Client(
			$s['api_base_url'],
			$s['comp_id'],
			$s['broker_secret'],
			ICOPAY_Flow::VENDOR_JPAY
		);
	}

	/**
	 * Register admin menu.
	 */
	public static function register_menu() {
		add_options_page(
			__( 'ICOPAY JPAY', 'icopay-jpay' ),
			__( 'ICOPAY JPAY', 'icopay-jpay' ),
			'manage_options',
			'icopay-jpay',
			array( __CLASS__, 'render_page' )
		);
	}

	/**
	 * Register settings.
	 */
	public static function register_settings() {
		register_setting( 'icopay_jpay_group', ICOPAY_JPAY_OPTION_KEY, array( __CLASS__, 'sanitize' ) );
	}

	/**
	 * @param array $input Input.
	 * @return array
	 */
	public static function sanitize( $input ) {
		if ( ! is_array( $input ) ) {
			return self::get_settings();
		}
		$out = self::get_settings();
		$out['comp_id']        = sanitize_text_field( $input['comp_id'] ?? '' );
		$out['api_base_url']   = esc_url_raw( trim( (string) ( $input['api_base_url'] ?? '' ) ) );
		$out['broker_secret']  = sanitize_text_field( $input['broker_secret'] ?? '' );
		$out['flow_mode']      = ICOPAY_Flow::normalize( $input['flow_mode'] ?? 'inline' );
		$out['lang']           = sanitize_text_field( $input['lang'] ?? 'auto' );
		$out['webhook_secret'] = sanitize_text_field( $input['webhook_secret'] ?? '' );
		$out['return_page_id'] = absint( $input['return_page_id'] ?? 0 );
		return $out;
	}

	/**
	 * Render settings page.
	 */
	public static function render_page() {
		if ( ! current_user_can( 'manage_options' ) ) {
			return;
		}
		$s       = self::get_settings();
		$webhook = ICOPAY_JPAY_Webhook::get_webhook_url();
		?>
		<div class="wrap">
			<h1><?php esc_html_e( 'ICOPAY JPAY Settings', 'icopay-jpay' ); ?></h1>
			<form method="post" action="options.php">
				<?php settings_fields( 'icopay_jpay_group' ); ?>
				<table class="form-table">
					<tr>
						<th scope="row"><?php esc_html_e( 'compId', 'icopay-jpay' ); ?></th>
						<td><input type="text" class="regular-text" name="<?php echo esc_attr( ICOPAY_JPAY_OPTION_KEY ); ?>[comp_id]" value="<?php echo esc_attr( $s['comp_id'] ); ?>" /></td>
					</tr>
					<tr>
						<th scope="row"><?php esc_html_e( 'API Base URL', 'icopay-jpay' ); ?></th>
						<td><input type="url" class="regular-text" name="<?php echo esc_attr( ICOPAY_JPAY_OPTION_KEY ); ?>[api_base_url]" value="<?php echo esc_attr( $s['api_base_url'] ); ?>" /></td>
					</tr>
					<tr>
						<th scope="row"><?php esc_html_e( 'Broker secret', 'icopay-jpay' ); ?></th>
						<td><input type="password" class="regular-text" name="<?php echo esc_attr( ICOPAY_JPAY_OPTION_KEY ); ?>[broker_secret]" value="<?php echo esc_attr( $s['broker_secret'] ); ?>" autocomplete="new-password" /></td>
					</tr>
					<tr>
						<th scope="row"><?php esc_html_e( 'Checkout flow', 'icopay-jpay' ); ?></th>
						<td>
							<select name="<?php echo esc_attr( ICOPAY_JPAY_OPTION_KEY ); ?>[flow_mode]">
								<option value="inline" <?php selected( $s['flow_mode'], 'inline' ); ?>><?php esc_html_e( 'Inline (iframe)', 'icopay-jpay' ); ?></option>
								<option value="redirect" <?php selected( $s['flow_mode'], 'redirect' ); ?>><?php esc_html_e( 'Redirect', 'icopay-jpay' ); ?></option>
							</select>
						</td>
					</tr>
					<tr>
						<th scope="row"><?php esc_html_e( 'Language', 'icopay-jpay' ); ?></th>
						<td>
							<select name="<?php echo esc_attr( ICOPAY_JPAY_OPTION_KEY ); ?>[lang]">
								<?php
								$langs = array( 'auto', 'KOR', 'ENG', 'JPN', 'CHN', 'THA' );
								foreach ( $langs as $lang ) {
									printf(
										'<option value="%1$s" %2$s>%1$s</option>',
										esc_attr( $lang ),
										selected( $s['lang'], $lang, false )
									);
								}
								?>
							</select>
						</td>
					</tr>
					<tr>
						<th scope="row"><?php esc_html_e( 'Return page', 'icopay-jpay' ); ?></th>
						<td>
							<?php
							wp_dropdown_pages(
								array(
									'name'              => ICOPAY_JPAY_OPTION_KEY . '[return_page_id]',
									'selected'          => (int) $s['return_page_id'],
									'show_option_none'  => __( '— Select —', 'icopay-jpay' ),
									'option_none_value' => '0',
								)
							);
							?>
							<p class="description"><?php esc_html_e( 'Redirect flow: customer returns here after payment.', 'icopay-jpay' ); ?></p>
						</td>
					</tr>
					<tr>
						<th scope="row"><?php esc_html_e( 'Webhook sign secret', 'icopay-jpay' ); ?></th>
						<td><input type="password" class="regular-text" name="<?php echo esc_attr( ICOPAY_JPAY_OPTION_KEY ); ?>[webhook_secret]" value="<?php echo esc_attr( $s['webhook_secret'] ); ?>" autocomplete="new-password" /></td>
					</tr>
					<tr>
						<th scope="row"><?php esc_html_e( 'Webhook URL', 'icopay-jpay' ); ?></th>
						<td><code style="word-break:break-all;"><?php echo esc_html( $webhook ); ?></code></td>
					</tr>
				</table>
				<p><strong><?php esc_html_e( 'Shortcode', 'icopay-jpay' ); ?>:</strong> <code>[icopay_jpay amount="100" currency="USD" product="Sample"]</code></p>
				<?php submit_button(); ?>
			</form>
		</div>
		<?php
	}
}
