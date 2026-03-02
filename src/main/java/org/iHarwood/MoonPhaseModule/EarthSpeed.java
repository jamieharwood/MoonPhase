package org.iHarwood.MoonPhaseModule;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Calculates Earth's current orbital speed around the Sun.
 *
 * Uses the vis-viva equation: v = sqrt(GM * (2/r - 1/a))
 * where GM is the standard gravitational parameter for the Sun,
 * r is the current Sun-Earth distance, and a is the semi-major axis.
 *
 * Speed varies from ~29.29 km/s at aphelion to ~30.29 km/s at perihelion.
 */
public final class EarthSpeed {
    private EarthSpeed() {}

    private static final double GM_SUN = 1.32712440018e11;
    private static final double A_EARTH_KM = 149_597_870.7;
    private static final double KM_PER_AU = 149_597_870.7;

    public static double speedKmPerSecNow() {
        return speedKmPerSec(ZonedDateTime.now(ZoneOffset.UTC));
    }

    public static double speedKmPerSec(ZonedDateTime zdt) {
        double rAu = SunDistance.distanceAU(zdt);
        double rKm = rAu * KM_PER_AU;
        return Math.sqrt(GM_SUN * (2.0 / rKm - 1.0 / A_EARTH_KM));
    }

    public static double speedKmPerHour(ZonedDateTime zdt) {
        return speedKmPerSec(zdt) * 3600.0;
    }

    public static double speedKmPerHourNow() {
        return speedKmPerHour(ZonedDateTime.now(ZoneOffset.UTC));
    }

    public static double speedMphNow() {
        return speedKmPerHourNow() / 1.60934;
    }
}
