package org.iHarwood;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Unit tests for POST /api/history/populate. */
class DashboardControllerPopulateTest {

    private HistoryService historyService;
    private Main main;
    private ExecutorService executor;
    private Runnable queued;

    @BeforeEach
    void setUp() {
        historyService = mock(HistoryService.class);
        main = mock(Main.class);
        when(main.calculateSnapshotForDate(any())).thenReturn(
                AstronomicalSnapshot.builder().moonPhaseName("Full Moon").lastUpdated("x").build());
        executor = mock(ExecutorService.class);
        doAnswer(inv -> { queued = inv.getArgument(0); return null; }).when(executor).execute(any());
    }

    private DashboardController controller(Optional<HistoryService> history) {
        return new DashboardController(mock(AstronomicalDataService.class), history, main, executor);
    }

    @SuppressWarnings("unchecked")
    private int totalSaved() {
        ArgumentCaptor<List<SnapshotDocument>> captor = ArgumentCaptor.forClass(List.class);
        verify(historyService, atLeastOnce()).saveAll(captor.capture());
        return captor.getAllValues().stream().mapToInt(List::size).sum();
    }

    @Test
    void acceptsMoreThan365DaysAndSavesInBatches() {
        assertEquals(202, controller(Optional.of(historyService)).populateHistory(2500).getStatusCode().value());
        queued.run();
        assertEquals(2500, totalSaved());
        verify(historyService, times(3)).saveAll(any()); // 1000 + 1000 + 500
        verify(main, times(2500)).calculateSnapshotForDate(any());
    }

    @Test
    void trimsRequestsBefore1900() {
        controller(Optional.of(historyService)).populateHistory(1_000_000).getStatusCode();
        queued.run();
        long expected = ChronoUnit.DAYS.between(DashboardController.EARLIEST_POPULATE_DATE,
                LocalDate.now(ZoneOffset.UTC));
        assertEquals(expected, totalSaved());
    }

    @Test
    void rejectsZeroOrNegativeDays() {
        assertEquals(400, controller(Optional.of(historyService)).populateHistory(0).getStatusCode().value());
        verifyNoInteractions(executor);
    }

    @Test
    void rejectsOverlappingPopulate() {
        DashboardController c = controller(Optional.of(historyService));
        assertEquals(202, c.populateHistory(5).getStatusCode().value());
        assertEquals(429, c.populateHistory(5).getStatusCode().value());
        queued.run();
        assertEquals(202, c.populateHistory(5).getStatusCode().value());
    }

    @Test
    void returns503WhenHistoryDisabled() {
        assertEquals(503, controller(Optional.empty()).populateHistory(10).getStatusCode().value());
    }
}
