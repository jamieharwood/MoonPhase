package org.iHarwood.MoonPhaseModule;

import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;

/**
 * Calculates days until Earth's next perihelion (closest to Sun) and aphelion (farthest).
 *
 * Approximate dates:
 *   Perihelion:  ~January 3 each year
 *   Aphelion:    ~July 4 each year
 *
 * At perihelion  Earth is ~0.983 AU from the Sun (~147.1 million km).
 * At aphelion    Earth is ~1.017 AU from the Sun (~152.1 million km).
 */
public final class PerihelionAphelion {
    private PerihelionAphelion() {}

    // Approximate fixed calendar dates
    private static final int PERIHELION_MONTH = 1;
    private static final int PERIHELION_DAY = 3;
    private static final int APHELION_MONTH = 7;
    private static final int APHELION_DAY = 4;

    // --- Perihelion ---
    public static LocalDate nextPerihelion() {
        return nextPerihelion(LocalDate.now());
    }

    public static LocalDate nextPerihelion(LocalDate from) {
        LocalDate candidate = LocalDate.of(from.getYear(), Month.of(PERIHELION_MONTH), PERIHELION_DAY);
        return from.isBefore(candidate) ? candidate : candidate.plusYears(1);
    }

    public static long daysUntilPerihelion() {
        return daysUntilPerihelion(LocalDate.now());
    }

    public static long daysUntilPerihelion(LocalDate from) {
        return ChronoUnit.DAYS.between(from, nextPerihelion(from));
    }

    // --- Aphelion ---
    public static LocalDate nextAphelion() {
        return nextAphelion(LocalDate.now());
    }

    public static LocalDate nextAphelion(LocalDate from) {
        LocalDate candidate = LocalDate.of(from.getYear(), Month.of(APHELION_MONTH), APHELION_DAY);
        return from.isBefore(candidate) ? candidate : candidate.plusYears(1);
    }

    public static long daysUntilAphelion() {
        return daysUntilAphelion(LocalDate.now());
    }

    public static long daysUntilAphelion(LocalDate from) {
        return ChronoUnit.DAYS.between(from, nextAphelion(from));
    }
}

