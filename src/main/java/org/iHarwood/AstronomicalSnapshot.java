package org.iHarwood;

/**
 * Immutable snapshot of all computed astronomical metrics.
 * Jackson-serialisable via its record accessors.
 *
 * Use {@link Builder} to construct instances safely — avoids fragile positional constructors.
 */
public record AstronomicalSnapshot(
        // Moon
        String moonPhaseName,
        int moonIlluminationPercent,
        String moonPhaseIcon,
        String[] moonAsciiArt,
        int moonAgeDays,
        int daysUntilFullMoon,

        // Distances in AU
        double sunDistanceAu,
        double marsDistanceAu,
        double jupiterDistanceAu,
        double saturnDistanceAu,

        // Moon distance in km
        double moonDistanceKm,

        // Deep-space probes in AU
        double voyager1DistanceAu,
        double voyager2DistanceAu,
        double newHorizonsDistanceAu,

        // Earth orbital speed
        double earthSpeedKmPerSec,
        double earthSpeedKmPerHour,

        // Daylight
        double daylightHours,

        // Earth axial tilt
        double earthAxialTiltDegrees,

        // Light travel times (formatted strings)
        String lightTimeSunToEarth,
        String lightTimeSunToMercury,
        String lightTimeSunToVenus,
        String lightTimeSunToMars,
        String lightTimeSunToJupiter,
        String lightTimeSunToSaturn,
        String lightTimeSunToUranus,
        String lightTimeSunToNeptune,
        String lightTimeSunToPluto,
        String lightTimeSunToVoyager1,
        String lightTimeSunToVoyager2,

        // LEO (Low Earth Orbit)
        double issAltitudeKm,
        double tiangongAltitudeKm,
        double hubbleAltitudeKm,
        int starlinkSatelliteCount,
        int kuiperSatelliteCount,
        int totalSatellitesInOrbit,

        // Upcoming events (days)
        long daysUntilSummerSolstice,
        long daysUntilWinterSolstice,
        long daysUntilPerihelion,
        long daysUntilAphelion,

        // Metadata
        String lastUpdated
) {
    /**
     * Named-field builder — prevents field-ordering bugs in the 35-arg record constructor.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String moonPhaseName;
        private int moonIlluminationPercent;
        private String moonPhaseIcon;
        private String[] moonAsciiArt;
        private int moonAgeDays;
        private int daysUntilFullMoon;
        private double sunDistanceAu;
        private double marsDistanceAu;
        private double jupiterDistanceAu;
        private double saturnDistanceAu;
        private double moonDistanceKm;
        private double voyager1DistanceAu;
        private double voyager2DistanceAu;
        private double newHorizonsDistanceAu;
        private double earthSpeedKmPerSec;
        private double earthSpeedKmPerHour;
        private double daylightHours;
        private double earthAxialTiltDegrees;
        private String lightTimeSunToEarth;
        private String lightTimeSunToMercury;
        private String lightTimeSunToVenus;
        private String lightTimeSunToMars;
        private String lightTimeSunToJupiter;
        private String lightTimeSunToSaturn;
        private String lightTimeSunToUranus;
        private String lightTimeSunToNeptune;
        private String lightTimeSunToPluto;
        private String lightTimeSunToVoyager1;
        private String lightTimeSunToVoyager2;
        private double issAltitudeKm;
        private double tiangongAltitudeKm;
        private double hubbleAltitudeKm;
        private int starlinkSatelliteCount;
        private int kuiperSatelliteCount;
        private int totalSatellitesInOrbit;
        private long daysUntilSummerSolstice;
        private long daysUntilWinterSolstice;
        private long daysUntilPerihelion;
        private long daysUntilAphelion;
        private String lastUpdated;

        private Builder() {}

        public Builder moonPhaseName(String v)          { this.moonPhaseName = v; return this; }
        public Builder moonIlluminationPercent(int v)    { this.moonIlluminationPercent = v; return this; }
        public Builder moonPhaseIcon(String v)           { this.moonPhaseIcon = v; return this; }
        public Builder moonAsciiArt(String[] v)          { this.moonAsciiArt = v; return this; }
        public Builder moonAgeDays(int v)                { this.moonAgeDays = v; return this; }
        public Builder daysUntilFullMoon(int v)          { this.daysUntilFullMoon = v; return this; }
        public Builder sunDistanceAu(double v)           { this.sunDistanceAu = v; return this; }
        public Builder marsDistanceAu(double v)          { this.marsDistanceAu = v; return this; }
        public Builder jupiterDistanceAu(double v)       { this.jupiterDistanceAu = v; return this; }
        public Builder saturnDistanceAu(double v)        { this.saturnDistanceAu = v; return this; }
        public Builder moonDistanceKm(double v)          { this.moonDistanceKm = v; return this; }
        public Builder voyager1DistanceAu(double v)      { this.voyager1DistanceAu = v; return this; }
        public Builder voyager2DistanceAu(double v)      { this.voyager2DistanceAu = v; return this; }
        public Builder newHorizonsDistanceAu(double v)   { this.newHorizonsDistanceAu = v; return this; }
        public Builder earthSpeedKmPerSec(double v)      { this.earthSpeedKmPerSec = v; return this; }
        public Builder earthSpeedKmPerHour(double v)     { this.earthSpeedKmPerHour = v; return this; }
        public Builder daylightHours(double v)           { this.daylightHours = v; return this; }
        public Builder earthAxialTiltDegrees(double v)   { this.earthAxialTiltDegrees = v; return this; }
        public Builder lightTimeSunToEarth(String v)     { this.lightTimeSunToEarth = v; return this; }
        public Builder lightTimeSunToMercury(String v)   { this.lightTimeSunToMercury = v; return this; }
        public Builder lightTimeSunToVenus(String v)     { this.lightTimeSunToVenus = v; return this; }
        public Builder lightTimeSunToMars(String v)      { this.lightTimeSunToMars = v; return this; }
        public Builder lightTimeSunToJupiter(String v)   { this.lightTimeSunToJupiter = v; return this; }
        public Builder lightTimeSunToSaturn(String v)    { this.lightTimeSunToSaturn = v; return this; }
        public Builder lightTimeSunToUranus(String v)    { this.lightTimeSunToUranus = v; return this; }
        public Builder lightTimeSunToNeptune(String v)   { this.lightTimeSunToNeptune = v; return this; }
        public Builder lightTimeSunToPluto(String v)     { this.lightTimeSunToPluto = v; return this; }
        public Builder lightTimeSunToVoyager1(String v)  { this.lightTimeSunToVoyager1 = v; return this; }
        public Builder lightTimeSunToVoyager2(String v)  { this.lightTimeSunToVoyager2 = v; return this; }
        public Builder issAltitudeKm(double v)           { this.issAltitudeKm = v; return this; }
        public Builder tiangongAltitudeKm(double v)      { this.tiangongAltitudeKm = v; return this; }
        public Builder hubbleAltitudeKm(double v)        { this.hubbleAltitudeKm = v; return this; }
        public Builder starlinkSatelliteCount(int v)     { this.starlinkSatelliteCount = v; return this; }
        public Builder kuiperSatelliteCount(int v)       { this.kuiperSatelliteCount = v; return this; }
        public Builder totalSatellitesInOrbit(int v)     { this.totalSatellitesInOrbit = v; return this; }
        public Builder daysUntilSummerSolstice(long v)   { this.daysUntilSummerSolstice = v; return this; }
        public Builder daysUntilWinterSolstice(long v)   { this.daysUntilWinterSolstice = v; return this; }
        public Builder daysUntilPerihelion(long v)       { this.daysUntilPerihelion = v; return this; }
        public Builder daysUntilAphelion(long v)         { this.daysUntilAphelion = v; return this; }
        public Builder lastUpdated(String v)             { this.lastUpdated = v; return this; }

        public AstronomicalSnapshot build() {
            return new AstronomicalSnapshot(
                    moonPhaseName, moonIlluminationPercent, moonPhaseIcon, moonAsciiArt,
                    moonAgeDays, daysUntilFullMoon,
                    sunDistanceAu, marsDistanceAu, jupiterDistanceAu, saturnDistanceAu,
                    moonDistanceKm,
                    voyager1DistanceAu, voyager2DistanceAu, newHorizonsDistanceAu,
                    earthSpeedKmPerSec, earthSpeedKmPerHour,
                    daylightHours, earthAxialTiltDegrees,
                    lightTimeSunToEarth, lightTimeSunToMercury, lightTimeSunToVenus,
                    lightTimeSunToMars, lightTimeSunToJupiter, lightTimeSunToSaturn,
                    lightTimeSunToUranus, lightTimeSunToNeptune, lightTimeSunToPluto,
                    lightTimeSunToVoyager1, lightTimeSunToVoyager2,
                    issAltitudeKm, tiangongAltitudeKm, hubbleAltitudeKm,
                    starlinkSatelliteCount, kuiperSatelliteCount, totalSatellitesInOrbit,
                    daysUntilSummerSolstice, daysUntilWinterSolstice,
                    daysUntilPerihelion, daysUntilAphelion,
                    lastUpdated
            );
        }
    }
}
