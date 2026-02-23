// java
package org.iHarwood.MoonPhaseModule;

import java.time.Year;
import java.time.ZonedDateTime;
import java.time.ZoneId;

/**
 Simple, fast approximation of Sun-Earth distance (AU).
 Uses JD relative to J2000.0 and an approximate mean-anomaly-based formula:
   r ≈ 1.00014 - 0.01671*cos(M) - 0.00014*cos(2M)
 This is accurate to a few 1e-4 AU which is fine for display/CLI purposes.
 * astronomical unit (AU)
 * https://en.wikipedia.org/wiki/Astronomical_unit
**/
public final class SunDistance {
    private SunDistance() {}

    // Orbital constants for Earth's elliptical orbit
    private static final double EARTH_ORBIT_MEAN_AU      = 1.00014;
    private static final double EARTH_ORBIT_ECCENTRICITY = 0.01671;
    private static final double EARTH_ORBIT_CORRECTION   = 0.00014;

    public static double distanceAUNow() {
        return distanceAU(ZonedDateTime.now(ZoneId.systemDefault()));
    }

    public static double distanceAU(ZonedDateTime zdt) {
        // Days since J2000.0 (centralized calculation to avoid duplication)
        double d = DateUtils.daysSinceJ2000(zdt);

        // Mean anomaly in degrees, then to radians
        double Mdeg = (DateUtils.EARTH_MEAN_ANOMALY_J2000_DEG + DateUtils.EARTH_MEAN_MOTION_DEG_PER_DAY * d) % 360.0;
        if (Mdeg < 0) Mdeg += 360.0;
        double M = Math.toRadians(Mdeg);

        // Approximate radius (AU)
        double r = EARTH_ORBIT_MEAN_AU - EARTH_ORBIT_ECCENTRICITY * Math.cos(M) - EARTH_ORBIT_CORRECTION * Math.cos(2 * M);
        return r;
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
            double r = distanceAU(start.plusDays(i));
            if (r < min) min = r;
            if (r > max) max = r;
        }
        return new double[] { min, max };
    }
}
