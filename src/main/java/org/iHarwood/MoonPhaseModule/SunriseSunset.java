package org.iHarwood.MoonPhaseModule;

import java.time.LocalDate;

/**
 * Sunrise and sunset times (UTC) using the real solar position for the date
 * ({@link SolarPosition}: NOAA algorithm, declination and equation of time) and the
 * official sunrise definition (Sun's upper limb on the horizon, with standard refraction).
 *
 * Each event is refined once using the Sun's position at the estimated event time.
 * Accuracy: about ±1 minute of almanac values at mid latitudes.
 *
 * Special return values: "Polar Night" or "Midnight Sun" for extreme latitudes.
 */
public final class SunriseSunset {

    private static final double POLAR_NIGHT = Double.NEGATIVE_INFINITY;
    private static final double MIDNIGHT_SUN = Double.POSITIVE_INFINITY;
    private static final int REFINEMENT_PASSES = 2;

    private SunriseSunset() {}

    /**
     * Returns sunrise time as "HH:mm" UTC string.
     * Returns "Polar Night" or "Midnight Sun" at extreme latitudes.
     */
    public static String sunriseUtc(LocalDate date, double latitudeDeg, double longitudeDeg) {
        return format(eventUtcHours(date, latitudeDeg, longitudeDeg, true));
    }

    /**
     * Returns sunset time as "HH:mm" UTC string.
     * Returns "Polar Night" or "Midnight Sun" at extreme latitudes.
     */
    public static String sunsetUtc(LocalDate date, double latitudeDeg, double longitudeDeg) {
        return format(eventUtcHours(date, latitudeDeg, longitudeDeg, false));
    }

    /** Convenience wrappers that use current date */
    public static String sunriseUtcNow(double latitudeDeg, double longitudeDeg) {
        return sunriseUtc(LocalDate.now(), latitudeDeg, longitudeDeg);
    }

    public static String sunsetUtcNow(double latitudeDeg, double longitudeDeg) {
        return sunsetUtc(LocalDate.now(), latitudeDeg, longitudeDeg);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * UTC hours after midnight of the event, or a polar sentinel.
     * The first pass samples the Sun at mean solar noon; the second at the estimated event time.
     *
     * @param longitudeDeg degrees east (negative = west)
     */
    private static double eventUtcHours(LocalDate date, double latitudeDeg, double longitudeDeg, boolean sunrise) {
        double longitudeOffset = -longitudeDeg / 15.0;
        double t = 12.0 + longitudeOffset;
        for (int pass = 0; pass < REFINEMENT_PASSES; pass++) {
            SolarPosition.Result sun = SolarPosition.at(date, t);
            double halfDay = SolarPosition.halfDayHours(latitudeDeg, sun.declinationRad());
            if (halfDay <= 0.0) return POLAR_NIGHT;
            if (halfDay >= 12.0) return MIDNIGHT_SUN;
            double solarNoon = 12.0 + longitudeOffset - sun.equationOfTimeMinutes() / 60.0;
            t = sunrise ? solarNoon - halfDay : solarNoon + halfDay;
        }
        return t;
    }

    private static String format(double hours) {
        if (hours == POLAR_NIGHT) return "Polar Night";
        if (hours == MIDNIGHT_SUN) return "Midnight Sun";
        return formatHour(hours);
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
