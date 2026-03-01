package org.iHarwood;

/**
 * Immutable snapshot of all computed astronomical metrics.
 * Jackson-serialisable via its record accessors.
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

        // Upcoming events (days)
        long daysUntilSummerSolstice,
        long daysUntilWinterSolstice,
        long daysUntilPerihelion,
        long daysUntilAphelion,

        // Metadata
        String lastUpdated
) {}
