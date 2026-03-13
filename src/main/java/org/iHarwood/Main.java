package org.iHarwood;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.iHarwood.MoonPhaseModule.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Component
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("EEEE");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter SHORT_DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yy");
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final String HOSTNAME_ENV_VAR = "AWTRIXHOSTNAME";
    private static final String DEFAULT_HOSTNAME = "http://moonclock.local";
    private static final int BAR_WIDTH = 30;
    private static final String AU_IN_MILES = "92,955,807.273026 miles";
    private static final int AWTRIX_MAX_ATTEMPTS = 3;
    private static final long AWTRIX_RETRY_DELAY_MS = 2000;

    private final String baseHostname = System.getenv().getOrDefault(HOSTNAME_ENV_VAR, DEFAULT_HOSTNAME);
    private final String hostname = baseHostname.concat("/api/custom?name=");

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final double DEFAULT_LATITUDE = 51.4769;

    @Value("${app.latitude:${LATITUDE:51.4769}}")
    private double latitude;

    private final Optional<AstronomicalDataService> dataService;

    private final AtomicInteger awtrixSuccessCount = new AtomicInteger(0);
    private final AtomicInteger awtrixFailureCount = new AtomicInteger(0);

    // Lock object for thread-safe update execution
    private final Object updateLock = new Object();

    public Main(Optional<AstronomicalDataService> dataService) {
        this.dataService = dataService;
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Application shutting down - Awtrix stats: {} succeeded, {} failed",
                awtrixSuccessCount.get(), awtrixFailureCount.get());
    }

    @PostConstruct
    public void init() {
        if (latitude < -90 || latitude > 90) {
            logger.warn("Invalid latitude {} - must be between -90 and 90. Falling back to {} (Greenwich).",
                    latitude, DEFAULT_LATITUDE);
            latitude = DEFAULT_LATITUDE;
        }
        logger.info("Application started - running initial update");
        checkAwtrixConnectivity();
        update();
    }

    private void checkAwtrixConnectivity() {
        String statsUrl = baseHostname + "/api/stats";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(statsUrl))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            int status = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            if (status >= 200 && status < 300) {
                logger.info("Awtrix reachable at {} (HTTP {})", baseHostname, status);
            } else {
                logger.warn("Awtrix at {} responded with unexpected status {} - pushes may fail",
                        baseHostname, status);
            }
        } catch (Exception e) {
            logger.warn("Awtrix device not reachable at {} - pushes will fail until it comes online ({})",
                    baseHostname, e.getMessage());
        }
    }

    /**
     * Main scheduled update - synchronized to prevent data races between
     * concurrent manual refreshes and scheduled runs.
     */
    @Scheduled(cron = "${app.cron:${CRON_SCHEDULE:0 1 0,12 * * *}}")
    public void update() {
        synchronized (updateLock) {
            doUpdate();
        }
    }

    /**
     * Calculates an astronomical snapshot for a specific historical date/time.
     * Uses parameterised module methods instead of the *Now() convenience wrappers.
     * LEO fields (ISS, Tiangong, Hubble, satellite counts) are set to 0 because live
     * TLE data cannot be reconstructed for past dates.
     */
    public AstronomicalSnapshot calculateSnapshotForDate(ZonedDateTime target) {
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
          .starlinkSatelliteCount(0).kuiperSatelliteCount(0).totalSatellitesInOrbit(0);

        sb.lastUpdated(target.format(ISO_FMT));
        return sb.build();
    }

    private void doUpdate() {
        logger.info("=== Scheduled task starting ===");
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

        logger.info("Date: {} | Day: {} | Time: {}",
                now.format(DATE_FMT), now.format(DAY_FMT), now.format(TIME_FMT));
        logger.info("1 astronomical unit = {}", AU_IN_MILES);

        // Build snapshot using the builder - no mutable instance fields needed
        AstronomicalSnapshot.Builder sb = AstronomicalSnapshot.builder();

        computeSunEarth(sb);
        computePlanetDistances(sb);
        computeDayLength(sb, now);
        computeVoyagerDistance(sb);
        computeNewHorizonsDistance(sb);
        computeEquinox(sb);
        computePerihelionAphelion(sb);
        computeEarthSpeed(sb);
        computeMoonDistance(sb);
        computeLightTravelTimes(sb);
        computeLeoData(sb);
        computeMoonPhase(sb, now);

        sb.lastUpdated(now.format(ISO_FMT));

        logger.info("Awtrix update summary: {} succeeded, {} failed",
                awtrixSuccessCount.get(), awtrixFailureCount.get());
        logger.info("=== Scheduled task completed ===");

        AstronomicalSnapshot snapshot = sb.build();
        dataService.ifPresent(svc -> svc.publishSnapshot(snapshot));
    }

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
        sendAwtrix("marsDistanceAu", String.format("%.1fau", marsAu), APIPost.IconType.MARS.name());

        double jupiterAu = Planets.JUPITER.distanceAUNow();
        sb.jupiterDistanceAu(jupiterAu);
        logPlanetDistance("Jupiter", jupiterAu, Planets.JUPITER);
        sendAwtrix("jupiterDistanceAu", String.format("%.1fau", jupiterAu), APIPost.IconType.JUPITER.name());

        double saturnAu = Planets.SATURN.distanceAUNow();
        sb.saturnDistanceAu(saturnAu);
        logPlanetDistance("Saturn", saturnAu, Planets.SATURN);
        sendAwtrix("saturnDistanceAu", String.format("%.1fau", saturnAu), APIPost.IconType.SATURN.name());

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

        sendAwtrix("voyager1", String.format("V1:%.0fau", v1Au), APIPost.IconType.VOYAGER.name());
        sendAwtrix("voyager2", String.format("V2:%.0fau", v2Au), APIPost.IconType.VOYAGER.name());
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

        sendAwtrix("CurrentDayLength", String.format("%.1fhrs", currentDayHours), APIPost.IconType.DAYLENGTH.name());
    }

    private void computeNewHorizonsDistance(AstronomicalSnapshot.Builder sb) {
        double nhAu = NewHorizonsDistance.distanceFromEarthAUNow();
        sb.newHorizonsDistanceAu(nhAu);
        logger.info("New Horizons distance from Earth: {} AU ({} km/s)",
                String.format("%.6f", nhAu), NewHorizonsDistance.speedKmPerSec());

        sendAwtrix("newhorizons", String.format("NH:%.0fau", nhAu), APIPost.IconType.NEWHORIZONS.name());
    }

    private void computeEquinox(AstronomicalSnapshot.Builder sb) {
        String nextSummer = EquinoxCalculator.nextSummerSolstice().format(SHORT_DATE_FMT);
        String nextWinter = EquinoxCalculator.nextWinterSolstice().format(SHORT_DATE_FMT);

        long daysUntilSummer = EquinoxCalculator.daysUntilSummerSolstice();
        long daysUntilWinter = EquinoxCalculator.daysUntilWinterSolstice();
        sb.daysUntilSummerSolstice(daysUntilSummer);
        sb.daysUntilWinterSolstice(daysUntilWinter);

        logger.info("Next summer solstice: {}", nextSummer);
        sendAwtrix("summersolstice", daysUntilSummer + "d", APIPost.IconType.SUMMER.name());

        logger.info("Next winter solstice: {}", nextWinter);
        sendAwtrix("wintersolstice", daysUntilWinter + "d", APIPost.IconType.WINTER.name());
    }

    private void computePerihelionAphelion(AstronomicalSnapshot.Builder sb) {
        long daysToPerihelion = PerihelionAphelion.daysUntilPerihelion();
        long daysToAphelion = PerihelionAphelion.daysUntilAphelion();
        sb.daysUntilPerihelion(daysToPerihelion);
        sb.daysUntilAphelion(daysToAphelion);

        logger.info("Days until next perihelion (closest to Sun): {}", daysToPerihelion);
        logger.info("Days until next aphelion (farthest from Sun): {}", daysToAphelion);

        sendAwtrix("perihelion", daysToPerihelion + "d", APIPost.IconType.PERIHELION.name());
        sendAwtrix("aphelion", daysToAphelion + "d", APIPost.IconType.PERIHELION.name());
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

        sendAwtrix("earthSpeed", String.format("%.1fkm/s", speedKmS), APIPost.IconType.EARTH.name());
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

        sendAwtrix("moonDistance", String.format("%,.0fkm", moonDistKm), APIPost.IconType.MOON.name());
    }

    private void computeLeoData(AstronomicalSnapshot.Builder sb) {
        logger.info("--- LEO Data ---");
        sb.issAltitudeKm(LeoDataFetcher.fetchIssAltitudeKm());
        sb.tiangongAltitudeKm(LeoDataFetcher.fetchTiangongAltitudeKm());
        sb.hubbleAltitudeKm(LeoDataFetcher.fetchHubbleAltitudeKm());
        sb.starlinkSatelliteCount(LeoDataFetcher.fetchStarlinkCount());
        sb.kuiperSatelliteCount(LeoDataFetcher.fetchKuiperCount());
        sb.totalSatellitesInOrbit(LeoDataFetcher.fetchTotalSatelliteCount());
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

        sendAwtrix("lightMars", "Lt:" + mars, APIPost.IconType.LIGHT.name());
        sendAwtrix("lightJupiter", "Lt:" + jupiter, APIPost.IconType.LIGHT.name());
    }

    private void computeMoonPhase(AstronomicalSnapshot.Builder sb, ZonedDateTime now) {
        MoonPhase mp = MoonPhase.fromDate(now.toLocalDate());
        logger.info("Current moon phase is {} ({} days, {}% illuminated).",
                mp.getPhaseName(), mp.getAgeDays(), mp.getIlluminationPercent());
        Arrays.asList(mp.getAscii()).forEach(row -> logger.info("{}", row));

        // Claude verification - LOG ONLY, never override deterministic calculation.
        // LLMs can hallucinate; formula-based answers are more reliable for astronomy.
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

        sendAwtrix("moonphase", mp.getPhaseName(), mp.getPhaseIcon());
        sendAwtrix("moonillumination", mp.getIlluminationPercent() + "%", mp.getPhaseIcon());
        sendAwtrix("fullmoon", daysUntilFull + "d", "FullMoon");
    }

    private void sendAwtrix(String appName, String text, String icon) {
        String localHostname = hostname.concat(appName);
        logger.info("Awtrix host API call:{}", localHostname);

        APIPost apiPost = new APIPost(appName, text, localHostname, "1", "", icon);

        for (int attempt = 1; attempt <= AWTRIX_MAX_ATTEMPTS; attempt++) {
            try {
                int responseCode = apiPost.sendPost();
                logger.info("Awtrix response ({}): {}", appName, responseCode);
                awtrixSuccessCount.incrementAndGet();
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Awtrix send interrupted ({})", appName);
                awtrixFailureCount.incrementAndGet();
                return;
            } catch (IOException e) {
                if (attempt < AWTRIX_MAX_ATTEMPTS) {
                    logger.warn("Awtrix send failed ({}) attempt {}/{} - retrying in {}ms: {}",
                            appName, attempt, AWTRIX_MAX_ATTEMPTS, AWTRIX_RETRY_DELAY_MS, e.getMessage());
                    try {
                        Thread.sleep(AWTRIX_RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        awtrixFailureCount.incrementAndGet();
                        return;
                    }
                } else {
                    logger.warn("Awtrix send failed ({}) after {} attempts: {}",
                            appName, AWTRIX_MAX_ATTEMPTS, e.getMessage());
                    awtrixFailureCount.incrementAndGet();
                }
            }
        }
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
