package org.iHarwood.MoonPhaseModule;

import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the ephemeris against JPL Horizons (https://ssd.jpl.nasa.gov/horizons/),
 * observer table, geocentric (CENTER=500@399), quantities 10 (illuminated %),
 * 19 (r: distance from Sun, AU) and 20 (delta: distance from Earth, AU).
 * Planet dates span both element tables (Table 1: 1800–2050, Table 2: 2075);
 * spacecraft dates span 2020–2035 around the 2026 state-vector epoch.
 */
class EphemerisAccuracyTest {

    // {body, UTC instant, r [AU], delta [AU], illuminated %}
    private static final Object[][] HORIZONS = {
        {"Moon", "2000-01-01T12:00:00Z", 0.981872991987, 0.00268998925442, 23.00638},
        {"Moon", "2015-07-14T12:00:00Z", 1.014093364195, 0.00258893367913, 2.80715},
        {"Moon", "2026-11-27T23:28:00Z", 0.988335675755, 0.00242629346672, 84.11571},
        {"Moon", "2040-06-01T00:00:00Z", 1.014622416956, 0.00247864868104, 62.3655},
        {"Moon", "2075-03-15T00:00:00Z", 0.991853156389, 0.00251286916114, 4.01549},
        {"Sun", "2000-01-01T12:00:00Z", 0.0, 0.98332762652701, 100.0},
        {"Sun", "2015-07-14T12:00:00Z", 0.0, 1.01653722930979, 100.0},
        {"Sun", "2026-11-27T23:28:00Z", 0.0, 0.98668159281108, 100.0},
        {"Sun", "2040-06-01T00:00:00Z", 0.0, 1.01401203003873, 100.0},
        {"Sun", "2075-03-15T00:00:00Z", 0.0, 0.99416461269571, 100.0},
        {"Mercury", "2000-01-01T12:00:00Z", 0.466468841931, 1.41546946904999, 97.48652},
        {"Mercury", "2015-07-14T12:00:00Z", 0.309190162025, 1.24239600766868, 89.42002},
        {"Mercury", "2026-11-27T23:28:00Z", 0.378519674543, 1.16083625301682, 79.42448},
        {"Mercury", "2040-06-01T00:00:00Z", 0.420104868102, 0.79730382988487, 33.88215},
        {"Mercury", "2075-03-15T00:00:00Z", 0.419357052028, 1.34002851911789, 93.73588},
        {"Venus", "2000-01-01T12:00:00Z", 0.720212286809, 1.13757924245212, 75.80544},
        {"Venus", "2015-07-14T12:00:00Z", 0.727063246288, 0.41812775457853, 22.87572},
        {"Venus", "2026-11-27T23:28:00Z", 0.719887281313, 0.40643257821581, 25.20597},
        {"Venus", "2040-06-01T00:00:00Z", 0.720916066211, 1.73489962490728, 99.99888},
        {"Venus", "2075-03-15T00:00:00Z", 0.72679128062, 1.27090944065987, 81.25808},
        {"Mars", "2000-01-01T12:00:00Z", 1.391202576247, 1.84968786270204, 92.65057},
        {"Mars", "2015-07-14T12:00:00Z", 1.588259587485, 2.58663164091405, 99.77691},
        {"Mars", "2026-11-27T23:28:00Z", 1.621036289394, 1.20386500513761, 89.75553},
        {"Mars", "2040-06-01T00:00:00Z", 1.662191499153, 1.82338768718413, 91.73498},
        {"Mars", "2075-03-15T00:00:00Z", 1.385108951973, 2.37867167249401, 99.99085},
        {"Jupiter", "2000-01-01T12:00:00Z", 4.965377642079, 4.62117524331177, 99.07602},
        {"Jupiter", "2015-07-14T12:00:00Z", 5.377225371039, 6.20754388955521, 99.74263},
        {"Jupiter", "2026-11-27T23:28:00Z", 5.325365759933, 5.07976560617289, 99.15565},
        {"Jupiter", "2040-06-01T00:00:00Z", 5.446108558008, 5.16037454936609, 99.15835},
        {"Jupiter", "2075-03-15T00:00:00Z", 5.407321443042, 4.44813980830185, 99.92909},
        {"Saturn", "2000-01-01T12:00:00Z", 9.183858256403, 8.65279676096631, 99.78468},
        {"Saturn", "2015-07-14T12:00:00Z", 9.986643162591, 9.34477940561514, 99.83367},
        {"Saturn", "2026-11-27T23:28:00Z", 9.417060471932, 8.85272387588537, 99.8037},
        {"Saturn", "2040-06-01T00:00:00Z", 9.590117268223, 9.1311306095071, 99.76675},
        {"Saturn", "2075-03-15T00:00:00Z", 10.02644726861, 9.87098068694784, 99.75629},
        {"Uranus", "2000-01-01T12:00:00Z", 19.9240150244, 20.727171130885, 99.98055},
        {"Uranus", "2015-07-14T12:00:00Z", 19.99032142132, 19.9412638083133, 99.93529},
        {"Uranus", "2026-11-27T23:28:00Z", 19.43076834045, 18.4447497346863, 99.99991},
        {"Uranus", "2040-06-01T00:00:00Z", 18.56896819234, 19.2113531895362, 99.95691},
        {"Uranus", "2075-03-15T00:00:00Z", 19.45569681685, 19.7716814268018, 99.9422},
        {"Neptune", "2000-01-01T12:00:00Z", 30.12058818217, 31.024498221734, 99.996},
        {"Neptune", "2015-07-14T12:00:00Z", 29.96382363343, 29.2715099470401, 99.98418},
        {"Neptune", "2026-11-27T23:28:00Z", 29.87667954767, 29.4320638256214, 99.97797},
        {"Neptune", "2040-06-01T00:00:00Z", 29.81042658002, 30.6197572090097, 99.98976},
        {"Neptune", "2075-03-15T00:00:00Z", 29.96440392234, 29.5329657015062, 99.97736},
        {"Pluto", "2000-01-01T12:00:00Z", 30.22318615617, 31.0643527312227, 99.99308},
        {"Pluto", "2015-07-14T12:00:00Z", 32.90832829897, 31.9012331276215, 99.99955},
        {"Pluto", "2026-11-27T23:28:00Z", 35.65038403251, 36.1654464971515, 99.98629},
        {"Pluto", "2040-06-01T00:00:00Z", 39.10115251431, 38.8470547272902, 99.98412},
        {"Pluto", "2075-03-15T00:00:00Z", 46.34361095354, 47.2697102650016, 99.99851},
        {"Voyager1", "2020-01-01T00:00:00Z", 147.8673484958, 148.605563056091, 99.99952},
        {"Voyager1", "2024-01-01T00:00:00Z", 162.1186258501, 162.859045233514, 99.9996},
        {"Voyager1", "2026-11-27T23:28:00Z", 172.4726207521, 173.264340821595, 99.99971},
        {"Voyager1", "2030-01-01T00:00:00Z", 183.4854610277, 184.225761621311, 99.99969},
        {"Voyager1", "2035-01-01T00:00:00Z", 201.2506300484, 201.994116220434, 99.99975},
        {"Voyager2", "2020-01-01T00:00:00Z", 122.6759061005, 123.45125333803, 99.9994},
        {"Voyager2", "2024-01-01T00:00:00Z", 135.3586174591, 136.122235415381, 99.99948},
        {"Voyager2", "2026-11-27T23:28:00Z", 144.5821025366, 145.126611289025, 99.9992},
        {"Voyager2", "2030-01-01T00:00:00Z", 154.4049823751, 155.15561907585, 99.99958},
        {"Voyager2", "2035-01-01T00:00:00Z", 170.2696843896, 171.009897717743, 99.99964},
        {"NewHorizons", "2020-01-01T00:00:00Z", 46.21582350257, 47.1916615967235, 99.99983},
        {"NewHorizons", "2024-01-01T00:00:00Z", 57.83054644178, 58.8034435272464, 99.99985},
        {"NewHorizons", "2026-11-27T23:28:00Z", 66.16268960782, 66.8851603769669, 99.99745},
        {"NewHorizons", "2030-01-01T00:00:00Z", 74.96070702767, 75.9318217327052, 99.9999},
        {"NewHorizons", "2035-01-01T00:00:00Z", 89.03964718231, 90.0081078701794, 99.99991}
    };

    /** Tolerances: relative for planets, absolute for spacecraft and the Moon. */
    private static final double PLANET_HELIO_REL = 0.002;   // 0.2 %
    private static final double PLANET_GEO_REL = 0.004;     // 0.4 %
    private static final double CRAFT_AU = 0.03;
    private static final double MOON_KM = 1_200;
    private static final double MOON_ILLUM_PCT = 1.0;
    private static final double SUN_AU = 0.0002;

    @Test
    void matchesHorizons() {
        List<String> failures = new ArrayList<>();
        StringBuilder table = new StringBuilder("\nbody          date                  quantity    app            horizons       error\n");
        for (Object[] row : HORIZONS) {
            String body = (String) row[0];
            ZonedDateTime t = ZonedDateTime.parse((String) row[1]);
            double r = (Double) row[2], delta = (Double) row[3], illum = (Double) row[4];
            switch (body) {
                case "Sun" -> check(table, failures, body, t, "distance", SunDistance.distanceAU(t), delta, SUN_AU);
                case "Moon" -> {
                    check(table, failures, body, t, "km", MoonDistance.distanceKm(t), delta * MoonEphemeris.KM_PER_AU, MOON_KM);
                    double appIllum = MoonEphemeris.at(DateUtils.julianDate(t)).illuminatedFraction() * 100;
                    check(table, failures, body, t, "illum %", appIllum, illum, MOON_ILLUM_PCT);
                }
                case "Voyager1" -> {
                    check(table, failures, body, t, "from Sun", VoyagerDistance.heliocentricDistanceV1AU(t), r, CRAFT_AU);
                    check(table, failures, body, t, "from Earth", VoyagerDistance.distanceFromEarthV1AU(t), delta, CRAFT_AU);
                }
                case "Voyager2" -> {
                    check(table, failures, body, t, "from Sun", VoyagerDistance.heliocentricDistanceV2AU(t), r, CRAFT_AU);
                    check(table, failures, body, t, "from Earth", VoyagerDistance.distanceFromEarthV2AU(t), delta, CRAFT_AU);
                }
                case "NewHorizons" -> {
                    check(table, failures, body, t, "from Sun", NewHorizonsDistance.heliocentricDistanceAU(t), r, CRAFT_AU);
                    check(table, failures, body, t, "from Earth", NewHorizonsDistance.distanceFromEarthAU(t), delta, CRAFT_AU);
                }
                default -> {
                    PlanetDistance p = planet(body);
                    check(table, failures, body, t, "from Sun", p.heliocentricDistanceAU(t), r, r * PLANET_HELIO_REL);
                    check(table, failures, body, t, "from Earth", p.distanceAU(t), delta, delta * PLANET_GEO_REL);
                }
            }
        }
        System.out.println(table);
        assertTrue(failures.isEmpty(), "Outside tolerance vs JPL Horizons:\n" + String.join("\n", failures));
    }

    /**
     * New/full moon = Moon−Sun ecliptic longitude 0°/180°. Reference times found from Horizons
     * quantity 31 (observer ecliptic longitude) for the Moon and Sun at 2-minute steps:
     * new moon 2026-12-09 00:52 UTC, full moon 2026-12-24 01:28 UTC.
     */
    @Test
    void newAndFullMoonTimesMatchHorizons() {
        MoonPhase phase = MoonPhase.at(ZonedDateTime.parse("2026-11-27T23:28:00Z"));
        assertWithinMinutes(ZonedDateTime.parse("2026-12-09T00:52:00Z"), phase.getNextNewMoon(), 15);
        assertWithinMinutes(ZonedDateTime.parse("2026-12-24T01:28:00Z"), phase.getNextFullMoon(), 15);
        assertTrue(phase.getPhaseName().equals("Waning Gibbous"));
        assertTrue(Math.abs(phase.getIlluminationPercent() - 84) <= 1, "illumination " + phase.getIlluminationPercent());
    }

    private static void assertWithinMinutes(ZonedDateTime expected, ZonedDateTime actual, long minutes) {
        long diff = Math.abs(java.time.Duration.between(expected, actual).toMinutes());
        assertTrue(diff <= minutes, "expected " + expected + " but was " + actual + " (" + diff + " min)");
    }

    private static PlanetDistance planet(String name) {
        return switch (name) {
            case "Mercury" -> Planets.MERCURY;
            case "Venus" -> Planets.VENUS;
            case "Mars" -> Planets.MARS;
            case "Jupiter" -> Planets.JUPITER;
            case "Saturn" -> Planets.SATURN;
            case "Uranus" -> Planets.URANUS;
            case "Neptune" -> Planets.NEPTUNE;
            case "Pluto" -> Planets.PLUTO;
            default -> throw new IllegalArgumentException(name);
        };
    }

    private static void check(StringBuilder table, List<String> failures, String body, ZonedDateTime t,
                              String what, double app, double ref, double tolerance) {
        double err = app - ref;
        String line = String.format("%-13s %-21s %-11s %-14.6f %-14.6f %+.6f", body, t, what, app, ref, err);
        table.append(line).append('\n');
        if (Math.abs(err) > tolerance) {
            failures.add(line + "  (tolerance " + String.format("%.6f", tolerance) + ")");
        }
    }
}
