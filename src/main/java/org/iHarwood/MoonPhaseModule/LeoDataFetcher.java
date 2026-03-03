package org.iHarwood.MoonPhaseModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Fetches live Low Earth Orbit data from CelesTrak.
 *
 * Altitudes are derived from TLE mean motion via Kepler's third law.
 * Constellation counts are derived by counting TLE entries in each group.
 * Total satellite count is derived from the CelesTrak active SATCAT.
 *
 * All methods return -1 if the data cannot be fetched or parsed.
 */
public final class LeoDataFetcher {
    private static final Logger logger = LoggerFactory.getLogger(LeoDataFetcher.class);
    private LeoDataFetcher() {}

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // Physical constants
    private static final double GM_EARTH = 3.986004418e14; // m³/s²
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double TWO_PI = 2.0 * Math.PI;

    // NORAD catalog IDs
    private static final int ISS_NORAD      = 25544;  // International Space Station
    private static final int TIANGONG_NORAD = 48274;  // Tianhe core module (CSS)
    private static final int HUBBLE_NORAD   = 20580;  // Hubble Space Telescope

    // CelesTrak endpoints
    private static final String TLE_URL          = "https://celestrak.org/NORAD/elements/gp.php?CATNR=%d&FORMAT=TLE";
    private static final String GROUP_URL         = "https://celestrak.org/NORAD/elements/gp.php?GROUP=%s&FORMAT=TLE";
    private static final String ACTIVE_TLE_URL = "https://celestrak.org/NORAD/elements/gp.php?GROUP=active&FORMAT=TLE";

    // ── Public API ─────────────────────────────────────────────────────────────

    /** Mean altitude of the ISS in km, or -1 if unavailable. */
    public static double fetchIssAltitudeKm() {
        return fetchAltitudeKm(ISS_NORAD, "ISS");
    }

    /** Mean altitude of the Tiangong CSS (Tianhe core) in km, or -1 if unavailable. */
    public static double fetchTiangongAltitudeKm() {
        return fetchAltitudeKm(TIANGONG_NORAD, "Tiangong");
    }

    /** Mean altitude of the Hubble Space Telescope in km, or -1 if unavailable. */
    public static double fetchHubbleAltitudeKm() {
        return fetchAltitudeKm(HUBBLE_NORAD, "Hubble");
    }

    /** Number of active Starlink satellites, or -1 if unavailable. */
    public static int fetchStarlinkCount() {
        return fetchGroupCount("starlink", "Starlink");
    }

    /** Number of active Amazon Kuiper satellites, or -1 if unavailable. */
    public static int fetchKuiperCount() {
        return fetchGroupCount("kuiper", "Kuiper");
    }

    /** Total number of tracked active satellites in orbit, or -1 if unavailable. */
    public static int fetchTotalSatelliteCount() {
        try {
            String body = fetch(ACTIVE_TLE_URL, 60);
            if (body == null) return -1;
            // TLE format: 3 lines per satellite (name, line 1, line 2)
            // Count lines starting with "2 " to get the satellite count
            long count = body.lines()
                    .filter(l -> l.startsWith("2 "))
                    .count();
            logger.info("Total active satellites tracked: {}", count);
            return (int) count;
        } catch (Exception e) {
            logger.warn("Failed to fetch total satellite count: {}", e.getMessage());
            return -1;
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private static double fetchAltitudeKm(int noradId, String name) {
        try {
            String body = fetch(String.format(TLE_URL, noradId), 15);
            if (body == null) return -1;
            return parseMeanAltitudeKm(body, name);
        } catch (Exception e) {
            logger.warn("Failed to fetch altitude for {}: {}", name, e.getMessage());
            return -1;
        }
    }

    private static int fetchGroupCount(String groupName, String label) {
        try {
            String body = fetch(String.format(GROUP_URL, groupName), 60);
            if (body == null) return -1;
            int count = (int) body.lines()
                    .filter(l -> l.startsWith("1 "))
                    .count();
            logger.info("{} satellite count: {}", label, count);
            return count;
        } catch (Exception e) {
            logger.warn("Failed to fetch count for {}: {}", label, e.getMessage());
            return -1;
        }
    }

    /**
     * Derives mean orbital altitude from TLE line 2 mean motion field.
     * Uses Kepler's third law: a = (GM × (T/2π)²)^(1/3)
     * Mean motion is at fixed-width columns 52–63 (0-indexed) in TLE line 2.
     */
    private static double parseMeanAltitudeKm(String tle, String name) {
        for (String line : tle.split("\\r?\\n")) {
            if (line.startsWith("2 ") && line.length() >= 63) {
                String meanMotionStr = line.substring(52, 63).trim();
                double meanMotionRevDay = Double.parseDouble(meanMotionStr);
                double periodSec = 86400.0 / meanMotionRevDay;
                double semiMajorAxisM = Math.pow(GM_EARTH * Math.pow(periodSec / TWO_PI, 2), 1.0 / 3.0);
                double altKm = semiMajorAxisM / 1000.0 - EARTH_RADIUS_KM;
                logger.info("{} mean altitude from TLE: {} km", name, String.format("%.1f", altKm));
                return altKm;
            }
        }
        logger.warn("Could not parse TLE line 2 for {}", name);
        return -1;
    }

    private static String fetch(String url, int timeoutSeconds) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("User-Agent", "MoonPhaseAI/1.0")
                    .GET()
                    .build();
            HttpResponse<String> res = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                return res.body();
            }
            logger.warn("HTTP {} from {}", res.statusCode(), url);
            return null;
        } catch (Exception e) {
            logger.warn("HTTP request failed for {}: {}", url, e.getMessage());
            return null;
        }
    }
}
