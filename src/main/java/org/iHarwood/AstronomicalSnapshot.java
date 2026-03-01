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

        // Light travel times (formatted strings)
        String lightTimeSunToEarth,
        String lightTimeEarthToMars,
        String lightTimeEarthToJupiter,
        String lightTimeEarthToSaturn,
        String lightTimeEarthToVoyager1,
        String lightTimeEarthToVoyager2,

        // Upcoming events (days)
        long daysUntilSummerSolstice,
        long daysUntilWinterSolstice,
        long daysUntilPerihelion,
        long daysUntilAphelion,

        // Metadata
        String lastUpdated
) {}
