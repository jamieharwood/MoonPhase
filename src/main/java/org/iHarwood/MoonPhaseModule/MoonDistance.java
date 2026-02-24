package org.iHarwood.MoonPhaseModule;

import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Approximate Moon distance from Earth in kilometres.
 *
 * The Moon's orbit is elliptical with:
 *   Mean distance:  384,400 km
 *   Perigee:       ~356,500 km (closest)
 *   Apogee:        ~406,700 km (farthest)
 *
 * Uses a simplified model based on the Moon's mean anomaly to approximate
 * the distance variation over its ~27.3-day orbital period.
 */
public final class MoonDistance {
    private MoonDistance() {}

    // Mean distance in km
    private static final double MEAN_DISTANCE_KM = 384_400.0;

    // Semi-amplitude of distance variation (~26,200 km)
    private static final double DISTANCE_AMPLITUDE_KM = 21_000.0;

    // Moon's mean anomaly parameters (from J2000)
    // Mean anomaly at J2000 (degrees)
    private static final double M0_MOON_DEG = 134.9634;
    // Mean motion (degrees per day) — sidereal month ~27.3217 days
    private static final double N_MOON_DEG_PER_DAY = 13.0649929509;

    /**
     * Approximate Moon distance from Earth in km at the given moment.
     */
    public static double distanceKm(ZonedDateTime zdt) {
        double d = DateUtils.daysSinceJ2000(zdt);
        double mDeg = DateUtils.normalizeAngle(M0_MOON_DEG + N_MOON_DEG_PER_DAY * d);
        // Distance ≈ mean - amplitude * cos(M)
        // At M=0 (perigee), cos=1 → closest; at M=180 (apogee), cos=-1 → farthest
        return MEAN_DISTANCE_KM - DISTANCE_AMPLITUDE_KM * Math.cos(Math.toRadians(mDeg));
    }

    /**
     * Approximate Moon distance from Earth in km right now.
     */
    public static double distanceKmNow() {
        return distanceKm(ZonedDateTime.now(ZoneId.systemDefault()));
    }

    /**
     * Approximate Moon distance in miles right now.
     */
    public static double distanceMilesNow() {
        return distanceKmNow() / 1.60934;
    }

    /**
     * Get min/max Moon distance over the current lunar cycle (roughly 30 days).
     */
    public static double[] minMaxDistanceKmNow() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime start = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        // Sample over 30 days (one lunar cycle)
        for (int i = 0; i < 30; i++) {
            double d = distanceKm(start.plusDays(i));
            if (d < min) min = d;
            if (d > max) max = d;
        }
        return new double[]{min, max};
    }

    /**
     * Format distance as a readable string with comma separators.
     */
    public static String formatDistanceKm(double km) {
        return String.format("%,.0f km", km);
    }
}

