import java.time.LocalDate;
import org.iHarwood.MoonPhaseModule.*;

public class MoonPhaseDebugTest {
    public static void main(String[] args) {
        // Known moon phases for verification
        // Dec 30, 2025 = New Moon
        // Jan 6, 2026 = First Quarter (about 7 days old)
        // Jan 13, 2026 = Full Moon (about 14 days old)

        System.out.println("=== Moon Phase Debug Test ===\n");

        testDate(LocalDate.of(2025, 12, 30), "New Moon", 0);
        testDate(LocalDate.of(2026, 1, 6), "First Quarter", 7);
        testDate(LocalDate.of(2026, 1, 13), "Full Moon", 14);
        testDate(LocalDate.of(2026, 1, 21), "Last Quarter", 22);
        testDate(LocalDate.of(2026, 1, 29), "New Moon", 0);

        System.out.println("\n=== Testing Algorithm ===");
        LocalDate testDate = LocalDate.of(2026, 1, 6);
        int jdn = DateUtils.dateToJulianDayNumber(6, 1, 2026);
        System.out.println("JDN for Jan 6, 2026: " + jdn);
        System.out.println("Expected: 2460681");

        // Known new moon: Jan 29, 2025 JDN = 2460345
        // Days since that new moon to Jan 6, 2026: 336 days
        // 336 / 29.53059 = 11.378 lunations = 0.378 into current cycle
        // 0.378 * 29.53059 = 11.17 days old (should be near First Quarter)
    }

    private static void testDate(LocalDate date, String expectedPhase, int expectedAge) {
        MoonPhase mp = MoonPhase.fromDate(date);
        System.out.printf("%s: %s (%.1f days) - Expected: %s (~%d days)\n",
            date, mp.getPhaseName(), mp.getAgeDays(), expectedPhase, expectedAge);
    }
}

