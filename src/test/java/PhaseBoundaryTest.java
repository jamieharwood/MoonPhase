import java.time.LocalDate;
import org.iHarwood.MoonPhaseModule.*;

public class PhaseBoundaryTest {
    public static void main(String[] args) {
        System.out.println("=== Phase Boundary Analysis ===\n");

        // Test around First Quarter (expected ~Jan 7-8)
        System.out.println("Around First Quarter:");
        for (int day = 5; day <= 10; day++) {
            MoonPhase mp = MoonPhase.fromDate(LocalDate.of(2026, 1, day));
            System.out.printf("Jan %d: %.2f days - %s\n", day, mp.getAgeDays(), mp.getPhaseName());
        }

        // Test around Full Moon (expected ~Jan 13-14)
        System.out.println("\nAround Full Moon:");
        for (int day = 12; day <= 16; day++) {
            MoonPhase mp = MoonPhase.fromDate(LocalDate.of(2026, 1, day));
            System.out.printf("Jan %d: %.2f days - %s\n", day, mp.getAgeDays(), mp.getPhaseName());
        }

        // Test around Last Quarter (expected ~Jan 21-22)
        System.out.println("\nAround Last Quarter:");
        for (int day = 19; day <= 23; day++) {
            MoonPhase mp = MoonPhase.fromDate(LocalDate.of(2026, 1, day));
            System.out.printf("Jan %d: %.2f days - %s\n", day, mp.getAgeDays(), mp.getPhaseName());
        }
    }
}

