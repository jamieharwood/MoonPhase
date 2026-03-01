package org.iHarwood.MoonPhaseModule;

import java.time.ZonedDateTime;
import java.time.ZoneId;

/**
 * Rough approximation of Earth–Neptune distance (AU).
 */
public final class NeptuneDistance {
    private NeptuneDistance() {}

    private static final double A_NEPTUNE_AU = 30.06992276;
    private static final double E_NEPTUNE = 0.00858587;
    private static final double M0_NEPTUNE_DEG = 256.228347;
    private static final double N_NEPTUNE_DEG_PER_DAY = 0.00598103;
    private static final double W_EARTH_DEG = 102.93735;
    private static final double W_NEPTUNE_DEG = 276.33630;

    public static double distanceAUNow() {
        return distanceAU(ZonedDateTime.now(ZoneId.systemDefault()));
    }

    public static double distanceAU(ZonedDateTime zdt) {
        double d = DateUtils.daysSinceJ2000(zdt);

        double rEarth = SunDistance.distanceAU(zdt);
        double mEarthDeg = DateUtils.normalizeAngle(DateUtils.EARTH_MEAN_ANOMALY_J2000_DEG + DateUtils.EARTH_MEAN_MOTION_DEG_PER_DAY * d);
        double longEarthDeg = mEarthDeg + W_EARTH_DEG;

        double mNeptuneDeg = DateUtils.normalizeAngle(M0_NEPTUNE_DEG + N_NEPTUNE_DEG_PER_DAY * d);
        double rNeptune = A_NEPTUNE_AU * (1.0 - E_NEPTUNE * Math.cos(Math.toRadians(mNeptuneDeg)));
        double longNeptuneDeg = mNeptuneDeg + W_NEPTUNE_DEG;

        double longEarthRad = Math.toRadians(longEarthDeg);
        double longNeptuneRad = Math.toRadians(longNeptuneDeg);

        double delta = Math.abs(longNeptuneRad - longEarthRad);
        return Math.sqrt(rEarth * rEarth + rNeptune * rNeptune - 2.0 * rEarth * rNeptune * Math.cos(delta));
    }
}
