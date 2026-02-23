package org.iHarwood.MoonPhaseModule;

import java.time.ZonedDateTime;

/**
 * Shared utility for astronomical date/time calculations.
 * Centralizes Julian Day Number and other common calculations to avoid duplication.
 */
public final class DateUtils {
    private DateUtils() {}

    // Earth orbital elements at J2000.0 — shared by SunDistance and MarsDistance
    public static final double EARTH_MEAN_ANOMALY_J2000_DEG  = 357.529;
    public static final double EARTH_MEAN_MOTION_DEG_PER_DAY = 0.98560028;

    /**
     * Normalise an angle in degrees to [0, 360).
     */
    public static double normalizeAngle(double deg) {
        return ((deg % 360.0) + 360.0) % 360.0;
    }

    /**
     * Calculate days since J2000.0 (2000-01-01 12:00:00 TT).
     * Uses Fliegel-Van Flandern integer Julian Day Number algorithm.
     */
    public static double daysSinceJ2000(ZonedDateTime zdt) {
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

        // Fractional day
        double fracDay = (hour - 12) / 24.0 + minute / 1440.0 + second / 86400.0;
        double jd = jdn + fracDay;

        // Days since J2000.0
        return jd - 2451545.0;
    }

    /**
     * Convert local date (day, month, year) to Julian Day Number.
     * Uses Fliegel-Van Flandern algorithm with Gregorian/Julian calendar cutoff.
     */
    public static int dateToJulianDayNumber(int day, int month, int year) {
        int a = (14 - month) / 12;
        int y = year + 4800 - a;
        int m = month + 12 * a - 3;
        boolean gregorian = (year > 1582) || (year == 1582 && (month > 10 || (month == 10 && day >= 15)));

        if (gregorian) {
            return (int)(day + (153 * m + 2) / 5 + 365L * y + y / 4 - y / 100 + y / 400 - 32045);
        } else {
            return (int)(day + (153 * m + 2) / 5 + 365L * y + y / 4 - 32083);
        }
    }
}

