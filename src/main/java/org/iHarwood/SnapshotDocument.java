package org.iHarwood;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB document that persists a single scheduled calculation result.
 */
@Document(collection = "snapshots")
public class SnapshotDocument {

    @Id
    private String id;

    @Indexed
    private Instant timestamp;

    private double daylightHours;
    private int moonIlluminationPercent;
    private double sunDistanceAu;
    private double marsDistanceAu;
    private double jupiterDistanceAu;
    private double saturnDistanceAu;
    private double moonDistanceKm;
    private double earthSpeedKmPerSec;
    private double earthSpeedKmPerHour;
    private double voyager1DistanceAu;
    private double voyager2DistanceAu;
    private double newHorizonsDistanceAu;
    private int daysUntilFullMoon;
    private long daysUntilSummerSolstice;
    private long daysUntilWinterSolstice;
    private long daysUntilPerihelion;
    private long daysUntilAphelion;

    public static SnapshotDocument from(AstronomicalSnapshot s) {
        SnapshotDocument doc = new SnapshotDocument();
        doc.timestamp = Instant.now();
        doc.daylightHours = s.daylightHours();
        doc.moonIlluminationPercent = s.moonIlluminationPercent();
        doc.sunDistanceAu = s.sunDistanceAu();
        doc.marsDistanceAu = s.marsDistanceAu();
        doc.jupiterDistanceAu = s.jupiterDistanceAu();
        doc.saturnDistanceAu = s.saturnDistanceAu();
        doc.moonDistanceKm = s.moonDistanceKm();
        doc.earthSpeedKmPerSec = s.earthSpeedKmPerSec();
        doc.earthSpeedKmPerHour = s.earthSpeedKmPerHour();
        doc.voyager1DistanceAu = s.voyager1DistanceAu();
        doc.voyager2DistanceAu = s.voyager2DistanceAu();
        doc.newHorizonsDistanceAu = s.newHorizonsDistanceAu();
        doc.daysUntilFullMoon = s.daysUntilFullMoon();
        doc.daysUntilSummerSolstice = s.daysUntilSummerSolstice();
        doc.daysUntilWinterSolstice = s.daysUntilWinterSolstice();
        doc.daysUntilPerihelion = s.daysUntilPerihelion();
        doc.daysUntilAphelion = s.daysUntilAphelion();
        return doc;
    }

    public Instant getTimestamp() { return timestamp; }
    public double getDaylightHours() { return daylightHours; }
    public int getMoonIlluminationPercent() { return moonIlluminationPercent; }
    public double getSunDistanceAu() { return sunDistanceAu; }
    public double getMarsDistanceAu() { return marsDistanceAu; }
    public double getJupiterDistanceAu() { return jupiterDistanceAu; }
    public double getSaturnDistanceAu() { return saturnDistanceAu; }
    public double getMoonDistanceKm() { return moonDistanceKm; }
    public double getEarthSpeedKmPerSec() { return earthSpeedKmPerSec; }
    public double getEarthSpeedKmPerHour() { return earthSpeedKmPerHour; }
    public double getVoyager1DistanceAu() { return voyager1DistanceAu; }
    public double getVoyager2DistanceAu() { return voyager2DistanceAu; }
    public double getNewHorizonsDistanceAu() { return newHorizonsDistanceAu; }
    public int getDaysUntilFullMoon() { return daysUntilFullMoon; }
    public long getDaysUntilSummerSolstice() { return daysUntilSummerSolstice; }
    public long getDaysUntilWinterSolstice() { return daysUntilWinterSolstice; }
    public long getDaysUntilPerihelion() { return daysUntilPerihelion; }
    public long getDaysUntilAphelion() { return daysUntilAphelion; }
}
