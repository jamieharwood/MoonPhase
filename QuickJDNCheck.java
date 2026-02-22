import org.iHarwood.MoonPhaseModule.*;

public class QuickJDNCheck {
    public static void main(String[] args) {
        // Calculate JDN for Dec 15, 2025
        int jdnDec15 = DateUtils.dateToJulianDayNumber(15, 12, 2025);
        System.out.println("Dec 15, 2025 JDN: " + jdnDec15);

        // Calculate JDN for Jan 6, 2026
        int jdnJan6 = DateUtils.dateToJulianDayNumber(6, 1, 2026);
        System.out.println("Jan 6, 2026 JDN: " + jdnJan6);

        // Days between
        int days = jdnJan6 - jdnDec15;
        System.out.println("Days between: " + days);

        // Phase calculation
        double phaseFraction = (days % 29.53059) / 29.53059;
        int phaseIndex = (int)(phaseFraction * 8);
        String[] phases = {"New Moon", "Waxing Crescent", "First Quarter", "Waxing Gibbous",
                          "Full Moon", "Waning Gibbous", "Last Quarter", "Waning Crescent"};

        System.out.println("Phase fraction: " + phaseFraction);
        System.out.println("Phase index: " + phaseIndex);
        System.out.println("Phase: " + phases[phaseIndex]);
        System.out.println("\nExpected: Waning Gibbous (phase index 5)");
    }
}

