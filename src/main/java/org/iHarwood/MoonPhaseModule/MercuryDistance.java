package org.iHarwood.MoonPhaseModule;

import java.time.ZonedDateTime;
import java.time.ZoneId;

/**
 * Rough approximation of Earth–Mercury distance (AU).
 */
public final class MercuryDistance {
    private MercuryDistance() {}

    private static final double A_MERCURY_AU = 0.38709927;
    private static final double E_MERCURY = 0.20563593;
    private static final double M0_MERCURY_DEG = 174.7947670;
    private static final double N_MERCURY_DEG_PER_DAY = 4.09233445;
    private static final double W_EARTH_DEG = 102.93735;
    private static final double W_MERCURY_DEG = 29.12427;

    public static double distanceAUNow() {
        return distanceAU(ZonedDateTime.now(ZoneId.systemDefault()));
    }

    public static double distanceAU(ZonedDateTime zdt) {
        double d = DateUtils.daysSinceJ2000(zdt);

        double rEarth = SunDistance.distanceAU(zdt);
        double mEarthDeg = DateUtils.normalizeAngle(DateUtils.EARTH_MEAN_ANOMALY_J2000_DEG + DateUtils.EARTH_MEAN_MOTION_DEG_PER_DAY * d);
        double longEarthDeg = mEarthDeg + W_EARTH_DEG;

        double mMercuryDeg = DateUtils.normalizeAngle(M0_MERCURY_DEG + N_MERCURY_DEG_PER_DAY * d);
        double rMercury = A_MERCURY_AU * (1.0 - E_MERCURY * Math.cos(Math.toRadians(mMercuryDeg)));
        double longMercuryDeg = mMercuryDeg + W_MERCURY_DEG;

        double longEarthRad = Math.toRadians(longEarthDeg);
        double longMercuryRad = Math.toRadians(longMercuryDeg);

        double delta = Math.abs(longMercuryRad - longEarthRad);
        return Math.sqrt(rEarth * rEarth + rMercury * rMercury - 2.0 * rEarth * rMercury * Math.cos(delta));
    }
}
