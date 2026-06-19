package org.iHarwood.calculation;

import org.iHarwood.APIPost;
import org.iHarwood.AstronomicalSnapshot;
import org.iHarwood.MoonPhaseModule.*;
import org.iHarwood.integration.awtrix.AwtrixPusher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
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
    private final double latitude;
    private final double longitude;

    public CalculationOrchestrator(
            AwtrixPusher awtrixPusher,
            @Value("${app.latitude:${LATITUDE:51.4769}}") double latitude,
            @Value("${app.longitude:${LONGITUDE:0.0}}") double longitude) {

        this.awtrixPusher = awtrixPusher;
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
     * Performs a full live calculation cycle.
     * Includes rich logging and all Awtrix device pushes.
     */
    public AstronomicalSnapshot computeCurrent() {
        logger.info("=== Scheduled task starting ===");
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

        logger.info("Date: {} | Day: {} | Time: {}",
                now.format(DATE_FMT), now.format(DAY_FMT), now.format(TIME_FMT));
        logger.info("1 astronomical unit = {}", AU_IN_MILES);

        AstronomicalSnapshot.Builder sb = AstronomicalSnapshot.builder();

        computeSunEarth(sb);
        computePlanetDistances(sb);
        computeDayLength(sb, now);
        computeVoyagerDistance(sb);
        computeNewHorizonsDistance(sb);
        computeJamesWebbDistance(sb);
        computeEquinox(sb);
        computePerihelionAphelion(sb);
        computeEarthSpeed(sb);
        computeMoonDistance(sb);
        computeLightTravelTimes(sb);
        computeLeoData(sb);
        computeSunriseSunset(sb, now);
        computeAurora(sb);
        computeMoonPhase(sb, now);

        sb.lastUpdated(now.format(ISO_FMT));

        var stats = awtrixPusher.getStats();
        logger.info("Awtrix update summary: {} succeeded, {} failed", stats.success(), stats.failure());
        logger.info("=== Scheduled task completed ===");

        return sb.build();
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

        // Planet distances
        sb.mercuryDistanceAu(Planets.MERCURY.heliocentricDistanceAU(target));
        sb.venusDistanceAu(Planets.VENUS.heliocentricDistanceAU(target));
        sb.marsDistanceAu(Planets.MARS.distanceAU(target));
        sb.jupiterDistanceAu(Planets.JUPITER.distanceAU(target));
        sb.saturnDistanceAu(Planets.SATURN.distanceAU(target));
        sb.uranusDistanceAu(Planets.URANUS.heliocentricDistanceAU(target));
        sb.neptuneDistanceAu(Planets.NEPTUNE.heliocentricDistanceAU(target));
        sb.plutoDistanceAu(Planets.PLUTO.heliocentricDistanceAU(target));

        // Daylight
        sb.daylightHours(DayLight.dayLengthHours(targetDate, latitude));

        // Deep-space probes
        sb.voyager1DistanceAu(VoyagerDistance.distanceFromEarthV1AU(target));
        sb.voyager2DistanceAu(VoyagerDistance.distanceFromEarthV2AU(target));
        sb.voyager1HelioDistanceAu(VoyagerDistance.heliocentricDistanceV1AU(target));
        sb.voyager2HelioDistanceAu(VoyagerDistance.heliocentricDistanceV2AU(target));
        sb.newHorizonsDistanceAu(NewHorizonsDistance.distanceFromEarthAU(target));
        sb.jamesWebbDistanceKm(JamesWebbDistance.distanceKmAt(target));

        // Upcoming events (relative to target date)
        sb.daysUntilSummerSolstice(EquinoxCalculator.daysUntilSummerSolstice(targetDate));
        sb.daysUntilWinterSolstice(EquinoxCalculator.daysUntilWinterSolstice(targetDate));
        sb.daysUntilPerihelion(PerihelionAphelion.daysUntilPerihelion(targetDate));
        sb.daysUntilAphelion(PerihelionAphelion.daysUntilAphelion(targetDate));

        // Earth speed and axial tilt
        sb.earthSpeedKmPerSec(EarthSpeed.speedKmPerSec(target));
        sb.earthSpeedKmPerHour(EarthSpeed.speedKmPerHour(target));
        sb.earthAxialTiltDegrees(EarthAxialTilt.tiltDegrees(target));

        // Moon distance
        sb.moonDistanceKm(MoonDistance.distanceKm(target));

        // Light travel times (using historical distances)
        sb.lightTimeSunToEarth(LightTravelTime.formatTravelTime(sunDistAu));
        sb.lightTimeSunToMercury(LightTravelTime.formatTravelTime(Planets.MERCURY.heliocentricDistanceAU(target)));
        sb.lightTimeSunToVenus(LightTravelTime.formatTravelTime(Planets.VENUS.heliocentricDistanceAU(target)));
        sb.lightTimeSunToMars(LightTravelTime.formatTravelTime(Planets.MARS.heliocentricDistanceAU(target)));
        sb.lightTimeSunToJupiter(LightTravelTime.formatTravelTime(Planets.JUPITER.heliocentricDistanceAU(target)));
        sb.lightTimeSunToSaturn(LightTravelTime.formatTravelTime(Planets.SATURN.heliocentricDistanceAU(target)));
        sb.lightTimeSunToUranus(LightTravelTime.formatTravelTime(Planets.URANUS.heliocentricDistanceAU(target)));
        sb.lightTimeSunToNeptune(LightTravelTime.formatTravelTime(Planets.NEPTUNE.heliocentricDistanceAU(target)));
        sb.lightTimeSunToPluto(LightTravelTime.formatTravelTime(Planets.PLUTO.heliocentricDistanceAU(target)));
        sb.lightTimeSunToVoyager1(LightTravelTime.formatTravelTime(VoyagerDistance.heliocentricDistanceV1AU(target)));
        sb.lightTimeSunToVoyager2(LightTravelTime.formatTravelTime(VoyagerDistance.heliocentricDistanceV2AU(target)));

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
    // Private computation methods (moved from Main)
    // ─────────────────────────────────────────────────────────────────────────

    private void computeSunEarth(AstronomicalSnapshot.Builder sb) {
        double[] range = SunDistance.minMaxDistanceAUNow();
        double sunDistanceAu = SunDistance.distanceAUNow();
        sb.sunDistanceAu(sunDistanceAu);

        logger.info("Current Earth-Sun distance: {} AU", String.format("%.6f", sunDistanceAu));
        String bar = buildRelativeBar(sunDistanceAu, range[0], range[1], BAR_WIDTH);
        logger.info("{}", bar);
        logger.info("Sun Earth:{}        {}        {}",
                String.format("%.6f", range[0]),
                String.format("%.6f", sunDistanceAu),
                String.format("%.6f", range[1]));
    }

    private void computePlanetDistances(AstronomicalSnapshot.Builder sb) {
        sb.mercuryDistanceAu(Planets.MERCURY.heliocentricDistanceAUNow());
        sb.venusDistanceAu(Planets.VENUS.heliocentricDistanceAUNow());

        double marsAu = Planets.MARS.distanceAUNow();
        sb.marsDistanceAu(marsAu);
        logPlanetDistance("Mars", marsAu, Planets.MARS);
        awtrixPusher.push("marsDistanceAu", String.format("%.1fau", marsAu), APIPost.IconType.MARS.name());

        double jupiterAu = Planets.JUPITER.distanceAUNow();
        sb.jupiterDistanceAu(jupiterAu);
        logPlanetDistance("Jupiter", jupiterAu, Planets.JUPITER);
        awtrixPusher.push("jupiterDistanceAu", String.format("%.1fau", jupiterAu), APIPost.IconType.JUPITER.name());

        double saturnAu = Planets.SATURN.distanceAUNow();
        sb.saturnDistanceAu(saturnAu);
        logPlanetDistance("Saturn", saturnAu, Planets.SATURN);
        awtrixPusher.push("saturnDistanceAu", String.format("%.1fau", saturnAu), APIPost.IconType.SATURN.name());

        sb.uranusDistanceAu(Planets.URANUS.heliocentricDistanceAUNow());
        sb.neptuneDistanceAu(Planets.NEPTUNE.heliocentricDistanceAUNow());
        sb.plutoDistanceAu(Planets.PLUTO.heliocentricDistanceAUNow());
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

    private void computeVoyagerDistance(AstronomicalSnapshot.Builder sb) {
        double v1Au = VoyagerDistance.distanceFromEarthV1AUNow();
        double v2Au = VoyagerDistance.distanceFromEarthV2AUNow();
        sb.voyager1DistanceAu(v1Au);
        sb.voyager2DistanceAu(v2Au);
        sb.voyager1HelioDistanceAu(VoyagerDistance.heliocentricDistanceV1AUNow());
        sb.voyager2HelioDistanceAu(VoyagerDistance.heliocentricDistanceV2AUNow());

        logger.info("Voyager 1 distance from Earth: {} AU", String.format("%.6f", v1Au));
        logger.info("Voyager 2 distance from Earth: {} AU", String.format("%.6f", v2Au));

        awtrixPusher.push("voyager1", String.format("V1:%.0fau", v1Au), APIPost.IconType.VOYAGER.name());
        awtrixPusher.push("voyager2", String.format("V2:%.0fau", v2Au), APIPost.IconType.VOYAGER.name());
    }

    private void computeDayLength(AstronomicalSnapshot.Builder sb, ZonedDateTime now) {
        double[] dayRange = DayLight.minMaxDayLengthHoursNow(latitude);
        double currentDayHours = DayLight.dayLengthHours(now.toLocalDate(), latitude);
        sb.daylightHours(currentDayHours);

        logger.info("Daylight length (hours) at latitude {}", latitude);
        String dayBar = buildRelativeBar(currentDayHours, dayRange[0], dayRange[1], BAR_WIDTH);
        logger.info("{}", dayBar);
        logger.info("{}        {}        {}",
                String.format("%.2f", dayRange[0]),
                String.format("%.2f", currentDayHours),
                String.format("%.2f", dayRange[1]));

        awtrixPusher.push("CurrentDayLength", String.format("%.1fhrs", currentDayHours), APIPost.IconType.DAYLENGTH.name());
    }

    private void computeNewHorizonsDistance(AstronomicalSnapshot.Builder sb) {
        double nhAu = NewHorizonsDistance.distanceFromEarthAUNow();
        sb.newHorizonsDistanceAu(nhAu);
        logger.info("New Horizons distance from Earth: {} AU ({} km/s)",
                String.format("%.6f", nhAu), NewHorizonsDistance.speedKmPerSec());

        awtrixPusher.push("newhorizons", String.format("NH:%.0fau", nhAu), APIPost.IconType.NEWHORIZONS.name());
    }

    private void computeJamesWebbDistance(AstronomicalSnapshot.Builder sb) {
        double jwstKm = JamesWebbDistance.distanceKmNow();
        sb.jamesWebbDistanceKm(jwstKm);
        logger.info("James Webb Space Telescope distance from Earth: {} km", String.format("%,.0f", jwstKm));
    }

    private void computeEquinox(AstronomicalSnapshot.Builder sb) {
        String nextSummer = EquinoxCalculator.nextSummerSolstice().format(SHORT_DATE_FMT);
        String nextWinter = EquinoxCalculator.nextWinterSolstice().format(SHORT_DATE_FMT);

        long daysUntilSummer = EquinoxCalculator.daysUntilSummerSolstice();
        long daysUntilWinter = EquinoxCalculator.daysUntilWinterSolstice();
        sb.daysUntilSummerSolstice(daysUntilSummer);
        sb.daysUntilWinterSolstice(daysUntilWinter);

        logger.info("Next summer solstice: {}", nextSummer);
        awtrixPusher.push("summersolstice", daysUntilSummer + "d", APIPost.IconType.SUMMER.name());

        logger.info("Next winter solstice: {}", nextWinter);
        awtrixPusher.push("wintersolstice", daysUntilWinter + "d", APIPost.IconType.WINTER.name());
    }

    private void computePerihelionAphelion(AstronomicalSnapshot.Builder sb) {
        long daysToPerihelion = PerihelionAphelion.daysUntilPerihelion();
        long daysToAphelion = PerihelionAphelion.daysUntilAphelion();
        sb.daysUntilPerihelion(daysToPerihelion);
        sb.daysUntilAphelion(daysToAphelion);

        logger.info("Days until next perihelion (closest to Sun): {}", daysToPerihelion);
        logger.info("Days until next aphelion (farthest from Sun): {}", daysToAphelion);

        awtrixPusher.push("perihelion", daysToPerihelion + "d", APIPost.IconType.PERIHELION.name());
        awtrixPusher.push("aphelion", daysToAphelion + "d", APIPost.IconType.PERIHELION.name());
    }

    private void computeEarthSpeed(AstronomicalSnapshot.Builder sb) {
        double speedKmS = EarthSpeed.speedKmPerSecNow();
        double speedKmH = EarthSpeed.speedKmPerHourNow();
        sb.earthSpeedKmPerSec(speedKmS);
        sb.earthSpeedKmPerHour(speedKmH);

        double tilt = EarthAxialTilt.tiltDegreesNow();
        sb.earthAxialTiltDegrees(tilt);

        logger.info("Earth's orbital speed: {} km/s ({} km/h)",
                String.format("%.2f", speedKmS), String.format("%,.0f", speedKmH));
        logger.info("Earth's axial tilt: {} deg", String.format("%.3f", tilt));

        awtrixPusher.push("earthSpeed", String.format("%.1fkm/s", speedKmS), APIPost.IconType.EARTH.name());
    }

    private void computeMoonDistance(AstronomicalSnapshot.Builder sb) {
        double moonDistKm = MoonDistance.distanceKmNow();
        double[] moonRange = MoonDistance.minMaxDistanceKmNow();
        sb.moonDistanceKm(moonDistKm);

        logger.info("Current Moon distance: {}", MoonDistance.formatDistanceKm(moonDistKm));
        String moonBar = buildRelativeBar(moonDistKm, moonRange[0], moonRange[1], BAR_WIDTH);
        logger.info("{}", moonBar);
        logger.info("{}        {}        {}",
                MoonDistance.formatDistanceKm(moonRange[0]),
                MoonDistance.formatDistanceKm(moonDistKm),
                MoonDistance.formatDistanceKm(moonRange[1]));

        awtrixPusher.push("moonDistance", String.format("%,.0fkm", moonDistKm), APIPost.IconType.MOON.name());
    }

    private void computeLeoData(AstronomicalSnapshot.Builder sb) {
        logger.info("--- LEO Data ---");
        sb.issAltitudeKm(LeoDataFetcher.fetchIssAltitudeKm());
        sb.tiangongAltitudeKm(LeoDataFetcher.fetchTiangongAltitudeKm());
        sb.hubbleAltitudeKm(LeoDataFetcher.fetchHubbleAltitudeKm());
        sb.starlinkSatelliteCount(LeoDataFetcher.fetchStarlinkCount());
        sb.kuiperSatelliteCount(LeoDataFetcher.fetchKuiperCount());
        sb.totalSatellitesInOrbit(LeoDataFetcher.fetchTotalSatelliteCount());

        LeoDataFetcher.PeopleInSpace people = LeoDataFetcher.fetchPeopleInSpace();
        sb.issCrew(people.issCrew());
        sb.totalPeopleInSpace(people.total());
        sb.craftOccupancy(people.craftOccupancy());
        logger.info("People in space: {} total, {} on ISS", people.total(), people.issCrew());
    }

    private void computeSunriseSunset(AstronomicalSnapshot.Builder sb, ZonedDateTime now) {
        String sunrise = SunriseSunset.sunriseUtc(now.toLocalDate(), latitude, longitude);
        String sunset  = SunriseSunset.sunsetUtc(now.toLocalDate(), latitude, longitude);
        sb.sunriseTime(sunrise);
        sb.sunsetTime(sunset);
        logger.info("Sunrise: {} UTC | Sunset: {} UTC (lat={}, lon={})", sunrise, sunset, latitude, longitude);
    }

    private void computeAurora(AstronomicalSnapshot.Builder sb) {
        double kp = AuroraKpFetcher.fetchKpIndex();
        sb.auroraKpIndex(kp);
        if (kp >= 0) {
            String activity = kp < 2 ? "Quiet" : kp < 4 ? "Unsettled" : kp < 5 ? "Active" : "Storm (Kp≥5)";
            logger.info("Aurora Kp index: {} ({})", kp, activity);
            // Push to Awtrix — always show current Kp, storm alert when Kp ≥ 5
            String label = kp >= 5 ? String.format("⚡Kp%.1f", kp) : String.format("Kp%.1f", kp);
            awtrixPusher.push("auroraKp", label, APIPost.IconType.AURORA.name());
        }
    }

    private void computeLightTravelTimes(AstronomicalSnapshot.Builder sb) {
        String earth = LightTravelTime.sunToEarthNow();
        String mercury = LightTravelTime.sunToMercuryNow();
        String venus = LightTravelTime.sunToVenusNow();
        String mars = LightTravelTime.sunToMarsNow();
        String jupiter = LightTravelTime.sunToJupiterNow();
        String saturn = LightTravelTime.sunToSaturnNow();
        String uranus = LightTravelTime.sunToUranusNow();
        String neptune = LightTravelTime.sunToNeptuneNow();
        String pluto = LightTravelTime.sunToPlutoNow();
        String v1 = LightTravelTime.sunToVoyager1Now();
        String v2 = LightTravelTime.sunToVoyager2Now();

        sb.lightTimeSunToEarth(earth);
        sb.lightTimeSunToMercury(mercury);
        sb.lightTimeSunToVenus(venus);
        sb.lightTimeSunToMars(mars);
        sb.lightTimeSunToJupiter(jupiter);
        sb.lightTimeSunToSaturn(saturn);
        sb.lightTimeSunToUranus(uranus);
        sb.lightTimeSunToNeptune(neptune);
        sb.lightTimeSunToPluto(pluto);
        sb.lightTimeSunToVoyager1(v1);
        sb.lightTimeSunToVoyager2(v2);

        logger.info("--- Light Travel Times ---");
        logger.info("Sun -> Earth:    {}", earth);
        logger.info("Sun -> Mercury:  {}", mercury);
        logger.info("Sun -> Venus:    {}", venus);
        logger.info("Sun -> Mars:     {}", mars);
        logger.info("Sun -> Jupiter:  {}", jupiter);
        logger.info("Sun -> Saturn:   {}", saturn);
        logger.info("Sun -> Uranus:   {}", uranus);
        logger.info("Sun -> Neptune:  {}", neptune);
        logger.info("Sun -> Pluto:    {}", pluto);
        logger.info("Sun -> Voyager 1: {}", v1);
        logger.info("Sun -> Voyager 2: {}", v2);

        awtrixPusher.push("lightMars", "Lt:" + mars, APIPost.IconType.LIGHT.name());
        awtrixPusher.push("lightJupiter", "Lt:" + jupiter, APIPost.IconType.LIGHT.name());
    }

    private void computeMoonPhase(AstronomicalSnapshot.Builder sb, ZonedDateTime now) {
        MoonPhase mp = MoonPhase.fromDate(now.toLocalDate());
        logger.info("Current moon phase is {} ({} days, {}% illuminated).",
                mp.getPhaseName(), mp.getAgeDays(), mp.getIlluminationPercent());
        Arrays.asList(mp.getAscii()).forEach(row -> logger.info("{}", row));

        // Claude verification - LOG ONLY, never override deterministic calculation.
        ClaudeMoonPhaseVerifier.VerificationResult verification =
                ClaudeMoonPhaseVerifier.verify(now.toLocalDate(), mp.getPhaseName());
        if ("N/A".equals(verification.claudePhase())) {
            logger.info("Claude verification skipped (API key not set)");
        } else if (verification.matches()) {
            logger.info("Claude AI confirms moon phase: {}", verification.claudePhase());
        } else {
            logger.warn("Claude AI disagrees! Calculated: {} | Claude says: {} ({}). "
                            + "Keeping deterministic result - investigate algorithm if this persists.",
                    verification.calculatedPhase(), verification.claudePhase(), verification.details());
        }

        int daysUntilFull = mp.getDaysUntilFullMoon();
        logger.info("Days until next full moon: {}", daysUntilFull);

        sb.moonPhaseName(mp.getPhaseName());
        sb.moonIlluminationPercent(mp.getIlluminationPercent());
        sb.moonPhaseIcon(mp.getPhaseIcon());
        sb.moonAsciiArt(mp.getAscii());
        sb.moonAgeDays(mp.getAgeDays());
        sb.daysUntilFullMoon(daysUntilFull);

        awtrixPusher.push("moonphase", mp.getPhaseName(), mp.getPhaseIcon());
        awtrixPusher.push("moonillumination", mp.getIlluminationPercent() + "%", mp.getPhaseIcon());
        awtrixPusher.push("fullmoon", daysUntilFull + "d", "FullMoon");
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
