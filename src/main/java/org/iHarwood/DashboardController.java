package org.iHarwood;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE;
/**
 * HTTP endpoints for the live astronomical dashboard.
 * Only active when running as a servlet web application.
 */
@Controller
@ConditionalOnWebApplication
public class DashboardController {
    private final AstronomicalDataService dataService;
    private final Optional<HistoryService> historyService;
    private final Main main;
    private final ExecutorService applicationTaskExecutor;
    /** Guards against concurrent manual refresh calls. */
    private final AtomicBoolean refreshRunning = new AtomicBoolean(false);
    /** Guards against overlapping history populate jobs, which can now run for a long time. */
    private final AtomicBoolean populateRunning = new AtomicBoolean(false);
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    /** Earliest date the calculation formulas are valid for (matches the MCP date range). */
    static final LocalDate EARLIEST_POPULATE_DATE = LocalDate.of(1900, 1, 1);
    /** Snapshots saved per Mongo bulk insert, so long backfills don't hold everything in memory. */
    static final int POPULATE_BATCH_SIZE = 1000;
    public DashboardController(AstronomicalDataService dataService,
                               Optional<HistoryService> historyService,
                               Main main,
                               ExecutorService applicationTaskExecutor) {
        this.dataService = dataService;
        this.historyService = historyService;
        this.main = main;
        this.applicationTaskExecutor = applicationTaskExecutor;
    }
    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }
    @GetMapping(value = "/api/data", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<AstronomicalSnapshot> currentData() {
        AstronomicalSnapshot snapshot = dataService.getLatestSnapshot();
        if (snapshot == null) {
            return ResponseEntity.status(503).build();
        }
        return ResponseEntity.ok(snapshot);
    }
    /**
     * Manual refresh — rate-limited to one concurrent execution.
     * Returns 429 if a refresh is already in progress.
     */
    @PostMapping(value = "/api/refresh")
    @ResponseBody
    public ResponseEntity<Void> refresh() {
        if (!refreshRunning.compareAndSet(false, true)) {
            return ResponseEntity.status(429).build();
        }
        applicationTaskExecutor.execute(() -> {
            try {
                main.update();
            } finally {
                refreshRunning.set(false);
            }
        });
        return ResponseEntity.accepted().build();
    }
    @GetMapping(value = "/api/events", produces = TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter subscribe() {
        return dataService.subscribe();
    }
    /**
     * Health endpoint — reports status of each subsystem.
     * Useful for container-level liveness checks and operator troubleshooting.
     * Actuator probes are also available at /actuator/health.
     */
    @GetMapping(value = "/api/health", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> health() {
        AstronomicalSnapshot snapshot = dataService.getLatestSnapshot();
        boolean dataAvailable = snapshot != null;
        Map<String, Object> components = new LinkedHashMap<>();
        components.put("snapshot", dataAvailable ? "UP" : "WAITING");
        components.put("history", historyService.isPresent() ? "UP" : "DISABLED");
        components.put("refreshRunning", refreshRunning.get());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", dataAvailable ? "UP" : "STARTING");
        body.put("dataAvailable", dataAvailable);
        body.put("lastUpdated", dataAvailable ? snapshot.lastUpdated() : null);
        body.put("historyEnabled", historyService.isPresent());
        body.put("components", components);
        return ResponseEntity.ok(body);
    }
    /**
     * Populate history asynchronously — runs on a background virtual thread so the
     * HTTP request returns immediately (202 Accepted) rather than blocking.
     * There is no upper limit on {@code days}: requests reaching further back than
     * {@link #EARLIEST_POPULATE_DATE} (where the formulas stop being valid) are trimmed to it.
     * Returns 400 for days &lt; 1 and 429 if a populate is already running.
     */
    @PostMapping("/api/history/populate")
    @ResponseBody
    public ResponseEntity<Void> populateHistory(
            @RequestParam(defaultValue = "30") long days) {
        if (historyService.isEmpty()) {
            return ResponseEntity.status(503).build();
        }
        if (days < 1) {
            return ResponseEntity.badRequest().build();
        }
        if (!populateRunning.compareAndSet(false, true)) {
            return ResponseEntity.status(429).build();
        }
        HistoryService history = historyService.get();
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        long maxDays = ChronoUnit.DAYS.between(EARLIEST_POPULATE_DATE, now.toLocalDate());
        long effectiveDays = Math.min(days, maxDays);
        try {
            applicationTaskExecutor.execute(() -> {
                try {
                    populate(history, now, effectiveDays);
                } catch (RuntimeException e) {
                    logger.warn("History populate failed: {}", e.getMessage(), e);
                } finally {
                    populateRunning.set(false);
                }
            });
        } catch (RuntimeException e) {
            populateRunning.set(false);
            throw e;
        }
        return ResponseEntity.accepted().build();
    }

    /** Computes and saves one snapshot per day at 12:00 UTC, oldest first, in bounded batches. */
    private void populate(HistoryService history, ZonedDateTime now, long days) {
        logger.info("Populating {} day(s) of history.", days);
        List<SnapshotDocument> batch = new ArrayList<>(POPULATE_BATCH_SIZE);
        for (long i = days; i >= 1; i--) {
            ZonedDateTime target = now.minusDays(i).withHour(12).withMinute(0).withSecond(0).withNano(0);
            AstronomicalSnapshot snapshot = main.calculateSnapshotForDate(target);
            batch.add(SnapshotDocument.from(snapshot, target.toInstant()));
            if (batch.size() == POPULATE_BATCH_SIZE) {
                history.saveAll(batch);
                batch = new ArrayList<>(POPULATE_BATCH_SIZE);
            }
        }
        if (!batch.isEmpty()) {
            history.saveAll(batch);
        }
        logger.info("Finished populating {} day(s) of history.", days);
    }
    @DeleteMapping("/api/history")
    @ResponseBody
    public ResponseEntity<Void> clearHistory() {
        historyService.ifPresent(HistoryService::clearAll);
        return ResponseEntity.noContent().build();
    }
    @GetMapping(value = "/api/history", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> history(
            @RequestParam(defaultValue = "daylightHours") String metric,
            @RequestParam(defaultValue = "60") int limit) {
        if (historyService.isEmpty()) {
            return ResponseEntity.status(503).build();
        }
        if (!HistoryService.ALLOWED_METRICS.contains(metric)) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(historyService.get().getHistory(metric, limit));
    }
    @GetMapping("/api/history/export")
    @ResponseBody
    public ResponseEntity<String> exportHistory(
            @RequestParam(defaultValue = "daylightHours") String metric,
            @RequestParam(defaultValue = "0") int limit) {
        if (historyService.isEmpty()) {
            return ResponseEntity.status(503).build();
        }
        if (!HistoryService.ALLOWED_METRICS.contains(metric)) {
            return ResponseEntity.badRequest().build();
        }
        String csv = historyService.get().exportCsv(metric, limit);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + metric + ".csv\"")
                .header("Content-Type", "text/csv")
                .body(csv);
    }
}
