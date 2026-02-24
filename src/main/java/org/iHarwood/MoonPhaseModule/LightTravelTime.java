package org.iHarwood.MoonPhaseModule;


/**
 * Calculates light travel time from Earth to various solar system objects.
 *
 * Speed of light: 299,792.458 km/s
 * 1 AU = 149,597,870.7 km
 * Light travel time for 1 AU ≈ 499.0 seconds ≈ 8 min 19 sec
 */
public final class LightTravelTime {
    private LightTravelTime() {}

    // Speed of light in km/s
    private static final double C_KM_S = 299_792.458;

    // 1 AU in km
    private static final double KM_PER_AU = 149_597_870.7;

    /**
     * Light travel time in seconds for a given distance in AU.
     */
    public static double travelTimeSeconds(double distanceAU) {
        return (distanceAU * KM_PER_AU) / C_KM_S;
    }

    /**
     * Light travel time in minutes for a given distance in AU.
     */
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

    // --- Convenience methods for current distances ---

    /** Light travel time from Sun to Earth right now. */
    public static String sunToEarthNow() {
        return formatTravelTime(SunDistance.distanceAUNow());
    }

    /** Light travel time from Earth to Mars right now. */
    public static String earthToMarsNow() {
        return formatTravelTime(MarsDistance.distanceAUNow());
    }

    /** Light travel time from Earth to Jupiter right now. */
    public static String earthToJupiterNow() {
        return formatTravelTime(JupiterDistance.distanceAUNow());
    }

    /** Light travel time from Earth to Saturn right now. */
    public static String earthToSaturnNow() {
        return formatTravelTime(SaturnDistance.distanceAUNow());
    }

    /** Light travel time from Earth to Voyager 1 right now. */
    public static String earthToVoyager1Now() {
        return formatTravelTime(VoyagerDistance.distanceFromEarthV1AUNow());
    }

    /** Light travel time from Earth to Voyager 2 right now. */
    public static String earthToVoyager2Now() {
        return formatTravelTime(VoyagerDistance.distanceFromEarthV2AUNow());
    }
}

