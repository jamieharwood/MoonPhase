package org.iHarwood;

import org.iHarwood.MoonPhaseModule.MoonPhase;
import org.iHarwood.MoonPhaseModule.SunDistance;
import org.iHarwood.MoonPhaseModule.VoyagerDistance;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {
    public static void main(String[] args) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("EEEE");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");
        System.out.println("Date: " + now.format(dateFmt) + " | Day: " + now.format(dayFmt) + " | Time: " + now.format(timeFmt));

        // Compute approximate min/max over the next year (daily sampling)
        double[] range = SunDistance.minMaxDistanceAUNow();
        double min = range[0];
        double max = range[1];
        System.out.printf("Approx. minimum Earth-Sun distance (next 365 days): %.6f AU%n", min);
        System.out.printf("Approx. maximum Earth-Sun distance (next 365 days): %.6f AU%n", max);

        // Get Sun-Earth distance and print to console
        double sunDistanceAu = SunDistance.distanceAUNow();
        System.out.printf("Current Earth-Sun distance: %.6f AU%n", sunDistanceAu);

        // Print relative ASCII bar (example: |--------------\*--------------|)
        String bar = buildRelativeBar(sunDistanceAu, min, max, 30);
        System.out.println("Relative distance: " + bar);

        // Voyager distances (simple approximations)
        double v1FromEarthAu = VoyagerDistance.distanceFromEarthV1AUNow();
        double v2FromEarthAu = VoyagerDistance.distanceFromEarthV2AUNow();
        System.out.printf("Voyager 1 distance from Earth: %.6f AU%n", v1FromEarthAu);
        System.out.printf("Voyager 2 distance from Earth: %.6f AU%n", v2FromEarthAu);

        // Moon phase
        MoonPhase mp = MoonPhase.fromDate(LocalDate.now());
        System.out.println("Current moon phase is " + mp.getPhaseName() + " (" + mp.getAgeDays() + " days).");
        for (String row : mp.getAscii()) {
            System.out.println(row);
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
