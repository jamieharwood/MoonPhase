package org.iHarwood.MoonPhaseModule;

import java.time.Year;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Heliocentric and geocentric distances of a planet from JPL's
 * "Approximate Positions of the Planets" (E.M. Standish, JPL/Caltech):
 * <a href="https://ssd.jpl.nasa.gov/planets/approx_pos.html">ssd.jpl.nasa.gov/planets/approx_pos.html</a>.
 *
 * <p>Each element (a, e, I, L, long. perihelion, long. node) is a value at J2000 plus a
 * rate per Julian century. Table 1 is used for 1800–2050 and Table 2a/2b (with the
 * extra mean-anomaly terms for Jupiter–Neptune) outside that range. Kepler's equation
 * is solved fully and the position is taken in 3-D (J2000 ecliptic), so distances are
 * accurate to roughly 0.001–0.01 AU — far better than display precision.
 *
 * <p>Geocentric distances use the Earth–Moon barycentre for Earth (≤ 4,700 km offset).
 */
public final class PlanetDistance {

    /** Table 1 validity window (years). */
    private static final int TABLE1_FROM = 1800;
    private static final int TABLE1_TO = 2050;

    private final String name;
    /** Table 1: {a, e, I, L, varpi, Omega} at J2000 followed by the same six rates per century. */
    private final double[] table1;
    /** Table 2a in the same layout, or {@code null} when only Table 1 exists (Pluto). */
    private final double[] table2;
    /** Table 2b extra mean-anomaly terms {b, c, s, f}, or {@code null}. */
    private final double[] table2b;

    private PlanetDistance(String name, double[] table1, double[] table2, double[] table2b) {
        this.name = name;
        this.table1 = table1;
        this.table2 = table2;
        this.table2b = table2b;
    }

    /**
     * @param table1  12 values: a, e, I, L, varpi, Omega at J2000 then their rates per century
     * @param table2  same layout for Table 2a, or {@code null}
     * @param table2b b, c, s, f for Table 2b, or {@code null}
     */
    static PlanetDistance of(String name, double[] table1, double[] table2, double[] table2b) {
        if (table1.length != 12 || (table2 != null && table2.length != 12)
                || (table2b != null && table2b.length != 4)) {
            throw new IllegalArgumentException("Bad element table for " + name);
        }
        return new PlanetDistance(name, table1, table2, table2b);
    }

    public String getName() {
        return name;
    }

    /**
     * Heliocentric position in AU, J2000 ecliptic frame: {x, y, z}.
     */
    public double[] heliocentricPosition(ZonedDateTime zdt) {
        double jd = DateUtils.julianDate(zdt);
        double t = DateUtils.centuriesSinceJ2000(jd);
        int year = zdt.withZoneSameInstant(ZoneOffset.UTC).getYear();
        boolean useTable1 = table2 == null || (year >= TABLE1_FROM && year <= TABLE1_TO);
        double[] el = useTable1 ? table1 : table2;

        double a = el[0] + el[6] * t;
        double e = el[1] + el[7] * t;
        double inc = el[2] + el[8] * t;
        double meanLong = el[3] + el[9] * t;
        double varpi = el[4] + el[10] * t;
        double node = el[5] + el[11] * t;

        double argPeri = varpi - node;
        double meanAnomaly = meanLong - varpi;
        if (!useTable1 && table2b != null) {
            double b = table2b[0], c = table2b[1], s = table2b[2], f = table2b[3];
            meanAnomaly += b * t * t + c * Math.cos(Math.toRadians(f * t)) + s * Math.sin(Math.toRadians(f * t));
        }
        double m = Math.toRadians(normalizeSigned(meanAnomaly));
        double eccAnomaly = solveKepler(m, e);

        double xp = a * (Math.cos(eccAnomaly) - e);
        double yp = a * Math.sqrt(1.0 - e * e) * Math.sin(eccAnomaly);

        double w = Math.toRadians(argPeri);
        double o = Math.toRadians(node);
        double i = Math.toRadians(inc);
        double cw = Math.cos(w), sw = Math.sin(w), co = Math.cos(o), so = Math.sin(o), ci = Math.cos(i), si = Math.sin(i);
        double x = (cw * co - sw * so * ci) * xp + (-sw * co - cw * so * ci) * yp;
        double y = (cw * so + sw * co * ci) * xp + (-sw * so + cw * co * ci) * yp;
        double z = (sw * si) * xp + (cw * si) * yp;
        return new double[]{x, y, z};
    }

    /** Heliocentric distance (AU) at the given moment. */
    public double heliocentricDistanceAU(ZonedDateTime zdt) {
        return norm(heliocentricPosition(zdt));
    }

    /** Heliocentric distance (AU) right now (UTC). */
    public double heliocentricDistanceAUNow() {
        return heliocentricDistanceAU(ZonedDateTime.now(ZoneOffset.UTC));
    }

    /** Distance from Earth (AU) at the given moment. */
    public double distanceAU(ZonedDateTime zdt) {
        return distanceBetween(heliocentricPosition(zdt), Planets.EARTH.heliocentricPosition(zdt));
    }

    /** Distance from Earth (AU) right now (UTC). */
    public double distanceAUNow() {
        return distanceAU(ZonedDateTime.now(ZoneOffset.UTC));
    }

    /** Min/max distance from Earth (AU) over the current year. */
    public double[] minMaxDistanceAUNow() {
        return minMaxDistanceAU(ZonedDateTime.now(ZoneOffset.UTC));
    }

    /** Min/max distance from Earth (AU) over the year containing the given date. */
    public double[] minMaxDistanceAU(ZonedDateTime zdt) {
        ZonedDateTime start = zdt.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        int daysInYear = Year.of(start.getYear()).length();
        // 5-day stride is enough for log bar charts.
        final int step = 5;
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (int day = 0; day < daysInYear; day += step) {
            double d = distanceAU(start.plusDays(day));
            if (d < min) min = d;
            if (d > max) max = d;
        }
        double end = distanceAU(start.plusDays(daysInYear - 1L));
        if (end < min) min = end;
        if (end > max) max = end;
        return new double[]{min, max};
    }

    // ── Geometry helpers ──────────────────────────────────────────────────────

    static double norm(double[] v) {
        return Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    }

    static double distanceBetween(double[] p, double[] q) {
        double dx = p[0] - q[0], dy = p[1] - q[1], dz = p[2] - q[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Normalise degrees to (-180, 180]. */
    private static double normalizeSigned(double deg) {
        double d = DateUtils.normalizeAngle(deg);
        return d > 180.0 ? d - 360.0 : d;
    }

    /** Solve Kepler's equation E − e·sin E = M (radians) by Newton iteration. */
    static double solveKepler(double m, double e) {
        double ecc = m + e * Math.sin(m);
        for (int k = 0; k < 30; k++) {
            double delta = (ecc - e * Math.sin(ecc) - m) / (1.0 - e * Math.cos(ecc));
            ecc -= delta;
            if (Math.abs(delta) < 1e-12) break;
        }
        return ecc;
    }
}
