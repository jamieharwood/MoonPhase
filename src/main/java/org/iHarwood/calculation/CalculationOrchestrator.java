package org.iHarwood.calculation;
import org.iHarwood.APIPost;
import org.iHarwood.AstronomicalSnapshot;
import org.iHarwood.MoonPhaseModule.*;
import org.iHarwood.integration.awtrix.AwtrixPusher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.iHarwood.config.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;
/**
 * Central orchestrator for all astronomical calculations.
 *
 * Responsibilities:
 * - Building complete AstronomicalSnapshot instances (both live and historical)
 * - Performing all side-effecting Awtrix pushes during live runs
 * - Rich logging (including relative bar charts)
 *
 * This class was extracted from Main.java to reduce the god-class problem.
 * Further splitting (e.g. separating formatting/logging concerns) can be done later.
 */
@Component
public class CalculationOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(CalculationOrchestrator.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("EEEE");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter SHORT_DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yy");
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final int BAR_WIDTH = 30;
    private static final String AU_IN_MILES = "92,955,807.273026 miles";
    private static final double DEFAULT_LATITUDE  = 51.4769;
    private static final double DEFAULT_LONGITUDE = 0.0;
    private final AwtrixPusher awtrixPusher;
    private final ExecutorService applicationTaskExecutor;
    private final double latitude;
    private final double longitude;
    @Autowired
    public CalculationOrchestrator(
            AwtrixPusher awtrixPusher,
            ExecutorService applicationTaskExecutor,
            AppProperties appProperties) {
        this.awtrixPusher = awtrixPusher;
        this.applicationTaskExecutor = applicationTaskExecutor;
        this.latitude  = validateLatitude(appProperties.getLatitude());
        this.longitude = validateLongitude(appProperties.getLongitude());
    }
    /** Test / manual construction without Spring. */
    CalculationOrchestrator(AwtrixPusher awtrixPusher, double latitude, double longitude) {
        this(awtrixPusher, java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor(), latitude, longitude);
    }
    private CalculationOrchestrator(
            AwtrixPusher awtrixPusher,
            ExecutorService applicationTaskExecutor,
            double latitude,
            double longitude) {
        this.awtrixPusher = awtrixPusher;
        this.applicationTaskExecutor = applicationTaskExecutor;
        this.latitude  = validateLatitude(latitude);
        this.longitude = validateLongitude(longitude);
    }
    private double validateLatitude(double lat) {
        if (lat < -90 || lat > 90) {
            logger.warn("Invalid latitude {} - must be between -90 and 90. Falling back to {} (Greenwich).",
                    lat, DEFAULT_LATITUDE);
            return DEFAULT_LATITUDE;
        }
        return lat;
    }
    private double validateLongitude(double lon) {
        if (lon < -180 || lon > 180) {
            logger.warn("Invalid longitude {} - must be between -180 and 180. Falling back to {} (Greenwich).",
                    lon, DEFAULT_LONGITUDE);
            return DEFAULT_LONGITUDE;
        }
        return lon;
    }
    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Result of a live compute cycle: snapshot for the dashboard plus pending Awtrix apps.
     * Publish the snapshot first, then call {@link #deliverSideEffects(LiveComputeResult)}.
     */
    public record LiveComputeResult(
            AstronomicalSnapshot snapshot,
            List<AwtrixPusher.PushRequest> awtrixPushes,
            LocalDate moonPhaseDate,
            String moonPhaseName) {}
    /**
     * Performs a full live calculation cycle (no network side effects except LEO/Aurora fetches).
     * Call {@link #deliverSideEffects(LiveComputeResult)} after publishing the snapshot.
     */
    public LiveComputeResult computeCurrentResult() {
        logger.info("=== Scheduled task starting ===");
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        logger.info("Date: {} | Day: {} | Time: {}",
                now.format(DATE_FMT), now.format(DAY_FMT), now.format(TIME_FMT));
        logger.info("1 astronomical unit = {}", AU_IN_MILES);
        AstronomicalSnapshot.Builder sb = AstronomicalSnapshot.builder();
        List<AwtrixPusher.PushRequest> pushes = new ArrayList<>();
        // Sun / Earth — compute once, reuse for speed + light time
        double[] sunRange = SunDistance.minMaxDistanceAUNow();
        double sunDistanceAu = SunDistance.distanceAUNow();
        sb.sunDistanceAu(sunDistanceAu);
        logDistanceBar("Current Earth-Sun distance", sunDistanceAu, sunRange[0], sunRange[1], 6);
        logger.info("Sun Earth:{}        {}        {}",
                String.format("%.6f", sunRange[0]),
                String.format("%.6f", sunDistanceAu),
                String.format("%.6f", sunRange[1]));
        // Planets — heliocentric used for light travel; geocentric for display/Awtrix
        double mercuryHelio = Planets.MERCURY.heliocentricDistanceAUNow();
        double venusHelio = Planets.VENUS.heliocentricDistanceAUNow();
        double marsHelio = Planets.MARS.heliocentricDistanceAUNow();
        double jupiterHelio = Planets.JUPITER.heliocentricDistanceAUNow();
        double saturnHelio = Planets.SATURN.heliocentricDistanceAUNow();
        double uranusHelio = Planets.URANUS.heliocentricDistanceAUNow();
        double neptuneHelio = Planets.NEPTUNE.heliocentricDistanceAUNow();
        double plutoHelio = Planets.PLUTO.heliocentricDistanceAUNow();
        sb.mercuryDistanceAu(mercuryHelio);
        sb.venusDistanceAu(venusHelio);
        double marsAu = Planets.MARS.distanceAUNow();
        sb.marsDistanceAu(marsAu);
        logPlanetDistance("Mars", marsAu, Planets.MARS);
        pushes.add(push("marsDistanceAu", String.format("%.1fau", marsAu), APIPost.IconType.MARS.name()));
        double jupiterAu = Planets.JUPITER.distanceAUNow();
        sb.jupiterDistanceAu(jupiterAu);
        logPlanetDistance("Jupiter", jupiterAu, Planets.JUPITER);
        pushes.add(push("jupiterDistanceAu", String.format("%.1fau", jupiterAu), APIPost.IconType.JUPITER.name()));
        double saturnAu = Planets.SATURN.distanceAUNow();
        sb.saturnDistanceAu(saturnAu);
        logPlanetDistance("Saturn", saturnAu, Planets.SATURN);
        pushes.add(push("saturnDistanceAu", String.format("%.1fau", saturnAu), APIPost.IconType.SATURN.name()));
        sb.uranusDistanceAu(uranusHelio);
        sb.neptuneDistanceAu(neptuneHelio);
        sb.plutoDistanceAu(plutoHelio);
        // Daylight
        double[] dayRange = DayLight.minMaxDayLengthHoursNow(latitude);
        double currentDayHours = DayLight.dayLengthHours(now.toLocalDate(), latitude);
        sb.daylightHours(currentDayHours);
        logger.info("Daylight length (hours) at latitude {}", latitude);
        logDistanceBar(null, currentDayHours, dayRange[0], dayRange[1], 2);
        logger.info("{}        {}        {}",
                String.format("%.2f", dayRange[0]),
                String.format("%.2f", currentDayHours),
                String.format("%.2f", dayRange[1]));
        pushes.add(push("CurrentDayLength", String.format("%.1fhrs", currentDayHours), APIPost.IconType.DAYLENGTH.name()));
        // Deep-space probes
        double v1Helio = VoyagerDistance.heliocentricDistanceV1AUNow();
        double v2Helio = VoyagerDistance.heliocentricDistanceV2AUNow();
        double v1Au = VoyagerDistance.distanceFromEarthV1AUNow();
        double v2Au = VoyagerDistance.distanceFromEarthV2AUNow();
        sb.voyager1DistanceAu(v1Au);
        sb.voyager2DistanceAu(v2Au);
        sb.voyager1HelioDistanceAu(v1Helio);
        sb.voyager2HelioDistanceAu(v2Helio);
        logger.info("Voyager 1 distance from Earth: {} AU", String.format("%.6f", v1Au));
        logger.info("Voyager 2 distance from Earth: {} AU", String.format("%.6f", v2Au));
        pushes.add(push("voyager1", String.format("V1:%.0fau", v1Au), APIPost.IconType.VOYAGER.name()));
        pushes.add(push("voyager2", String.format("V2:%.0fau", v2Au), APIPost.IconType.VOYAGER.name()));
        double nhAu = NewHorizonsDistance.distanceFromEarthAUNow();
        sb.newHorizonsDistanceAu(nhAu);
        logger.info("New Horizons distance from Earth: {} AU ({} km/s)",
                String.format("%.6f", nhAu), NewHorizonsDistance.speedKmPerSec());
        pushes.add(push("newhorizons", String.format("NH:%.0fau", nhAu), APIPost.IconType.NEWHORIZONS.name()));
        double jwstKm = JamesWebbDistance.distanceKmNow();
        sb.jamesWebbDistanceKm(jwstKm);
        logger.info("James Webb Space Telescope distance from Earth: {} km", String.format("%,.0f", jwstKm));
        // Equinox / perihelion
        String nextSummer = EquinoxCalculator.nextSummerSolstice().format(SHORT_DATE_FMT);
        String nextWinter = EquinoxCalculator.nextWinterSolstice().format(SHORT_DATE_FMT);
        long daysUntilSummer = EquinoxCalculator.daysUntilSummerSolstice();
        long daysUntilWinter = EquinoxCalculator.daysUntilWinterSolstice();
        sb.daysUntilSummerSolstice(daysUntilSummer);
        sb.daysUntilWinterSolstice(daysUntilWinter);
        logger.info("Next summer solstice: {}", nextSummer);
        pushes.add(push("summersolstice", daysUntilSummer + "d", APIPost.IconType.SUMMER.name()));
        logger.info("Next winter solstice: {}", nextWinter);
        pushes.add(push("wintersolstice", daysUntilWinter + "d", APIPost.IconType.WINTER.name()));
        long daysToPerihelion = PerihelionAphelion.daysUntilPerihelion();
        long daysToAphelion = PerihelionAphelion.daysUntilAphelion();
        sb.daysUntilPerihelion(daysToPerihelion);
        sb.daysUntilAphelion(daysToAphelion);
        logger.info("Days until next perihelion (closest to Sun): {}", daysToPerihelion);
        logger.info("Days until next aphelion (farthest from Sun): {}", daysToAphelion);
        pushes.add(push("perihelion", daysToPerihelion + "d", APIPost.IconType.PERIHELION.name()));
        pushes.add(push("aphelion", daysToAphelion + "d", APIPost.IconType.PERIHELION.name()));
        // Earth speed from already-computed sun distance
        double speedKmS = EarthSpeed.speedKmPerSecFromDistanceAu(sunDistanceAu);
        double speedKmH = EarthSpeed.speedKmPerHourFromDistanceAu(sunDistanceAu);
        sb.earthSpeedKmPerSec(speedKmS);
        sb.earthSpeedKmPerHour(speedKmH);
        double tilt = EarthAxialTilt.tiltDegreesNow();
        sb.earthAxialTiltDegrees(tilt);
        logger.info("Earth's orbital speed: {} km/s ({} km/h)",
                String.format("%.2f", speedKmS), String.format("%,.0f", speedKmH));
        logger.info("Earth's axial tilt: {} deg", String.format("%.3f", tilt));
        pushes.add(push("earthSpeed", String.format("%.1fkm/s", speedKmS), APIPost.IconType.EARTH.name()));
        // Moon distance
        double moonDistKm = MoonDistance.distanceKmNow();
        double[] moonRange = MoonDistance.minMaxDistanceKmNow();
        sb.moonDistanceKm(moonDistKm);
        logger.info("Current Moon distance: {}", MoonDistance.formatDistanceKm(moonDistKm));
        logDistanceBar(null, moonDistKm, moonRange[0], moonRange[1], 0);
        logger.info("{}        {}        {}",
                MoonDistance.formatDistanceKm(moonRange[0]),
                MoonDistance.formatDistanceKm(moonDistKm),
                MoonDistance.formatDistanceKm(moonRange[1]));
        pushes.add(push("moonDistance", String.format("%,.0fkm", moonDistKm), APIPost.IconType.MOON.name()));
        // Light travel times from reused distances (no second orbital pass)
        String ltEarth = LightTravelTime.formatTravelTime(sunDistanceAu);
        String ltMercury = LightTravelTime.formatTravelTime(mercuryHelio);
        String ltVenus = LightTravelTime.formatTravelTime(venusHelio);
        String ltMars = LightTravelTime.formatTravelTime(marsHelio);
        String ltJupiter = LightTravelTime.formatTravelTime(jupiterHelio);
        String ltSaturn = LightTravelTime.formatTravelTime(saturnHelio);
        String ltUranus = LightTravelTime.formatTravelTime(uranusHelio);
        String ltNeptune = LightTravelTime.formatTravelTime(neptuneHelio);
        String ltPluto = LightTravelTime.formatTravelTime(plutoHelio);
        String ltV1 = LightTravelTime.formatTravelTime(v1Helio);
        String ltV2 = LightTravelTime.formatTravelTime(v2Helio);
        sb.lightTimeSunToEarth(ltEarth);
        sb.lightTimeSunToMercury(ltMercury);
        sb.lightTimeSunToVenus(ltVenus);
        sb.lightTimeSunToMars(ltMars);
        sb.lightTimeSunToJupiter(ltJupiter);
        sb.lightTimeSunToSaturn(ltSaturn);
        sb.lightTimeSunToUranus(ltUranus);
        sb.lightTimeSunToNeptune(ltNeptune);
        sb.lightTimeSunToPluto(ltPluto);
        sb.lightTimeSunToVoyager1(ltV1);
        sb.lightTimeSunToVoyager2(ltV2);
        logger.info("--- Light Travel Times ---");
        logger.info("Sun -> Earth:    {}", ltEarth);
        logger.info("Sun -> Mercury:  {}", ltMercury);
        logger.info("Sun -> Venus:    {}", ltVenus);
        logger.info("Sun -> Mars:     {}", ltMars);
        logger.info("Sun -> Jupiter:  {}", ltJupiter);
        logger.info("Sun -> Saturn:   {}", ltSaturn);
        logger.info("Sun -> Uranus:   {}", ltUranus);
        logger.info("Sun -> Neptune:  {}", ltNeptune);
        logger.info("Sun -> Pluto:    {}", ltPluto);
        logger.info("Sun -> Voyager 1: {}", ltV1);
        logger.info("Sun -> Voyager 2: {}", ltV2);
        pushes.add(push("lightMars", "Lt:" + ltMars, APIPost.IconType.LIGHT.name()));
        pushes.add(push("lightJupiter", "Lt:" + ltJupiter, APIPost.IconType.LIGHT.name()));
        // LEO — single batched (parallel) fetch
        logger.info("--- LEO Data ---");
        LeoDataFetcher.LeoSnapshot leo = LeoDataFetcher.fetchAll();
        sb.issAltitudeKm(leo.issAltitudeKm());
        sb.tiangongAltitudeKm(leo.tiangongAltitudeKm());
        sb.hubbleAltitudeKm(leo.hubbleAltitudeKm());
        sb.starlinkSatelliteCount(leo.starlinkCount());
        sb.kuiperSatelliteCount(leo.kuiperCount());
        sb.totalSatellitesInOrbit(leo.totalSatelliteCount());
        sb.issCrew(leo.people().issCrew());
        sb.totalPeopleInSpace(leo.people().total());
        sb.craftOccupancy(leo.people().craftOccupancy());
        logger.info("People in space: {} total, {} on ISS", leo.people().total(), leo.people().issCrew());
        // Sunrise / sunset
        String sunrise = SunriseSunset.sunriseUtc(now.toLocalDate(), latitude, longitude);
        String sunset  = SunriseSunset.sunsetUtc(now.toLocalDate(), latitude, longitude);
        sb.sunriseTime(sunrise);
        sb.sunsetTime(sunset);
        logger.info("Sunrise: {} UTC | Sunset: {} UTC (lat={}, lon={})", sunrise, sunset, latitude, longitude);
        // Aurora (cached)
        double kp = AuroraKpFetcher.fetchKpIndex();
        sb.auroraKpIndex(kp);
        if (kp >= 0) {
            String activity = kp < 2 ? "Quiet" : kp < 4 ? "Unsettled" : kp < 5 ? "Active" : "Storm (Kp≥5)";
            logger.info("Aurora Kp index: {} ({})", kp, activity);
            String label = kp >= 5 ? String.format("⚡Kp%.1f", kp) : String.format("Kp%.1f", kp);
            pushes.add(push("auroraKp", label, APIPost.IconType.AURORA.name()));
        }
        // Moon phase (deterministic); Claude verification is async in deliverSideEffects
        MoonPhase mp = MoonPhase.at(now);
        logger.info("Current moon phase is {} ({} days, {}% illuminated).",
                mp.getPhaseName(), mp.getAgeDays(), mp.getIlluminationPercent());
        Arrays.asList(mp.getAscii()).forEach(row -> logger.info("{}", row));
        int daysUntilFull = mp.getDaysUntilFullMoon();
        logger.info("Days until next full moon: {}", daysUntilFull);
        sb.moonPhaseName(mp.getPhaseName());
        sb.moonIlluminationPercent(mp.getIlluminationPercent());
        sb.moonPhaseIcon(mp.getPhaseIcon());
        sb.moonAsciiArt(mp.getAscii());
        sb.moonAgeDays(mp.getAgeDays());
        sb.daysUntilFullMoon(daysUntilFull);
        pushes.add(push("moonphase", mp.getPhaseName(), mp.getPhaseIcon()));
        pushes.add(push("moonillumination", mp.getIlluminationPercent() + "%", mp.getPhaseIcon()));
        pushes.add(push("fullmoon", daysUntilFull + "d", "FullMoon"));
        sb.lastUpdated(now.format(ISO_FMT));
        AstronomicalSnapshot snapshot = sb.build();
        logger.info("=== Snapshot built (Awtrix/Claude deferred) ===");
        return new LiveComputeResult(snapshot, List.copyOf(pushes), now.toLocalDate(), mp.getPhaseName());
    }
    /**
     * Pushes Awtrix apps in parallel and kicks off async Claude moon-phase verification.
     * Safe to call after the dashboard snapshot has already been published.
     */
    public void deliverSideEffects(LiveComputeResult result) {
        if (result == null) {
            return;
        }
        // Claude is log-only and must never block the update cycle.
        LocalDate date = result.moonPhaseDate();
        String phase = result.moonPhaseName();
        CompletableFuture.runAsync(() -> verifyMoonPhaseAsync(date, phase), applicationTaskExecutor);
        awtrixPusher.pushAll(result.awtrixPushes());
        var stats = awtrixPusher.getStats();
        logger.info("Awtrix update summary: {} succeeded, {} failed", stats.success(), stats.failure());
        logger.info("=== Scheduled task completed ===");
    }
    /**
     * Convenience wrapper used by tests and callers that do not need early publish.
     */
    public AstronomicalSnapshot computeCurrent() {
        LiveComputeResult result = computeCurrentResult();
        deliverSideEffects(result);
        return result.snapshot();
    }
    /**
     * Computes a snapshot for a specific historical date/time.
     * Used for backfilling history. Does NOT perform Awtrix pushes or live LEO fetches.
     */
    public AstronomicalSnapshot computeForDate(ZonedDateTime target) {
        LocalDate targetDate = target.toLocalDate();
        AstronomicalSnapshot.Builder sb = AstronomicalSnapshot.builder();
        // Sun / Earth distance
        double sunDistAu = SunDistance.distanceAU(target);
        sb.sunDistanceAu(sunDistAu);
        // Planet distances — compute once, reuse for light travel
        double mercuryHelio = Planets.MERCURY.heliocentricDistanceAU(target);
        double venusHelio = Planets.VENUS.heliocentricDistanceAU(target);
        double marsHelio = Planets.MARS.heliocentricDistanceAU(target);
        double jupiterHelio = Planets.JUPITER.heliocentricDistanceAU(target);
        double saturnHelio = Planets.SATURN.heliocentricDistanceAU(target);
        double uranusHelio = Planets.URANUS.heliocentricDistanceAU(target);
        double neptuneHelio = Planets.NEPTUNE.heliocentricDistanceAU(target);
        double plutoHelio = Planets.PLUTO.heliocentricDistanceAU(target);
        sb.mercuryDistanceAu(mercuryHelio);
        sb.venusDistanceAu(venusHelio);
        sb.marsDistanceAu(Planets.MARS.distanceAU(target));
        sb.jupiterDistanceAu(Planets.JUPITER.distanceAU(target));
        sb.saturnDistanceAu(Planets.SATURN.distanceAU(target));
        sb.uranusDistanceAu(uranusHelio);
        sb.neptuneDistanceAu(neptuneHelio);
        sb.plutoDistanceAu(plutoHelio);
        // Daylight
        sb.daylightHours(DayLight.dayLengthHours(targetDate, latitude));
        // Deep-space probes
        double v1Helio = VoyagerDistance.heliocentricDistanceV1AU(target);
        double v2Helio = VoyagerDistance.heliocentricDistanceV2AU(target);
        sb.voyager1DistanceAu(VoyagerDistance.distanceFromEarthV1AU(target));
        sb.voyager2DistanceAu(VoyagerDistance.distanceFromEarthV2AU(target));
        sb.voyager1HelioDistanceAu(v1Helio);
        sb.voyager2HelioDistanceAu(v2Helio);
        sb.newHorizonsDistanceAu(NewHorizonsDistance.distanceFromEarthAU(target));
        sb.jamesWebbDistanceKm(JamesWebbDistance.distanceKmAt(target));
        // Upcoming events (relative to target date)
        sb.daysUntilSummerSolstice(EquinoxCalculator.daysUntilSummerSolstice(targetDate));
        sb.daysUntilWinterSolstice(EquinoxCalculator.daysUntilWinterSolstice(targetDate));
        sb.daysUntilPerihelion(PerihelionAphelion.daysUntilPerihelion(targetDate));
        sb.daysUntilAphelion(PerihelionAphelion.daysUntilAphelion(targetDate));
        // Earth speed and axial tilt (reuse sun distance)
        sb.earthSpeedKmPerSec(EarthSpeed.speedKmPerSecFromDistanceAu(sunDistAu));
        sb.earthSpeedKmPerHour(EarthSpeed.speedKmPerHourFromDistanceAu(sunDistAu));
        sb.earthAxialTiltDegrees(EarthAxialTilt.tiltDegrees(target));
        // Moon distance
        sb.moonDistanceKm(MoonDistance.distanceKm(target));
        // Light travel times from reused distances
        sb.lightTimeSunToEarth(LightTravelTime.formatTravelTime(sunDistAu));
        sb.lightTimeSunToMercury(LightTravelTime.formatTravelTime(mercuryHelio));
        sb.lightTimeSunToVenus(LightTravelTime.formatTravelTime(venusHelio));
        sb.lightTimeSunToMars(LightTravelTime.formatTravelTime(marsHelio));
        sb.lightTimeSunToJupiter(LightTravelTime.formatTravelTime(jupiterHelio));
        sb.lightTimeSunToSaturn(LightTravelTime.formatTravelTime(saturnHelio));
        sb.lightTimeSunToUranus(LightTravelTime.formatTravelTime(uranusHelio));
        sb.lightTimeSunToNeptune(LightTravelTime.formatTravelTime(neptuneHelio));
        sb.lightTimeSunToPluto(LightTravelTime.formatTravelTime(plutoHelio));
        sb.lightTimeSunToVoyager1(LightTravelTime.formatTravelTime(v1Helio));
        sb.lightTimeSunToVoyager2(LightTravelTime.formatTravelTime(v2Helio));
        // Moon phase
        MoonPhase mp = MoonPhase.fromDate(targetDate);
        sb.moonPhaseName(mp.getPhaseName());
        sb.moonPhaseIcon(mp.getPhaseIcon());
        sb.moonAsciiArt(mp.getAscii());
        sb.moonIlluminationPercent(mp.getIlluminationPercent());
        sb.moonAgeDays(mp.getAgeDays());
        sb.daysUntilFullMoon(mp.getDaysUntilFullMoon());
        // LEO — live-only, cannot be reconstructed for historical dates
        sb.issAltitudeKm(0).tiangongAltitudeKm(0).hubbleAltitudeKm(0)
          .starlinkSatelliteCount(0).kuiperSatelliteCount(0).totalSatellitesInOrbit(0)
          .issCrew(0).totalPeopleInSpace(0).craftOccupancy(java.util.Collections.emptyMap());
        // Aurora — live-only
        sb.auroraKpIndex(-1.0);
        // Sunrise / sunset can be computed for historical dates
        sb.sunriseTime(SunriseSunset.sunriseUtc(targetDate, latitude, longitude));
        sb.sunsetTime(SunriseSunset.sunsetUtc(targetDate, latitude, longitude));
        sb.lastUpdated(target.format(ISO_FMT));
        return sb.build();
    }
    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────
    private static AwtrixPusher.PushRequest push(String app, String text, String icon) {
        return new AwtrixPusher.PushRequest(app, text, icon);
    }
    private void verifyMoonPhaseAsync(LocalDate date, String calculatedPhase) {
        try {
            ClaudeMoonPhaseVerifier.VerificationResult verification =
                    ClaudeMoonPhaseVerifier.verify(date, calculatedPhase);
            if ("N/A".equals(verification.claudePhase())) {
                logger.info("Claude verification skipped (API key not set)");
            } else if (verification.matches()) {
                logger.info("Claude AI confirms moon phase: {}", verification.claudePhase());
            } else {
                logger.warn("Claude AI disagrees! Calculated: {} | Claude says: {} ({}). "
                                + "Keeping deterministic result - investigate algorithm if this persists.",
                        verification.calculatedPhase(), verification.claudePhase(), verification.details());
            }
        } catch (Exception e) {
            logger.warn("Claude verification failed: {}", e.getMessage());
        }
    }
    private void logPlanetDistance(String name, double currentAu, PlanetDistance planet) {
        double[] range = planet.minMaxDistanceAUNow();
        logger.info("Current Earth-{} distance: {} AU", name, String.format("%.6f", currentAu));
        String bar = buildRelativeBar(currentAu, range[0], range[1], BAR_WIDTH);
        logger.info("{}", bar);
        logger.info("{}        {}        {}",
                String.format("%.6f", range[0]),
                String.format("%.6f", currentAu),
                String.format("%.6f", range[1]));
    }
    private void logDistanceBar(String title, double current, double min, double max, int decimals) {
        if (title != null) {
            String fmt = "%." + decimals + "f";
            logger.info("{}: {} AU", title, String.format(fmt, current));
        }
        logger.info("{}", buildRelativeBar(current, min, max, BAR_WIDTH));
    }
    private String buildRelativeBar(double current, double min, double max, int innerWidth) {
        if (innerWidth < 1) innerWidth = 1;
        StringBuilder buf = new StringBuilder();
        buf.append("Min |");
        int pos;
        if (!Double.isFinite(min) || !Double.isFinite(max) || max <= min) {
            logger.warn("buildRelativeBar: invalid range min={} max={} - defaulting to centre", min, max);
            pos = innerWidth / 2;
        } else {
            double frac = (current - min) / (max - min);
            if (Double.isNaN(frac)) frac = 0.5;
            frac = Math.max(0.0, Math.min(1.0, frac));
            pos = (int) Math.round(frac * (innerWidth - 1));
        }
        int w = innerWidth;
        IntStream.range(0, w).forEach(i -> buf.append(i == pos ? '0' : '-'));
        buf.append("| Max");
        return buf.toString();
    }
}
