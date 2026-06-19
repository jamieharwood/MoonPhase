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

/**
 * Fetches the current geomagnetic Kp index from NOAA Space Weather Services.
 *
 * The Kp index measures global geomagnetic activity:
 *   0–1 = Quiet,  2–3 = Unsettled,  4 = Active,  5+ = Geomagnetic Storm
 * Aurora may be visible at mid-latitudes when Kp ≥ 5.
 *
 * Returns -1.0 if the data is unavailable.
 */
public final class AuroraKpFetcher {

    private static final Logger logger = LoggerFactory.getLogger(AuroraKpFetcher.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String KP_URL =
            "https://services.swpc.noaa.gov/json/planetary_k_index_1m.json";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private AuroraKpFetcher() {}

    /**
     * Returns the most recent Kp index (0.0–9.0), or -1.0 if unavailable.
     * The value is rounded to one decimal place by NOAA.
     */
    public static double fetchKpIndex() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(KP_URL))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "MoonPhaseAI/1.0")
                    .GET()
                    .build();
            HttpResponse<String> res = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() < 200 || res.statusCode() >= 300) {
                logger.warn("HTTP {} from NOAA Kp endpoint", res.statusCode());
                return -1.0;
            }
            double kp = parseLatestKp(res.body());
            if (kp >= 0) logger.info("Current aurora Kp index: {}", kp);
            return kp;
        } catch (Exception e) {
            logger.warn("Failed to fetch Kp index from NOAA: {}", e.getMessage());
            return -1.0;
        }
    }

    /**
     * Parses the latest estimated Kp value from the NOAA JSON array using Jackson.
     * The array is ordered oldest-first, so the last element is most recent.
     * Format: [{"time_tag":"...","kp_index":2,"estimated_kp":2.33,"kp":"2P"}, ...]
     */
    private static double parseLatestKp(String json) {
        try {
            JsonNode array = OBJECT_MAPPER.readTree(json);
            if (!array.isArray() || array.size() == 0) {
                logger.warn("Unexpected NOAA Kp response format (empty or not an array)");
                return -1.0;
            }
            JsonNode last = array.get(array.size() - 1);
            JsonNode kpNode = last.path("estimated_kp");
            if (kpNode.isMissingNode()) {
                logger.warn("Could not find 'estimated_kp' field in NOAA Kp response");
                return -1.0;
            }
            return kpNode.asDouble(-1.0);
        } catch (Exception e) {
            logger.warn("Could not parse Kp index from NOAA response: {}", e.getMessage());
            return -1.0;
        }
    }
}
