package org.iHarwood.MoonPhaseModule;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;

/**
 * Utility to compute the next astronomical event dates (approximate fixed-date values).
 *
 * Notes:
 * - Uses common average dates (approximate):
 *   Vernal Equinox: March 20
 *   Autumnal Equinox: September 22
 *   Summer Solstice: June 21
 *   Winter Solstice: December 21
 * - Each "next" method returns the next occurrence strictly after the supplied date.
 */
public final class EquinoxCalculator {

    public EquinoxCalculator() { /* no instances */ }

    // --- Vernal (March) Equinox ---
    public static LocalDate nextVernalEquinox() {
        return nextVernalEquinox(LocalDate.now());
    }

    public static LocalDate nextVernalEquinox(LocalDate from) {
        LocalDate candidate = LocalDate.of(from.getYear(), Month.MARCH, 20);
        return from.isBefore(candidate) ? candidate : candidate.plusYears(1);
    }

    // --- Autumnal (September) Equinox ---
    public static LocalDate nextAutumnalEquinox() {
        return nextAutumnalEquinox(LocalDate.now());
    }

    public static LocalDate nextAutumnalEquinox(LocalDate from) {
        LocalDate candidate = LocalDate.of(from.getYear(), Month.SEPTEMBER, 22);
        return from.isBefore(candidate) ? candidate : candidate.plusYears(1);
    }

    // --- Summer Solstice (approximate) ---
    public static LocalDate nextSummerSolstice() {
        return nextSummerSolstice(LocalDate.now());
    }

    public static LocalDate nextSummerSolstice(LocalDate from) {
        LocalDate candidate = LocalDate.of(from.getYear(), Month.JUNE, 21);
        return from.isBefore(candidate) ? candidate : candidate.plusYears(1);
    }

    // --- Winter Solstice (approximate) ---
    public static LocalDate nextWinterSolstice() {
        return nextWinterSolstice(LocalDate.now());
    }

    public static LocalDate nextWinterSolstice(LocalDate from) {
        LocalDate candidate = LocalDate.of(from.getYear(), Month.DECEMBER, 21);
        return from.isBefore(candidate) ? candidate : candidate.plusYears(1);
    }

    // --- Days Until Methods ---
    /**
     * Calculates the number of days between today and the next Vernal Equinox.
     *
     * @return the number of days until the next Vernal Equinox
     */
    public static long daysUntilVernalEquinox() {
        return daysUntilVernalEquinox(LocalDate.now());
    }

    /**
     * Calculates the number of days between the given date and the next Vernal Equinox.
     *
     * @param from the starting date
     * @return the number of days until the next Vernal Equinox
     */
    public static long daysUntilVernalEquinox(LocalDate from) {
        LocalDate equinox = nextVernalEquinox(from);
        return ChronoUnit.DAYS.between(from, equinox);
    }

    /**
     * Calculates the number of days between today and the next Autumnal Equinox.
     *
     * @return the number of days until the next Autumnal Equinox
     */
    public static long daysUntilAutumnalEquinox() {
        return daysUntilAutumnalEquinox(LocalDate.now());
    }

    /**
     * Calculates the number of days between the given date and the next Autumnal Equinox.
     *
     * @param from the starting date
     * @return the number of days until the next Autumnal Equinox
     */
    public static long daysUntilAutumnalEquinox(LocalDate from) {
        LocalDate equinox = nextAutumnalEquinox(from);
        return ChronoUnit.DAYS.between(from, equinox);
    }

    /**
     * Calculates the number of days between today and the next Summer Solstice.
     *
     * @return the number of days until the next Summer Solstice
     */
    public static long daysUntilSummerSolstice() {
        return daysUntilSummerSolstice(LocalDate.now());
    }

    /**
     * Calculates the number of days between the given date and the next Summer Solstice.
     *
     * @param from the starting date
     * @return the number of days until the next Summer Solstice
     */
    public static long daysUntilSummerSolstice(LocalDate from) {
        LocalDate solstice = nextSummerSolstice(from);
        return ChronoUnit.DAYS.between(from, solstice);
    }

    /**
     * Calculates the number of days between today and the next Winter Solstice.
     *
     * @return the number of days until the next Winter Solstice
     */
    public static long daysUntilWinterSolstice() {
        return daysUntilWinterSolstice(LocalDate.now());
    }

    /**
     * Calculates the number of days between the given date and the next Winter Solstice.
     *
     * @param from the starting date
     * @return the number of days until the next Winter Solstice
     */
    public static long daysUntilWinterSolstice(LocalDate from) {
        LocalDate solstice = nextWinterSolstice(from);
        return ChronoUnit.DAYS.between(from, solstice);
    }
}
