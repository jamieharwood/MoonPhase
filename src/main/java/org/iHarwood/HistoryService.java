package org.iHarwood;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeMap;
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

    /** Days of history to retain; 0 means keep forever. */
    private final int retentionDays;

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

    public static final Set<String> ALLOWED_METRICS = METRIC_EXTRACTORS.keySet();

    /**
     * Metrics where a month's value is the sum of its daily values (e.g. total daylight hours).
     * Every other metric is averaged over the month.
     */
    public static final Set<String> SUMMED_METRICS = Set.of("daylightHours");

    private final SnapshotRepository repository;

    public HistoryService(
            SnapshotRepository repository,
            @Value("${app.history.retention-days:0}") int retentionDays) {
        this.repository = repository;
        this.retentionDays = retentionDays;
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
    /**
     * Bulk-insert historical snapshots in one Mongo round-trip.
     * @param docs pre-built documents (timestamp already set)
     */
    public int saveAll(List<SnapshotDocument> docs) {
        if (docs == null || docs.isEmpty()) {
            return 0;
        }
        try {
            repository.saveAll(docs);
            logger.info("Bulk-saved {} historical snapshot(s) to MongoDB.", docs.size());
            return docs.size();
        } catch (Exception e) {
            logger.warn("Failed to bulk-save historical snapshots: {}", e.getMessage());
            return 0;
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
     * Returns the newest {@code limit} data points for the requested metric,
     * oldest first, formatted for the chart. {@code limit <= 0} returns every stored point.
     */
    public List<Map<String, Object>> getHistory(String metric, int limit) {
        if (!ALLOWED_METRICS.contains(metric)) {
            throw new IllegalArgumentException("Unknown metric: " + metric);
        }
        try {
            List<SnapshotDocument> docs;
            if (limit <= 0) {
                docs = repository.findAll(Sort.by(Sort.Direction.ASC, "timestamp"));
            } else {
                docs = new ArrayList<>(repository.findAllByOrderByTimestampDesc(PageRequest.of(0, limit)));
                Collections.reverse(docs); // oldest → newest for the chart
            }
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

    /**
     * Aggregates every stored point for {@code metric} into one value per calendar month (UTC).
     * Snapshots on the same day are averaged first, so days with two scheduled runs aren't
     * counted twice. The month's value is then the sum of its daily values for
     * {@link #SUMMED_METRICS} and the mean otherwise.
     *
     * @return oldest month first; each entry has year, month (1–12), value, days (days with data)
     *         and daysInMonth, so callers can spot partial months
     */
    public List<Map<String, Object>> getMonthlyByYear(String metric) {
        if (!ALLOWED_METRICS.contains(metric)) {
            throw new IllegalArgumentException("Unknown metric: " + metric);
        }
        try {
            List<SnapshotDocument> docs = repository.findAll(Sort.by(Sort.Direction.ASC, "timestamp"));
            Map<LocalDate, double[]> daily = new TreeMap<>(); // {sum, count}
            for (SnapshotDocument doc : docs) {
                if (doc.getTimestamp() == null) continue;
                LocalDate day = doc.getTimestamp().atZone(ZoneOffset.UTC).toLocalDate();
                double[] acc = daily.computeIfAbsent(day, d -> new double[2]);
                acc[0] += extractMetric(doc, metric);
                acc[1]++;
            }
            boolean summed = SUMMED_METRICS.contains(metric);
            Map<YearMonth, double[]> monthly = new TreeMap<>(); // {sum of daily means, days}
            daily.forEach((day, acc) -> {
                double[] m = monthly.computeIfAbsent(YearMonth.from(day), ym -> new double[2]);
                m[0] += acc[0] / acc[1];
                m[1]++;
            });
            List<Map<String, Object>> result = new ArrayList<>(monthly.size());
            monthly.forEach((ym, m) -> {
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("year", ym.getYear());
                point.put("month", ym.getMonthValue());
                point.put("value", summed ? m[0] : m[0] / m[1]);
                point.put("days", (int) m[1]);
                point.put("daysInMonth", ym.lengthOfMonth());
                result.add(point);
            });
            return result;
        } catch (Exception e) {
            logger.warn("Failed to aggregate monthly history from MongoDB: {}", e.getMessage());
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
