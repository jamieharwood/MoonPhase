package org.iHarwood.MoonPhaseModule;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public final class MoonPhase {
    private static final double SYNODIC_MONTH = 29.53059;
    // Known new moon: January 2, 2026 (JDN 2461033)
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

    public double getAgeDays() {
        return Math.round(ageDays);
    }

    public String getPhaseName() {
        int idx = (int) Math.floor((phaseFraction + 0.0625) * 8.0);
        idx = Math.min(Math.max(idx, 0), 7);
        return NAMES[idx];
    }

    public String getPhaseIcon() {
        int idx = (int) Math.floor((phaseFraction + 0.0625) * 8.0);
        idx = Math.min(Math.max(idx, 0), 7);
        return PHASE_ICON_NAME[idx];
    }

    public String[] getAscii() {
        int idx = (int) Math.floor((phaseFraction + 0.0625) * 8.0);
        idx = Math.min(Math.max(idx, 0), 7);
        return PHASES.get(idx);
    }
}
