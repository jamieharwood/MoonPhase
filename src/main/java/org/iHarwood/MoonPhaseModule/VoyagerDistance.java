package org.iHarwood.MoonPhaseModule;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Distances of Voyager 1 and 2 from the Sun and from Earth, in AU.
 *
 * <p>Propagated from JPL Horizons state vectors at 2026-01-01 00:00 TDB
 * (heliocentric, J2000 ecliptic). See {@link SpacecraftTrajectory} for the accuracy notes.
 */
public final class VoyagerDistance {
    private VoyagerDistance() {}

    /** Horizons target -31, epoch JD 2461041.5 (2026-01-01 TDB). */
    private static final SpacecraftTrajectory VOYAGER_1 = new SpacecraftTrajectory(2443391.5, 2461041.5, // launched 1977-09-05
            -31.83641491929577, -134.6784911967567, 97.44481075134993,
            -0.001197151937928288, -0.00786107792125082, 0.005680005843310299);

    /** Horizons target -32, epoch JD 2461041.5 (2026-01-01 TDB). */
    private static final SpacecraftTrajectory VOYAGER_2 = new SpacecraftTrajectory(2443375.5, 2461041.5, // launched 1977-08-20
            39.22753395002931, -103.9915540306074, -87.91866331046981,
            0.002427683334171075, -0.005395071892829595, -0.006538419567248777);

    public static double heliocentricDistanceV1AU(ZonedDateTime zdt) {
        return VOYAGER_1.heliocentricDistanceAU(zdt);
    }

    public static double heliocentricDistanceV2AU(ZonedDateTime zdt) {
        return VOYAGER_2.heliocentricDistanceAU(zdt);
    }

    public static double distanceFromEarthV1AU(ZonedDateTime zdt) {
        return VOYAGER_1.distanceFromEarthAU(zdt);
    }

    public static double distanceFromEarthV2AU(ZonedDateTime zdt) {
        return VOYAGER_2.distanceFromEarthAU(zdt);
    }

    public static double heliocentricDistanceV1AUNow() {
        return heliocentricDistanceV1AU(ZonedDateTime.now(ZoneOffset.UTC));
    }

    public static double heliocentricDistanceV2AUNow() {
        return heliocentricDistanceV2AU(ZonedDateTime.now(ZoneOffset.UTC));
    }

    public static double distanceFromEarthV1AUNow() {
        return distanceFromEarthV1AU(ZonedDateTime.now(ZoneOffset.UTC));
    }

    public static double distanceFromEarthV2AUNow() {
        return distanceFromEarthV2AU(ZonedDateTime.now(ZoneOffset.UTC));
    }
}
