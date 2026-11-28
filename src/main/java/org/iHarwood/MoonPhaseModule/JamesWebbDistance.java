package org.iHarwood.MoonPhaseModule;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Live distance of the James Webb Space Telescope from Earth, in km, from JPL Horizons
 * (<a href="https://ssd.jpl.nasa.gov/horizons/">ssd.jpl.nasa.gov/horizons</a>, target -170,
 * geocentric observer range).
 *
 * <p>JWST flies a large, station-kept halo orbit around Sun–Earth L2, so its distance
 * (roughly 1.2–1.8 million km) cannot be predicted from a simple formula. Like the LEO
 * data it is therefore live-only: historical snapshots get {@code -1} (unavailable), as
 * does any failed fetch. Results are cached for {@value #CACHE_TTL_MS} ms.
 */
public final class JamesWebbDistance {
    private static final Logger logger = LoggerFactory.getLogger(JamesWebbDistance.class);
    private static final JsonMapper OBJECT_MAPPER = JsonMapper.shared();

    static final long CACHE_TTL_MS = 60L * 60 * 1000;
    private static final double KM_PER_AU = 149_597_870.7;
    private static final String HORIZONS_URL = "https://ssd.jpl.nasa.gov/api/horizons.api";
    private static final DateTimeFormatter HORIZONS_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private record Cached(double km, long fetchedAtMs) {}
    private static volatile Cached cache = null;

    private JamesWebbDistance() {}

    static synchronized void resetCache() { cache = null; }

    /**
     * JWST distance from Earth for a historical moment. Not available offline — returns -1.
     */
    public static double distanceKmAt(ZonedDateTime zdt) {
        return -1.0;
    }

    /**
     * Current JWST distance from Earth in km (live from JPL Horizons), or -1 if unavailable.
     */
    public static double distanceKmNow() {
        Cached c = cache;
        if (c != null && (System.currentTimeMillis() - c.fetchedAtMs()) < CACHE_TTL_MS) {
            return c.km();
        }
        return fetchAndCache();
    }

    private static synchronized double fetchAndCache() {
        Cached c = cache;
        if (c != null && (System.currentTimeMillis() - c.fetchedAtMs()) < CACHE_TTL_MS) {
            return c.km();
        }
        double km = doFetch(ZonedDateTime.now(ZoneOffset.UTC));
        // Only cache successes so a transient failure is retried on the next update.
        if (km > 0) {
            cache = new Cached(km, System.currentTimeMillis());
        }
        return km;
    }

    private static double doFetch(ZonedDateTime now) {
        try {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("format", "json");
            params.put("COMMAND", "'-170'");
            params.put("OBJ_DATA", "'NO'");
            params.put("MAKE_EPHEM", "'YES'");
            params.put("EPHEM_TYPE", "'OBSERVER'");
            params.put("CENTER", "'500@399'");
            params.put("START_TIME", "'" + HORIZONS_TIME.format(now) + "'");
            params.put("STOP_TIME", "'" + HORIZONS_TIME.format(now.plusMinutes(1)) + "'");
            params.put("STEP_SIZE", "'1m'");
            params.put("QUANTITIES", "'20'");
            params.put("CSV_FORMAT", "'YES'");
            String query = params.entrySet().stream()
                    .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(HORIZONS_URL + "?" + query))
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", "MoonPhase/1.0")
                    .GET()
                    .build();
            HttpResponse<String> res = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() < 200 || res.statusCode() >= 300) {
                logger.warn("HTTP {} from JPL Horizons (JWST)", res.statusCode());
                return -1.0;
            }
            double km = parseDeltaKm(res.body());
            if (km > 0) logger.info("James Webb Space Telescope distance from Earth: {} km (JPL Horizons)",
                    String.format("%,.0f", km));
            return km;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1.0;
        } catch (Exception e) {
            logger.warn("Failed to fetch JWST distance from JPL Horizons: {}", e.getMessage());
            return -1.0;
        }
    }

    /**
     * Extracts the observer range (delta, AU) from a Horizons JSON response and converts it to km.
     * The ephemeris row looks like: {@code " 2026-Nov-27 23:28, , ,  0.01136300827198,  0.0069837,"}.
     */
    static double parseDeltaKm(String json) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            String result = root.path("result").asString("");
            int soe = result.indexOf("$$SOE");
            int eoe = result.indexOf("$$EOE");
            if (soe < 0 || eoe < soe) {
                logger.warn("Unexpected JPL Horizons response (no ephemeris block)");
                return -1.0;
            }
            String firstRow = result.substring(soe + 5, eoe).strip().lines().findFirst().orElse("");
            String[] cols = firstRow.split(",");
            if (cols.length < 4) {
                logger.warn("Unexpected JPL Horizons ephemeris row: {}", firstRow);
                return -1.0;
            }
            double deltaAu = Double.parseDouble(cols[3].strip());
            return deltaAu > 0 ? deltaAu * KM_PER_AU : -1.0;
        } catch (Exception e) {
            logger.warn("Could not parse JWST distance from JPL Horizons: {}", e.getMessage());
            return -1.0;
        }
    }
}
