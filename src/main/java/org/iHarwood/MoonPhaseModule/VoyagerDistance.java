package org.iHarwood.MoonPhaseModule;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Approximate distances of Voyager 1 and 2 from Earth in AU.
 *
 * Uses a reference epoch distance (AU) and an approximate radial speed (km/s),
 * then extrapolates heliocentric distance by days elapsed since the epoch.
 *
 * Note: This is a rough linear approximation for display/CLI purposes only.
 */
public final class VoyagerDistance {
    private VoyagerDistance() {}

    private static final double KM_PER_AU = 149_597_870.7;
    private static final double SECONDS_PER_DAY = 86_400.0;

    // Reference epoch (UTC)
    private static final ZonedDateTime EPOCH = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    // Voyager 1 reference (approx)
    private static final double V1_REF_AU = 159.0;
    private static final double V1_SPEED_KM_S = 17.0;

    // Voyager 2 reference (approx)
    private static final double V2_REF_AU = 133.0;
    private static final double V2_SPEED_KM_S = 15.4;

    private static double auPerDay(double kmPerSec) {
        return (kmPerSec * SECONDS_PER_DAY) / KM_PER_AU;
    }

    private static double daysSinceEpoch(ZonedDateTime zdt) {
        return Duration.between(EPOCH, zdt.withZoneSameInstant(ZoneOffset.UTC)).toSeconds() / SECONDS_PER_DAY;
    }

    public static double heliocentricDistanceV1AU(ZonedDateTime zdt) {
        return V1_REF_AU + auPerDay(V1_SPEED_KM_S) * daysSinceEpoch(zdt);
    }

    public static double heliocentricDistanceV2AU(ZonedDateTime zdt) {
        return V2_REF_AU + auPerDay(V2_SPEED_KM_S) * daysSinceEpoch(zdt);
    }

    public static double distanceFromEarthV1AU(ZonedDateTime zdt) {
        return Math.max(0.0, Math.abs(heliocentricDistanceV1AU(zdt) - 1.0));
    }

    public static double distanceFromEarthV2AU(ZonedDateTime zdt) {
        return Math.max(0.0, Math.abs(heliocentricDistanceV2AU(zdt) - 1.0));
    }

    public static double heliocentricDistanceV1AUNow() {
        return heliocentricDistanceV1AU(ZonedDateTime.now(ZoneOffset.UTC));
    }

    public static double heliocentricDistanceV2AUNow() {
        return heliocentricDistanceV2AU(ZonedDateTime.now(ZoneOffset.UTC));
    }

    public static double distanceFromEarthV1AUNow() {
        return distanceFromEarthV1AU(ZonedDateTime.now(ZoneOffset.UTC));
    }

    public static double distanceFromEarthV2AUNow() {
        return distanceFromEarthV2AU(ZonedDateTime.now(ZoneOffset.UTC));
    }
}
