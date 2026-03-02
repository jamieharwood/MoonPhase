package org.iHarwood.MoonPhaseModule;

import java.time.Year;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Generic orbital distance calculator for any planet (or dwarf planet).
 *
 * Uses mean-anomaly-based orbital mechanics:
 *   - Heliocentric radius: r = a * (1 - e * cos(M))
 *   - Earth-planet distance via law of cosines in the ecliptic plane
 *
 * This is an approximation for display purposes only.
 */
public final class PlanetDistance {

    private static final double W_EARTH_DEG = 102.93735;

    private final String name;
    private final double semiMajorAxisAu;
    private final double eccentricity;
    private final double meanAnomalyJ2000Deg;
    private final double meanMotionDegPerDay;
    private final double longitudeOfPerihelionDeg;

    /**
     * Creates a planet distance calculator with the given orbital elements.
     *
     * @param name                     human-readable name (e.g. "Mars")
     * @param semiMajorAxisAu          semi-major axis in AU
     * @param eccentricity             orbital eccentricity
     * @param meanAnomalyJ2000Deg      mean anomaly at J2000 epoch (degrees)
     * @param meanMotionDegPerDay      mean motion (degrees per day)
     * @param longitudeOfPerihelionDeg longitude of perihelion (degrees)
     */
    public PlanetDistance(String name, double semiMajorAxisAu, double eccentricity,
                          double meanAnomalyJ2000Deg, double meanMotionDegPerDay,
                          double longitudeOfPerihelionDeg) {
        this.name = name;
        this.semiMajorAxisAu = semiMajorAxisAu;
        this.eccentricity = eccentricity;
        this.meanAnomalyJ2000Deg = meanAnomalyJ2000Deg;
        this.meanMotionDegPerDay = meanMotionDegPerDay;
        this.longitudeOfPerihelionDeg = longitudeOfPerihelionDeg;
    }

    public String getName() {
        return name;
    }

    /**
     * Heliocentric distance (AU) at the given moment.
     */
    public double heliocentricDistanceAU(ZonedDateTime zdt) {
        double d = DateUtils.daysSinceJ2000(zdt);
        double mDeg = DateUtils.normalizeAngle(meanAnomalyJ2000Deg + meanMotionDegPerDay * d);
        return semiMajorAxisAu * (1.0 - eccentricity * Math.cos(Math.toRadians(mDeg)));
    }

    /**
     * Heliocentric distance (AU) right now (UTC).
     */
    public double heliocentricDistanceAUNow() {
        return heliocentricDistanceAU(ZonedDateTime.now(ZoneOffset.UTC));
    }

    /**
     * Distance from Earth (AU) at the given moment using law of cosines.
     */
    public double distanceAU(ZonedDateTime zdt) {
        double d = DateUtils.daysSinceJ2000(zdt);

        // Earth's heliocentric radius (AU)
        double rEarth = SunDistance.distanceAU(zdt);

        // Earth's mean anomaly and longitude
        double mEarthDeg = DateUtils.normalizeAngle(
                DateUtils.EARTH_MEAN_ANOMALY_J2000_DEG + DateUtils.EARTH_MEAN_MOTION_DEG_PER_DAY * d);
        double longEarthDeg = mEarthDeg + W_EARTH_DEG;

        // Planet mean anomaly and approximate radius
        double mPlanetDeg = DateUtils.normalizeAngle(meanAnomalyJ2000Deg + meanMotionDegPerDay * d);
        double rPlanet = semiMajorAxisAu * (1.0 - eccentricity * Math.cos(Math.toRadians(mPlanetDeg)));
        double longPlanetDeg = mPlanetDeg + longitudeOfPerihelionDeg;

        double longEarthRad = Math.toRadians(longEarthDeg);
        double longPlanetRad = Math.toRadians(longPlanetDeg);

        // Distance using law of cosines in the ecliptic plane
        double delta = Math.abs(longPlanetRad - longEarthRad);
        return Math.sqrt(rEarth * rEarth + rPlanet * rPlanet
                - 2.0 * rEarth * rPlanet * Math.cos(delta));
    }

    /**
     * Distance from Earth (AU) right now (UTC).
     */
    public double distanceAUNow() {
        return distanceAU(ZonedDateTime.now(ZoneOffset.UTC));
    }

    /**
     * Min/max distance from Earth (AU) over the current year.
     */
    public double[] minMaxDistanceAUNow() {
        return minMaxDistanceAU(ZonedDateTime.now(ZoneOffset.UTC));
    }

    /**
     * Min/max distance from Earth (AU) over the year containing the given date.
     */
    public double[] minMaxDistanceAU(ZonedDateTime zdt) {
        ZonedDateTime start = zdt.withHour(0).withMinute(0).withSecond(0).withNano(0);
        int daysInYear = Year.of(start.getYear()).length();
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < daysInYear; i++) {
            double d = distanceAU(start.plusDays(i));
            if (d < min) min = d;
            if (d > max) max = d;
        }
        return new double[]{min, max};
    }
}
