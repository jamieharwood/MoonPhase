import org.iHarwood.MoonPhaseModule.*;

public class JulianDayTest {
    public static void main(String[] args) {
        // Test known dates
        // Jan 1, 2000 12:00 TT = JDN 2451545 (J2000.0 epoch)
        // Jan 6, 2026 = JDN 2460681

        System.out.println("=== Julian Day Number Tests ===\n");

        // Test J2000.0
        int jdn2000 = DateUtils.dateToJulianDayNumber(1, 1, 2000);
        System.out.println("Jan 1, 2000: " + jdn2000 + " (Expected: 2451545)");

        // Test Jan 6, 2026
        int jdn2026 = DateUtils.dateToJulianDayNumber(6, 1, 2026);
        System.out.println("Jan 6, 2026: " + jdn2026 + " (Expected: 2460681)");

        // Days between
        System.out.println("Days between: " + (jdn2026 - jdn2000) + " (Expected: 9502)");

        // Manual calculation for Jan 6, 2026
        int day = 6;
        int month = 1;
        int year = 2026;

        int a = (14 - month) / 12;
        int y = year + 4800 - a;
        int m = month + 12 * a - 3;

        System.out.println("\nDebug values:");
        System.out.println("a = " + a);
        System.out.println("y = " + y);
        System.out.println("m = " + m);

        int calc = (int)(day + (153 * m + 2) / 5 + 365L * y + y / 4 - y / 100 + y / 400 - 32045);
        System.out.println("Calculated JDN = " + calc);
    }
}

