import java.time.LocalDate;
import org.iHarwood.MoonPhaseModule.*;

public class MoonPhaseVerificationTest {
    public static void main(String[] args) {
        System.out.println("=== Moon Phase Verification Test ===\n");
        System.out.println("Reference: New Moon on December 30, 2025\n");

        // Test various dates
        testDate(LocalDate.of(2025, 12, 30), "New Moon", 0);
        testDate(LocalDate.of(2026, 1, 6), "Waxing Crescent", 7);
        testDate(LocalDate.of(2026, 1, 8), "First Quarter", 8);
        testDate(LocalDate.of(2026, 1, 13), "Full Moon", 14);
        testDate(LocalDate.of(2026, 1, 20), "Last Quarter", 21);
        testDate(LocalDate.of(2026, 1, 28), "Waning Crescent", 29);
        testDate(LocalDate.of(2026, 1, 29), "New Moon", 0);

        System.out.println("\n=== Phase Boundaries ===");
        System.out.println("New Moon: 0-3.7 days");
        System.out.println("Waxing Crescent: 3.7-7.4 days");
        System.out.println("First Quarter: 7.4-11.1 days");
        System.out.println("Waxing Gibbous: 11.1-14.8 days");
        System.out.println("Full Moon: 14.8-18.4 days");
        System.out.println("Waning Gibbous: 18.4-22.1 days");
        System.out.println("Last Quarter: 22.1-25.9 days");
        System.out.println("Waning Crescent: 25.9-29.5 days");
    }

    private static void testDate(LocalDate date, String expectedPhase, int expectedAge) {
        MoonPhase mp = MoonPhase.fromDate(date);
        String status = mp.getPhaseName().startsWith(expectedPhase.split(" ")[0]) ? "✓" : "✗";
        System.out.printf("%s %s: %s (%d days) - Expected: %s (~%d days)\n",
            status, date, mp.getPhaseName(), mp.getAgeDays(), expectedPhase, expectedAge);
    }
}

