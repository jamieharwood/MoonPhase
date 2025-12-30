package org.iHarwood.MoonPhaseModule;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public final class MoonPhase {
    private static final double SYNODIC_MONTH = 29.53059;
    private static final double JULIAN_OFFSET = 4.867;

    private static final List<String[]> PHASES = Arrays.asList(
            new String[] { "       _..._     ", "     .:::::::.   ", "    :::::::::::  ", "    :::::::::::  ", "    `:::::::::'  ", "      `':::''    " }, // New
            new String[] { "       _..._     ", "     .::::. `.   ", "    :::::::.  :  ", "    ::::::::  :  ", "    `::::::' .'  ", "      `'::'-'    " }, // Waxing crescent
            new String[] { "       _..._     ", "     .::::  `.   ", "    ::::::    :  ", "    ::::::    :  ", "    `:::::   .'  ", "      `'::.-'    " }, // First quarter
            new String[] { "       _..._     ", "     .::'   `.   ", "    :::       :  ", "    :::       :  ", "    `::.     .'  ", "      `':..-'    " }, // Waxing gibbous
            new String[] { "       _..._     ", "     .'     `.   ", "    :         :  ", "    :         :  ", "    `.       .'  ", "      `-...-'    " }, // Full
            new String[] { "       _..._     ", "     .'   `::.   ", "    :       :::  ", "    :       :::  ", "    `.     .::'  ", "      `-..:''    " }, // Waning gibbous
            new String[] { "       _..._     ", "     .'  ::::.   ", "    :    ::::::  ", "    :    ::::::  ", "    `.   :::::'  ", "      `-.::''    " }, // Last quarter
            new String[] { "       _..._     ", "     .' .::::.   ", "    :  ::::::::  ", "    :  ::::::::  ", "    `. '::::::'  ", "      `-.::''    " }  // Waning crescent
    );
    private static final String[] PHASE_ICON_NAME = {
            "nwmoon", "wancrebmoon", "fqmoon", "wgmoon",
            "FullMoon", "wangmoon", "lqmoon", "wcmoon"
    };
    private static final String[] NAMES = {
            "New Moon", "Waxing Crescent", "First Quarter", "Waxing Gibbous",
            "Full Moon", "Waning Gibbous", "Last Quarter", "Waning Crescent"
    };

    private final double phaseFraction; // [0,1)
    private final double ageDays;

    private MoonPhase(double phaseFraction, double ageDays) {
        this.phaseFraction = phaseFraction;
        this.ageDays = ageDays;
    }

    public static MoonPhase fromDate(LocalDate date) {
        int jdn = DateUtils.dateToJulianDayNumber(date.getDayOfMonth(), date.getMonthValue(), date.getYear());

        double phaseFraction = ((jdn + JULIAN_OFFSET) / SYNODIC_MONTH);
        phaseFraction = phaseFraction - Math.floor(phaseFraction);
        // shift so original algorithm's age mapping is preserved
        double shifted = (phaseFraction + 0.5) % 1.0;
        double ageDays = Math.floor(shifted * SYNODIC_MONTH) + 1.0;

        return new MoonPhase(phaseFraction, ageDays);
    }

    /*public double getPhaseFraction() {
        return phaseFraction;
    }*/

    public double getAgeDays() {
        return ageDays;
    }

    public String getPhaseName() {
        int idx = (int) Math.floor(phaseFraction * 8.0);
        idx = Math.min(Math.max(idx, 0), 7);
        return NAMES[idx];
    }

    public String getPhaseIcon() {
        int idx = (int) Math.floor(phaseFraction * 8.0);
        idx = Math.min(Math.max(idx, 0), 7);
        return PHASE_ICON_NAME[idx];
    }

    public String[] getAscii() {
        int idx = (int) Math.floor(phaseFraction * 8.0);
        idx = Math.min(Math.max(idx, 0), 7);
        return PHASES.get(idx);
    }
}
