package org.iHarwood.health;

import org.iHarwood.AstronomicalDataService;
import org.iHarwood.AstronomicalSnapshot;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Actuator health contribution reflecting whether a live snapshot is available.
 */
@Component
@ConditionalOnWebApplication
@ConditionalOnBean(AstronomicalDataService.class)
public class SnapshotHealthIndicator implements HealthIndicator {

    private final AstronomicalDataService dataService;

    public SnapshotHealthIndicator(AstronomicalDataService dataService) {
        this.dataService = dataService;
    }

    @Override
    public Health health() {
        AstronomicalSnapshot snapshot = dataService.getLatestSnapshot();
        if (snapshot == null) {
            return Health.status("STARTING")
                    .withDetail("snapshot", "WAITING")
                    .build();
        }
        return Health.up()
                .withDetail("snapshot", "UP")
                .withDetail("lastUpdated", snapshot.lastUpdated())
                .build();
    }
}

