package org.iHarwood.MoonPhaseModule;

/**
 * Geocentric positions of the Moon and the Sun, following Paul Schlyter's
 * "How to compute planetary positions" (<a href="https://stjarnhimlen.se/comp/ppcomp.html">stjarnhimlen.se/comp/ppcomp.html</a>):
 * Keplerian lunar elements plus the principal perturbations (evection, variation,
 * yearly equation, parallactic inequality, …). Longitude is good to ~1–2 arcmin and
 * distance to a few hundred km — enough to place new/full moon within minutes.
 *
 * <p>All angles are in degrees, ecliptic of date. Time is a Julian Date (UT).
 */
final class MoonEphemeris {
    private MoonEphemeris() {}

    static final double EARTH_RADIUS_KM = 6378.14;
    static final double KM_PER_AU = 149_597_870.7;
    /** Mean synodic month, used only to seed root-finding. */
    static final double SYNODIC_MONTH = 29.530588853;

    /** Moon (ecliptic longitude/latitude in degrees, distance in km) and Sun (longitude, distance in km). */
    record Position(double moonLon, double moonLat, double moonDistanceKm, double sunLon, double sunDistanceKm) {

        /** Moon − Sun ecliptic longitude in [0, 360): 0 = new moon, 180 = full moon. */
        double elongationLongitude() {
            return DateUtils.normalizeAngle(moonLon - sunLon);
        }

        /** Fraction of the Moon's disc that is illuminated, 0–1. */
        double illuminatedFraction() {
            double cosElong = cosd(moonLat) * cosd(moonLon - sunLon);
            double elong = Math.acos(Math.max(-1.0, Math.min(1.0, cosElong)));
            double phaseAngle = Math.atan2(sunDistanceKm * Math.sin(elong), moonDistanceKm - sunDistanceKm * Math.cos(elong));
            return (1.0 + Math.cos(phaseAngle)) / 2.0;
        }
    }

    static Position at(double jd) {
        double d = jd - 2451543.5; // Schlyter's day number: 2000 Jan 0.0 UT

        // Sun
        double ws = 282.9404 + 4.70935e-5 * d;
        double es = 0.016709 - 1.151e-9 * d;
        double ms = DateUtils.normalizeAngle(356.0470 + 0.9856002585 * d);
        double eSun = Math.toRadians(ms) + es * Math.sin(Math.toRadians(ms)) * (1.0 + es * Math.cos(Math.toRadians(ms)));
        double xs = Math.cos(eSun) - es;
        double ys = Math.sqrt(1.0 - es * es) * Math.sin(eSun);
        double sunDistAu = Math.hypot(xs, ys);
        double sunLon = DateUtils.normalizeAngle(Math.toDegrees(Math.atan2(ys, xs)) + ws);

        // Moon orbital elements
        double node = 125.1228 - 0.0529538083 * d;
        double inc = 5.1454;
        double w = 318.0634 + 0.1643573223 * d;
        double a = 60.2666; // Earth radii
        double e = 0.054900;
        double mm = DateUtils.normalizeAngle(115.3654 + 13.0649929509 * d);

        double ecc = PlanetDistance.solveKepler(Math.toRadians(mm), e);
        double xv = a * (Math.cos(ecc) - e);
        double yv = a * Math.sqrt(1.0 - e * e) * Math.sin(ecc);
        double v = Math.toDegrees(Math.atan2(yv, xv));
        double r = Math.hypot(xv, yv);

        double vw = v + w;
        double xh = r * (cosd(node) * cosd(vw) - sind(node) * sind(vw) * cosd(inc));
        double yh = r * (sind(node) * cosd(vw) + cosd(node) * sind(vw) * cosd(inc));
        double zh = r * (sind(vw) * sind(inc));
        double lon = Math.toDegrees(Math.atan2(yh, xh));
        double lat = Math.toDegrees(Math.atan2(zh, Math.hypot(xh, yh)));

        // Perturbations
        double ls = ms + ws;                 // Sun mean longitude
        double lm = mm + w + node;           // Moon mean longitude
        double dd = lm - ls;                 // mean elongation
        double f = lm - node;                // argument of latitude

        lon += -1.274 * sind(mm - 2 * dd)    // evection
                + 0.658 * sind(2 * dd)       // variation
                - 0.186 * sind(ms)           // yearly equation
                - 0.059 * sind(2 * mm - 2 * dd)
                - 0.057 * sind(mm - 2 * dd + ms)
                + 0.053 * sind(mm + 2 * dd)
                + 0.046 * sind(2 * dd - ms)
                + 0.041 * sind(mm - ms)
                - 0.035 * sind(dd)           // parallactic equation
                - 0.031 * sind(mm + ms)
                - 0.015 * sind(2 * f - 2 * dd)
                + 0.011 * sind(mm - 4 * dd);
        lat += -0.173 * sind(f - 2 * dd)
                - 0.055 * sind(mm - f - 2 * dd)
                - 0.046 * sind(mm + f - 2 * dd)
                + 0.033 * sind(f + 2 * dd)
                + 0.017 * sind(2 * mm + f);
        r += -0.58 * cosd(mm - 2 * dd)
                - 0.46 * cosd(2 * dd);

        return new Position(DateUtils.normalizeAngle(lon), lat, r * EARTH_RADIUS_KM,
                sunLon, sunDistAu * KM_PER_AU);
    }

    /**
     * Julian Date of the next moment after {@code jd} when the Moon−Sun longitude equals
     * {@code targetDeg} (0 = new moon, 180 = full moon).
     */
    static double nextPhase(double jd, double targetDeg) {
        double behind = DateUtils.normalizeAngle(targetDeg - at(jd).elongationLongitude());
        double t = refine(jd + behind / 360.0 * SYNODIC_MONTH, targetDeg);
        if (t <= jd) {
            t = refine(t + SYNODIC_MONTH, targetDeg);
        }
        return t;
    }

    /** Julian Date of the most recent moment at or before {@code jd} with the given elongation. */
    static double previousPhase(double jd, double targetDeg) {
        double ahead = DateUtils.normalizeAngle(at(jd).elongationLongitude() - targetDeg);
        double t = refine(jd - ahead / 360.0 * SYNODIC_MONTH, targetDeg);
        if (t > jd) {
            t = refine(t - SYNODIC_MONTH, targetDeg);
        }
        return t;
    }

    /** Newton iteration on the Moon−Sun longitude (≈12.19°/day). */
    private static double refine(double t, double targetDeg) {
        for (int k = 0; k < 8; k++) {
            double diff = DateUtils.normalizeAngle(at(t).elongationLongitude() - targetDeg);
            if (diff > 180.0) diff -= 360.0;
            double rate = (at(t + 0.01).elongationLongitude() - at(t - 0.01).elongationLongitude());
            if (rate < -180.0) rate += 360.0;
            if (rate > 180.0) rate -= 360.0;
            rate /= 0.02;
            double step = diff / rate;
            t -= step;
            if (Math.abs(step) < 1e-5) break; // < 1 second
        }
        return t;
    }

    private static double sind(double deg) {
        return Math.sin(Math.toRadians(deg));
    }

    private static double cosd(double deg) {
        return Math.cos(Math.toRadians(deg));
    }
}
