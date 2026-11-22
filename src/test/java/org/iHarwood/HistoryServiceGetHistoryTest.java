package org.iHarwood;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Unit tests for HistoryService.getHistory read limits. */
class HistoryServiceGetHistoryTest {

    private SnapshotRepository repository;
    private HistoryService service;

    private static List<SnapshotDocument> docs(int n, boolean newestFirst) {
        List<SnapshotDocument> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int day = newestFirst ? n - 1 - i : i;
            AstronomicalSnapshot s = AstronomicalSnapshot.builder().daylightHours(day).lastUpdated("x").build();
            list.add(SnapshotDocument.from(s, Instant.parse("2020-01-01T12:00:00Z").plusSeconds(86_400L * day)));
        }
        return list;
    }

    @BeforeEach
    void setUp() {
        repository = mock(SnapshotRepository.class);
        service = new HistoryService(repository, 0);
    }

    @Test
    void limitAbove500IsNotCapped() {
        when(repository.findAllByOrderByTimestampDesc(any())).thenReturn(docs(3000, true));
        List<Map<String, Object>> result = service.getHistory("daylightHours", 3000);
        verify(repository).findAllByOrderByTimestampDesc(PageRequest.of(0, 3000));
        assertEquals(3000, result.size());
        assertEquals(0.0, result.get(0).get("value"), "oldest first");
        assertEquals(2999.0, result.get(2999).get("value"));
    }

    @Test
    void zeroLimitReturnsEverythingOldestFirst() {
        when(repository.findAll(any(Sort.class))).thenReturn(docs(5000, false));
        List<Map<String, Object>> result = service.getHistory("daylightHours", 0);
        verify(repository).findAll(Sort.by(Sort.Direction.ASC, "timestamp"));
        verify(repository, never()).findAllByOrderByTimestampDesc(any());
        assertEquals(5000, result.size());
        assertEquals(0.0, result.get(0).get("value"));
    }

    private static SnapshotDocument at(String instant, double daylight, double moonKm) {
        AstronomicalSnapshot s = AstronomicalSnapshot.builder()
                .daylightHours(daylight).moonDistanceKm(moonKm).lastUpdated("x").build();
        return SnapshotDocument.from(s, Instant.parse(instant));
    }

    @Test
    void monthlySumsDaylightAndAveragesSameDayRuns() {
        when(repository.findAll(any(Sort.class))).thenReturn(List.of(
                at("2025-01-01T00:01:00Z", 8.0, 0), at("2025-01-01T12:01:00Z", 10.0, 0), // same day → 9
                at("2025-01-02T12:00:00Z", 8.0, 0),
                at("2026-01-15T12:00:00Z", 7.5, 0)));
        List<Map<String, Object>> months = service.getMonthlyByYear("daylightHours");
        assertEquals(2, months.size());
        assertEquals(Map.of("year", 2025, "month", 1, "value", 17.0, "days", 2, "daysInMonth", 31), months.get(0));
        assertEquals(2026, months.get(1).get("year"));
        assertEquals(7.5, months.get(1).get("value"));
    }

    @Test
    void monthlyAveragesNonSummedMetrics() {
        when(repository.findAll(any(Sort.class))).thenReturn(List.of(
                at("2024-02-01T12:00:00Z", 0, 360_000), at("2024-02-02T12:00:00Z", 0, 400_000)));
        Map<String, Object> feb = service.getMonthlyByYear("moonDistanceKm").get(0);
        assertEquals(380_000.0, feb.get("value"));
        assertEquals(29, feb.get("daysInMonth"));
    }

    @Test
    void unknownMetricIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.getHistory("nope", 10));
    }
}
