package org.iHarwood.MoonPhaseModule;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Earth–Moon distance (centre to centre) in kilometres.
 *
 * Computed by {@link MoonEphemeris}: the Keplerian lunar orbit plus the evection and
 * variation terms, which alone move the distance by up to ~6,600 km and are why a
 * single-cosine model can be thousands of km out.
 *
 *   Mean distance:  384,400 km
 *   Perigee range:  ~356,400–370,400 km
 *   Apogee range:   ~404,000–406,700 km
 */
public final class MoonDistance {
    private MoonDistance() {}

    /** Extreme perigee / apogee distances (km), for log bars. */
    private static final double MIN_DISTANCE_KM = 356_400.0;
    private static final double MAX_DISTANCE_KM = 406_700.0;

    public static double distanceKm(ZonedDateTime zdt) {
        return MoonEphemeris.at(DateUtils.julianDate(zdt)).moonDistanceKm();
    }

    public static double distanceKmNow() {
        return distanceKm(ZonedDateTime.now(ZoneOffset.UTC));
    }

    public static double distanceMilesNow() {
        return distanceKmNow() / 1.60934;
    }

    /**
     * Extreme perigee / apogee distances, used as the range for log bars.
     */
    public static double[] minMaxDistanceKmNow() {
        return new double[]{MIN_DISTANCE_KM, MAX_DISTANCE_KM};
    }

    public static String formatDistanceKm(double km) {
        return String.format("%,.0f km", km);
    }
}
