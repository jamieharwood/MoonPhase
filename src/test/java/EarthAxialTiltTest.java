import org.iHarwood.MoonPhaseModule.EarthAxialTilt;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EarthAxialTilt utility class.
 */
class EarthAxialTiltTest {

    @Test
    void tiltDegrees_j2000_isApprox23_44() {
        // J2000.0 = January 1.5, 2000 (noon UT)
        ZonedDateTime j2000 = ZonedDateTime.of(2000, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        double tilt = EarthAxialTilt.tiltDegrees(j2000);
        // IAU formula: 23.439291° at J2000 — should be very close
        assertEquals(23.439291, tilt, 0.001, "Tilt at J2000 should be ~23.439291°");
    }

    @Test
    void tiltDegrees_currentEpoch_isReasonable() {
        // Current tilt is ~23.44° and changes very slowly
        ZonedDateTime now = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        double tilt = EarthAxialTilt.tiltDegrees(now);
        assertTrue(tilt > 23.0 && tilt < 24.0,
                "Current axial tilt should be roughly 23–24°, got: " + tilt);
    }

    @Test
    void tiltDegrees_decreasesOverTime() {
        // Earth's axial tilt is currently decreasing at ~0.013°/century
        ZonedDateTime past   = ZonedDateTime.of(1900, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime future = ZonedDateTime.of(2100, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        double pastTilt   = EarthAxialTilt.tiltDegrees(past);
        double futureTilt = EarthAxialTilt.tiltDegrees(future);
        assertTrue(pastTilt > futureTilt,
                "Tilt in 1900 (" + pastTilt + "°) should exceed tilt in 2100 (" + futureTilt + "°)");
    }

    @Test
    void tiltDegreesNow_returnsReasonableValue() {
        double tilt = EarthAxialTilt.tiltDegreesNow();
        assertTrue(tilt > 22.0 && tilt < 24.5,
                "Current tilt should be in historical range 22–24.5°, got: " + tilt);
    }

    @Test
    void tiltDegrees_changeOverCenturyIsSmall() {
        // Change over one century should be ~0.013° (very small)
        ZonedDateTime t1 = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime t2 = ZonedDateTime.of(2100, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        double tilt1 = EarthAxialTilt.tiltDegrees(t1);
        double tilt2 = EarthAxialTilt.tiltDegrees(t2);
        double change = Math.abs(tilt1 - tilt2);
        assertTrue(change < 0.02, "Tilt change per century should be < 0.02°, got: " + change);
    }
}

