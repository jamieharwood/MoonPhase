import org.iHarwood.MoonPhaseModule.SunriseSunset;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SunriseSunset, checked against almanac times for the Royal Observatory, Greenwich.
 */
class SunriseSunsetTest {

    private static final double GREENWICH_LAT = 51.4769;
    private static final double GREENWICH_LON = 0.0;
    private static final long TOLERANCE_MINUTES = 2;

    private static void assertNear(String expectedUtc, String actualUtc) {
        long diff = Math.abs(ChronoUnit.MINUTES.between(LocalTime.parse(expectedUtc), LocalTime.parse(actualUtc)));
        assertTrue(diff <= TOLERANCE_MINUTES, "Expected ~" + expectedUtc + " UTC, got " + actualUtc);
    }

    @Test
    void summerSolsticeGreenwich() {
        LocalDate date = LocalDate.of(2026, Month.JUNE, 21);
        assertNear("03:43", SunriseSunset.sunriseUtc(date, GREENWICH_LAT, GREENWICH_LON));
        assertNear("20:21", SunriseSunset.sunsetUtc(date, GREENWICH_LAT, GREENWICH_LON));
    }

    @Test
    void winterSolsticeGreenwich() {
        LocalDate date = LocalDate.of(2026, Month.DECEMBER, 21);
        assertNear("08:04", SunriseSunset.sunriseUtc(date, GREENWICH_LAT, GREENWICH_LON));
        assertNear("15:53", SunriseSunset.sunsetUtc(date, GREENWICH_LAT, GREENWICH_LON));
    }

    @Test
    void westernLongitudeShiftsTimesLater() {
        LocalDate date = LocalDate.of(2026, Month.MARCH, 20);
        LocalTime greenwich = LocalTime.parse(SunriseSunset.sunriseUtc(date, GREENWICH_LAT, 0.0));
        LocalTime west15 = LocalTime.parse(SunriseSunset.sunriseUtc(date, GREENWICH_LAT, -15.0));
        assertEquals(60, ChronoUnit.MINUTES.between(greenwich, west15), 2);
    }

    @Test
    void polarCases() {
        assertEquals("Polar Night", SunriseSunset.sunriseUtc(LocalDate.of(2026, Month.DECEMBER, 21), 89.0, 0.0));
        assertEquals("Midnight Sun", SunriseSunset.sunsetUtc(LocalDate.of(2026, Month.JUNE, 21), 89.0, 0.0));
    }
}
