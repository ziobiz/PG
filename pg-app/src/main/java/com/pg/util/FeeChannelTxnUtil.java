package com.pg.util;

import com.pg.crypto.CryptoCommissionChargeService;
import com.pg.entity.PgTrnsctn;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.List;
import java.util.Locale;

/**
 * 결제·수수료·정산 리포트용 FIAT|QR|CRYPTO 채널 판별.
 * CRYPTO = origin CRYPTO 또는 van CRYPTO.
 * QR = paymentChannel QR (카드 승인과 수수료 정책을 나눕니다).
 */
public final class FeeChannelTxnUtil {

    private FeeChannelTxnUtil() {
    }

    public static boolean isCryptoTxn(PgTrnsctn t) {
        if (t == null) {
            return false;
        }
        String origin = t.getOrigin() != null ? t.getOrigin().trim() : "";
        if (CryptoCommissionChargeService.ORIGIN_CRYPTO.equalsIgnoreCase(origin)) {
            return true;
        }
        String van = t.getVan() != null ? t.getVan().trim() : "";
        return CommissionFeeChannel.CRYPTO.equalsIgnoreCase(van);
    }

    public static boolean isQrTxn(PgTrnsctn t) {
        if (t == null || isCryptoTxn(t)) {
            return false;
        }
        String channel = t.getPaymentChannel() != null ? t.getPaymentChannel().trim() : "";
        return CommissionFeeChannel.QR.equalsIgnoreCase(channel);
    }

    public static String resolveChannel(PgTrnsctn t) {
        if (isCryptoTxn(t)) {
            return CommissionFeeChannel.CRYPTO;
        }
        if (isQrTxn(t)) {
            return CommissionFeeChannel.QR;
        }
        return CommissionFeeChannel.FIAT;
    }

    /** 빈값·ALL = 전체. FIAT|QR|CRYPTO 만 필터. */
    public static String normalizeFilter(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if ("ALL".equals(u)) {
            return "";
        }
        if (CommissionFeeChannel.CRYPTO.equals(u) || CommissionFeeChannel.QR.equals(u) || CommissionFeeChannel.FIAT.equals(u)) {
            return u;
        }
        return "";
    }

    public static boolean matchesFilter(PgTrnsctn t, String normalizedFilter) {
        if (normalizedFilter == null || normalizedFilter.isBlank()) {
            return true;
        }
        return normalizedFilter.equals(resolveChannel(t));
    }

    public static boolean matchesFilter(String channel, String normalizedFilter) {
        if (normalizedFilter == null || normalizedFilter.isBlank()) {
            return true;
        }
        return normalizedFilter.equals(CommissionFeeChannel.normalize(channel));
    }

    public static void addJpaPredicate(List<Predicate> parts,
                                       CriteriaBuilder cb,
                                       Root<PgTrnsctn> root,
                                       String searchFeeChannel) {
        String ch = normalizeFilter(searchFeeChannel);
        if (ch.isEmpty()) {
            return;
        }
        Path<String> origin = root.get("origin");
        Path<String> van = root.get("van");
        Path<String> payCh = root.get("paymentChannel");
        Predicate crypto = cb.or(
                cb.equal(cb.upper(cb.trim(cb.coalesce(origin, cb.literal("")))), "CRYPTO"),
                cb.equal(cb.upper(cb.trim(cb.coalesce(van, cb.literal("")))), "CRYPTO"));
        Predicate qr = cb.and(
                cb.not(crypto),
                cb.equal(cb.upper(cb.trim(cb.coalesce(payCh, cb.literal("")))), "QR"));
        if (CommissionFeeChannel.CRYPTO.equals(ch)) {
            parts.add(crypto);
        } else if (CommissionFeeChannel.QR.equals(ch)) {
            parts.add(qr);
        } else {
            parts.add(cb.and(cb.not(crypto), cb.not(qr)));
        }
    }
}
