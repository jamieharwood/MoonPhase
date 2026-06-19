package org.iHarwood.MoonPhaseModule;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Fetches live Low Earth Orbit and human spaceflight data.
 *
 * Sources:
 *  - Altitudes + counts: https://tle.ivanstanojevic.me/api/tle/
 *  - People in space:    https://corquaid.github.io/international-space-station-APIs/JSON/people-in-space.json
 *
 * All fetches are batched and cached for {@value #CACHE_TTL_MS} ms (6 hours).
 */
public final class LeoDataFetcher {

    private static final Logger logger = LoggerFactory.getLogger(LeoDataFetcher.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private LeoDataFetcher() {}

    // ── People-in-space DTO ───────────────────────────────────────────────────

    /**
     * Snapshot of all humans currently in space.
     * craftOccupancy keys have a " ★" suffix for ISS-docked vehicles.
     * Map is ordered by crew count descending.
     */
    public record PeopleInSpace(int total, int issCrew, Map<String, Integer> craftOccupancy) {
        static PeopleInSpace unavailable() {
            return new PeopleInSpace(-1, -1, Collections.emptyMap());
        }
    }

    // ── Cache ─────────────────────────────────────────────────────────────────

    static final long CACHE_TTL_MS = 6L * 60 * 60 * 1000;

    private record LeoCache(
            double issAltitudeKm,
            double tiangongAltitudeKm,
            double hubbleAltitudeKm,
            int starlinkCount,
            int kuiperCount,
            int totalSatelliteCount,
            PeopleInSpace people,
            long fetchedAtMs) {}

    private static volatile LeoCache cache = null;

    static synchronized void resetCache() { cache = null; }

    private static synchronized LeoCache getOrFetch() {
        LeoCache c = cache;
        if (c != null && (System.currentTimeMillis() - c.fetchedAtMs()) < CACHE_TTL_MS) {
            logger.debug("LEO data served from cache (age {}s)",
                    (System.currentTimeMillis() - c.fetchedAtMs()) / 1000);
            return c;
        }
        logger.info("Fetching fresh LEO data...");
        int starlink = fetchConstellationCount("STARLINK", "Starlink");
        int kuiper   = fetchConstellationCount("KUIPER", "Kuiper");
        int total    = fetchAllSatellitesFromApi();
        LeoCache fresh = new LeoCache(
                fetchAltitudeKm(ISS_NORAD, "ISS"),
                fetchAltitudeKm(TIANGONG_NORAD, "Tiangong"),
                fetchAltitudeKm(HUBBLE_NORAD, "Hubble"),
                starlink, kuiper,
                total,
                fetchPeopleInSpaceData(),
                System.currentTimeMillis()
        );
        cache = fresh;
        return fresh;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public static double        fetchIssAltitudeKm()       { return getOrFetch().issAltitudeKm(); }
    public static double        fetchTiangongAltitudeKm()  { return getOrFetch().tiangongAltitudeKm(); }
    public static double        fetchHubbleAltitudeKm()    { return getOrFetch().hubbleAltitudeKm(); }
    public static int           fetchStarlinkCount()       { return getOrFetch().starlinkCount(); }
    public static int           fetchKuiperCount()         { return getOrFetch().kuiperCount(); }
    public static int           fetchTotalSatelliteCount() { return getOrFetch().totalSatelliteCount(); }
    public static int           fetchIssCrew()             { return getOrFetch().people().issCrew(); }
    public static PeopleInSpace fetchPeopleInSpace()       { return getOrFetch().people(); }

    // ── Constants ─────────────────────────────────────────────────────────────

    private static final double GM_EARTH        = 3.986004418e14;
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double TWO_PI          = 2.0 * Math.PI;

    private static final int ISS_NORAD      = 25544;
    private static final int TIANGONG_NORAD = 48274;
    private static final int HUBBLE_NORAD   = 20580;

    private static final String TLE_API_URL       = "https://tle.ivanstanojevic.me/api/tle/%d";
    private static final String GROUP_API_URL      = "https://tle.ivanstanojevic.me/api/tle/?search=%s&page-size=1";
    /** Queries the entire catalogue (no search filter) to get the real total object count. */
    private static final String TOTAL_COUNT_URL    = "https://tle.ivanstanojevic.me/api/tle/?page-size=1";
    private static final String CREW_API_URL  =
            "https://corquaid.github.io/international-space-station-APIs/JSON/people-in-space.json";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    // ── Private helpers ───────────────────────────────────────────────────────

    private static double fetchAltitudeKm(int noradId, String name) {
        try {
            String json = fetch(String.format(TLE_API_URL, noradId), 15);
            if (json == null) return -1;
            JsonNode root = OBJECT_MAPPER.readTree(json);
            String line2 = root.path("line2").asText(null);
            if (line2 == null) { logger.warn("No line2 in TLE JSON for {}", name); return -1; }
            if (line2.length() < 63) { logger.warn("TLE line 2 too short for {}", name); return -1; }
            double meanMotion = Double.parseDouble(line2.substring(52, 63).trim());
            double period = 86400.0 / meanMotion;
            double sma = Math.pow(GM_EARTH * Math.pow(period / TWO_PI, 2), 1.0 / 3.0);
            double alt = sma / 1000.0 - EARTH_RADIUS_KM;
            logger.info("{} altitude: {} km", name, String.format("%.1f", alt));
            return alt;
        } catch (Exception e) {
            logger.warn("Failed to fetch altitude for {}: {}", name, e.getMessage());
            return -1;
        }
    }

    private static int fetchConstellationCount(String term, String label) {
        try {
            String json = fetch(String.format(GROUP_API_URL, term), 15);
            if (json == null) return -1;
            int n = OBJECT_MAPPER.readTree(json).path("totalItems").asInt(-1);
            if (n >= 0) logger.info("{} count: {}", label, n);
            return n;
        } catch (Exception e) {
            logger.warn("Failed to fetch count for {}: {}", label, e.getMessage());
            return -1;
        }
    }

    /**
     * Fetches the total number of catalogued objects from the TLE API (no search filter).
     * This is the real "total satellites in orbit" count, not just starlink + kuiper.
     */
    private static int fetchAllSatellitesFromApi() {
        try {
            String json = fetch(TOTAL_COUNT_URL, 15);
            if (json == null) return -1;
            int n = OBJECT_MAPPER.readTree(json).path("totalItems").asInt(-1);
            if (n >= 0) logger.info("Total catalogued objects in orbit: {}", n);
            return n;
        } catch (Exception e) {
            logger.warn("Failed to fetch total satellite count: {}", e.getMessage());
            return -1;
        }
    }

    /**
     * Parses the people-in-space JSON using Jackson.
     * ISS-docked craft (where any crew member has "iss": true) get a " ★" suffix.
     * Result is sorted largest-crew-first.
     */
    private static PeopleInSpace fetchPeopleInSpaceData() {
        try {
            String json = fetch(CREW_API_URL, 10);
            if (json == null) return PeopleInSpace.unavailable();

            JsonNode root = OBJECT_MAPPER.readTree(json);
            int total = root.path("number").asInt(-1);

            JsonNode people = root.path("people");
            if (people.isMissingNode() || !people.isArray()) {
                return new PeopleInSpace(total, 0, Collections.emptyMap());
            }

            Map<String, Integer> craftCounts = new LinkedHashMap<>();
            Set<String> issDockedCraft = new LinkedHashSet<>();

            for (JsonNode person : people) {
                String craft = person.path("spacecraft").asText(null);
                if (craft != null && !craft.isEmpty()) {
                    craftCounts.merge(craft, 1, Integer::sum);
                    if (person.path("iss").asBoolean(false)) {
                        issDockedCraft.add(craft);
                    }
                }
            }

            int issCrew = issDockedCraft.stream()
                    .mapToInt(c -> craftCounts.getOrDefault(c, 0)).sum();

            Map<String, Integer> sortedCraft = new LinkedHashMap<>();
            craftCounts.entrySet().stream()
                    .sorted((a, b) -> b.getValue() - a.getValue())
                    .forEach(e -> sortedCraft.put(
                            issDockedCraft.contains(e.getKey()) ? e.getKey() + " \u2605" : e.getKey(),
                            e.getValue()));

            logger.info("People in space: {} total, {} on ISS, craft: {}", total, issCrew, craftCounts);
            return new PeopleInSpace(total, issCrew, sortedCraft);

        } catch (Exception e) {
            logger.warn("Failed to fetch people-in-space data: {}", e.getMessage());
            return PeopleInSpace.unavailable();
        }
    }


    private static String fetch(String url, int timeoutSeconds) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("User-Agent", "MoonPhaseAI/1.0")
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> res = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 200 && res.statusCode() < 300) return res.body();
            logger.warn("HTTP {} from {}", res.statusCode(), url);
            return null;
        } catch (Exception e) {
            logger.warn("HTTP request failed for {}: {}", url, e.getMessage());
            return null;
        }
    }
}
