<?php
/**
 * ICOPAY payment page template.
 *
 * @package ICOPAY_WooCommerce
 * @var WC_Order $order
 * @var string   $title
 * @var string   $embed
 * @var string   $order_no
 */

defined( 'ABSPATH' ) || exit;

?><!DOCTYPE html>
<html <?php language_attributes(); ?>>
<head>
	<meta charset="<?php bloginfo( 'charset' ); ?>">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title><?php echo esc_html( $title ); ?> — <?php esc_html_e( 'Payment', 'icopay-woocommerce' ); ?></title>
	<?php wp_head(); ?>
</head>
<body <?php body_class( 'icopay-wc-payment-page' ); ?>>
<div class="icopay-wc-wrap">
	<header class="icopay-wc-header">
		<h1><?php echo esc_html( $title ); ?></h1>
		<p class="icopay-wc-order-ref">
			<?php
			printf(
				/* translators: 1: WC order number 2: ICOPAY orderNo */
				esc_html__( 'Order #%1$s · Ref %2$s', 'icopay-woocommerce' ),
				esc_html( $order->get_order_number() ),
				esc_html( $order_no )
			);
			?>
		</p>
	</header>
	<div id="icopay-wc-status" class="icopay-wc-status" aria-live="polite" hidden></div>
	<div class="icopay-wc-embed">
		<?php echo $embed; // phpcs:ignore WordPress.Security.EscapeOutput.OutputNotEscaped -- built with esc_url/esc_attr ?>
	</div>
</div>
<?php wp_footer(); ?>
</body>
</html>
