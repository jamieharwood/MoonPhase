package org.iHarwood.MoonPhaseModule;

import java.time.ZonedDateTime;

/**
 * Trajectory of an escaping deep-space probe, propagated from a JPL Horizons heliocentric
 * state vector (J2000 ecliptic, AU and AU/day) under the Sun's gravity.
 *
 * <p>Beyond the planets the Sun is the only significant force, so integrating two-body
 * motion (RK4, ≤ 5-day steps) from a single state vector stays within a few thousandths of
 * an AU for decades either side of the epoch. It is not valid before the probe's last
 * planetary flyby, and returns 0 before launch. Distance from Earth uses Earth's real
 * heliocentric position, so it carries the ±1 AU annual swing that a
 * "heliocentric − 1 AU" shortcut misses.
 *
 * <p>Source: JPL Horizons, <a href="https://ssd.jpl.nasa.gov/horizons/">ssd.jpl.nasa.gov/horizons</a>,
 * vector table, CENTER=500@10 (Sun), REF_PLANE=ECLIPTIC, OUT_UNITS=AU-D.
 */
final class SpacecraftTrajectory {

    private static final double KM_PER_AU = 149_597_870.7;
    private static final double SECONDS_PER_DAY = 86_400.0;
    /** Sun's GM in AU³/day² (Gaussian gravitational constant squared). */
    private static final double GM_SUN = 0.01720209895 * 0.01720209895;
    private static final double MAX_STEP_DAYS = 5.0;

    private final double launchJd;
    private final double epochJd;
    private final double[] position;
    private final double[] velocityPerDay;

    SpacecraftTrajectory(double launchJd, double epochJd,
                         double x, double y, double z, double vx, double vy, double vz) {
        this.launchJd = launchJd;
        this.epochJd = epochJd;
        this.position = new double[]{x, y, z};
        this.velocityPerDay = new double[]{vx, vy, vz};
    }

    /** Heliocentric position (AU, J2000 ecliptic) at the given moment, or {@code null} before launch. */
    double[] heliocentricPosition(ZonedDateTime zdt) {
        double jd = DateUtils.julianDate(zdt);
        if (jd < launchJd) {
            return null;
        }
        double dt = jd - epochJd;
        int steps = Math.max(1, (int) Math.ceil(Math.abs(dt) / MAX_STEP_DAYS));
        double h = dt / steps;
        double[] s = {position[0], position[1], position[2], velocityPerDay[0], velocityPerDay[1], velocityPerDay[2]};
        for (int i = 0; i < steps; i++) {
            s = rk4(s, h);
        }
        return new double[]{s[0], s[1], s[2]};
    }

    double heliocentricDistanceAU(ZonedDateTime zdt) {
        double[] p = heliocentricPosition(zdt);
        return p == null ? 0.0 : PlanetDistance.norm(p);
    }

    double distanceFromEarthAU(ZonedDateTime zdt) {
        double[] p = heliocentricPosition(zdt);
        return p == null ? 0.0 : PlanetDistance.distanceBetween(p, Planets.EARTH.heliocentricPosition(zdt));
    }

    /** One classical Runge–Kutta step of heliocentric two-body motion; state = {x,y,z,vx,vy,vz}. */
    private static double[] rk4(double[] s, double h) {
        double[] k1 = derivative(s);
        double[] k2 = derivative(add(s, k1, h / 2));
        double[] k3 = derivative(add(s, k2, h / 2));
        double[] k4 = derivative(add(s, k3, h));
        double[] out = new double[6];
        for (int i = 0; i < 6; i++) {
            out[i] = s[i] + h / 6.0 * (k1[i] + 2 * k2[i] + 2 * k3[i] + k4[i]);
        }
        return out;
    }

    private static double[] derivative(double[] s) {
        double r = Math.sqrt(s[0] * s[0] + s[1] * s[1] + s[2] * s[2]);
        double k = -GM_SUN / (r * r * r);
        return new double[]{s[3], s[4], s[5], k * s[0], k * s[1], k * s[2]};
    }

    private static double[] add(double[] s, double[] d, double h) {
        double[] out = new double[6];
        for (int i = 0; i < 6; i++) {
            out[i] = s[i] + d[i] * h;
        }
        return out;
    }

    /** Heliocentric speed in km/s. */
    double speedKmPerSec() {
        return PlanetDistance.norm(velocityPerDay) * KM_PER_AU / SECONDS_PER_DAY;
    }
}
