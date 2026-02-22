import java.time.LocalDate;
import org.iHarwood.MoonPhaseModule.*;

public class ActualMoonPhaseCheck {
    public static void main(String[] args) {
        // According to moonphases.co.uk, January 6, 2026 is Waning Gibbous
        // This means we're AFTER a full moon, not after a new moon

        // Let's work backwards:
        // If Jan 6 is Waning Gibbous, the full moon was recently
        // Waning Gibbous occurs 3-7 days after full moon

        // Testing: If full moon was around Dec 30-Jan 2, then Jan 6 would be Waning Gibbous
        // Let's check when the last full moon actually was

        System.out.println("=== Checking Current Algorithm ===");
        MoonPhase current = MoonPhase.fromDate(LocalDate.of(2026, 1, 6));
        System.out.println("Current calculation: " + current.getPhaseName() + " (" + (int)current.getAgeDays() + " days)");
        System.out.println("Expected (moonphases.co.uk): Waning Gibbous");

        // If Waning Gibbous on Jan 6, and the cycle is 29.53 days:
        // Waning Gibbous = 16-22 days after new moon
        // So new moon was around Dec 15-21

        System.out.println("\n=== Testing Different New Moon Dates ===");

        // Try Dec 15, 2025 as new moon
        testWithNewMoon(2461025, "Dec 15, 2025");

        // Try Dec 16, 2025 as new moon
        testWithNewMoon(2461026, "Dec 16, 2025");

        // Try Dec 17, 2025 as new moon
        testWithNewMoon(2461027, "Dec 17, 2025");

        // Try Dec 18, 2025 as new moon
        testWithNewMoon(2461028, "Dec 18, 2025");

        // Try Dec 19, 2025 as new moon
        testWithNewMoon(2461029, "Dec 19, 2025");
    }

    private static void testWithNewMoon(int newMoonJDN, String date) {
        int jan6JDN = DateUtils.dateToJulianDayNumber(6, 1, 2026);
        double daysSince = jan6JDN - newMoonJDN;
        double phaseFraction = (daysSince % 29.53059) / 29.53059;
        if (phaseFraction < 0) phaseFraction += 1.0;

        int phaseIndex = (int)(phaseFraction * 8);
        String[] phases = {"New Moon", "Waxing Crescent", "First Quarter", "Waxing Gibbous",
                          "Full Moon", "Waning Gibbous", "Last Quarter", "Waning Crescent"};

        System.out.printf("%s (JDN %d): %.1f days since -> %s\n",
            date, newMoonJDN, daysSince, phases[phaseIndex]);
    }
}

