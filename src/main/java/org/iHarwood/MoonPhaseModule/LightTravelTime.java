package org.iHarwood.MoonPhaseModule;


/**
 * Calculates light travel time from the Sun to various solar system objects.
 *
 * Speed of light: 299,792.458 km/s
 * 1 AU = 149,597,870.7 km
 * Light travel time for 1 AU ~ 499.0 seconds ~ 8 min 19 sec
 */
public final class LightTravelTime {
    private LightTravelTime() {}

    private static final double C_KM_S = 299_792.458;
    private static final double KM_PER_AU = 149_597_870.7;

    public static double travelTimeSeconds(double distanceAU) {
        return (distanceAU * KM_PER_AU) / C_KM_S;
    }

    public static double travelTimeMinutes(double distanceAU) {
        return travelTimeSeconds(distanceAU) / 60.0;
    }

    /**
     * Format light travel time as a human-readable string (e.g. "8m 19s" or "4h 12m").
     */
    public static String formatTravelTime(double distanceAU) {
        double totalSeconds = travelTimeSeconds(distanceAU);
        if (totalSeconds < 60) {
            return String.format("%.0fs", totalSeconds);
        }
        long totalMinutes = (long) (totalSeconds / 60.0);
        long secs = (long) (totalSeconds % 60.0);

        if (totalMinutes < 60) {
            return String.format("%dm %ds", totalMinutes, secs);
        }
        long hours = totalMinutes / 60;
        long mins = totalMinutes % 60;
        return String.format("%dh %dm", hours, mins);
    }

    // --- Convenience methods using the Planets registry ---

    public static String sunToEarthNow() {
        return formatTravelTime(SunDistance.distanceAUNow());
    }

    public static String sunToMercuryNow() {
        return formatTravelTime(Planets.MERCURY.heliocentricDistanceAUNow());
    }

    public static String sunToVenusNow() {
        return formatTravelTime(Planets.VENUS.heliocentricDistanceAUNow());
    }

    public static String sunToMarsNow() {
        return formatTravelTime(Planets.MARS.heliocentricDistanceAUNow());
    }

    public static String sunToJupiterNow() {
        return formatTravelTime(Planets.JUPITER.heliocentricDistanceAUNow());
    }

    public static String sunToSaturnNow() {
        return formatTravelTime(Planets.SATURN.heliocentricDistanceAUNow());
    }

    public static String sunToUranusNow() {
        return formatTravelTime(Planets.URANUS.heliocentricDistanceAUNow());
    }

    public static String sunToNeptuneNow() {
        return formatTravelTime(Planets.NEPTUNE.heliocentricDistanceAUNow());
    }

    public static String sunToPlutoNow() {
        return formatTravelTime(Planets.PLUTO.heliocentricDistanceAUNow());
    }

    public static String sunToVoyager1Now() {
        return formatTravelTime(VoyagerDistance.heliocentricDistanceV1AUNow());
    }

    public static String sunToVoyager2Now() {
        return formatTravelTime(VoyagerDistance.heliocentricDistanceV2AUNow());
    }
}
