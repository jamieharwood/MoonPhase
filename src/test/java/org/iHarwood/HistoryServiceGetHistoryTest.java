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

    @Test
    void unknownMetricIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.getHistory("nope", 10));
    }
}
