package org.iHarwood.mcp;

import org.iHarwood.AstronomicalDataService;
import org.iHarwood.AstronomicalSnapshot;
import org.iHarwood.HistoryService;
import org.iHarwood.Main;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpTool.McpAnnotations;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Read-only MCP tools exposing the astronomical data to AI agents over Streamable HTTP
 * ({@code /mcp}). Tools never trigger Awtrix pushes, refreshes or history writes.
 */
@Component
@ConditionalOnWebApplication
public class AstronomyMcpTools {

    static final LocalDate MIN_DATE = LocalDate.of(1900, 1, 1);
    static final LocalDate MAX_DATE = LocalDate.of(2100, 12, 31);
    private static final int MAX_HISTORY_POINTS = 500;

    private final AstronomicalDataService dataService;
    private final Optional<HistoryService> historyService;
    private final Main main;

    public AstronomyMcpTools(AstronomicalDataService dataService,
                             Optional<HistoryService> historyService,
                             Main main) {
        this.dataService = dataService;
        this.historyService = historyService;
        this.main = main;
    }

    @McpTool(name = "get_current_astronomical_data",
            title = "Current astronomical data",
            description = """
                    Returns the latest live snapshot: moon phase/illumination/age/distance, Sun and planet \
                    distances (AU), deep-space probe distances (Voyager 1/2, New Horizons, JWST), light travel \
                    times, Earth orbital speed and axial tilt, daylight hours, sunrise/sunset (UTC), days until \
                    solstices/perihelion/aphelion, LEO data (ISS/Tiangong/Hubble altitude, Starlink/Kuiper counts), \
                    aurora Kp index and people in space. The snapshot is recomputed twice daily (00:01 and 12:01 UTC); \
                    check 'lastUpdated'. A value of -1 means a live source was unavailable.""",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false))
    public Map<String, Object> getCurrentAstronomicalData() {
        return toToolView(requireLatest());
    }

    @McpTool(name = "get_moon_phase",
            title = "Current moon phase",
            description = "Returns just the current moon data: phase name, illumination %, age in days, "
                    + "days until next full moon and Earth–Moon distance in km.",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false))
    public Map<String, Object> getMoonPhase() {
        AstronomicalSnapshot s = requireLatest();
        Map<String, Object> moon = new LinkedHashMap<>();
        moon.put("moonPhaseName", s.moonPhaseName());
        moon.put("moonIlluminationPercent", s.moonIlluminationPercent());
        moon.put("moonAgeDays", s.moonAgeDays());
        moon.put("daysUntilFullMoon", s.daysUntilFullMoon());
        moon.put("moonDistanceKm", s.moonDistanceKm());
        moon.put("lastUpdated", s.lastUpdated());
        return moon;
    }

    @McpTool(name = "get_astronomical_data_for_date",
            title = "Astronomical data for a date",
            description = """
                    Calculates the astronomical snapshot for a given date (computed at 12:00 UTC) between \
                    1900-01-01 and 2100-12-31, using the same formulas as the live data. Live-only values \
                    (LEO satellites, aurora Kp, people in space) cannot be reconstructed and are returned as 0 or -1.""",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false))
    public Map<String, Object> getAstronomicalDataForDate(
            @McpToolParam(description = "ISO-8601 date, e.g. 2027-03-20") String date) {
        LocalDate day;
        try {
            day = LocalDate.parse(date.trim());
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException("date must be an ISO-8601 date like 2027-03-20, got: " + date);
        }
        if (day.isBefore(MIN_DATE) || day.isAfter(MAX_DATE)) {
            throw new IllegalArgumentException("date must be between " + MIN_DATE + " and " + MAX_DATE);
        }
        ZonedDateTime target = ZonedDateTime.of(day, LocalTime.NOON, ZoneOffset.UTC);
        return toToolView(main.calculateSnapshotForDate(target));
    }

    @McpTool(name = "list_history_metrics",
            title = "List history metrics",
            description = "Lists the metric names accepted by get_metric_history and whether history storage is enabled.",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false))
    public Map<String, Object> listHistoryMetrics() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("historyEnabled", historyService.isPresent());
        result.put("metrics", new TreeSet<>(HistoryService.ALLOWED_METRICS));
        return result;
    }

    @McpTool(name = "get_metric_history",
            title = "Metric history",
            description = "Returns stored historical values (oldest first, timestamps in UTC) for one metric. "
                    + "Snapshots are stored at most twice daily. Requires history storage to be enabled; "
                    + "call list_history_metrics for valid metric names.",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false))
    public List<Map<String, Object>> getMetricHistory(
            @McpToolParam(description = "Metric name, e.g. moonIlluminationPercent or daylightHours") String metric,
            @McpToolParam(description = "Max data points to return, newest kept (1-500, default 60)",
                    required = false) Integer limit) {
        HistoryService history = historyService.orElseThrow(() -> new IllegalStateException(
                "History storage is disabled on this server (APP_HISTORY_ENABLED=false)."));
        if (metric == null || !HistoryService.ALLOWED_METRICS.contains(metric)) {
            throw new IllegalArgumentException("Unknown metric '" + metric
                    + "'. Valid metrics: " + new TreeSet<>(HistoryService.ALLOWED_METRICS));
        }
        int n = limit == null ? 60 : Math.min(Math.max(limit, 1), MAX_HISTORY_POINTS);
        return history.getHistory(metric, n);
    }

    @McpTool(name = "get_service_status",
            title = "Service status",
            description = "Reports whether live data is available, when it was last updated and whether history is enabled.",
            annotations = @McpAnnotations(readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false))
    public Map<String, Object> getServiceStatus() {
        AstronomicalSnapshot s = dataService.getLatestSnapshot();
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("dataAvailable", s != null);
        status.put("lastUpdated", s == null ? null : s.lastUpdated());
        status.put("historyEnabled", historyService.isPresent());
        return status;
    }

    private AstronomicalSnapshot requireLatest() {
        AstronomicalSnapshot s = dataService.getLatestSnapshot();
        if (s == null) {
            throw new IllegalStateException("No data yet — the first calculation is still running. Retry shortly.");
        }
        return s;
    }

    /**
     * Snapshot as an ordered map minus display-only fields (ASCII art, Awtrix icon id)
     * that would only waste an agent's context.
     */
    static Map<String, Object> toToolView(AstronomicalSnapshot s) {
        Map<String, Object> view = new LinkedHashMap<>();
        for (var component : AstronomicalSnapshot.class.getRecordComponents()) {
            String name = component.getName();
            if (name.equals("moonAsciiArt") || name.equals("moonPhaseIcon")) {
                continue;
            }
            try {
                view.put(name, component.getAccessor().invoke(s));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot read snapshot field " + name, e);
            }
        }
        return view;
    }
}
