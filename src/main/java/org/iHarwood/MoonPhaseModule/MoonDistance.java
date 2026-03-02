package org.iHarwood.MoonPhaseModule;

import java.time.ZoneOffset;
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

    private static final double MEAN_DISTANCE_KM = 384_400.0;
    private static final double DISTANCE_AMPLITUDE_KM = 21_000.0;
    private static final double M0_MOON_DEG = 134.9634;
    private static final double N_MOON_DEG_PER_DAY = 13.0649929509;

    public static double distanceKm(ZonedDateTime zdt) {
        double d = DateUtils.daysSinceJ2000(zdt);
        double mDeg = DateUtils.normalizeAngle(M0_MOON_DEG + N_MOON_DEG_PER_DAY * d);
        return MEAN_DISTANCE_KM - DISTANCE_AMPLITUDE_KM * Math.cos(Math.toRadians(mDeg));
    }

    public static double distanceKmNow() {
        return distanceKm(ZonedDateTime.now(ZoneOffset.UTC));
    }

    public static double distanceMilesNow() {
        return distanceKmNow() / 1.60934;
    }

    /**
     * Get min/max Moon distance over the current lunar cycle (roughly 30 days).
     */
    public static double[] minMaxDistanceKmNow() {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        ZonedDateTime start = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < 30; i++) {
            double d = distanceKm(start.plusDays(i));
            if (d < min) min = d;
            if (d > max) max = d;
        }
        return new double[]{min, max};
    }

    public static String formatDistanceKm(double km) {
        return String.format("%,.0f km", km);
    }
}
