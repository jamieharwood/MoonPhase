package org.iHarwood.MoonPhaseModule;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Calculates Earth's current axial tilt (obliquity of the ecliptic).
 *
 * Uses the IAU formula:
 *   e = 23.439291 deg - 0.013004 deg * T
 * where T is Julian centuries since J2000.0.
 *
 * The tilt varies very slowly (~0.013 deg per century) between roughly 22.1 deg and 24.5 deg
 * over a 41,000-year Milankovitch cycle. The current value is approximately 23.44 deg.
 */
public final class EarthAxialTilt {
    private EarthAxialTilt() {}

    private static final double OBLIQUITY_J2000_DEG = 23.439291;
    private static final double OBLIQUITY_RATE_DEG_PER_CENTURY = 0.013004;
    private static final double DAYS_PER_CENTURY = 36525.0;

    public static double tiltDegrees(ZonedDateTime zdt) {
        double T = DateUtils.daysSinceJ2000(zdt) / DAYS_PER_CENTURY;
        return OBLIQUITY_J2000_DEG - OBLIQUITY_RATE_DEG_PER_CENTURY * T;
    }

    public static double tiltDegreesNow() {
        return tiltDegrees(ZonedDateTime.now(ZoneOffset.UTC));
    }
}
