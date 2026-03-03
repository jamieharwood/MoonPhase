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

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
     * Manual refresh - update() is already synchronized via updateLock,
     * so concurrent calls are safe (they will queue up, not race).
     */
    @PostMapping(value = "/api/refresh")
    @ResponseBody
    public ResponseEntity<Void> refresh() {
        new Thread(main::update, "manual-refresh").start();
        return ResponseEntity.accepted().build();
    }

    @GetMapping(value = "/api/events", produces = TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter subscribe() {
        return dataService.subscribe();
    }

    @PostMapping("/api/history/populate")
    @ResponseBody
    public ResponseEntity<Void> populateHistory(
            @RequestParam(defaultValue = "30") int days) {
        if (!historyService.isPresent()) {
            return ResponseEntity.status(503).build();
        }
        int clampedDays = Math.min(Math.max(days, 1), 365);
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        for (int i = clampedDays; i >= 1; i--) {
            ZonedDateTime target = now.minusDays(i).withHour(12).withMinute(0).withSecond(0).withNano(0);
            AstronomicalSnapshot snapshot = main.calculateSnapshotForDate(target);
            historyService.get().saveAt(snapshot, target.toInstant());
        }
        return ResponseEntity.noContent().build();
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
}
