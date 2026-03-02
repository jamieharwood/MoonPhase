package org.iHarwood.MoonPhaseModule;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Approximate distance of the New Horizons spacecraft from Earth in AU.
 *
 * Uses a reference epoch distance (AU) and an approximate radial speed (km/s),
 * then extrapolates heliocentric distance by days elapsed since the epoch.
 */
public final class NewHorizonsDistance {
    private NewHorizonsDistance() {}

    private static final double KM_PER_AU = 149_597_870.7;
    private static final double SECONDS_PER_DAY = 86_400.0;

    private static final ZonedDateTime EPOCH = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    private static final double NH_REF_AU = 58.0;
    private static final double NH_SPEED_KM_S = 13.8;

    private static double auPerDay(double kmPerSec) {
        return (kmPerSec * SECONDS_PER_DAY) / KM_PER_AU;
    }

    private static double daysSinceEpoch(ZonedDateTime zdt) {
        return Duration.between(EPOCH, zdt.withZoneSameInstant(ZoneOffset.UTC)).toSeconds() / SECONDS_PER_DAY;
    }

    public static double heliocentricDistanceAU(ZonedDateTime zdt) {
        return NH_REF_AU + auPerDay(NH_SPEED_KM_S) * daysSinceEpoch(zdt);
    }

    public static double distanceFromEarthAU(ZonedDateTime zdt) {
        return Math.max(0.0, Math.abs(heliocentricDistanceAU(zdt) - 1.0));
    }

    public static double heliocentricDistanceAUNow() {
        return heliocentricDistanceAU(ZonedDateTime.now(ZoneOffset.UTC));
    }

    public static double distanceFromEarthAUNow() {
        return distanceFromEarthAU(ZonedDateTime.now(ZoneOffset.UTC));
    }

    public static double speedKmPerSec() {
        return NH_SPEED_KM_S;
    }
}
