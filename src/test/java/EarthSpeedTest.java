import org.iHarwood.MoonPhaseModule.EarthSpeed;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EarthSpeed utility class.
 */
class EarthSpeedTest {

    // Earth orbital speed ranges from ~29.29 km/s (aphelion, July) to ~30.29 km/s (perihelion, Jan)
    private static final double MIN_SPEED_KM_S = 29.0;
    private static final double MAX_SPEED_KM_S = 31.0;

    @Test
    void speedKmPerSec_perihelion_isFaster() {
        // Perihelion is around January 4 — Earth moves fastest
        ZonedDateTime perihelion = ZonedDateTime.of(2026, 1, 4, 0, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime aphelion   = ZonedDateTime.of(2026, 7, 4, 0, 0, 0, 0, ZoneOffset.UTC);

        double v_perihelion = EarthSpeed.speedKmPerSec(perihelion);
        double v_aphelion   = EarthSpeed.speedKmPerSec(aphelion);

        assertTrue(v_perihelion > v_aphelion,
                "Speed at perihelion (" + v_perihelion + ") should exceed speed at aphelion (" + v_aphelion + ")");
    }

    @Test
    void speedKmPerSec_isInAnnualRange() {
        for (int month = 1; month <= 12; month++) {
            ZonedDateTime date = ZonedDateTime.of(2026, month, 15, 0, 0, 0, 0, ZoneOffset.UTC);
            double speed = EarthSpeed.speedKmPerSec(date);
            assertTrue(speed > MIN_SPEED_KM_S && speed < MAX_SPEED_KM_S,
                    "Speed for month " + month + " out of range: " + speed + " km/s");
        }
    }

    @Test
    void speedKmPerHour_isConsistentWithKmPerSec() {
        ZonedDateTime date = ZonedDateTime.of(2026, 5, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        double speedSec  = EarthSpeed.speedKmPerSec(date);
        double speedHour = EarthSpeed.speedKmPerHour(date);
        assertEquals(speedSec * 3600.0, speedHour, 0.01,
                "km/h should be exactly 3600 * km/s");
    }

    @Test
    void speedKmPerSecNow_returnsReasonableValue() {
        double speed = EarthSpeed.speedKmPerSecNow();
        assertTrue(speed > MIN_SPEED_KM_S && speed < MAX_SPEED_KM_S,
                "Current speed out of expected range: " + speed);
    }

    @Test
    void speedKmPerHourNow_isApproximately107000() {
        // Earth averages ~107,000 km/h
        double speed = EarthSpeed.speedKmPerHourNow();
        assertTrue(speed > 105_000 && speed < 110_000,
                "Expected ~107,000 km/h, got: " + speed);
    }

    @Test
    void speedMphNow_isApproximately67000() {
        // Earth averages ~67,000 mph
        double speed = EarthSpeed.speedMphNow();
        assertTrue(speed > 65_000 && speed < 69_000,
                "Expected ~67,000 mph, got: " + speed);
    }
}

