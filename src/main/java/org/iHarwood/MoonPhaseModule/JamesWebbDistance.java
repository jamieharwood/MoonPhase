package org.iHarwood.MoonPhaseModule;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Approximate distance of the James Webb Space Telescope (JWST) from Earth in km.
 *
 * JWST operates in a halo orbit around the Sun-Earth L2 Lagrange point,
 * approximately 1,500,000 km (1.5 million km) from Earth.
 *
 * The halo orbit has an amplitude of ~500,000 km with a period of ~6 months,
 * so the Earth–JWST distance oscillates roughly between 1.0 and 2.0 million km.
 * This class models that oscillation using a sinusoidal approximation anchored
 * to the known reference distance at the L2 insertion epoch (2022-01-24).
 */
public final class JamesWebbDistance {

    private JamesWebbDistance() {}

    /** Mean Earth–L2 distance in km. */
    private static final double L2_MEAN_KM = 1_500_000.0;

    /** Halo-orbit semi-amplitude in km (~500,000 km). */
    private static final double HALO_AMPLITUDE_KM = 500_000.0;

    /** Halo orbit period in days (~6 months). */
    private static final double HALO_PERIOD_DAYS = 180.0;

    /** Reference epoch: JWST L2 insertion. */
    private static final ZonedDateTime EPOCH =
            ZonedDateTime.of(2022, 1, 24, 0, 0, 0, 0, ZoneOffset.UTC);

    private static double distanceKm(ZonedDateTime zdt) {
        double daysSince = Duration.between(EPOCH, zdt.withZoneSameInstant(ZoneOffset.UTC))
                                   .toSeconds() / 86_400.0;
        double phase = (2.0 * Math.PI * daysSince) / HALO_PERIOD_DAYS;
        return L2_MEAN_KM + HALO_AMPLITUDE_KM * Math.sin(phase);
    }

    /**
     * Returns the approximate distance of JWST from Earth at the given time, in km.
     */
    public static double distanceKmAt(ZonedDateTime zdt) {
        return distanceKm(zdt);
    }

    /**
     * Returns the approximate distance of JWST from Earth right now, in km.
     */
    public static double distanceKmNow() {
        return distanceKm(ZonedDateTime.now(ZoneOffset.UTC));
    }
}

