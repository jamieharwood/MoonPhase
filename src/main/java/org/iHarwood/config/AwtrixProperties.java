package org.iHarwood.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Awtrix LED matrix device connection settings.
 */
@Validated
@ConfigurationProperties(prefix = "awtrix")
public class AwtrixProperties {

    @NotBlank
    private String hostname = "http://moonclock.local";

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
}

