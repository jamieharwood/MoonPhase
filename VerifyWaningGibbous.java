import java.time.LocalDate;
import org.iHarwood.MoonPhaseModule.*;

public class VerifyWaningGibbous {
    public static void main(String[] args) {
        System.out.println("=== Moon Phase Verification - January 6, 2026 ===\n");

        // Get current moon phase
        MoonPhase mp = MoonPhase.fromDate(LocalDate.of(2026, 1, 6));

        System.out.println("Calculated Phase: " + mp.getPhaseName());
        System.out.println("Age: " + (int)mp.getAgeDays() + " days");
        System.out.println("Icon: " + mp.getPhaseIcon());

        System.out.println("\n=== Expected (moonphases.co.uk) ===");
        System.out.println("Expected Phase: Waning Gibbous");
        System.out.println("Expected Age: ~22 days (7 days after full moon on Dec 30)");

        System.out.println("\n=== ASCII Art ===");
        for (String row : mp.getAscii()) {
            System.out.println(row);
        }

        // Verify the calculation
        System.out.println("\n=== Calculation Details ===");
        System.out.println("New Moon: December 15, 2025");
        System.out.println("Full Moon: December 30, 2025 (15 days later)");
        System.out.println("Today (Jan 6): 22 days after new moon");
        System.out.println("Phase: " + (22.0/29.53059 * 8.0) + " → index 5 (Waning Gibbous)");

        boolean isCorrect = mp.getPhaseName().equals("Waning Gibbous");
        System.out.println("\n" + (isCorrect ? "✓ CORRECT!" : "✗ INCORRECT"));
    }
}

