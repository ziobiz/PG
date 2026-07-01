package com.pg.service;

import com.pg.entity.PgTrnsctn;
import com.pg.urlpay.JpayCustomerIpFieldParser;
import com.pg.urlpay.JpayLocationHintEnglishMapper;
import com.pg.urlpay.PayerGeoIpLookupService;
import com.pg.urlpay.PayerLocationLabelFormatter;
import com.pg.util.PayerCountryIso2Util;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

/** 결제·배치 — payer_location_label 적재(로컬 GeoIP + JPAY Export 폴백). */
@Service
public class PayerLocationEnrichmentService {

    private final PayerGeoIpLookupService geoIpLookupService;

    public PayerLocationEnrichmentService(PayerGeoIpLookupService geoIpLookupService) {
        this.geoIpLookupService = geoIpLookupService;
    }

    /** 결제 직후 — 저장된 IP·국가·도시 기준( MMDB 없으면 즉시 skip ). */
    public void enrichFromTxnContext(PgTrnsctn t) {
        if (t == null || hasCompleteLabel(t)) {
            return;
        }
        String ip = t.getPayerClientIp();
        if (ip != null && !ip.isBlank()) {
            applyFromIp(t, ip.trim(), null);
            if (hasCompleteLabel(t)) {
                return;
            }
        }
        applyLegacyOverview(t);
    }

    /** JPAY Export {@code Customer IP} — GeoIP 우선, 실패 시 접미사 파싱. */
    public boolean enrichFromJpayCustomerIpField(PgTrnsctn t, String customerIpRaw) {
        if (t == null || hasCompleteLabel(t)) {
            return false;
        }
        Optional<JpayCustomerIpFieldParser.ParsedCustomerIp> parsed = JpayCustomerIpFieldParser.parse(customerIpRaw);
        if (parsed.isEmpty()) {
            return false;
        }
        JpayCustomerIpFieldParser.ParsedCustomerIp p = parsed.get();
        if ((t.getPayerClientIp() == null || t.getPayerClientIp().isBlank()) && !p.ip().isBlank()) {
            String ip = p.ip().length() > 64 ? p.ip().substring(0, 64) : p.ip();
            t.setPayerClientIp(ip);
        }
        applyFromIp(t, p.ip(), p.locationSuffix());
        return hasCompleteLabel(t) || (t.getPayerLocationLabel() != null && !t.getPayerLocationLabel().isBlank());
    }

    private void applyFromIp(PgTrnsctn t, String ip, String jpaySuffix) {
        Optional<PayerGeoIpLookupService.GeoResult> geo = geoIpLookupService.lookup(ip);
        if (geo.isPresent()) {
            PayerGeoIpLookupService.GeoResult g = geo.get();
            applyGeoFields(t, g);
            setLabel(t, PayerLocationLabelFormatter.formatOverview(g.countryIso2(), g.locationEnglish()));
            return;
        }
        if (jpaySuffix != null && !jpaySuffix.isBlank()) {
            String fromHint = JpayLocationHintEnglishMapper.toOverviewLabel(jpaySuffix);
            if (!fromHint.isBlank()) {
                setLabel(t, fromHint);
            }
        }
    }

    private void applyGeoFields(PgTrnsctn t, PayerGeoIpLookupService.GeoResult g) {
        if (g.countryIso2() != null && !g.countryIso2().isBlank()) {
            t.setPayerCountryIso2(PayerCountryIso2Util.normalize(g.countryIso2()));
        }
        String loc = g.locationEnglish();
        if (loc != null && !loc.isBlank()) {
            String v = loc.trim();
            t.setPayerCity(v.length() > 128 ? v.substring(0, 128) : v);
        }
    }

    private void applyLegacyOverview(PgTrnsctn t) {
        String label = PayerLocationLabelFormatter.formatOverview(
                PayerCountryIso2Util.normalize(t.getPayerCountryIso2()),
                t.getPayerCity());
        if (!label.isBlank()) {
            setLabel(t, label);
        }
    }

    private static boolean hasCompleteLabel(PgTrnsctn t) {
        return PayerLocationLabelFormatter.isCompleteOverviewLabel(t.getPayerLocationLabel());
    }

    private static void setLabel(PgTrnsctn t, String label) {
        if (label == null || label.isBlank()) {
            return;
        }
        String v = label.trim();
        t.setPayerLocationLabel(v.length() > 256 ? v.substring(0, 256) : v);
    }
}
