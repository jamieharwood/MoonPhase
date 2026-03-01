package org.iHarwood.MoonPhaseModule;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Calculates Earth's current axial tilt (obliquity of the ecliptic).
 *
 * Uses the IAU formula:
 *   ε = 23.439291° − 0.013004° × T
 * where T is Julian centuries since J2000.0.
 *
 * The tilt varies very slowly (~0.013° per century) between roughly 22.1° and 24.5°
 * over a 41,000-year Milankovitch cycle. The current value is approximately 23.44°.
 */
public final class EarthAxialTilt {
    private EarthAxialTilt() {}

    private static final double OBLIQUITY_J2000_DEG = 23.439291;
    private static final double OBLIQUITY_RATE_DEG_PER_CENTURY = 0.013004;
    private static final double DAYS_PER_CENTURY = 36525.0;

    /**
     * Earth's axial tilt in degrees at the given moment.
     */
    public static double tiltDegrees(ZonedDateTime zdt) {
        double T = DateUtils.daysSinceJ2000(zdt) / DAYS_PER_CENTURY;
        return OBLIQUITY_J2000_DEG - OBLIQUITY_RATE_DEG_PER_CENTURY * T;
    }

    /**
     * Earth's axial tilt in degrees at the current moment.
     */
    public static double tiltDegreesNow() {
        return tiltDegrees(ZonedDateTime.now(ZoneId.systemDefault()));
    }
}
