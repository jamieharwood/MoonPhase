package org.iHarwood.MoonPhaseModule;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Approximate distances of Voyager 1 and 2 from Earth in AU.
 *
 * Approach:
 * - Use a reference epoch distance (AU) and an approximate radial speed (km/s).
 * - Extrapolate heliocentric distance by days elapsed since the epoch.
 * - Approximate Earth-spacecraft distance as |heliocentricDistance - 1.0 AU|.
 * - astronomical unit (AU)
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
    private static final double V1_REF_AU = 159.0;       // approximate heliocentric distance at epoch (AU)
    private static final double V1_SPEED_KM_S = 17.0;    // approx radial speed (km/s)

    // Voyager 2 reference (approx)
    private static final double V2_REF_AU = 133.0;       // approximate heliocentric distance at epoch (AU)
    private static final double V2_SPEED_KM_S = 15.4;    // approx radial speed (km/s)

    private static double kmPerDay(double kmPerSec) {
        return kmPerSec * SECONDS_PER_DAY;
    }

    private static double auPerDay(double kmPerSec) {
        return kmPerDay(kmPerSec) / KM_PER_AU;
    }

    private static double daysSinceEpoch(ZonedDateTime zdt) {
        return Duration.between(EPOCH, zdt.withZoneSameInstant(ZoneOffset.UTC)).toSeconds() / SECONDS_PER_DAY;
    }

    /**
     * Approximate heliocentric distance of Voyager 1 (AU) at the given moment.
     */
    public static double heliocentricDistanceV1AU(ZonedDateTime zdt) {
        double days = daysSinceEpoch(zdt);
        return V1_REF_AU + auPerDay(V1_SPEED_KM_S) * days;
    }

    /**
     * Approximate heliocentric distance of Voyager 2 (AU) at the given moment.
     */
    public static double heliocentricDistanceV2AU(ZonedDateTime zdt) {
        double days = daysSinceEpoch(zdt);
        return V2_REF_AU + auPerDay(V2_SPEED_KM_S) * days;
    }

    /**
     * Approximate Earth-spacecraft distance (AU) for Voyager 1 at the given moment.
     * Uses |heliocentric - 1.0 AU| as a simple approximation.
     */
    public static double distanceFromEarthV1AU(ZonedDateTime zdt) {
        double helioc = heliocentricDistanceV1AU(zdt);
        return Math.max(0.0, Math.abs(helioc - 1.0));
    }

    /**
     * Approximate Earth-spacecraft distance (AU) for Voyager 2 at the given moment.
     * Uses |heliocentric - 1.0 AU| as a simple approximation.
     */
    public static double distanceFromEarthV2AU(ZonedDateTime zdt) {
        double helioc = heliocentricDistanceV2AU(zdt);
        return Math.max(0.0, Math.abs(helioc - 1.0));
    }

    /**
     * Convenience helpers for "now" in system default zone.
     */
    public static double heliocentricDistanceV1AUNow() {
        return heliocentricDistanceV1AU(ZonedDateTime.now());
    }

    public static double heliocentricDistanceV2AUNow() {
        return heliocentricDistanceV2AU(ZonedDateTime.now());
    }

    public static double distanceFromEarthV1AUNow() {
        return distanceFromEarthV1AU(ZonedDateTime.now());
    }

    public static double distanceFromEarthV2AUNow() {
        return distanceFromEarthV2AU(ZonedDateTime.now());
    }
}

