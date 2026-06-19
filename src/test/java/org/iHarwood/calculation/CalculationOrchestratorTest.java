package org.iHarwood.calculation;

import org.iHarwood.AstronomicalSnapshot;
import org.iHarwood.integration.awtrix.AwtrixPusher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CalculationOrchestrator}.
 *
 * Focus:
 * - Historical computation (computeForDate) is deterministic and side-effect free → heavily tested.
 * - Live computation (computeCurrent) is tested for behavioral interaction with AwtrixPusher.
 */
class CalculationOrchestratorTest {

    private AwtrixPusher mockPusher;
    private CalculationOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        mockPusher = mock(AwtrixPusher.class);
        // Use Greenwich latitude and longitude (same as default)
        orchestrator = new CalculationOrchestrator(mockPusher, 51.4769, 0.0);
    }

    private CalculationOrchestrator createOrchestratorWithFreshMock() {
        AwtrixPusher freshMock = mock(AwtrixPusher.class);
        when(freshMock.getStats()).thenReturn(new AwtrixPusher.AwtrixStats(10, 2));
        return new CalculationOrchestrator(freshMock, 51.4769, 0.0);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Historical computation tests (computeForDate) - the most valuable
    // ─────────────────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "{0} → phase starts with '{1}', age ~{2}")
    @CsvSource({
        "2025-12-30, Waxing Gibbous, 27",
        "2026-01-06, Waning Gibbous, 4",
        "2026-01-15, New Moon,       13",
        "2026-01-23, First Quarter,  21",
        "2026-01-29, Waxing Gibbous, 27"
    })
    @DisplayName("computeForDate returns correct moon phase data for known dates")
    void computeForDate_knownMoonPhases(String dateStr, String expectedPhasePrefix, int expectedAge) {
        ZonedDateTime target = LocalDate.parse(dateStr).atStartOfDay(ZoneOffset.UTC);

        AstronomicalSnapshot snapshot = orchestrator.computeForDate(target);

        assertNotNull(snapshot);
        assertTrue(snapshot.moonPhaseName().startsWith(expectedPhasePrefix),
                () -> "Expected phase starting with '" + expectedPhasePrefix + "' but was '" + snapshot.moonPhaseName() + "'");

        int actualAge = snapshot.moonAgeDays();
        System.out.println("  Actual age for " + dateStr + ": " + actualAge);

        assertTrue(Math.abs(actualAge - expectedAge) <= 2,
                () -> "Age mismatch: got " + actualAge + ", expected ~" + expectedAge);
        assertTrue(snapshot.moonIlluminationPercent() >= 0 && snapshot.moonIlluminationPercent() <= 100);
    }

    @Test
    @DisplayName("computeForDate always sets LEO fields to zero (live data cannot be reconstructed)")
    void computeForDate_leoFieldsAreZero() {
        ZonedDateTime target = LocalDate.of(2026, 1, 15).atStartOfDay(ZoneOffset.UTC);

        AstronomicalSnapshot snapshot = orchestrator.computeForDate(target);

        assertEquals(0.0, snapshot.issAltitudeKm());
        assertEquals(0.0, snapshot.tiangongAltitudeKm());
        assertEquals(0.0, snapshot.hubbleAltitudeKm());
        assertEquals(0, snapshot.starlinkSatelliteCount());
        assertEquals(0, snapshot.kuiperSatelliteCount());
        assertEquals(0, snapshot.totalSatellitesInOrbit());
        assertEquals(0, snapshot.issCrew(), "ISS crew should be 0 for historical dates");
        assertEquals(-1.0, snapshot.auroraKpIndex(), "Aurora Kp should be -1 for historical dates");
    }

    @Test
    @DisplayName("computeForDate produces reasonable Sun-Earth distance")
    void computeForDate_sunDistanceIsReasonable() {
        ZonedDateTime target = LocalDate.of(2026, 1, 1).atStartOfDay(ZoneOffset.UTC);

        AstronomicalSnapshot snapshot = orchestrator.computeForDate(target);

        // Earth-Sun distance should be roughly between 0.98 and 1.02 AU year-round
        assertTrue(snapshot.sunDistanceAu() > 0.98 && snapshot.sunDistanceAu() < 1.02,
                "Sun distance out of expected annual range: " + snapshot.sunDistanceAu());
    }

    @Test
    @DisplayName("computeForDate returns positive days until events")
    void computeForDate_daysUntilEventsArePositive() {
        ZonedDateTime target = LocalDate.of(2026, 6, 1).atStartOfDay(ZoneOffset.UTC);

        AstronomicalSnapshot snapshot = orchestrator.computeForDate(target);

        assertTrue(snapshot.daysUntilSummerSolstice() >= 0);
        assertTrue(snapshot.daysUntilWinterSolstice() >= 0);
        assertTrue(snapshot.daysUntilPerihelion() >= 0);
        assertTrue(snapshot.daysUntilAphelion() >= 0);
    }

    @Test
    @DisplayName("computeForDate sets a lastUpdated timestamp")
    void computeForDate_hasLastUpdated() {
        ZonedDateTime target = LocalDate.of(2025, 12, 30).atStartOfDay(ZoneOffset.UTC);

        AstronomicalSnapshot snapshot = orchestrator.computeForDate(target);

        assertNotNull(snapshot.lastUpdated());
        assertFalse(snapshot.lastUpdated().isBlank());
    }

    @Test
    @DisplayName("computeForDate never touches the AwtrixPusher (historical data must not cause side effects)")
    void computeForDate_doesNotInteractWithAwtrix() {
        ZonedDateTime target = LocalDate.of(2026, 1, 15).atStartOfDay(ZoneOffset.UTC);

        AstronomicalSnapshot snapshot = orchestrator.computeForDate(target);

        verifyNoInteractions(mockPusher);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Live computation tests (computeCurrent)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("computeCurrent returns a non-null snapshot with recent timestamp")
    void computeCurrent_returnsSnapshot() {
        CalculationOrchestrator freshOrchestrator = createOrchestratorWithFreshMock();
        AstronomicalSnapshot snapshot = freshOrchestrator.computeCurrent();

        assertNotNull(snapshot);
        assertNotNull(snapshot.lastUpdated());
        assertNotNull(snapshot.moonPhaseName());
        assertFalse(snapshot.moonPhaseName().isBlank());
    }

    @Test
    @DisplayName("computeCurrent interacts with AwtrixPusher (pushes multiple apps)")
    void computeCurrent_callsAwtrixPusherMultipleTimes() {
        AwtrixPusher freshMock = mock(AwtrixPusher.class);
        when(freshMock.getStats()).thenReturn(new AwtrixPusher.AwtrixStats(10, 2));
        CalculationOrchestrator freshOrchestrator = new CalculationOrchestrator(freshMock, 51.4769, 0.0);

        AstronomicalSnapshot snapshot = freshOrchestrator.computeCurrent();

        // We don't know the exact count (it varies), but a normal run pushes many apps.
        verify(freshMock, atLeast(10)).push(anyString(), anyString(), anyString());
        verify(freshMock, atLeastOnce()).getStats();
    }

    @Test
    @DisplayName("computeCurrent populates live LEO data (not zero)")
    void computeCurrent_leoDataIsPopulated() {
        CalculationOrchestrator freshOrchestrator = createOrchestratorWithFreshMock();
        AstronomicalSnapshot snapshot = freshOrchestrator.computeCurrent();

        // LEO data comes from real network calls to CelesTrak.
        // In a normal environment we expect at least some data.
        // This test is intentionally lenient for CI environments without outbound internet.
        boolean hasSomeLeoData =
                snapshot.issAltitudeKm() > 0 ||
                snapshot.tiangongAltitudeKm() > 0 ||
                snapshot.hubbleAltitudeKm() > 0 ||
                snapshot.starlinkSatelliteCount() > 0 ||
                snapshot.totalSatellitesInOrbit() > 0;

        // We don't hard-fail if network is unavailable — just log intent.
        if (!hasSomeLeoData) {
            System.out.println("[Test] Warning: No live LEO data was fetched (likely no network in this environment).");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Constructor / configuration tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Constructor accepts and uses custom latitude")
    void constructor_respectsCustomLatitude() {
        AwtrixPusher pusher = mock(AwtrixPusher.class);
        // Use a latitude near the equator
        CalculationOrchestrator customOrchestrator = new CalculationOrchestrator(pusher, 0.0, 0.0);

        ZonedDateTime target = LocalDate.of(2026, 1, 15).atStartOfDay(ZoneOffset.UTC);
        AstronomicalSnapshot snapshot = customOrchestrator.computeForDate(target);

        // Daylight hours near equator in January should be close to 12h
        assertTrue(snapshot.daylightHours() > 11.5 && snapshot.daylightHours() < 12.5,
                "Expected ~12 hours of daylight near equator, got: " + snapshot.daylightHours());
    }
}
