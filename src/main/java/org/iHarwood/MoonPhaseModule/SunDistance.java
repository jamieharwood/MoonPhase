// java
package org.iHarwood.MoonPhaseModule;

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

    public static double distanceAUNow() {
        return distanceAU(ZonedDateTime.now(ZoneId.systemDefault()));
    }

    public static double distanceAU(ZonedDateTime zdt) {
        int year = zdt.getYear();
        int month = zdt.getMonthValue();
        int day = zdt.getDayOfMonth();
        int hour = zdt.getHour();
        int minute = zdt.getMinute();
        int second = zdt.getSecond();

        // Fliegel–Van Flandern integer Julian Day Number (JDN)
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

        // Fractional day: JD uses day fraction where 0.0 is midnight UTC and J2000.0 is 2000-01-01 12:00 TT.
        // Use (hour - 12) so JD corresponds to astronomical convention (day starts at noon for JD integer).
        double fracDay = (hour - 12) / 24.0 + minute / 1440.0 + second / 86400.0;
        double jd = jdn + fracDay;

        // Days since J2000.0
        double d = jd - 2451545.0;

        // Mean anomaly in degrees, then to radians
        double Mdeg = (357.529 + 0.98560028 * d) % 360.0;
        if (Mdeg < 0) Mdeg += 360.0;
        double M = Math.toRadians(Mdeg);

        // Approximate radius (AU)
        double r = 1.00014 - 0.01671 * Math.cos(M) - 0.00014 * Math.cos(2 * M);
        return r;
    }

    public static double[] minMaxDistanceAUNow() {
        return minMaxDistanceAU(ZonedDateTime.now(ZoneId.systemDefault()));
    }

    public static double[] minMaxDistanceAU(ZonedDateTime zdt) {
        ZonedDateTime start = zdt.withHour(0).withMinute(0).withSecond(0).withNano(0);
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < 365; i++) {
            double r = distanceAU(start.plusDays(i));
            if (r < min) min = r;
            if (r > max) max = r;
        }
        return new double[] { min, max };
    }

    /*public static double[] minMaxDistanceAU(ZonedDateTime zdt) {
        double[] range = new double[2];
        for (int i = 0; i < 365; i++) {
            range[0] = Math.min(range[0], distanceAU(zdt.plusDays(i)));
            range[1] = Math.max(range[1], distanceAU(zdt.plusDays(i)));
        }
        return range;
    }*/
}
