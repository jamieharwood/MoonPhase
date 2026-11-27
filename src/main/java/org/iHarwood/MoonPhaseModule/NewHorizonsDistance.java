package org.iHarwood.MoonPhaseModule;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Distance of the New Horizons spacecraft from the Sun and from Earth, in AU.
 *
 * <p>Propagated from the JPL Horizons state vector at 2026-01-01 00:00 TDB
 * (heliocentric, J2000 ecliptic). See {@link SpacecraftTrajectory} for the accuracy notes.
 */
public final class NewHorizonsDistance {
    private NewHorizonsDistance() {}

    /** Horizons target -98, epoch JD 2461041.5 (2026-01-01 TDB). */
    private static final SpacecraftTrajectory NEW_HORIZONS = new SpacecraftTrajectory(2453754.5, 2461041.5, // launched 2006-01-19
            20.00963528566975, -60.30445704637835, 2.212929493946444,
            0.003070495042525652, -0.007229267480329574, 0.0002845557285883782);

    public static double heliocentricDistanceAU(ZonedDateTime zdt) {
        return NEW_HORIZONS.heliocentricDistanceAU(zdt);
    }

    public static double distanceFromEarthAU(ZonedDateTime zdt) {
        return NEW_HORIZONS.distanceFromEarthAU(zdt);
    }

    public static double heliocentricDistanceAUNow() {
        return heliocentricDistanceAU(ZonedDateTime.now(ZoneOffset.UTC));
    }

    public static double distanceFromEarthAUNow() {
        return distanceFromEarthAU(ZonedDateTime.now(ZoneOffset.UTC));
    }

    /** Heliocentric speed in km/s. */
    public static double speedKmPerSec() {
        return NEW_HORIZONS.speedKmPerSec();
    }
}
