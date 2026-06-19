import org.iHarwood.MoonPhaseModule.EquinoxCalculator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EquinoxCalculator utility class.
 */
class EquinoxCalculatorTest {

    @Test
    void nextSummerSolstice_returnsJune21_whenBeforeJune() {
        LocalDate beforeSolstice = LocalDate.of(2026, Month.MAY, 1);
        LocalDate solstice = EquinoxCalculator.nextSummerSolstice(beforeSolstice);
        assertEquals(LocalDate.of(2026, Month.JUNE, 21), solstice);
    }

    @Test
    void nextSummerSolstice_returnsNextYear_whenAfterJune21() {
        LocalDate afterSolstice = LocalDate.of(2026, Month.JULY, 1);
        LocalDate solstice = EquinoxCalculator.nextSummerSolstice(afterSolstice);
        assertEquals(LocalDate.of(2027, Month.JUNE, 21), solstice);
    }

    @Test
    void nextWinterSolstice_returnsDecember21_whenBeforeDecember() {
        LocalDate before = LocalDate.of(2026, Month.JUNE, 1);
        LocalDate solstice = EquinoxCalculator.nextWinterSolstice(before);
        assertEquals(LocalDate.of(2026, Month.DECEMBER, 21), solstice);
    }

    @Test
    void nextWinterSolstice_returnsNextYear_whenAfterDecember21() {
        LocalDate after = LocalDate.of(2026, Month.DECEMBER, 25);
        LocalDate solstice = EquinoxCalculator.nextWinterSolstice(after);
        assertEquals(LocalDate.of(2027, Month.DECEMBER, 21), solstice);
    }

    @Test
    void nextVernalEquinox_returnsMarch20() {
        LocalDate before = LocalDate.of(2026, Month.JANUARY, 1);
        LocalDate equinox = EquinoxCalculator.nextVernalEquinox(before);
        assertEquals(LocalDate.of(2026, Month.MARCH, 20), equinox);
    }

    @Test
    void nextAutumnalEquinox_returnsSeptember22() {
        LocalDate before = LocalDate.of(2026, Month.JANUARY, 1);
        LocalDate equinox = EquinoxCalculator.nextAutumnalEquinox(before);
        assertEquals(LocalDate.of(2026, Month.SEPTEMBER, 22), equinox);
    }

    @Test
    void daysUntilSummerSolstice_isPositive() {
        LocalDate before = LocalDate.of(2026, Month.JANUARY, 1);
        long days = EquinoxCalculator.daysUntilSummerSolstice(before);
        assertTrue(days > 0, "Days until summer solstice should be positive: " + days);
    }

    @Test
    void daysUntilWinterSolstice_isPositive() {
        LocalDate before = LocalDate.of(2026, Month.JANUARY, 1);
        long days = EquinoxCalculator.daysUntilWinterSolstice(before);
        assertTrue(days > 0, "Days until winter solstice should be positive: " + days);
    }

    @Test
    void daysUntilSummerSolstice_correctlyCountsDays() {
        LocalDate from = LocalDate.of(2026, Month.JUNE, 11); // 10 days before June 21
        long days = EquinoxCalculator.daysUntilSummerSolstice(from);
        assertEquals(10, days, "Should be 10 days before summer solstice");
    }

    @Test
    void daysUntilWinterSolstice_correctlyCountsDays() {
        LocalDate from = LocalDate.of(2026, Month.DECEMBER, 1); // 20 days before Dec 21
        long days = EquinoxCalculator.daysUntilWinterSolstice(from);
        assertEquals(20, days, "Should be 20 days before winter solstice");
    }

    @Test
    void daysUntilVernalEquinox_correctlyCountsDays() {
        LocalDate from = LocalDate.of(2026, Month.MARCH, 10); // 10 days before March 20
        long days = EquinoxCalculator.daysUntilVernalEquinox(from);
        assertEquals(10, days);
    }

    @Test
    void daysUntilAutumnalEquinox_correctlyCountsDays() {
        LocalDate from = LocalDate.of(2026, Month.SEPTEMBER, 17); // 5 days before Sep 22
        long days = EquinoxCalculator.daysUntilAutumnalEquinox(from);
        assertEquals(5, days);
    }

    @Test
    void summerAndWinterSolsticeAreNotOnSameDay() {
        LocalDate from = LocalDate.of(2026, Month.MARCH, 1);
        LocalDate summer = EquinoxCalculator.nextSummerSolstice(from);
        LocalDate winter = EquinoxCalculator.nextWinterSolstice(from);
        assertNotEquals(summer, winter, "Summer and winter solstice should be different dates");
    }
}

