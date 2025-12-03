// java
package org.iHarwood.MoonPhaseModule;

import java.time.ZonedDateTime;
import java.time.ZoneId;

/**
 * Rough approximation of Earth–Mars distance (AU).
 *
 * Uses:
 *  - Earth radius from SunDistance.distanceAU(...)
 *  - Mars semi-major axis and eccentricity with a simple mean-anomaly-based radius approximation
 *  - Approximate longitudes built from mean anomaly + longitude of perihelion
 *
 * This is an approximation for CLI/display use only.
 */
public final class MarsDistance {
    private MarsDistance() {}

    // Mars orbital parameters (approx)
    private static final double A_MARS_AU = 1.523679;
    private static final double E_MARS = 0.0934;
    // Mean anomaly at J2000 (deg) and mean motion (deg/day) (approx)
    private static final double M0_MARS_DEG = 19.3870;
    private static final double N_MARS_DEG_PER_DAY = 0.5240207766;
    // Longitude of perihelion (deg) for Earth and Mars (approx)
    private static final double W_EARTH_DEG = 102.93735;
    private static final double W_MARS_DEG = 336.04084;

    public static double distanceAUNow() {
        return distanceAU(ZonedDateTime.now(ZoneId.systemDefault()));
    }

    public static double distanceAU(ZonedDateTime zdt) {
        double d = daysSinceJ2000(zdt);

        // Earth's heliocentric radius (AU) from existing helper
        double rEarth = SunDistance.distanceAU(zdt);

        // Earth's mean anomaly (deg) — same linear formula used in SunDistance
        double mEarthDeg = (357.529 + 0.98560028 * d) % 360.0;
        if (mEarthDeg < 0) mEarthDeg += 360.0;
        double longEarthDeg = mEarthDeg + W_EARTH_DEG;

        // Mars mean anomaly and approximate radius
        double mMarsDeg = (M0_MARS_DEG + N_MARS_DEG_PER_DAY * d) % 360.0;
        if (mMarsDeg < 0) mMarsDeg += 360.0;
        // approximate Mars radius using first-order radial approximation r ≈ a * (1 - e * cos(M))
        double rMars = A_MARS_AU * (1.0 - E_MARS * Math.cos(Math.toRadians(mMarsDeg)));
        double longMarsDeg = mMarsDeg + W_MARS_DEG;

        double longEarthRad = Math.toRadians(longEarthDeg);
        double longMarsRad = Math.toRadians(longMarsDeg);

        // Distance using law of cosines in the ecliptic plane
        double delta = Math.abs(longMarsRad - longEarthRad);
        double dist = Math.sqrt(rEarth * rEarth + rMars * rMars - 2.0 * rEarth * rMars * Math.cos(delta));
        return dist;
    }

    public static double[] minMaxDistanceAUNow() {
        return minMaxDistanceAU(ZonedDateTime.now(ZoneId.systemDefault()));
    }

    public static double[] minMaxDistanceAU(ZonedDateTime zdt) {
        ZonedDateTime start = zdt.withHour(0).withMinute(0).withSecond(0).withNano(0);
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < 365; i++) {
            double d = distanceAU(start.plusDays(i));
            if (d < min) min = d;
            if (d > max) max = d;
        }
        return new double[] { min, max };
    }

    // Compute days since J2000.0 (same approach as in SunDistance)
    private static double daysSinceJ2000(ZonedDateTime zdt) {
        int year = zdt.getYear();
        int month = zdt.getMonthValue();
        int day = zdt.getDayOfMonth();
        int hour = zdt.getHour();
        int minute = zdt.getMinute();
        int second = zdt.getSecond();

        int a = (14 - month) / 12;
        int y = year + 4800 - a;
        int m = month + 12 * a - 3;
        boolean gregorian = (year > 1582) || (year == 1582 && (month > 10 || (month == 10 && day >= 15)));

        long jdn;
        if (gregorian) {
            jdn = day + (153 * m + 2) / 5 + 365L * y + y / 4 - y / 100 + y / 400 - 32045;
        } else {
            jdn = day + (153 * m + 2) / 5 + 365L * y + y / 4 - 32083;
        }

        double fracDay = (hour - 12) / 24.0 + minute / 1440.0 + second / 86400.0;
        double jd = jdn + fracDay;
        return jd - 2451545.0;
    }
}
