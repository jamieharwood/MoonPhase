package org.iHarwood.MoonPhaseModule;

import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Rough approximation of Earth–Jupiter distance (AU).
 *
 * Uses mean-anomaly-based orbital mechanics similar to MarsDistance.
 * This is an approximation for CLI/display use only.
 */
public final class JupiterDistance {
    private JupiterDistance() {}

    // Jupiter orbital parameters (approx)
    private static final double A_JUPITER_AU = 5.2026;
    private static final double E_JUPITER = 0.0489;
    // Mean anomaly at J2000 (deg) and mean motion (deg/day)
    private static final double M0_JUPITER_DEG = 20.0202;
    private static final double N_JUPITER_DEG_PER_DAY = 0.0831294;
    // Longitude of perihelion (deg)
    private static final double W_EARTH_DEG = 102.93735;
    private static final double W_JUPITER_DEG = 14.75385;

    public static double distanceAUNow() {
        return distanceAU(ZonedDateTime.now(ZoneId.systemDefault()));
    }

    public static double distanceAU(ZonedDateTime zdt) {
        double d = DateUtils.daysSinceJ2000(zdt);

        // Earth's heliocentric radius (AU)
        double rEarth = SunDistance.distanceAU(zdt);

        // Earth's mean anomaly and longitude
        double mEarthDeg = DateUtils.normalizeAngle(
                DateUtils.EARTH_MEAN_ANOMALY_J2000_DEG + DateUtils.EARTH_MEAN_MOTION_DEG_PER_DAY * d);
        double longEarthDeg = mEarthDeg + W_EARTH_DEG;

        // Jupiter mean anomaly and approximate radius
        double mJupiterDeg = DateUtils.normalizeAngle(M0_JUPITER_DEG + N_JUPITER_DEG_PER_DAY * d);
        double rJupiter = A_JUPITER_AU * (1.0 - E_JUPITER * Math.cos(Math.toRadians(mJupiterDeg)));
        double longJupiterDeg = mJupiterDeg + W_JUPITER_DEG;

        double longEarthRad = Math.toRadians(longEarthDeg);
        double longJupiterRad = Math.toRadians(longJupiterDeg);

        // Distance using law of cosines in the ecliptic plane
        double delta = Math.abs(longJupiterRad - longEarthRad);
        return Math.sqrt(rEarth * rEarth + rJupiter * rJupiter
                - 2.0 * rEarth * rJupiter * Math.cos(delta));
    }

    public static double[] minMaxDistanceAUNow() {
        return minMaxDistanceAU(ZonedDateTime.now(ZoneId.systemDefault()));
    }

    public static double[] minMaxDistanceAU(ZonedDateTime zdt) {
        ZonedDateTime start = zdt.withHour(0).withMinute(0).withSecond(0).withNano(0);
        int daysInYear = Year.of(start.getYear()).length();
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < daysInYear; i++) {
            double d = distanceAU(start.plusDays(i));
            if (d < min) min = d;
            if (d > max) max = d;
        }
        return new double[]{min, max};
    }
}

