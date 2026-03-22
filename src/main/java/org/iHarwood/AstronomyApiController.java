package org.iHarwood;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnWebApplication
@RequestMapping("/api")
public class AstronomyApiController {

    private final AstronomicalDataService dataService;

    public AstronomyApiController(AstronomicalDataService dataService) {
        this.dataService = dataService;
    }

    // ── Response records ──────────────────────────────────────────────────────

    public record MoonData(
            String moonPhaseName,
            int moonIlluminationPercent,
            String moonPhaseIcon,
            int moonAgeDays,
            int daysUntilFullMoon,
            double moonDistanceKm
    ) {}

    public record PlanetDistances(
            double sunDistanceAu,
            double mercuryDistanceAu,
            double venusDistanceAu,
            double marsDistanceAu,
            double jupiterDistanceAu,
            double saturnDistanceAu,
            double uranusDistanceAu,
            double neptuneDistanceAu,
            double plutoDistanceAu
    ) {}

    public record EarthData(
            double earthSpeedKmPerSec,
            double earthSpeedKmPerHour,
            double daylightHours,
            double earthAxialTiltDegrees
    ) {}

    public record LeoData(
            double issAltitudeKm,
            double tiangongAltitudeKm,
            double hubbleAltitudeKm,
            int starlinkSatelliteCount,
            int kuiperSatelliteCount,
            int totalSatellitesInOrbit
    ) {}

    public record ProbeDistances(
            double voyager1DistanceAu,
            double voyager1HelioDistanceAu,
            double voyager2DistanceAu,
            double voyager2HelioDistanceAu,
            double newHorizonsDistanceAu
    ) {}

    public record UpcomingEvents(
            long daysUntilSummerSolstice,
            long daysUntilWinterSolstice,
            long daysUntilPerihelion,
            long daysUntilAphelion
    ) {}

    public record LightTravelTimes(
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
            String lightTimeSunToVoyager2
    ) {}

    // ── Endpoints ─────────────────────────────────────────────────────────────

    @GetMapping("/moon")
    public ResponseEntity<?> getMoon() {
        AstronomicalSnapshot s = dataService.getLatestSnapshot();
        if (s == null) return notReady();
        return ResponseEntity.ok(new MoonData(
                s.moonPhaseName(), s.moonIlluminationPercent(), s.moonPhaseIcon(),
                s.moonAgeDays(), s.daysUntilFullMoon(), s.moonDistanceKm()
        ));
    }

    @GetMapping("/planets")
    public ResponseEntity<?> getPlanets() {
        AstronomicalSnapshot s = dataService.getLatestSnapshot();
        if (s == null) return notReady();
        return ResponseEntity.ok(new PlanetDistances(
                s.sunDistanceAu(), s.mercuryDistanceAu(), s.venusDistanceAu(),
                s.marsDistanceAu(), s.jupiterDistanceAu(), s.saturnDistanceAu(),
                s.uranusDistanceAu(), s.neptuneDistanceAu(), s.plutoDistanceAu()
        ));
    }

    @GetMapping("/earth")
    public ResponseEntity<?> getEarth() {
        AstronomicalSnapshot s = dataService.getLatestSnapshot();
        if (s == null) return notReady();
        return ResponseEntity.ok(new EarthData(
                s.earthSpeedKmPerSec(), s.earthSpeedKmPerHour(),
                s.daylightHours(), s.earthAxialTiltDegrees()
        ));
    }

    @GetMapping("/leo")
    public ResponseEntity<?> getLeo() {
        AstronomicalSnapshot s = dataService.getLatestSnapshot();
        if (s == null) return notReady();
        return ResponseEntity.ok(new LeoData(
                s.issAltitudeKm(), s.tiangongAltitudeKm(), s.hubbleAltitudeKm(),
                s.starlinkSatelliteCount(), s.kuiperSatelliteCount(), s.totalSatellitesInOrbit()
        ));
    }

    @GetMapping("/probes")
    public ResponseEntity<?> getProbes() {
        AstronomicalSnapshot s = dataService.getLatestSnapshot();
        if (s == null) return notReady();
        return ResponseEntity.ok(new ProbeDistances(
                s.voyager1DistanceAu(), s.voyager1HelioDistanceAu(),
                s.voyager2DistanceAu(), s.voyager2HelioDistanceAu(),
                s.newHorizonsDistanceAu()
        ));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<?> getUpcoming() {
        AstronomicalSnapshot s = dataService.getLatestSnapshot();
        if (s == null) return notReady();
        return ResponseEntity.ok(new UpcomingEvents(
                s.daysUntilSummerSolstice(), s.daysUntilWinterSolstice(),
                s.daysUntilPerihelion(), s.daysUntilAphelion()
        ));
    }

    @GetMapping("/light-times")
    public ResponseEntity<?> getLightTimes() {
        AstronomicalSnapshot s = dataService.getLatestSnapshot();
        if (s == null) return notReady();
        return ResponseEntity.ok(new LightTravelTimes(
                s.lightTimeSunToEarth(), s.lightTimeSunToMercury(), s.lightTimeSunToVenus(),
                s.lightTimeSunToMars(), s.lightTimeSunToJupiter(), s.lightTimeSunToSaturn(),
                s.lightTimeSunToUranus(), s.lightTimeSunToNeptune(), s.lightTimeSunToPluto(),
                s.lightTimeSunToVoyager1(), s.lightTimeSunToVoyager2()
        ));
    }

    private ResponseEntity<String> notReady() {
        return ResponseEntity.status(503).body("Snapshot not yet available — first update still running.");
    }
}
