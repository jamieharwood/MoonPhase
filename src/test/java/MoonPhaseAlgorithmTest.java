import org.iHarwood.MoonPhaseModule.*;

public class MoonPhaseAlgorithmTest {
    public static void main(String[] args) {
        // Known new moon: December 30, 2025 at 22:27 UTC (JDN 2460674.434)
        // This means JDN 2460674 (Dec 30, 2025) is essentially a new moon

        int jdnNewMoon = DateUtils.dateToJulianDayNumber(30, 12, 2025);
        System.out.println("JDN for Dec 30, 2025 (New Moon): " + jdnNewMoon);

        int jdnJan6 = DateUtils.dateToJulianDayNumber(6, 1, 2026);
        System.out.println("JDN for Jan 6, 2026: " + jdnJan6);

        int daysSinceNewMoon = jdnJan6 - jdnNewMoon;
        System.out.println("Days since new moon: " + daysSinceNewMoon);

        double synodicMonth = 29.53059;
        double phaseAge = daysSinceNewMoon % synodicMonth;
        System.out.println("Phase age: " + phaseAge + " days");

        double phaseFraction = phaseAge / synodicMonth;
        System.out.println("Phase fraction: " + phaseFraction);

        // Phase mapping: 0-0.125=New, 0.125-0.375=Waxing Crescent, 0.375-0.625=First Quarter, etc
        String[] phaseNames = {"New Moon", "Waxing Crescent", "First Quarter", "Waxing Gibbous",
                              "Full Moon", "Waning Gibbous", "Last Quarter", "Waning Crescent"};
        int phaseIndex = (int)(phaseFraction * 8);
        if (phaseIndex >= 8) phaseIndex = 7;
        System.out.println("Phase: " + phaseNames[phaseIndex]);

        // Now test the current algorithm
        System.out.println("\n=== Current Algorithm ===");
        double SYNODIC_MONTH = 29.53059;
        double JULIAN_OFFSET = 4.867;

        double currentPhaseFrac = ((jdnJan6 + JULIAN_OFFSET) / SYNODIC_MONTH);
        currentPhaseFrac = currentPhaseFrac - Math.floor(currentPhaseFrac);
        double shifted = (currentPhaseFrac + 0.5) % 1.0;
        double ageDays = Math.floor(shifted * SYNODIC_MONTH) + 1.0;

        System.out.println("Current phase fraction: " + currentPhaseFrac);
        System.out.println("Current age days: " + ageDays);
        System.out.println("Current phase index: " + (int)(currentPhaseFrac * 8));
    }
}

