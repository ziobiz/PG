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
 * 결제·수수료·정산 리포트용 FIAT|CRYPTO 채널 판별.
 * CRYPTO = origin CRYPTO 또는 van CRYPTO (확정입금 과금 적재와 동일).
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

    public static String resolveChannel(PgTrnsctn t) {
        return isCryptoTxn(t) ? CommissionFeeChannel.CRYPTO : CommissionFeeChannel.FIAT;
    }

    /** 빈값·ALL = 전체. FIAT|CRYPTO 만 필터. */
    public static String normalizeFilter(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String u = raw.trim().toUpperCase(Locale.ROOT);
        if ("ALL".equals(u)) {
            return "";
        }
        if (CommissionFeeChannel.CRYPTO.equals(u) || CommissionFeeChannel.FIAT.equals(u)) {
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
        Predicate crypto = cb.or(
                cb.equal(cb.upper(cb.trim(cb.coalesce(origin, cb.literal("")))), "CRYPTO"),
                cb.equal(cb.upper(cb.trim(cb.coalesce(van, cb.literal("")))), "CRYPTO"));
        if (CommissionFeeChannel.CRYPTO.equals(ch)) {
            parts.add(crypto);
        } else {
            parts.add(cb.not(crypto));
        }
    }
}
