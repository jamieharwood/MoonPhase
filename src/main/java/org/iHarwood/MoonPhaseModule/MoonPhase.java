package org.iHarwood.MoonPhaseModule;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public final class MoonPhase {
    private static final double SYNODIC_MONTH = 29.53059;
    private static final double TWO_PI = 2 * Math.PI;
    // Known new moon: January 2, 2026 (JDN 2461043)
    // Verified against astronomical data and https://moonphases.co.uk/
    private static final int KNOWN_NEW_MOON_JDN = 2461043;

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
        int jdn = DateUtils.dateToJulianDayNumber(date.getDayOfMonth(), date.getMonthValue(), date.getYear());

        // Calculate days since known new moon
        double daysSinceNewMoon = jdn - KNOWN_NEW_MOON_JDN;

        // Calculate current position in the synodic cycle
        double phaseFraction = (daysSinceNewMoon % SYNODIC_MONTH) / SYNODIC_MONTH;
        if (phaseFraction < 0) {
            phaseFraction += 1.0;
        }

        // Calculate age in days (0-29.53)
        double ageDays = phaseFraction * SYNODIC_MONTH;

        return new MoonPhase(phaseFraction, ageDays);
    }

    /**
     * Creates a MoonPhase instance from a phase name string.
     * Used when Claude AI corrects the calculated phase.
     *
     * @param phaseName one of the standard phase names (e.g. "Full Moon", "Waning Gibbous")
     * @return a MoonPhase with the matching phase index, or null if the name is not recognised
     */
    public static MoonPhase fromPhaseName(String phaseName) {
        for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equalsIgnoreCase(phaseName.trim())) {
                // Set phaseFraction to the centre of this phase's bin
                double phaseFraction = (i / 8.0);
                double ageDays = phaseFraction * SYNODIC_MONTH;
                return new MoonPhase(phaseFraction, ageDays);
            }
        }
        return null;
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

    private int phaseIndex() {
        int idx = (int) Math.floor((phaseFraction + 0.0625) * 8.0);
        return Math.min(Math.max(idx, 0), 7);
    }
}
