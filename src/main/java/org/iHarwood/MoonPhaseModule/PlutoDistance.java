package org.iHarwood.MoonPhaseModule;

import java.time.ZonedDateTime;
import java.time.ZoneId;

/**
 * Rough approximation of Earth–Pluto distance (AU).
 */
public final class PlutoDistance {
    private PlutoDistance() {}

    private static final double A_PLUTO_AU = 39.48211675;
    private static final double E_PLUTO = 0.24882730;
    private static final double M0_PLUTO_DEG = 14.53;
    private static final double N_PLUTO_DEG_PER_DAY = 0.003975;
    private static final double W_EARTH_DEG = 102.93735;
    private static final double W_PLUTO_DEG = 224.0;

    public static double distanceAUNow() {
        return distanceAU(ZonedDateTime.now(ZoneId.systemDefault()));
    }

    public static double distanceAU(ZonedDateTime zdt) {
        double d = DateUtils.daysSinceJ2000(zdt);

        double rEarth = SunDistance.distanceAU(zdt);
        double mEarthDeg = DateUtils.normalizeAngle(DateUtils.EARTH_MEAN_ANOMALY_J2000_DEG + DateUtils.EARTH_MEAN_MOTION_DEG_PER_DAY * d);
        double longEarthDeg = mEarthDeg + W_EARTH_DEG;

        double mPlutoDeg = DateUtils.normalizeAngle(M0_PLUTO_DEG + N_PLUTO_DEG_PER_DAY * d);
        double rPluto = A_PLUTO_AU * (1.0 - E_PLUTO * Math.cos(Math.toRadians(mPlutoDeg)));
        double longPlutoDeg = mPlutoDeg + W_PLUTO_DEG;

        double longEarthRad = Math.toRadians(longEarthDeg);
        double longPlutoRad = Math.toRadians(longPlutoDeg);

        double delta = Math.abs(longPlutoRad - longEarthRad);
        return Math.sqrt(rEarth * rEarth + rPluto * rPluto - 2.0 * rEarth * rPluto * Math.cos(delta));
    }
}
