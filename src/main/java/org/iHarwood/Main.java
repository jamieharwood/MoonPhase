package org.iHarwood;

import org.iHarwood.MoonPhaseModule.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {
    // Static formatters to avoid recreation on each run
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("EEEE");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter SHORT_DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yy");

    static void main(String[] args) {
        int barWidth = 30;
        
        LocalDateTime now = LocalDateTime.now();

        System.out.println("Date: " + now.format(DATE_FMT) + " | Day: " + now.format(DAY_FMT) + " | Time: " + now.format(TIME_FMT));

        System.out.println("1 astronomical units = 92,955,807.273026 miles");
        System.out.println();

        getSunEarth(barWidth);

        getEarthMars(barWidth);
        System.out.println();

        getDayLength(args, barWidth);
        System.out.println();
        System.out.println();

        getVoyagerDistance();
        System.out.println();

        getEquinox();
        System.out.println();

        getMoonPhase();
    }

    private static void getSunEarth(int barWidth) {
        // Compute approximate min/max over the next year (daily sampling)
        double[] range = SunDistance.minMaxDistanceAUNow();
        double min = range[0];
        double max = range[1];

        // Get Sun-Earth distance and print to console
        double sunDistanceAu = SunDistance.distanceAUNow();
        System.out.printf("Current Earth-Sun distance: %.6f AU%n", sunDistanceAu);

        // Print relative ASCII bar
        String bar = buildRelativeBar(sunDistanceAu, min, max, barWidth);
        System.out.println(bar);
        System.out.printf("%.6f        %.6f        %.6f\r\n\r\n", min, sunDistanceAu, max);
    }

    private static void getMoonPhase() {
        // Moon phase
        MoonPhase mp = MoonPhase.fromDate(LocalDate.now());
        System.out.println("Current moon phase is " + mp.getPhaseName() + " (" + mp.getAgeDays() + " days).");
        for (String row : mp.getAscii()) {
            System.out.println(row);
        }
        sendAwtrix("moonphase", mp.getPhaseName(), mp.getPhaseIcon());
    }

    // ...existing code...

    private static void getEquinox() {
        // EquinoxCalculator
        String nextSummer = EquinoxCalculator.nextSummerSolstice().format(SHORT_DATE_FMT);
        String nextWinter = EquinoxCalculator.nextWinterSolstice().format(SHORT_DATE_FMT);

        System.out.printf("Next summer solstice: %s", nextSummer);
        sendAwtrix("summersolstice", String.format("%s", EquinoxCalculator.daysUntilSummerSolstice()).concat("d"), APIPost.IconType.SUMMER.toString());

        System.out.printf("Next winter solstice: %s", nextWinter);
        sendAwtrix("wintersolstice", String.format("%s", EquinoxCalculator.daysUntilWinterSolstice()).concat("d"), APIPost.IconType.WINTER.toString());
    }

    private static void getEarthMars(int barWidth) {
        double[] marsRange = MarsDistance.minMaxDistanceAUNow();
        double marsMin = marsRange[0];
        double marsMax = marsRange[1];

        // --- Earth-Mars distance and ASCII bar ---
        double marsDistanceAu = MarsDistance.distanceAUNow();
        System.out.printf("Current Earth-Mars distance: %.6f AU%n", marsDistanceAu);

        // Print Mars relative ASCII bar
        String marsBar = buildRelativeBar(marsDistanceAu, marsMin, marsMax, barWidth);
        System.out.println(marsBar);
        System.out.printf("%.6f        %.6f        %.6f", marsMin, marsDistanceAu, marsMax);

        sendAwtrix("marsDistanceAu", String.format("%.1f", marsDistanceAu) + "au", APIPost.IconType.MARS.toString());
    }

    private static void getVoyagerDistance() {
        // Voyager distances (simple approximations)
        double v1FromEarthAu = VoyagerDistance.distanceFromEarthV1AUNow();
        double v2FromEarthAu = VoyagerDistance.distanceFromEarthV2AUNow();
        System.out.printf("Voyager 1 distance from Earth: %.6f AU%n", v1FromEarthAu);
        System.out.printf("Voyager 2 distance from Earth: %.6f AU%n", v2FromEarthAu);
    }

    private static void getDayLength(String[] args, int barWidth) {
        // --- Daylight length (hours) ---
        // Default latitude (Greenwich). Override by passing latitude in degrees as first argument.
        double latitude = 51.4769;
        if (args.length >= 1) {
            try {
                latitude = Double.parseDouble(args[0]);
            } catch (NumberFormatException ignored) { }
        }

        double[] dayRange = DayLight.minMaxDayLengthAUNow(latitude);
        double dayMin = dayRange[0];
        double dayMax = dayRange[1];
        double currentDayHours = DayLight.dayLengthHours(LocalDate.now(), latitude);

        System.out.println();
        System.out.println("Daylight length (hours) at latitude " + latitude + ":");
        String dayBar = buildRelativeBar(currentDayHours, dayMin, dayMax, barWidth);
        System.out.println(dayBar);
        System.out.printf("%.2f        %.2f        %.2f", dayMin, currentDayHours, dayMax);

        sendAwtrix("CurrentDayLength", String.format("%.1f", currentDayHours) + "hrs", APIPost.IconType.DAYLENGTH.toString());
    }

    private static void sendAwtrix(String appName, String text, String icon) {
        APIPost apiPost = new APIPost(appName, text, "http://192.168.86.103/api/custom?name=".concat(appName), "1", "", icon);
        try {
            System.out.printf("%s%n", apiPost.sendPost());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String buildRelativeBar(double current, double min, double max, int innerWidth) {
        if (innerWidth < 1) innerWidth = 1;
        StringBuilder sb = new StringBuilder();
        sb.append("Min |");

        // Handle degenerate case
        int pos;
        if (!Double.isFinite(min) || !Double.isFinite(max) || max <= min) {
            pos = innerWidth / 2;
        } else {
            double frac = (current - min) / (max - min);
            if (Double.isNaN(frac)) frac = 0.5;
            frac = Math.max(0.0, Math.min(1.0, frac));
            pos = (int) Math.round(frac * (innerWidth - 1));
        }

        for (int i = 0; i < innerWidth; i++) {
            sb.append(i == pos ? '0' : '-');
        }
        sb.append("| Max");
        return sb.toString();
    }
}
