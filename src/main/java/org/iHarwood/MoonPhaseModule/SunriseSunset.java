package org.iHarwood.MoonPhaseModule;

import java.time.LocalDate;
import java.time.Year;

/**
 * Calculates approximate sunrise and sunset times using solar declination.
 *
 * Uses the same declination formula as DayLight.java for consistency.
 * Results are in UTC and depend on both latitude and longitude.
 * Accuracy: ±5 minutes (simplified equation of time used).
 *
 * Special return values: "Polar Night" or "Midnight Sun" for extreme latitudes.
 */
public final class SunriseSunset {

    private static final double AXIAL_TILT_DEG = 23.44;

    private SunriseSunset() {}

    /**
     * Returns sunrise time as "HH:mm" UTC string.
     * Returns "Polar Night" or "Midnight Sun" at extreme latitudes.
     */
    public static String sunriseUtc(LocalDate date, double latitudeDeg, double longitudeDeg) {
        double halfDay = halfDayHours(date, latitudeDeg);
        if (halfDay < 0) return halfDay == POLAR_NIGHT ? "Polar Night" : "Midnight Sun";
        double solarNoon = solarNoonUtc(date, longitudeDeg);
        return formatHour(solarNoon - halfDay);
    }

    /**
     * Returns sunset time as "HH:mm" UTC string.
     * Returns "Polar Night" or "Midnight Sun" at extreme latitudes.
     */
    public static String sunsetUtc(LocalDate date, double latitudeDeg, double longitudeDeg) {
        double halfDay = halfDayHours(date, latitudeDeg);
        if (halfDay < 0) return halfDay == POLAR_NIGHT ? "Polar Night" : "Midnight Sun";
        double solarNoon = solarNoonUtc(date, longitudeDeg);
        return formatHour(solarNoon + halfDay);
    }

    /** Convenience wrappers that use current date */
    public static String sunriseUtcNow(double latitudeDeg, double longitudeDeg) {
        return sunriseUtc(LocalDate.now(), latitudeDeg, longitudeDeg);
    }

    public static String sunsetUtcNow(double latitudeDeg, double longitudeDeg) {
        return sunsetUtc(LocalDate.now(), latitudeDeg, longitudeDeg);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private static final double POLAR_NIGHT = -1.0;
    private static final double MIDNIGHT_SUN = -2.0;

    /**
     * Returns half the daylight duration in hours (offset from solar noon).
     * Returns POLAR_NIGHT or MIDNIGHT_SUN sentinel values.
     */
    private static double halfDayHours(LocalDate date, double latitudeDeg) {
        int n = date.getDayOfYear();
        double lat = Math.toRadians(latitudeDeg);
        double daysInYear = Year.of(date.getYear()).length();
        double decl = Math.toRadians(AXIAL_TILT_DEG) * Math.sin(2.0 * Math.PI * (284 + n) / daysInYear);
        double x = -Math.tan(lat) * Math.tan(decl);
        if (x >= 1.0) return POLAR_NIGHT;
        if (x <= -1.0) return MIDNIGHT_SUN;
        // hourAngle in radians → convert to hours: hours = radians * (12 / π)
        return Math.acos(x) * (12.0 / Math.PI);
    }

    /**
     * Solar noon in UTC hours for the given longitude and date.
     * Includes a simplified equation of time correction.
     *
     * @param longitudeDeg degrees east (negative = west)
     */
    private static double solarNoonUtc(LocalDate date, double longitudeDeg) {
        double longitudeOffset = -longitudeDeg / 15.0; // hours
        double eot = equationOfTimeHours(date.getDayOfYear());
        return 12.0 + longitudeOffset - eot;
    }

    /**
     * Simplified equation of time in hours.
     * Accounts for Earth's elliptical orbit and axial tilt (~±16 min accuracy).
     */
    private static double equationOfTimeHours(int dayOfYear) {
        double B = Math.toRadians(360.0 / 365.0 * (dayOfYear - 81));
        double eotMinutes = 9.87 * Math.sin(2 * B) - 7.53 * Math.cos(B) - 1.5 * Math.sin(B);
        return eotMinutes / 60.0;
    }

    private static String formatHour(double decimalHour) {
        decimalHour = ((decimalHour % 24) + 24) % 24;
        int hours = (int) decimalHour;
        int minutes = (int) Math.round((decimalHour - hours) * 60);
        if (minutes >= 60) { hours++; minutes -= 60; }
        if (hours >= 24) hours -= 24;
        return String.format("%02d:%02d", hours, minutes);
    }
}

