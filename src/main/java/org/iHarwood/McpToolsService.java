package org.iHarwood;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes astronomical calculations as MCP tools via Spring AI's @Tool discovery.
 * The ToolCallbackProvider bean is picked up by the MCP server auto-configuration.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication
public class McpToolsService {

    private final AstronomicalDataService dataService;

    public McpToolsService(AstronomicalDataService dataService) {
        this.dataService = dataService;
    }

    @Bean
    public ToolCallbackProvider astronomyTools() {
        return MethodToolCallbackProvider.builder()
                .toolObjects(this)
                .build();
    }

    // ── MCP Tools ─────────────────────────────────────────────────────────────

    @Tool(name = "get_moon_data",
          description = "Returns current moon phase, illumination %, age in days, distance in km, and days until full moon.")
    public AstronomyApiController.MoonData getMoonData() {
        AstronomicalSnapshot s = dataService.getLatestSnapshot();
        if (s == null) return null;
        return new AstronomyApiController.MoonData(
                s.moonPhaseName(), s.moonIlluminationPercent(), s.moonPhaseIcon(),
                s.moonAgeDays(), s.daysUntilFullMoon(), s.moonDistanceKm()
        );
    }

    @Tool(name = "get_planet_distances",
          description = "Returns current distances from Earth to all planets in astronomical units (AU).")
    public AstronomyApiController.PlanetDistances getPlanetDistances() {
        AstronomicalSnapshot s = dataService.getLatestSnapshot();
        if (s == null) return null;
        return new AstronomyApiController.PlanetDistances(
                s.sunDistanceAu(), s.mercuryDistanceAu(), s.venusDistanceAu(),
                s.marsDistanceAu(), s.jupiterDistanceAu(), s.saturnDistanceAu(),
                s.uranusDistanceAu(), s.neptuneDistanceAu(), s.plutoDistanceAu()
        );
    }

    @Tool(name = "get_earth_data",
          description = "Returns Earth's current orbital speed, daylight hours at the configured latitude, and axial tilt.")
    public AstronomyApiController.EarthData getEarthData() {
        AstronomicalSnapshot s = dataService.getLatestSnapshot();
        if (s == null) return null;
        return new AstronomyApiController.EarthData(
                s.earthSpeedKmPerSec(), s.earthSpeedKmPerHour(),
                s.daylightHours(), s.earthAxialTiltDegrees()
        );
    }

    @Tool(name = "get_leo_data",
          description = "Returns current altitudes of ISS, Tiangong, and Hubble in km, plus Starlink and Kuiper satellite counts.")
    public AstronomyApiController.LeoData getLeoData() {
        AstronomicalSnapshot s = dataService.getLatestSnapshot();
        if (s == null) return null;
        return new AstronomyApiController.LeoData(
                s.issAltitudeKm(), s.tiangongAltitudeKm(), s.hubbleAltitudeKm(),
                s.starlinkSatelliteCount(), s.kuiperSatelliteCount(), s.totalSatellitesInOrbit()
        );
    }

    @Tool(name = "get_probe_distances",
          description = "Returns heliocentric and Earth-relative distances of Voyager 1, Voyager 2, and New Horizons in AU.")
    public AstronomyApiController.ProbeDistances getProbeDistances() {
        AstronomicalSnapshot s = dataService.getLatestSnapshot();
        if (s == null) return null;
        return new AstronomyApiController.ProbeDistances(
                s.voyager1DistanceAu(), s.voyager1HelioDistanceAu(),
                s.voyager2DistanceAu(), s.voyager2HelioDistanceAu(),
                s.newHorizonsDistanceAu()
        );
    }

    @Tool(name = "get_upcoming_events",
          description = "Returns days until next summer solstice, winter solstice, perihelion, and aphelion.")
    public AstronomyApiController.UpcomingEvents getUpcomingEvents() {
        AstronomicalSnapshot s = dataService.getLatestSnapshot();
        if (s == null) return null;
        return new AstronomyApiController.UpcomingEvents(
                s.daysUntilSummerSolstice(), s.daysUntilWinterSolstice(),
                s.daysUntilPerihelion(), s.daysUntilAphelion()
        );
    }

    @Tool(name = "get_light_travel_times",
          description = "Returns light travel times from the Sun to Earth and all planets, formatted as human-readable strings.")
    public AstronomyApiController.LightTravelTimes getLightTravelTimes() {
        AstronomicalSnapshot s = dataService.getLatestSnapshot();
        if (s == null) return null;
        return new AstronomyApiController.LightTravelTimes(
                s.lightTimeSunToEarth(), s.lightTimeSunToMercury(), s.lightTimeSunToVenus(),
                s.lightTimeSunToMars(), s.lightTimeSunToJupiter(), s.lightTimeSunToSaturn(),
                s.lightTimeSunToUranus(), s.lightTimeSunToNeptune(), s.lightTimeSunToPluto(),
                s.lightTimeSunToVoyager1(), s.lightTimeSunToVoyager2()
        );
    }

    @Tool(name = "get_full_snapshot",
          description = "Returns all astronomical data in one call: moon, planets, earth, LEO, probes, and upcoming events.")
    public AstronomicalSnapshot getFullSnapshot() {
        return dataService.getLatestSnapshot();
    }
}
