package org.iHarwood.MoonPhaseModule;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class MoonPhase {
    private static final double SYNODIC_MONTH = 29.530588853;
    private static final double TWO_PI = 2 * Math.PI;
    // Mean new moon: 2000-01-06 18:14 UTC (JD 2451550.1).
    // Phase fraction 0 is new moon. Checked against https://moonphases.co.uk/.
    private static final double KNOWN_NEW_MOON_JD = 2451550.1;

    // https://moonphases.co.uk/moon-phases
    private static final List<String[]> PHASES = Arrays.asList(
            new String[] { "       _..._     ", "     .:::::::.   ", "    :::::::::::  ", "    :::::::::::  ", "    `:::::::::'  ", "      `':::''    " }, // Full
            new String[] { "       _..._     ", "     .::::. `.   ", "    :::::::.  :  ", "    ::::::::  :  ", "    `::::::' .'  ", "      `'::'-'    " }, // Waning gibbous
            new String[] { "       _..._     ", "     .::::  `.   ", "    ::::::    :  ", "    ::::::    :  ", "    `:::::   .'  ", "      `'::.-'    " }, // Last quarter
            new String[] { "       _..._     ", "     .::'   `.   ", "    :::       :  ", "    :::       :  ", "    `::.     .'  ", "      `':..-'    " }, // Waning crescent
            new String[] { "       _..._     ", "     .'     `.   ", "    :         :  ", "    :         :  ", "    `.       .'  ", "      `-...-'    " }, // New
            new String[] { "       _..._     ", "     .'   `::.   ", "    :       :::  ", "    :       :::  ", "    `.     .::'  ", "      `-..:''    " }, // Waxing crescent
            new String[] { "       _..._     ", "     .'  ::::.   ", "    :    ::::::  ", "    :    ::::::  ", "    `.   :::::'  ", "      `-.::''    " }, // First quarter
            new String[] { "       _..._     ", "     .' .::::.   ", "    :  ::::::::  ", "    :  ::::::::  ", "    `. '::::::'  ", "      `-.::''    " }  // Waxing gibbous

    );
    private static final String[] PHASE_ICON_NAME = {
            "FullMoon", "wangmoon", "lqmoon", "wcmoon",
            "nwmoon", "wancrebmoon", "fqmoon", "wgmoon"
    };
    private static final String[] NAMES = {
            "Full Moon", "Waning Gibbous", "Last Quarter", "Waning Crescent",
            "New Moon", "Waxing Crescent", "First Quarter", "Waxing Gibbous"
    };

    private final double phaseFraction; // [0,1)
    private final double ageDays;

    private MoonPhase(double phaseFraction, double ageDays) {
        this.phaseFraction = phaseFraction;
        this.ageDays = ageDays;
    }

    public static MoonPhase fromDate(LocalDate date) {
        // dateToJulianDayNumber is the Julian Date at 12:00 UTC, which matches
        // the daily phase published by moonphases.co.uk.
        double jd = DateUtils.dateToJulianDayNumber(date.getDayOfMonth(), date.getMonthValue(), date.getYear());
        return fromJulianDate(jd);
    }

    public static MoonPhase at(ZonedDateTime zdt) {
        ZonedDateTime utc = zdt.withZoneSameInstant(ZoneOffset.UTC);
        double noonJd = DateUtils.dateToJulianDayNumber(utc.getDayOfMonth(), utc.getMonthValue(), utc.getYear());
        double frac = (utc.getHour() + utc.getMinute() / 60.0 + utc.getSecond() / 3600.0) / 24.0;
        return fromJulianDate(noonJd - 0.5 + frac);
    }

    private static MoonPhase fromJulianDate(double jd) {
        double phaseFraction = (jd - KNOWN_NEW_MOON_JD) / SYNODIC_MONTH;
        phaseFraction = phaseFraction - Math.floor(phaseFraction);
        if (phaseFraction < 0) {
            phaseFraction += 1.0;
        }
        return new MoonPhase(phaseFraction, phaseFraction * SYNODIC_MONTH);
    }

    /**
     * Creates a MoonPhase instance from a phase name string.
     * Used when Claude AI corrects the calculated phase.
     *
     * @param phaseName one of the standard phase names (e.g. "Full Moon", "Waning Gibbous")
     * @return an Optional containing the matching MoonPhase, or empty if the name is not recognised
     */
    public static Optional<MoonPhase> fromPhaseName(String phaseName) {
        for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equalsIgnoreCase(phaseName.trim())) {
                // NAMES[0] is full moon, which is half a cycle after new moon (phaseFraction 0).
                double phaseFraction = (i / 8.0 + 0.5) % 1.0;
                double ageDays = phaseFraction * SYNODIC_MONTH;
                return Optional.of(new MoonPhase(phaseFraction, ageDays));
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the percentage of the moon's disc that is illuminated (0-100).
     */
    public int getIlluminationPercent() {
        return (int) Math.round((1 - Math.cos(TWO_PI * phaseFraction)) / 2 * 100);
    }

    /**
     * Returns the number of days until the next full moon (0 = today is full moon).
     */
    public int getDaysUntilFullMoon() {
        double fraction = ((0.5 - phaseFraction + 1.0) % 1.0);
        return (int) Math.round(fraction * SYNODIC_MONTH);
    }

    public int getAgeDays() {
        return (int) Math.round(ageDays);
    }

    public String getPhaseName() {
        return NAMES[phaseIndex()];
    }

    public String getPhaseIcon() {
        return PHASE_ICON_NAME[phaseIndex()];
    }

    public String[] getAscii() {
        return PHASES.get(phaseIndex());
    }

    /**
     * Index into {@link #NAMES}. Boundaries follow the phase ranges published by
     * moonphases.co.uk (phase 0 = new moon): waxing gibbous runs up to just before
     * full, rather than equal eighths that mislabel a 95% moon as full or new.
     */
    private int phaseIndex() {
        double p = phaseFraction;
        if (p < 0.03 || p >= 0.97) return 4; // New Moon
        if (p < 0.22) return 5;              // Waxing Crescent
        if (p < 0.28) return 6;              // First Quarter
        if (p < 0.47) return 7;              // Waxing Gibbous
        if (p < 0.53) return 0;              // Full Moon
        if (p < 0.72) return 1;              // Waning Gibbous
        if (p < 0.78) return 2;              // Last Quarter
        return 3;                            // Waning Crescent
    }
}
