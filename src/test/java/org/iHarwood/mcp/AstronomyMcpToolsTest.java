package org.iHarwood.mcp;

import org.iHarwood.AstronomicalDataService;
import org.iHarwood.AstronomicalSnapshot;
import org.iHarwood.HistoryService;
import org.iHarwood.Main;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Unit tests for the MCP tool surface and its optional bearer-token guard. */
class AstronomyMcpToolsTest {

    private AstronomicalDataService dataService;
    private HistoryService historyService;
    private Main main;

    private static AstronomicalSnapshot sampleSnapshot() {
        return AstronomicalSnapshot.builder()
                .moonPhaseName("Full Moon")
                .moonIlluminationPercent(99)
                .moonPhaseIcon("12345")
                .moonAsciiArt(new String[]{"(  )"})
                .moonAgeDays(15)
                .daysUntilFullMoon(0)
                .moonDistanceKm(384_400)
                .daylightHours(8.1)
                .lastUpdated("2026-11-22T12:01:00Z")
                .build();
    }

    @BeforeEach
    void setUp() {
        dataService = mock(AstronomicalDataService.class);
        historyService = mock(HistoryService.class);
        main = mock(Main.class);
    }

    private AstronomyMcpTools tools(boolean historyEnabled) {
        return new AstronomyMcpTools(dataService,
                historyEnabled ? Optional.of(historyService) : Optional.empty(), main);
    }

    @Test
    void currentData_dropsDisplayOnlyFields() {
        when(dataService.getLatestSnapshot()).thenReturn(sampleSnapshot());
        Map<String, Object> view = tools(false).getCurrentAstronomicalData();
        assertEquals("Full Moon", view.get("moonPhaseName"));
        assertEquals(8.1, view.get("daylightHours"));
        assertFalse(view.containsKey("moonAsciiArt"));
        assertFalse(view.containsKey("moonPhaseIcon"));
        assertTrue(view.containsKey("lastUpdated"));
    }

    @Test
    void currentData_beforeFirstUpdate_isAToolError() {
        when(dataService.getLatestSnapshot()).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> tools(false).getCurrentAstronomicalData());
        assertThrows(IllegalStateException.class, () -> tools(false).getMoonPhase());
    }

    @Test
    void moonPhase_returnsOnlyMoonFields() {
        when(dataService.getLatestSnapshot()).thenReturn(sampleSnapshot());
        Map<String, Object> moon = tools(false).getMoonPhase();
        assertEquals(Set.of("moonPhaseName", "moonIlluminationPercent", "moonAgeDays",
                "daysUntilFullMoon", "moonDistanceKm", "lastUpdated"), moon.keySet());
        assertEquals(99, moon.get("moonIlluminationPercent"));
    }

    @Test
    void dataForDate_computesAtNoonUtc() {
        when(main.calculateSnapshotForDate(any())).thenReturn(sampleSnapshot());
        tools(false).getAstronomicalDataForDate(" 2027-03-20 ");
        verify(main).calculateSnapshotForDate(
                ZonedDateTime.of(2027, 3, 20, 12, 0, 0, 0, ZoneOffset.UTC));
    }

    @Test
    void dataForDate_rejectsBadOrOutOfRangeDates() {
        AstronomyMcpTools t = tools(false);
        assertThrows(IllegalArgumentException.class, () -> t.getAstronomicalDataForDate("20/03/2027"));
        assertThrows(IllegalArgumentException.class, () -> t.getAstronomicalDataForDate(null));
        assertThrows(IllegalArgumentException.class, () -> t.getAstronomicalDataForDate("1899-12-31"));
        assertThrows(IllegalArgumentException.class, () -> t.getAstronomicalDataForDate("2101-01-01"));
        verifyNoInteractions(main);
    }

    @Test
    void metricHistory_requiresHistoryAndValidMetric() {
        assertThrows(IllegalStateException.class,
                () -> tools(false).getMetricHistory("daylightHours", 10));
        assertThrows(IllegalArgumentException.class,
                () -> tools(true).getMetricHistory("notAMetric", 10));
    }

    @Test
    void metricHistory_clampsLimitAndDefaultsTo60() {
        List<Map<String, Object>> points = List.of(Map.of("timestamp", "22 Nov 12:01", "value", 8.1));
        when(historyService.getHistory(any(), anyInt())).thenReturn(points);
        AstronomyMcpTools t = tools(true);
        assertSame(points, t.getMetricHistory("daylightHours", null));
        t.getMetricHistory("daylightHours", 10_000);
        t.getMetricHistory("daylightHours", -5);
        verify(historyService).getHistory("daylightHours", 60);
        verify(historyService).getHistory("daylightHours", 500);
        verify(historyService).getHistory("daylightHours", 1);
    }

    @Test
    void listMetrics_reportsHistoryFlag() {
        Map<String, Object> result = tools(false).listHistoryMetrics();
        assertEquals(false, result.get("historyEnabled"));
        assertTrue(((Set<?>) result.get("metrics")).contains("moonIlluminationPercent"));
    }

    @Test
    void serviceStatus_reflectsSnapshotAvailability() {
        when(dataService.getLatestSnapshot()).thenReturn(null);
        assertEquals(false, tools(true).getServiceStatus().get("dataAvailable"));
        when(dataService.getLatestSnapshot()).thenReturn(sampleSnapshot());
        Map<String, Object> status = tools(true).getServiceStatus();
        assertEquals(true, status.get("dataAvailable"));
        assertEquals(true, status.get("historyEnabled"));
    }

    @Test
    void authFilter_acceptsOnlyMatchingBearerToken() throws Exception {
        McpAuthFilter filter = new McpAuthFilter("s3cret");

        MockHttpServletResponse denied = new MockHttpServletResponse();
        MockFilterChain deniedChain = new MockFilterChain();
        filter.doFilter(new MockHttpServletRequest("POST", "/mcp"), denied, deniedChain);
        assertEquals(401, denied.getStatus());
        assertNull(deniedChain.getRequest());

        MockHttpServletRequest wrong = new MockHttpServletRequest("POST", "/mcp");
        wrong.addHeader("Authorization", "Bearer nope");
        MockHttpServletResponse wrongResp = new MockHttpServletResponse();
        filter.doFilter(wrong, wrongResp, new MockFilterChain());
        assertEquals(401, wrongResp.getStatus());

        MockHttpServletRequest ok = new MockHttpServletRequest("POST", "/mcp");
        ok.addHeader("Authorization", "Bearer s3cret");
        MockFilterChain okChain = new MockFilterChain();
        filter.doFilter(ok, new MockHttpServletResponse(), okChain);
        assertNotNull(okChain.getRequest());
    }
}
