package com.pg.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MaxMind GeoLite2 City MMDB — 결제 시·배치 보정용 로컬 GeoIP.
 * MMDB 파일이 없으면 조회는 즉시 no-op(결제·목록 성능 영향 없음).
 */
@Component
@ConfigurationProperties(prefix = "app.geoip")
public class GeoIpProperties {

    private boolean enabled = true;
    /** GeoLite2-City.mmdb 절대 경로. 없으면 GeoIP 조회 비활성. */
    private String mmdbPath = "/var/lib/pg/GeoLite2-City.mmdb";
    private int cacheSize = 10_000;

    private final Backfill backfill = new Backfill();
    private final JpayExportBackfill jpayExportBackfill = new JpayExportBackfill();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getMmdbPath() { return mmdbPath; }
    public void setMmdbPath(String mmdbPath) { this.mmdbPath = mmdbPath; }
    public int getCacheSize() { return cacheSize; }
    public void setCacheSize(int cacheSize) { this.cacheSize = cacheSize; }
    public Backfill getBackfill() { return backfill; }
    public JpayExportBackfill getJpayExportBackfill() { return jpayExportBackfill; }

    public static class Backfill {
        private boolean enabled = true;
        private String cron = "0 15 3 * * *";
        private int batchSize = 250;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getCron() { return cron; }
        public void setCron(String cron) { this.cron = cron; }
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    }

    public static class JpayExportBackfill {
        private boolean enabled = true;
        private int batchSize = 500;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    }
}
