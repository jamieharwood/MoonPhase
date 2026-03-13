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
import java.util.function.ToDoubleFunction;

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

    /**
     * Metric extractors keyed by metric name. Adding a new metric only requires
     * a single new entry here instead of updating both ALLOWED_METRICS and the switch.
     */
    private static final Map<String, ToDoubleFunction<SnapshotDocument>> METRIC_EXTRACTORS = Map.ofEntries(
            Map.entry("daylightHours",            SnapshotDocument::getDaylightHours),
            Map.entry("moonIlluminationPercent",  d -> d.getMoonIlluminationPercent()),
            Map.entry("moonAgeDays",              d -> d.getMoonAgeDays()),
            Map.entry("moonDistanceKm",           SnapshotDocument::getMoonDistanceKm),
            Map.entry("daysUntilFullMoon",        d -> d.getDaysUntilFullMoon()),
            Map.entry("sunDistanceAu",            SnapshotDocument::getSunDistanceAu),
            Map.entry("mercuryDistanceAu",        SnapshotDocument::getMercuryDistanceAu),
            Map.entry("venusDistanceAu",          SnapshotDocument::getVenusDistanceAu),
            Map.entry("marsDistanceAu",           SnapshotDocument::getMarsDistanceAu),
            Map.entry("jupiterDistanceAu",        SnapshotDocument::getJupiterDistanceAu),
            Map.entry("saturnDistanceAu",         SnapshotDocument::getSaturnDistanceAu),
            Map.entry("uranusDistanceAu",         SnapshotDocument::getUranusDistanceAu),
            Map.entry("neptuneDistanceAu",        SnapshotDocument::getNeptuneDistanceAu),
            Map.entry("plutoDistanceAu",          SnapshotDocument::getPlutoDistanceAu),
            Map.entry("voyager1HelioDistanceAu",  SnapshotDocument::getVoyager1HelioDistanceAu),
            Map.entry("voyager2HelioDistanceAu",  SnapshotDocument::getVoyager2HelioDistanceAu),
            Map.entry("voyager1DistanceAu",       SnapshotDocument::getVoyager1DistanceAu),
            Map.entry("voyager2DistanceAu",       SnapshotDocument::getVoyager2DistanceAu),
            Map.entry("newHorizonsDistanceAu",    SnapshotDocument::getNewHorizonsDistanceAu),
            Map.entry("earthSpeedKmPerSec",       SnapshotDocument::getEarthSpeedKmPerSec),
            Map.entry("earthSpeedKmPerHour",      SnapshotDocument::getEarthSpeedKmPerHour),
            Map.entry("earthAxialTiltDegrees",    SnapshotDocument::getEarthAxialTiltDegrees),
            Map.entry("issAltitudeKm",            SnapshotDocument::getIssAltitudeKm),
            Map.entry("tiangongAltitudeKm",       SnapshotDocument::getTiangongAltitudeKm),
            Map.entry("hubbleAltitudeKm",         SnapshotDocument::getHubbleAltitudeKm),
            Map.entry("starlinkSatelliteCount",   d -> d.getStarlinkSatelliteCount()),
            Map.entry("kuiperSatelliteCount",     d -> d.getKuiperSatelliteCount()),
            Map.entry("totalSatellitesInOrbit",   d -> d.getTotalSatellitesInOrbit()),
            Map.entry("daysUntilSummerSolstice",  d -> d.getDaysUntilSummerSolstice()),
            Map.entry("daysUntilWinterSolstice",  d -> d.getDaysUntilWinterSolstice()),
            Map.entry("daysUntilPerihelion",      d -> d.getDaysUntilPerihelion()),
            Map.entry("daysUntilAphelion",        d -> d.getDaysUntilAphelion())
    );

    static final Set<String> ALLOWED_METRICS = METRIC_EXTRACTORS.keySet();

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

    public void saveAt(AstronomicalSnapshot snapshot, java.time.Instant timestamp) {
        try {
            repository.save(SnapshotDocument.from(snapshot, timestamp));
            logger.debug("Historical snapshot saved for {}.", timestamp);
        } catch (Exception e) {
            logger.warn("Failed to save historical snapshot: {}", e.getMessage());
        }
    }

    public void clearAll() {
        try {
            repository.deleteAll();
            logger.info("All snapshots deleted from MongoDB.");
        } catch (Exception e) {
            logger.warn("Failed to clear snapshots from MongoDB: {}", e.getMessage());
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
        ToDoubleFunction<SnapshotDocument> extractor = METRIC_EXTRACTORS.get(metric);
        if (extractor == null) {
            throw new IllegalArgumentException("Unknown metric: " + metric);
        }
        return extractor.applyAsDouble(doc);
    }
}
