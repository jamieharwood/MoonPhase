import org.iHarwood.MoonPhaseModule.DayLight;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DayLight utility class.
 */
class DayLightTest {

    private static final double GREENWICH_LAT = 51.4769;

    @Test
    void dayLengthHours_equinoxIsApproxTwelveHours() {
        // At spring equinox (March 20) day length should be ~12h everywhere
        LocalDate equinox = LocalDate.of(2026, Month.MARCH, 20);
        double dayLength = DayLight.dayLengthHours(equinox, GREENWICH_LAT);
        assertTrue(dayLength > 11.5 && dayLength < 12.5,
                "Expected ~12h at equinox, got: " + dayLength);
    }

    @Test
    void dayLengthHours_summerLongerThanWinterInNorthernHemisphere() {
        LocalDate summer = LocalDate.of(2026, Month.JUNE, 21);
        LocalDate winter = LocalDate.of(2026, Month.DECEMBER, 21);
        double summerDay = DayLight.dayLengthHours(summer, GREENWICH_LAT);
        double winterDay = DayLight.dayLengthHours(winter, GREENWICH_LAT);
        assertTrue(summerDay > winterDay,
                "Summer day (" + summerDay + ") should be longer than winter (" + winterDay + ")");
    }

    @Test
    void dayLengthHours_summerShorterThanWinterInSouthernHemisphere() {
        double southLat = -33.9; // Cape Town
        LocalDate june = LocalDate.of(2026, Month.JUNE, 21);
        LocalDate december = LocalDate.of(2026, Month.DECEMBER, 21);
        double juneDay = DayLight.dayLengthHours(june, southLat);
        double decDay  = DayLight.dayLengthHours(december, southLat);
        assertTrue(decDay > juneDay,
                "Southern hemisphere summer (Dec) should be longer than June, got June=" + juneDay + " Dec=" + decDay);
    }

    @Test
    void dayLengthHours_equatorIsAlwaysNearTwelveHours() {
        double equatorLat = 0.0;
        for (Month m : Month.values()) {
            LocalDate date = LocalDate.of(2026, m, 15);
            double dayLength = DayLight.dayLengthHours(date, equatorLat);
            assertTrue(dayLength > 11.8 && dayLength < 12.2,
                    "Equator day length should be ~12h in any month, got " + dayLength + " for " + m);
        }
    }

    @Test
    void dayLengthHours_polarNight_returnsZero() {
        // North Pole in December has polar night
        double northPoleLat = 90.0;
        LocalDate december = LocalDate.of(2026, Month.DECEMBER, 21);
        double dayLength = DayLight.dayLengthHours(december, northPoleLat);
        assertEquals(0.0, dayLength, "North Pole in December should return 0 (polar night)");
    }

    @Test
    void dayLengthHours_midnightSun_returnsTwentyFourHours() {
        // North Pole in June has midnight sun
        double northPoleLat = 90.0;
        LocalDate june = LocalDate.of(2026, Month.JUNE, 21);
        double dayLength = DayLight.dayLengthHours(june, northPoleLat);
        assertEquals(24.0, dayLength, "North Pole in June should return 24 (midnight sun)");
    }

    @Test
    void minMaxDayLengthHours_summerLongerThanWinter() {
        double[] range = DayLight.minMaxDayLengthHoursNow(GREENWICH_LAT);
        assertEquals(2, range.length);
        assertTrue(range[1] > range[0], "Max day length should exceed min");
        assertTrue(range[0] > 0 && range[0] < 12, "Min day should be less than 12h");
        assertTrue(range[1] > 12 && range[1] < 24, "Max day should be more than 12h");
    }

    @Test
    void dayLengthHours_greenwichSummerReasonable() {
        LocalDate solstice = LocalDate.of(2026, Month.JUNE, 21);
        double dayLength = DayLight.dayLengthHours(solstice, GREENWICH_LAT);
        assertTrue(dayLength > 16.0 && dayLength < 18.0,
                "Greenwich summer solstice should be ~16.5h, got: " + dayLength);
    }

    @Test
    void dayLengthHours_greenwichWinterReasonable() {
        LocalDate solstice = LocalDate.of(2026, Month.DECEMBER, 21);
        double dayLength = DayLight.dayLengthHours(solstice, GREENWICH_LAT);
        assertTrue(dayLength > 7.0 && dayLength < 9.0,
                "Greenwich winter solstice should be ~7.7h, got: " + dayLength);
    }
}

