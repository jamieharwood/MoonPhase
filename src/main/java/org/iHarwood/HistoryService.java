package org.iHarwood;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
     * Number of days of history to retain. Snapshots older than this are purged nightly.
     * 0 (default) means no pruning — keep all history indefinitely.
     */
    @Value("${app.history.retention-days:0}")
    private int retentionDays;

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
            Map.entry("daysUntilAphelion",        d -> d.getDaysUntilAphelion()),
            Map.entry("auroraKpIndex",            SnapshotDocument::getAuroraKpIndex),
            Map.entry("issCrew",                  d -> d.getIssCrew()),
            Map.entry("totalPeopleInSpace",        d -> d.getTotalPeopleInSpace())
    );

    static final Set<String> ALLOWED_METRICS = METRIC_EXTRACTORS.keySet();

    private final SnapshotRepository repository;

    public HistoryService(SnapshotRepository repository) {
        this.repository = repository;
    }

    public void save(AstronomicalSnapshot snapshot) {
        try {
            // Deduplication: allow at most one snapshot per 12-hour window so both the
            // 00:01 and 12:01 scheduled runs are persisted, but rapid manual refreshes
            // within the same window are not duplicated.
            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
            Instant halfDayStart = now.getHour() < 12
                    ? now.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant()
                    : now.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().plus(12, ChronoUnit.HOURS);
            Instant halfDayEnd = halfDayStart.plus(12, ChronoUnit.HOURS);
            if (repository.countByTimestampBetween(halfDayStart, halfDayEnd) > 0) {
                logger.debug("Snapshot for this 12-hour window already exists in MongoDB — skipping duplicate save.");
                return;
            }
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
     * Purges snapshots older than {@code app.history.retention-days} days.
     * Runs nightly at 03:00 UTC. No-op when retention-days is 0 (the default).
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void purgeOldSnapshots() {
        if (retentionDays <= 0) return;
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        try {
            long deleted = repository.deleteByTimestampBefore(cutoff);
            if (deleted > 0) {
                logger.info("Purged {} snapshot(s) older than {} days.", deleted, retentionDays);
            } else {
                logger.debug("History purge: no snapshots older than {} days found.", retentionDays);
            }
        } catch (Exception e) {
            logger.warn("Failed to purge old snapshots: {}", e.getMessage());
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

    /**
     * Returns historical data as a CSV string (timestamp,value header + rows).
     */
    public String exportCsv(String metric, int limit) {
        List<Map<String, Object>> data = getHistory(metric, limit);
        StringBuilder sb = new StringBuilder("timestamp,value\n");
        for (Map<String, Object> point : data) {
            sb.append(point.get("timestamp")).append(",").append(point.get("value")).append("\n");
        }
        return sb.toString();
    }
}
