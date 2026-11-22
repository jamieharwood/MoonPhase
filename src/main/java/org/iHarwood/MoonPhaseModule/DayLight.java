package org.iHarwood.MoonPhaseModule;

import java.time.LocalDate;

/**
 * Day length (sunrise to sunset) using the real solar position for the date
 * ({@link SolarPosition}) and the official sunrise definition (Sun's upper limb on the
 * horizon, with standard refraction), so results match published almanac day lengths.
 */
public final class DayLight {

    /** Time of day (UTC hours) at which the Sun's declination is sampled when no longitude is known. */
    private static final double SAMPLE_HOUR_UTC = 12.0;

    private DayLight() {}

    /**
     * Compute day length in hours for the given date and latitude (degrees).
     * Returns 0 for polar night and 24 for midnight sun.
     */
    public static double dayLengthHours(LocalDate date, double latitudeDeg) {
        double declination = SolarPosition.at(date, SAMPLE_HOUR_UTC).declinationRad();
        return 2.0 * SolarPosition.halfDayHours(latitudeDeg, declination);
    }

    /**
     * Shortest and longest possible day length at this latitude, i.e. at the solstices,
     * using the current obliquity of the ecliptic.
     *
     * @return {min, max} in hours
     */
    public static double[] minMaxDayLengthHoursNow(double latitudeDeg) {
        double tilt = Math.toRadians(EarthAxialTilt.tiltDegreesNow());
        double a = 2.0 * SolarPosition.halfDayHours(latitudeDeg, tilt);
        double b = 2.0 * SolarPosition.halfDayHours(latitudeDeg, -tilt);
        return new double[]{Math.min(a, b), Math.max(a, b)};
    }
}
