<?php
/**
 * Shortcode payment form.
 *
 * @package ICOPAY_JPAY
 */

defined( 'ABSPATH' ) || exit;

/**
 * [icopay_jpay amount="100" currency="USD" product="Name" order_no="optional"]
 */
class ICOPAY_JPAY_Shortcode {

	/**
	 * Init.
	 */
	public static function init() {
		add_shortcode( 'icopay_jpay', array( __CLASS__, 'render' ) );
	}

	/**
	 * @param array $atts Attributes.
	 * @return string
	 */
	public static function render( $atts ) {
		$atts = shortcode_atts(
			array(
				'amount'   => '',
				'currency' => 'USD',
				'product'  => '',
				'order_no' => '',
				'button'   => __( 'Pay with ICOPAY', 'icopay-jpay' ),
			),
			$atts,
			'icopay_jpay'
		);

		if ( '' === trim( (string) $atts['amount'] ) ) {
			return '<p class="icopay-jpay-error">' . esc_html__( 'amount is required.', 'icopay-jpay' ) . '</p>';
		}

		ob_start();
		?>
		<form class="icopay-jpay-form" method="post" action="<?php echo esc_url( ICOPAY_JPAY_Payment::start_url() ); ?>">
			<?php wp_nonce_field( 'icopay_jpay_start', 'icopay_jpay_nonce' ); ?>
			<input type="hidden" name="amount" value="<?php echo esc_attr( $atts['amount'] ); ?>" />
			<input type="hidden" name="currency" value="<?php echo esc_attr( $atts['currency'] ); ?>" />
			<input type="hidden" name="product" value="<?php echo esc_attr( $atts['product'] ); ?>" />
			<?php if ( '' !== trim( (string) $atts['order_no'] ) ) : ?>
				<input type="hidden" name="order_no" value="<?php echo esc_attr( $atts['order_no'] ); ?>" />
			<?php endif; ?>
			<button type="submit" class="button icopay-jpay-submit"><?php echo esc_html( $atts['button'] ); ?></button>
		</form>
		<?php
		return ob_get_clean();
	}
}
