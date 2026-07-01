package com.pg.urlpay;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import com.pg.config.GeoIpProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 로컬 MaxMind MMDB 조회 — 외부 API 없음. MMDB 미설치·비활성 시 즉시 empty 반환.
 */
@Service
public class PayerGeoIpLookupService {

    private static final Logger log = LoggerFactory.getLogger(PayerGeoIpLookupService.class);

    public record GeoResult(String countryIso2, String cityEnglish, String regionEnglish) {
        public String locationEnglish() {
            if (cityEnglish != null && !cityEnglish.isBlank()) {
                return cityEnglish.trim();
            }
            return regionEnglish != null ? regionEnglish.trim() : "";
        }
    }

    private final GeoIpProperties props;
    private volatile DatabaseReader reader;
    private volatile boolean readerUnavailable;
    private final Map<String, Optional<GeoResult>> cache;

    public PayerGeoIpLookupService(GeoIpProperties props) {
        this.props = props;
        int cap = Math.max(256, Math.min(props.getCacheSize(), 50_000));
        this.cache = Collections.synchronizedMap(new LinkedHashMap<>(256, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Optional<GeoResult>> eldest) {
                return size() > cap;
            }
        });
    }

    public Optional<GeoResult> lookup(String ip) {
        if (!props.isEnabled() || ip == null || ip.isBlank()) {
            return Optional.empty();
        }
        if (!JpayCustomerIpFieldParser.looksLikeIp(ip)) {
            return Optional.empty();
        }
        String key = ip.trim();
        Optional<GeoResult> hit = cache.get(key);
        if (hit != null) {
            return hit;
        }
        Optional<GeoResult> resolved = lookupUncached(key);
        cache.put(key, resolved);
        return resolved;
    }

    private Optional<GeoResult> lookupUncached(String ip) {
        DatabaseReader db = reader();
        if (db == null) {
            return Optional.empty();
        }
        try {
            CityResponse city = db.city(InetAddress.getByName(ip));
            String iso2 = city.getCountry().getIsoCode();
            if (iso2 != null) {
                iso2 = iso2.trim().toUpperCase(java.util.Locale.ROOT);
            } else {
                iso2 = "";
            }
            String cityEn = englishName(city.getCity().getNames());
            String regionEn = englishName(city.getMostSpecificSubdivision().getNames());
            if (iso2.isEmpty() && cityEn.isEmpty() && regionEn.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new GeoResult(iso2, cityEn, regionEn));
        } catch (Exception e) {
            log.trace("GeoIP lookup skip ip={}: {}", ip, e.toString());
            return Optional.empty();
        }
    }

    private DatabaseReader reader() {
        if (readerUnavailable) {
            return null;
        }
        DatabaseReader local = reader;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (readerUnavailable) {
                return null;
            }
            if (reader != null) {
                return reader;
            }
            String path = props.getMmdbPath();
            if (path == null || path.isBlank()) {
                readerUnavailable = true;
                return null;
            }
            Path p = Path.of(path.trim());
            if (!Files.isRegularFile(p)) {
                log.info("GeoIP MMDB not found at {} — GeoIP lookup disabled (no payment-path impact)", p);
                readerUnavailable = true;
                return null;
            }
            try {
                reader = new DatabaseReader.Builder(p.toFile()).build();
                log.info("GeoIP MMDB loaded: {}", p.toAbsolutePath());
                return reader;
            } catch (Exception e) {
                log.warn("GeoIP MMDB load failed ({}): {}", p, e.getMessage());
                readerUnavailable = true;
                return null;
            }
        }
    }

    private static String englishName(java.util.Map<String, String> names) {
        if (names == null || names.isEmpty()) {
            return "";
        }
        String en = names.get("en");
        if (en != null && !en.isBlank()) {
            return en.trim();
        }
        return names.values().stream().filter(v -> v != null && !v.isBlank()).findFirst().orElse("").trim();
    }
}
