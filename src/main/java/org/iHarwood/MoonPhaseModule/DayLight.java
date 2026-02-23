package org.iHarwood.MoonPhaseModule;

import java.time.LocalDate;
import java.time.Year;

public final class DayLight {

    private static final double AXIAL_TILT_DEG = 23.44;

    private DayLight() {}

    // Compute day length in hours for given date and latitude (degrees) using solar declination approximation.
    public static double dayLengthHours(LocalDate date, double latitudeDeg) {
        int n = date.getDayOfYear();
        double lat = Math.toRadians(latitudeDeg);
        // Approximate solar declination (radians)
        double daysInYear = Year.of(date.getYear()).length();
        double decl = Math.toRadians(AXIAL_TILT_DEG) * Math.sin(2.0 * Math.PI * (284 + n) / daysInYear);
        double x = -Math.tan(lat) * Math.tan(decl);
        if (x >= 1.0) return 0.0;   // polar night
        if (x <= -1.0) return 24.0; // polar day
        double hourAngle = Math.acos(x); // radians
        return (24.0 / Math.PI) * hourAngle;
    }

    // Calculate min/max daylight hours analytically for given latitude.
    // Min/max occur near winter/summer solstices (Dec 21 / Jun 21).
    // For extreme latitudes (|lat| > 66.5°), polar day/night can reach 0 or 24 hours.
    public static double[] minMaxDayLengthAUNow(double latitudeDeg) {
        double lat = Math.toRadians(latitudeDeg);

        // Extreme polar cases
        double absLat = Math.abs(latitudeDeg);
        if (absLat >= 66.5) {
            return new double[]{0.0, 24.0}; // Polar regions can have polar day/night
        }

        // Calculate daylight at summer and winter solstices
        // Summer solstice: decl ≈ 23.44°, n ≈ 172
        // Winter solstice: decl ≈ -23.44°, n ≈ 355
        double maxDecl = Math.toRadians(AXIAL_TILT_DEG);
        double minDecl = Math.toRadians(-AXIAL_TILT_DEG);

        double maxLen = calculateDayLengthFromDeclination(lat, maxDecl);
        double minLen = calculateDayLengthFromDeclination(lat, minDecl);

        if (maxLen < minLen) {
            double tmp = maxLen;
            maxLen = minLen;
            minLen = tmp;
        }

        return new double[]{minLen, maxLen};
    }

    private static double calculateDayLengthFromDeclination(double latRad, double declRad) {
        double x = -Math.tan(latRad) * Math.tan(declRad);
        if (x >= 1.0) return 0.0;
        if (x <= -1.0) return 24.0;
        double hourAngle = Math.acos(x);
        return (24.0 / Math.PI) * hourAngle;
    }
}
