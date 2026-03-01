package org.iHarwood.MoonPhaseModule;

import java.time.ZonedDateTime;
import java.time.ZoneId;

/**
 * Rough approximation of Earth–Venus distance (AU).
 */
public final class VenusDistance {
    private VenusDistance() {}

    private static final double A_VENUS_AU = 0.72333566;
    private static final double E_VENUS = 0.00677672;
    private static final double M0_VENUS_DEG = 50.37663;
    private static final double N_VENUS_DEG_PER_DAY = 1.60213034;
    private static final double W_EARTH_DEG = 102.93735;
    private static final double W_VENUS_DEG = 54.85229;

    public static double distanceAUNow() {
        return distanceAU(ZonedDateTime.now(ZoneId.systemDefault()));
    }

    public static double heliocentricDistanceAUNow() {
        double d = DateUtils.daysSinceJ2000(ZonedDateTime.now(ZoneId.systemDefault()));
        double mDeg = DateUtils.normalizeAngle(M0_VENUS_DEG + N_VENUS_DEG_PER_DAY * d);
        return A_VENUS_AU * (1.0 - E_VENUS * Math.cos(Math.toRadians(mDeg)));
    }

    public static double distanceAU(ZonedDateTime zdt) {
        double d = DateUtils.daysSinceJ2000(zdt);

        double rEarth = SunDistance.distanceAU(zdt);
        double mEarthDeg = DateUtils.normalizeAngle(DateUtils.EARTH_MEAN_ANOMALY_J2000_DEG + DateUtils.EARTH_MEAN_MOTION_DEG_PER_DAY * d);
        double longEarthDeg = mEarthDeg + W_EARTH_DEG;

        double mVenusDeg = DateUtils.normalizeAngle(M0_VENUS_DEG + N_VENUS_DEG_PER_DAY * d);
        double rVenus = A_VENUS_AU * (1.0 - E_VENUS * Math.cos(Math.toRadians(mVenusDeg)));
        double longVenusDeg = mVenusDeg + W_VENUS_DEG;

        double longEarthRad = Math.toRadians(longEarthDeg);
        double longVenusRad = Math.toRadians(longVenusDeg);

        double delta = Math.abs(longVenusRad - longEarthRad);
        return Math.sqrt(rEarth * rEarth + rVenus * rVenus - 2.0 * rEarth * rVenus * Math.cos(delta));
    }
}
