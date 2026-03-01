package org.iHarwood;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Persists snapshots to MongoDB and serves historical data for the graph panel.
 * Only active when app.history.enabled=true.
 */
@Service
@ConditionalOnProperty(name = "app.history.enabled", havingValue = "true")
public class HistoryService {

    private static final Logger logger = LoggerFactory.getLogger(HistoryService.class);
    private static final DateTimeFormatter LABEL_FMT =
            DateTimeFormatter.ofPattern("dd MMM HH:mm").withZone(ZoneOffset.UTC);
    private static final int DEFAULT_LIMIT = 60;

    static final Set<String> ALLOWED_METRICS = Set.of(
            "daylightHours",
            "moonIlluminationPercent",
            "sunDistanceAu",
            "marsDistanceAu",
            "jupiterDistanceAu",
            "saturnDistanceAu",
            "moonDistanceKm",
            "earthSpeedKmPerSec",
            "earthSpeedKmPerHour",
            "voyager1DistanceAu",
            "voyager2DistanceAu",
            "newHorizonsDistanceAu",
            "daysUntilFullMoon",
            "daysUntilSummerSolstice",
            "daysUntilWinterSolstice",
            "daysUntilPerihelion",
            "daysUntilAphelion"
    );

    private final SnapshotRepository repository;

    public HistoryService(SnapshotRepository repository) {
        this.repository = repository;
    }

    public void save(AstronomicalSnapshot snapshot) {
        try {
            repository.save(SnapshotDocument.from(snapshot));
            logger.debug("Snapshot saved to MongoDB.");
        } catch (Exception e) {
            logger.warn("Failed to save snapshot to MongoDB: {}", e.getMessage());
        }
    }

    /**
     * Returns up to {@code limit} data points for the requested metric,
     * oldest first, formatted for the chart.
     */
    public List<Map<String, Object>> getHistory(String metric, int limit) {
        if (!ALLOWED_METRICS.contains(metric)) {
            throw new IllegalArgumentException("Unknown metric: " + metric);
        }
        int clampedLimit = Math.min(Math.max(limit, 1), 500);
        try {
            List<SnapshotDocument> docs = repository.findAllByOrderByTimestampDesc(
                    PageRequest.of(0, clampedLimit));
            Collections.reverse(docs); // oldest → newest for the chart
            return docs.stream().map(doc -> {
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("timestamp", LABEL_FMT.format(doc.getTimestamp()));
                point.put("value", extractMetric(doc, metric));
                return point;
            }).toList();
        } catch (Exception e) {
            logger.warn("Failed to query history from MongoDB: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private double extractMetric(SnapshotDocument doc, String metric) {
        return switch (metric) {
            case "daylightHours"           -> doc.getDaylightHours();
            case "moonIlluminationPercent" -> doc.getMoonIlluminationPercent();
            case "sunDistanceAu"           -> doc.getSunDistanceAu();
            case "marsDistanceAu"          -> doc.getMarsDistanceAu();
            case "jupiterDistanceAu"       -> doc.getJupiterDistanceAu();
            case "saturnDistanceAu"        -> doc.getSaturnDistanceAu();
            case "moonDistanceKm"          -> doc.getMoonDistanceKm();
            case "earthSpeedKmPerSec"      -> doc.getEarthSpeedKmPerSec();
            case "earthSpeedKmPerHour"     -> doc.getEarthSpeedKmPerHour();
            case "voyager1DistanceAu"      -> doc.getVoyager1DistanceAu();
            case "voyager2DistanceAu"      -> doc.getVoyager2DistanceAu();
            case "newHorizonsDistanceAu"   -> doc.getNewHorizonsDistanceAu();
            case "daysUntilFullMoon"       -> doc.getDaysUntilFullMoon();
            case "daysUntilSummerSolstice" -> doc.getDaysUntilSummerSolstice();
            case "daysUntilWinterSolstice" -> doc.getDaysUntilWinterSolstice();
            case "daysUntilPerihelion"     -> doc.getDaysUntilPerihelion();
            case "daysUntilAphelion"       -> doc.getDaysUntilAphelion();
            default -> throw new IllegalArgumentException("Unknown metric: " + metric);
        };
    }
}
