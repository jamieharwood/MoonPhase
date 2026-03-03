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
    private int moonAgeDays;
    private int daysUntilFullMoon;
    private long daysUntilSummerSolstice;
    private long daysUntilWinterSolstice;
    private long daysUntilPerihelion;
    private long daysUntilAphelion;
    private double earthAxialTiltDegrees;
    private double issAltitudeKm;
    private double tiangongAltitudeKm;
    private double hubbleAltitudeKm;
    private int starlinkSatelliteCount;
    private int kuiperSatelliteCount;
    private int totalSatellitesInOrbit;

    public static SnapshotDocument from(AstronomicalSnapshot s, Instant timestamp) {
        SnapshotDocument doc = from(s);
        doc.timestamp = timestamp;
        return doc;
    }

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
        doc.moonAgeDays = s.moonAgeDays();
        doc.daysUntilFullMoon = s.daysUntilFullMoon();
        doc.daysUntilSummerSolstice = s.daysUntilSummerSolstice();
        doc.daysUntilWinterSolstice = s.daysUntilWinterSolstice();
        doc.daysUntilPerihelion = s.daysUntilPerihelion();
        doc.daysUntilAphelion = s.daysUntilAphelion();
        doc.earthAxialTiltDegrees = s.earthAxialTiltDegrees();
        doc.issAltitudeKm = s.issAltitudeKm();
        doc.tiangongAltitudeKm = s.tiangongAltitudeKm();
        doc.hubbleAltitudeKm = s.hubbleAltitudeKm();
        doc.starlinkSatelliteCount = s.starlinkSatelliteCount();
        doc.kuiperSatelliteCount = s.kuiperSatelliteCount();
        doc.totalSatellitesInOrbit = s.totalSatellitesInOrbit();
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
    public int getMoonAgeDays() { return moonAgeDays; }
    public int getDaysUntilFullMoon() { return daysUntilFullMoon; }
    public long getDaysUntilSummerSolstice() { return daysUntilSummerSolstice; }
    public long getDaysUntilWinterSolstice() { return daysUntilWinterSolstice; }
    public long getDaysUntilPerihelion() { return daysUntilPerihelion; }
    public long getDaysUntilAphelion() { return daysUntilAphelion; }
    public double getEarthAxialTiltDegrees() { return earthAxialTiltDegrees; }
    public double getIssAltitudeKm() { return issAltitudeKm; }
    public double getTiangongAltitudeKm() { return tiangongAltitudeKm; }
    public double getHubbleAltitudeKm() { return hubbleAltitudeKm; }
    public int getStarlinkSatelliteCount() { return starlinkSatelliteCount; }
    public int getKuiperSatelliteCount() { return kuiperSatelliteCount; }
    public int getTotalSatellitesInOrbit() { return totalSatellitesInOrbit; }
}
