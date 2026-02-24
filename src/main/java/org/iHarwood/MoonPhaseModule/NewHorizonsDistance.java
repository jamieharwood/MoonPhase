package org.iHarwood.MoonPhaseModule;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Approximate distance of the New Horizons spacecraft from Earth in AU.
 *
 * Approach:
 * - Use a reference epoch distance (AU) and an approximate radial speed (km/s).
 * - Extrapolate heliocentric distance by days elapsed since the epoch.
 * - Approximate Earth-spacecraft distance as |heliocentricDistance - 1.0 AU|.
 *
 * New Horizons launched January 19, 2006 and performed a Pluto flyby on July 14, 2015.
 * It is now in the Kuiper Belt, heading outward.
 */
public final class NewHorizonsDistance {
    private NewHorizonsDistance() {}

    private static final double KM_PER_AU = 149_597_870.7;
    private static final double SECONDS_PER_DAY = 86_400.0;

    // Reference epoch (UTC)
    private static final ZonedDateTime EPOCH = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    // New Horizons reference (approx)
    private static final double NH_REF_AU = 58.0;         // approximate heliocentric distance at epoch (AU)
    private static final double NH_SPEED_KM_S = 13.8;     // approx radial speed (km/s)

    private static double auPerDay(double kmPerSec) {
        return (kmPerSec * SECONDS_PER_DAY) / KM_PER_AU;
    }

    private static double daysSinceEpoch(ZonedDateTime zdt) {
        return Duration.between(EPOCH, zdt.withZoneSameInstant(ZoneOffset.UTC)).toSeconds() / SECONDS_PER_DAY;
    }

    /**
     * Approximate heliocentric distance of New Horizons (AU) at the given moment.
     */
    public static double heliocentricDistanceAU(ZonedDateTime zdt) {
        double days = daysSinceEpoch(zdt);
        return NH_REF_AU + auPerDay(NH_SPEED_KM_S) * days;
    }

    /**
     * Approximate Earth-spacecraft distance (AU) at the given moment.
     */
    public static double distanceFromEarthAU(ZonedDateTime zdt) {
        double helioc = heliocentricDistanceAU(zdt);
        return Math.max(0.0, Math.abs(helioc - 1.0));
    }

    // Convenience helpers for "now"
    public static double heliocentricDistanceAUNow() {
        return heliocentricDistanceAU(ZonedDateTime.now());
    }

    public static double distanceFromEarthAUNow() {
        return distanceFromEarthAU(ZonedDateTime.now());
    }

    /**
     * Speed of New Horizons in km/s (constant approximation).
     */
    public static double speedKmPerSec() {
        return NH_SPEED_KM_S;
    }

    /**
     * Speed of Voyager 1 in km/s (for comparison).
     */
    public static double voyager1SpeedKmPerSec() {
        return 17.0;
    }
}

