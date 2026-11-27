package org.iHarwood.MoonPhaseModule;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class MoonPhase {
    private static final double SYNODIC_MONTH = MoonEphemeris.SYNODIC_MONTH;
    private static final double TWO_PI = 2 * Math.PI;
    private static final double JD_UNIX_EPOCH = 2440587.5;

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

    private final double phaseFraction;   // [0,1): Moon−Sun longitude / 360°, 0 = new moon
    private final double ageDays;         // days since the previous true new moon
    private final double illumination;    // illuminated fraction 0–1
    private final double jd;              // moment of the calculation (NaN when built from a name)

    private MoonPhase(double phaseFraction, double ageDays, double illumination, double jd) {
        this.phaseFraction = phaseFraction;
        this.ageDays = ageDays;
        this.illumination = illumination;
        this.jd = jd;
    }

    /** Phase at 12:00 UTC on the given date (the convention used for daily snapshots). */
    public static MoonPhase fromDate(LocalDate date) {
        return fromJulianDate(DateUtils.dateToJulianDayNumber(date.getDayOfMonth(), date.getMonthValue(), date.getYear()));
    }

    /** Phase at the given instant. */
    public static MoonPhase at(ZonedDateTime zdt) {
        return fromJulianDate(DateUtils.julianDate(zdt));
    }

    /**
     * True phase from the geocentric Sun and Moon positions ({@link MoonEphemeris}),
     * not a mean 29.53-day cycle, so new/full moon land within minutes of the published times.
     */
    private static MoonPhase fromJulianDate(double jd) {
        MoonEphemeris.Position pos = MoonEphemeris.at(jd);
        double phaseFraction = pos.elongationLongitude() / 360.0;
        double age = jd - MoonEphemeris.previousPhase(jd, 0.0);
        return new MoonPhase(phaseFraction, age, pos.illuminatedFraction(), jd);
    }

    /**
     * Creates a MoonPhase instance from a phase name string (representative mid-phase values).
     * Used when Claude AI corrects the calculated phase. Next new/full moon times are not
     * available on instances built this way.
     *
     * @param phaseName one of the standard phase names (e.g. "Full Moon", "Waning Gibbous")
     * @return an Optional containing the matching MoonPhase, or empty if the name is not recognised
     */
    public static Optional<MoonPhase> fromPhaseName(String phaseName) {
        for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equalsIgnoreCase(phaseName.trim())) {
                // NAMES[0] is full moon, which is half a cycle after new moon (phaseFraction 0).
                double phaseFraction = (i / 8.0 + 0.5) % 1.0;
                double illumination = (1 - Math.cos(TWO_PI * phaseFraction)) / 2;
                return Optional.of(new MoonPhase(phaseFraction, phaseFraction * SYNODIC_MONTH, illumination, Double.NaN));
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the percentage of the moon's disc that is illuminated (0-100).
     */
    public int getIlluminationPercent() {
        return (int) Math.round(illumination * 100);
    }

    /**
     * Returns the number of days until the next full moon (0 = full moon within the last
     * or next half day, i.e. "tonight").
     */
    public int getDaysUntilFullMoon() {
        if (Double.isNaN(jd)) {
            double fraction = ((0.5 - phaseFraction + 1.0) % 1.0);
            return (int) Math.round(fraction * SYNODIC_MONTH);
        }
        if (jd - MoonEphemeris.previousPhase(jd, 180.0) < 0.5) {
            return 0;
        }
        return (int) Math.round(MoonEphemeris.nextPhase(jd, 180.0) - jd);
    }

    /** Moment of the next new moon after this phase's time (UTC). */
    public ZonedDateTime getNextNewMoon() {
        return toUtc(MoonEphemeris.nextPhase(requireTime(), 0.0));
    }

    /** Moment of the next full moon after this phase's time (UTC). */
    public ZonedDateTime getNextFullMoon() {
        return toUtc(MoonEphemeris.nextPhase(requireTime(), 180.0));
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

    private double requireTime() {
        if (Double.isNaN(jd)) {
            throw new IllegalStateException("Phase built from a name has no time");
        }
        return jd;
    }

    private static ZonedDateTime toUtc(double julianDate) {
        long millis = Math.round((julianDate - JD_UNIX_EPOCH) * 86_400_000.0);
        return Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).withNano(0);
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
