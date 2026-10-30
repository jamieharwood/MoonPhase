package org.iHarwood.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Typed application configuration bound from {@code app.*} / {@code awtrix.*} properties.
 */
@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /** Spring cron for the twice-daily update cycle. */
    @NotBlank
    private String cron = "0 1 0,12 * * *";

    /** Observer latitude in decimal degrees (positive = North). */
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private double latitude = 51.4769;

    /** Observer longitude in decimal degrees (positive = East). */
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private double longitude = 0.0;

    private final History history = new History();

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public History getHistory() {
        return history;
    }

    public static class History {
        private boolean enabled = false;
        /** Days of history to keep; 0 = unlimited. */
        private int retentionDays = 0;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getRetentionDays() {
            return retentionDays;
        }

        public void setRetentionDays(int retentionDays) {
            this.retentionDays = retentionDays;
        }
    }
}

