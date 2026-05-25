<?php
/**
 * WooCommerce 주문 ↔ ICOPAY orderNo 매핑.
 *
 * @package ICOPAY_WooCommerce
 */

defined( 'ABSPATH' ) || exit;

/**
 * Order helper.
 */
class ICOPAY_Order_Helper {

	const META_ORDER_NO   = '_icopay_order_no';
	const META_SESSION    = '_icopay_session_token';
	const META_TRN_ID     = '_icopay_trn_id';
	const META_PG_TXN_ID  = '_icopay_pg_txn_id';

	/**
	 * ChillPay orderNo 규칙: ≤20자, 영숫자·하이픈·밑줄.
	 *
	 * @param int $order_id WC order id.
	 * @return string
	 */
	public static function build_order_no( $order_id ) {
		$no = 'WC' . absint( $order_id );
		$no = preg_replace( '/[^A-Za-z0-9_-]/', '', $no );
		if ( '' === $no ) {
			$no = 'WC' . time();
		}
		return substr( $no, 0, 20 );
	}

	/**
	 * @param WC_Order $order Order.
	 * @return string
	 */
	public static function get_or_create_order_no( WC_Order $order ) {
		$existing = $order->get_meta( self::META_ORDER_NO, true );
		if ( is_string( $existing ) && '' !== trim( $existing ) ) {
			return trim( $existing );
		}
		$no = self::build_order_no( $order->get_id() );
		$order->update_meta_data( self::META_ORDER_NO, $no );
		$order->save();
		return $no;
	}

	/**
	 * @param string $icopay_order_no ICOPAY orderNo.
	 * @return WC_Order|null
	 */
	public static function find_order_by_icopay_order_no( $icopay_order_no ) {
		$icopay_order_no = trim( (string) $icopay_order_no );
		if ( '' === $icopay_order_no ) {
			return null;
		}

		$orders = wc_get_orders(
			array(
				'limit'      => 1,
				'meta_key'   => self::META_ORDER_NO,
				'meta_value' => $icopay_order_no,
				'return'     => 'objects',
			)
		);
		if ( ! empty( $orders ) ) {
			return $orders[0];
		}

		// Fallback: WC{id} pattern from numeric suffix.
		if ( preg_match( '/^WC(\d+)$/i', $icopay_order_no, $m ) ) {
			$order = wc_get_order( (int) $m[1] );
			if ( $order ) {
				return $order;
			}
		}
		return null;
	}

	/**
	 * @param WC_Order $order Order.
	 * @return string
	 */
	public static function product_name_from_order( WC_Order $order ) {
		$items = $order->get_items();
		if ( empty( $items ) ) {
			/* translators: %d: order id */
			return sprintf( __( 'Order #%d', 'icopay-woocommerce' ), $order->get_id() );
		}
		$names = array();
		foreach ( $items as $item ) {
			$names[] = $item->get_name();
			if ( count( $names ) >= 3 ) {
				break;
			}
		}
		$label = implode( ', ', $names );
		if ( count( $items ) > 3 ) {
			$label .= '…';
		}
		return mb_substr( $label, 0, 500 );
	}

	/**
	 * @param WC_Order $order Order.
	 * @return string Plain amount for prepare API.
	 */
	public static function amount_plain( WC_Order $order ) {
		$total = $order->get_total();
		if ( function_exists( 'wc_format_decimal' ) ) {
			return wc_format_decimal( $total, wc_get_price_decimals() );
		}
		return number_format( (float) $total, wc_get_price_decimals(), '.', '' );
	}

	/**
	 * ICOPAY / PG status → paid?
	 *
	 * @param string $status Internal status code.
	 * @return bool
	 */
	public static function is_paid_status( $status ) {
		$s = trim( (string) $status );
		return in_array( $s, array( '00', '10', 'PAID' ), true );
	}

	/**
	 * @param WC_Order $order Order.
	 * @param array    $payload Webhook payload.
	 */
	public static function mark_paid_from_payload( WC_Order $order, array $payload ) {
		if ( ! empty( $payload['trnId'] ) ) {
			$order->update_meta_data( self::META_TRN_ID, sanitize_text_field( (string) $payload['trnId'] ) );
		}
		if ( ! empty( $payload['pgTxnId'] ) ) {
			$order->update_meta_data( self::META_PG_TXN_ID, sanitize_text_field( (string) $payload['pgTxnId'] ) );
		}
		$order->save();

		if ( $order->is_paid() ) {
			return;
		}

		$txn_id = ! empty( $payload['trnId'] ) ? (string) $payload['trnId'] : '';
		$order->payment_complete( $txn_id );
		$order->add_order_note(
			sprintf(
				/* translators: 1: ICOPAY trnId 2: orderNo */
				__( 'ICOPAY payment confirmed (trnId: %1$s, orderNo: %2$s).', 'icopay-woocommerce' ),
				$txn_id,
				$order->get_meta( self::META_ORDER_NO, true )
			)
		);
	}
}
