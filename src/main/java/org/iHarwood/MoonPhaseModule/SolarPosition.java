package org.iHarwood.MoonPhaseModule;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Sun position for a specific instant, using the NOAA solar calculator algorithm
 * (Meeus, "Astronomical Algorithms", low-precision solar coordinates).
 *
 * Unlike a day-of-year approximation, this tracks the real calendar drift of the seasons
 * (~6 h later each year, reset by leap days), orbital eccentricity and the slowly changing
 * obliquity, so the same calendar date gives slightly different results in different years.
 *
 * Accuracy: declination ~0.01°, equation of time ~0.1 min for 1900–2100, which puts
 * sunrise/sunset within about a minute of almanac values at mid latitudes.
 */
public final class SolarPosition {

    /**
     * Altitude of the Sun's centre at official sunrise/sunset: the upper limb on the horizon
     * (semi-diameter ~16') plus standard atmospheric refraction (~34').
     */
    public static final double SUNRISE_ALTITUDE_DEG = -0.833;

    private static final double DAYS_PER_CENTURY = 36525.0;

    private SolarPosition() {}

    /**
     * @param declinationRad        solar declination in radians
     * @param equationOfTimeMinutes apparent minus mean solar time, in minutes
     */
    public record Result(double declinationRad, double equationOfTimeMinutes) {}

    /** Sun position at the given instant (interpreted as UTC). */
    public static Result at(ZonedDateTime utc) {
        double t = DateUtils.daysSinceJ2000(utc.withZoneSameInstant(ZoneOffset.UTC)) / DAYS_PER_CENTURY;

        double meanLongitude = DateUtils.normalizeAngle(280.46646 + t * (36000.76983 + t * 0.0003032));
        double meanAnomaly = 357.52911 + t * (35999.05029 - 0.0001537 * t);
        double eccentricity = 0.016708634 - t * (0.000042037 + 0.0000001267 * t);

        double m = Math.toRadians(meanAnomaly);
        double equationOfCentre = Math.sin(m) * (1.914602 - t * (0.004817 + 0.000014 * t))
                + Math.sin(2 * m) * (0.019993 - 0.000101 * t)
                + Math.sin(3 * m) * 0.000289;

        double omega = Math.toRadians(125.04 - 1934.136 * t);
        double apparentLongitude = Math.toRadians(
                meanLongitude + equationOfCentre - 0.00569 - 0.00478 * Math.sin(omega));

        double meanObliquity = 23.0 + (26.0 + (21.448 - t * (46.815 + t * (0.00059 - t * 0.001813))) / 60.0) / 60.0;
        double obliquity = Math.toRadians(meanObliquity + 0.00256 * Math.cos(omega));

        double declination = Math.asin(Math.sin(obliquity) * Math.sin(apparentLongitude));

        double y = Math.pow(Math.tan(obliquity / 2.0), 2);
        double l0 = Math.toRadians(meanLongitude);
        double eotRad = y * Math.sin(2 * l0)
                - 2 * eccentricity * Math.sin(m)
                + 4 * eccentricity * y * Math.sin(m) * Math.cos(2 * l0)
                - 0.5 * y * y * Math.sin(4 * l0)
                - 1.25 * eccentricity * eccentricity * Math.sin(2 * m);
        double eotMinutes = 4.0 * Math.toDegrees(eotRad);

        return new Result(declination, eotMinutes);
    }

    /** Sun position at {@code hoursUtc} hours after midnight UTC on {@code date} (may be negative or >24). */
    public static Result at(LocalDate date, double hoursUtc) {
        return at(date.atStartOfDay(ZoneOffset.UTC).plusSeconds(Math.round(hoursUtc * 3600.0)));
    }

    /**
     * Hours between solar noon and sunrise (or sunset) for the official sunrise altitude.
     *
     * @return 0 for polar night, 12 for midnight sun, otherwise a value in (0, 12)
     */
    public static double halfDayHours(double latitudeDeg, double declinationRad) {
        double lat = Math.toRadians(latitudeDeg);
        double cosHourAngle = (Math.sin(Math.toRadians(SUNRISE_ALTITUDE_DEG)) - Math.sin(lat) * Math.sin(declinationRad))
                / (Math.cos(lat) * Math.cos(declinationRad));
        if (cosHourAngle >= 1.0) return 0.0;   // Sun never rises
        if (cosHourAngle <= -1.0) return 12.0; // Sun never sets
        return Math.toDegrees(Math.acos(cosHourAngle)) / 15.0;
    }
}
