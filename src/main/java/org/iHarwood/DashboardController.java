package org.iHarwood;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    /** Guards against concurrent manual refresh calls. */
    private final AtomicBoolean refreshRunning = new AtomicBoolean(false);

    public DashboardController(AstronomicalDataService dataService,
                               Optional<HistoryService> historyService,
                               Main main) {
        this.dataService = dataService;
        this.historyService = historyService;
        this.main = main;
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
        new Thread(() -> {
            try {
                main.update();
            } finally {
                refreshRunning.set(false);
            }
        }, "manual-refresh").start();
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
     */
    @GetMapping(value = "/api/health", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> health() {
        AstronomicalSnapshot snapshot = dataService.getLatestSnapshot();
        boolean dataAvailable = snapshot != null;

        Map<String, Object> components = new LinkedHashMap<>();
        components.put("snapshot", dataAvailable ? "UP" : "WAITING");
        components.put("history",  historyService.isPresent() ? "UP" : "DISABLED");
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
     * Populate history asynchronously — runs on a background thread so the
     * HTTP request returns immediately (202 Accepted) rather than blocking.
     */
    @PostMapping("/api/history/populate")
    @ResponseBody
    public ResponseEntity<Void> populateHistory(
            @RequestParam(defaultValue = "30") int days) {
        if (!historyService.isPresent()) {
            return ResponseEntity.status(503).build();
        }
        int clampedDays = Math.min(Math.max(days, 1), 365);
        new Thread(() -> {
            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
            for (int i = clampedDays; i >= 1; i--) {
                ZonedDateTime target = now.minusDays(i).withHour(12).withMinute(0).withSecond(0).withNano(0);
                AstronomicalSnapshot snapshot = main.calculateSnapshotForDate(target);
                historyService.get().saveAt(snapshot, target.toInstant());
            }
        }, "history-populate").start();
        return ResponseEntity.accepted().build();
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
        if (!historyService.isPresent()) {
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
            @RequestParam(defaultValue = "500") int limit) {
        if (!historyService.isPresent()) {
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
