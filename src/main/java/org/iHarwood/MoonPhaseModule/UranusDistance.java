package org.iHarwood.MoonPhaseModule;

import java.time.ZonedDateTime;
import java.time.ZoneId;

/**
 * Rough approximation of Earth–Uranus distance (AU).
 */
public final class UranusDistance {
    private UranusDistance() {}

    private static final double A_URANUS_AU = 19.18916464;
    private static final double E_URANUS = 0.04716771;
    private static final double M0_URANUS_DEG = 142.238599;
    private static final double N_URANUS_DEG_PER_DAY = 0.01176904;
    private static final double W_EARTH_DEG = 102.93735;
    private static final double W_URANUS_DEG = 96.93735;

    public static double distanceAUNow() {
        return distanceAU(ZonedDateTime.now(ZoneId.systemDefault()));
    }

    public static double heliocentricDistanceAUNow() {
        double d = DateUtils.daysSinceJ2000(ZonedDateTime.now(ZoneId.systemDefault()));
        double mDeg = DateUtils.normalizeAngle(M0_URANUS_DEG + N_URANUS_DEG_PER_DAY * d);
        return A_URANUS_AU * (1.0 - E_URANUS * Math.cos(Math.toRadians(mDeg)));
    }

    public static double distanceAU(ZonedDateTime zdt) {
        double d = DateUtils.daysSinceJ2000(zdt);

        double rEarth = SunDistance.distanceAU(zdt);
        double mEarthDeg = DateUtils.normalizeAngle(DateUtils.EARTH_MEAN_ANOMALY_J2000_DEG + DateUtils.EARTH_MEAN_MOTION_DEG_PER_DAY * d);
        double longEarthDeg = mEarthDeg + W_EARTH_DEG;

        double mUranusDeg = DateUtils.normalizeAngle(M0_URANUS_DEG + N_URANUS_DEG_PER_DAY * d);
        double rUranus = A_URANUS_AU * (1.0 - E_URANUS * Math.cos(Math.toRadians(mUranusDeg)));
        double longUranusDeg = mUranusDeg + W_URANUS_DEG;

        double longEarthRad = Math.toRadians(longEarthDeg);
        double longUranusRad = Math.toRadians(longUranusDeg);

        double delta = Math.abs(longUranusRad - longEarthRad);
        return Math.sqrt(rEarth * rEarth + rUranus * rUranus - 2.0 * rEarth * rUranus * Math.cos(delta));
    }
}
