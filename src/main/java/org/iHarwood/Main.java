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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

@Component
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    // Static formatters to avoid recreation on each run
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("EEEE");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter SHORT_DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yy");
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final String HOSTNAME_ENV_VAR = "AWTRIXHOSTNAME";
    private static final String DEFAULT_HOSTNAME = "http://moonclock.local";
    private static final int BAR_WIDTH = 30;
    private static final String AU_IN_MILES = "92,955,807.273026 miles";
    private final String baseHostname = System.getenv().getOrDefault(HOSTNAME_ENV_VAR, DEFAULT_HOSTNAME);
    private final String hostname = baseHostname.concat("/api/custom?name=");

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final double DEFAULT_LATITUDE = 51.4769;

    @Value("${app.latitude:${LATITUDE:51.4769}}")
    private double latitude;

    // Optional SSE publisher — absent in headless mode (ConditionalOnWebApplication)
    private final Optional<AstronomicalDataService> dataService;

    // Awtrix error rate tracking
    private final AtomicInteger awtrixSuccessCount = new AtomicInteger(0);
    private final AtomicInteger awtrixFailureCount = new AtomicInteger(0);

    // Snapshot fields — populated during each update() cycle
    private double snapshotSunDistanceAu;
    private double snapshotMarsDistanceAu;
    private double snapshotJupiterDistanceAu;
    private double snapshotSaturnDistanceAu;
    private double snapshotMoonDistanceKm;
    private double snapshotVoyager1DistanceAu;
    private double snapshotVoyager2DistanceAu;
    private double snapshotNewHorizonsDistanceAu;
    private double snapshotEarthSpeedKmPerSec;
    private double snapshotEarthSpeedKmPerHour;
    private double snapshotDaylightHours;
    private String snapshotLightTimeSunToEarth;
    private String snapshotLightTimeEarthToMars;
    private String snapshotLightTimeEarthToJupiter;
    private String snapshotLightTimeEarthToSaturn;
    private String snapshotLightTimeEarthToVoyager1;
    private String snapshotLightTimeEarthToVoyager2;
    private long snapshotDaysUntilSummerSolstice;
    private long snapshotDaysUntilWinterSolstice;
    private long snapshotDaysUntilPerihelion;
    private long snapshotDaysUntilAphelion;
    private MoonPhase snapshotMoonPhase;

    public Main(Optional<AstronomicalDataService> dataService) {
        this.dataService = dataService;
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Application shutting down – Awtrix stats: {} succeeded, {} failed", awtrixSuccessCount.get(), awtrixFailureCount.get());
    }

    @PostConstruct
    public void init() {
        if (latitude < -90 || latitude > 90) {
            logger.warn("Invalid latitude {} – must be between -90 and 90. Falling back to {} (Greenwich).", latitude, DEFAULT_LATITUDE);
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
                logger.warn("Awtrix at {} responded with unexpected status {} – pushes may fail", baseHostname, status);
            }
        } catch (Exception e) {
            logger.warn("Awtrix device not reachable at {} – pushes will fail until it comes online ({})", baseHostname, e.getMessage());
        }
    }

    @Scheduled(cron = "${app.cron:${CRON_SCHEDULE:0 1 0,12 * * *}}")
    public void update() {
        logger.info("=== Scheduled task starting ===");
        LocalDateTime now = LocalDateTime.now();

        logger.info("Date: {} | Day: {} | Time: {}", now.format(DATE_FMT), now.format(DAY_FMT), now.format(TIME_FMT));
        logger.info("1 astronomical unit = {}", AU_IN_MILES);

        getSunEarth();
        getEarthMars();
        getEarthJupiter();
        getEarthSaturn();
        getDayLength();
        getVoyagerDistance();
        getNewHorizonsDistance();
        getEquinox();
        getPerihelionAphelion();
        getEarthSpeed();
        getMoonDistance();
        getLightTravelTimes();
        getMoonPhase();

        logger.info("Awtrix update summary: {} succeeded, {} failed", awtrixSuccessCount.get(), awtrixFailureCount.get());
        logger.info("=== Scheduled task completed ===");

        // Publish snapshot to SSE subscribers (no-op in headless mode)
        dataService.ifPresent(svc -> {
            MoonPhase mp = snapshotMoonPhase;
            AstronomicalSnapshot snapshot = new AstronomicalSnapshot(
                    mp.getPhaseName(),
                    mp.getIlluminationPercent(),
                    mp.getPhaseIcon(),
                    mp.getAscii(),
                    mp.getAgeDays(),
                    mp.getDaysUntilFullMoon(),
                    snapshotSunDistanceAu,
                    snapshotMarsDistanceAu,
                    snapshotJupiterDistanceAu,
                    snapshotSaturnDistanceAu,
                    snapshotMoonDistanceKm,
                    snapshotVoyager1DistanceAu,
                    snapshotVoyager2DistanceAu,
                    snapshotNewHorizonsDistanceAu,
                    snapshotEarthSpeedKmPerSec,
                    snapshotEarthSpeedKmPerHour,
                    snapshotDaylightHours,
                    snapshotLightTimeSunToEarth,
                    snapshotLightTimeEarthToMars,
                    snapshotLightTimeEarthToJupiter,
                    snapshotLightTimeEarthToSaturn,
                    snapshotLightTimeEarthToVoyager1,
                    snapshotLightTimeEarthToVoyager2,
                    snapshotDaysUntilSummerSolstice,
                    snapshotDaysUntilWinterSolstice,
                    snapshotDaysUntilPerihelion,
                    snapshotDaysUntilAphelion,
                    now.format(ISO_FMT)
            );
            svc.publishSnapshot(snapshot);
        });
    }

    private void getSunEarth() {
        // Compute approximate min/max over the next year (daily sampling)
        double[] range = SunDistance.minMaxDistanceAUNow();
        double min = range[0];
        double max = range[1];

        // Get Sun-Earth distance and print to console
        double sunDistanceAu = SunDistance.distanceAUNow();
        snapshotSunDistanceAu = sunDistanceAu;
        logger.info("Current Earth-Sun distance: {} AU", String.format("%.6f", sunDistanceAu));

        // Print relative ASCII bar
        String bar = buildRelativeBar(sunDistanceAu, min, max, BAR_WIDTH);
        logger.info("{}", bar);
        logger.info("Sun Earth:{}        {}        {}", String.format("%.6f", min), String.format("%.6f", sunDistanceAu), String.format("%.6f", max));
    }

    private void getMoonPhase() {
        // Moon phase
        MoonPhase mp = MoonPhase.fromDate(LocalDate.now());
        logger.info("Current moon phase is {} ({} days, {}% illuminated).", mp.getPhaseName(), mp.getAgeDays(), mp.getIlluminationPercent());
        Arrays.asList(mp.getAscii()).forEach(row -> logger.info("{}", row));

        // Double-check with Claude API
        ClaudeMoonPhaseVerifier.VerificationResult verification =
                ClaudeMoonPhaseVerifier.verify(LocalDate.now(), mp.getPhaseName());
        if ("N/A".equals(verification.claudePhase())) {
            logger.info("Claude verification skipped (API key not set)");
        } else if (verification.matches()) {
            logger.info("✓ Claude AI confirms moon phase: {}", verification.claudePhase());
        } else {
            logger.warn("✗ Claude AI disagrees! Calculated: {} | Claude says: {} ({})",
                    verification.calculatedPhase(), verification.claudePhase(), verification.details());
            // Correct the moon phase using Claude's answer
            var corrected = MoonPhase.fromPhaseName(verification.claudePhase());
            if (corrected.isPresent()) {
                mp = corrected.get();
                logger.info("Moon phase corrected to: {} (from Claude AI)", mp.getPhaseName());
                Arrays.asList(mp.getAscii()).forEach(row -> logger.info("{}", row));
            } else {
                logger.warn("Could not correct moon phase – unrecognised phase name from Claude: {}", verification.claudePhase());
            }
        }

        int daysUntilFull = mp.getDaysUntilFullMoon();
        logger.info("Days until next full moon: {}", daysUntilFull);

        snapshotMoonPhase = mp;

        sendAwtrix("moonphase", mp.getPhaseName(), mp.getPhaseIcon());
        sendAwtrix("moonillumination", mp.getIlluminationPercent() + "%", mp.getPhaseIcon());
        sendAwtrix("fullmoon", daysUntilFull + "d", "FullMoon");
    }

    private void getEquinox() {
        // EquinoxCalculator
        String nextSummer = EquinoxCalculator.nextSummerSolstice().format(SHORT_DATE_FMT);
        String nextWinter = EquinoxCalculator.nextWinterSolstice().format(SHORT_DATE_FMT);

        snapshotDaysUntilSummerSolstice = EquinoxCalculator.daysUntilSummerSolstice();
        snapshotDaysUntilWinterSolstice = EquinoxCalculator.daysUntilWinterSolstice();

        logger.info("Next summer solstice: {}", nextSummer);
        sendAwtrix("summersolstice", snapshotDaysUntilSummerSolstice + "d", APIPost.IconType.SUMMER.toString());

        logger.info("Next winter solstice: {}", nextWinter);
        sendAwtrix("wintersolstice", snapshotDaysUntilWinterSolstice + "d", APIPost.IconType.WINTER.toString());
    }

    private void getEarthMars() {
        double[] marsRange = MarsDistance.minMaxDistanceAUNow();
        double marsMin = marsRange[0];
        double marsMax = marsRange[1];

        // --- Earth-Mars distance and ASCII bar ---
        double marsDistanceAu = MarsDistance.distanceAUNow();
        snapshotMarsDistanceAu = marsDistanceAu;
        logger.info("Current Earth-Mars distance: {} AU", String.format("%.6f", marsDistanceAu));

        // Print Mars relative ASCII bar
        String marsBar = buildRelativeBar(marsDistanceAu, marsMin, marsMax, BAR_WIDTH);
        logger.info("{}", marsBar);
        logger.info("{}        {}        {}", String.format("%.6f", marsMin), String.format("%.6f", marsDistanceAu), String.format("%.6f", marsMax));

        sendAwtrix("marsDistanceAu", String.format("%.1fau", marsDistanceAu), APIPost.IconType.MARS.toString());
    }

    private void getVoyagerDistance() {
        // Voyager distances (simple approximations)
        double v1FromEarthAu = VoyagerDistance.distanceFromEarthV1AUNow();
        double v2FromEarthAu = VoyagerDistance.distanceFromEarthV2AUNow();
        snapshotVoyager1DistanceAu = v1FromEarthAu;
        snapshotVoyager2DistanceAu = v2FromEarthAu;

        logger.info("Voyager 1 distance from Earth: {} AU", String.format("%.6f", v1FromEarthAu));
        logger.info("Voyager 2 distance from Earth: {} AU", String.format("%.6f", v2FromEarthAu));

        sendAwtrix("voyager1", String.format("V1:%.0fau", v1FromEarthAu), APIPost.IconType.VOYAGER.toString());
        sendAwtrix("voyager2", String.format("V2:%.0fau", v2FromEarthAu), APIPost.IconType.VOYAGER.toString());
    }

    private void getDayLength() {
        // --- Daylight length (hours) ---
        double[] dayRange = DayLight.minMaxDayLengthAUNow(latitude);
        double dayMin = dayRange[0];
        double dayMax = dayRange[1];
        double currentDayHours = DayLight.dayLengthHours(LocalDate.now(), latitude);
        snapshotDaylightHours = currentDayHours;

        logger.info("Daylight length (hours) at latitude {}", latitude);
        String dayBar = buildRelativeBar(currentDayHours, dayMin, dayMax, BAR_WIDTH);
        logger.info("{}", dayBar);
        logger.info("{}        {}        {}", String.format("%.2f", dayMin), String.format("%.2f", currentDayHours), String.format("%.2f", dayMax));

        sendAwtrix("CurrentDayLength", String.format("%.1fhrs", currentDayHours), APIPost.IconType.DAYLENGTH.toString());
    }

    private void getEarthJupiter() {
        double[] jupiterRange = JupiterDistance.minMaxDistanceAUNow();
        double jupiterMin = jupiterRange[0];
        double jupiterMax = jupiterRange[1];

        double jupiterDistanceAu = JupiterDistance.distanceAUNow();
        snapshotJupiterDistanceAu = jupiterDistanceAu;
        logger.info("Current Earth-Jupiter distance: {} AU", String.format("%.6f", jupiterDistanceAu));

        String jupiterBar = buildRelativeBar(jupiterDistanceAu, jupiterMin, jupiterMax, BAR_WIDTH);
        logger.info("{}", jupiterBar);
        logger.info("{}        {}        {}", String.format("%.6f", jupiterMin), String.format("%.6f", jupiterDistanceAu), String.format("%.6f", jupiterMax));

        sendAwtrix("jupiterDistanceAu", String.format("%.1fau", jupiterDistanceAu), APIPost.IconType.JUPITER.toString());
    }

    private void getEarthSaturn() {
        double[] saturnRange = SaturnDistance.minMaxDistanceAUNow();
        double saturnMin = saturnRange[0];
        double saturnMax = saturnRange[1];

        double saturnDistanceAu = SaturnDistance.distanceAUNow();
        snapshotSaturnDistanceAu = saturnDistanceAu;
        logger.info("Current Earth-Saturn distance: {} AU", String.format("%.6f", saturnDistanceAu));

        String saturnBar = buildRelativeBar(saturnDistanceAu, saturnMin, saturnMax, BAR_WIDTH);
        logger.info("{}", saturnBar);
        logger.info("{}        {}        {}", String.format("%.6f", saturnMin), String.format("%.6f", saturnDistanceAu), String.format("%.6f", saturnMax));

        sendAwtrix("saturnDistanceAu", String.format("%.1fau", saturnDistanceAu), APIPost.IconType.SATURN.toString());
    }

    private void getNewHorizonsDistance() {
        double nhFromEarthAu = NewHorizonsDistance.distanceFromEarthAUNow();
        snapshotNewHorizonsDistanceAu = nhFromEarthAu;
        logger.info("New Horizons distance from Earth: {} AU ({} km/s)", String.format("%.6f", nhFromEarthAu), NewHorizonsDistance.speedKmPerSec());

        sendAwtrix("newhorizons", String.format("NH:%.0fau", nhFromEarthAu), APIPost.IconType.NEWHORIZONS.toString());
    }

    private void getPerihelionAphelion() {
        long daysToPerihelion = PerihelionAphelion.daysUntilPerihelion();
        long daysToAphelion = PerihelionAphelion.daysUntilAphelion();
        snapshotDaysUntilPerihelion = daysToPerihelion;
        snapshotDaysUntilAphelion = daysToAphelion;

        logger.info("Days until next perihelion (closest to Sun): {}", daysToPerihelion);
        logger.info("Days until next aphelion (farthest from Sun): {}", daysToAphelion);

        sendAwtrix("perihelion", daysToPerihelion + "d", APIPost.IconType.PERIHELION.toString());
        sendAwtrix("aphelion", daysToAphelion + "d", APIPost.IconType.PERIHELION.toString());
    }

    private void getEarthSpeed() {
        double speedKmS = EarthSpeed.speedKmPerSecNow();
        double speedKmH = EarthSpeed.speedKmPerHourNow();
        snapshotEarthSpeedKmPerSec = speedKmS;
        snapshotEarthSpeedKmPerHour = speedKmH;

        logger.info("Earth's orbital speed: {} km/s ({} km/h)",
                String.format("%.2f", speedKmS), String.format("%,.0f", speedKmH));

        sendAwtrix("earthSpeed", String.format("%.1fkm/s", speedKmS), APIPost.IconType.EARTH.toString());
    }

    private void getMoonDistance() {
        double moonDistKm = MoonDistance.distanceKmNow();
        double[] moonRange = MoonDistance.minMaxDistanceKmNow();
        snapshotMoonDistanceKm = moonDistKm;

        logger.info("Current Moon distance: {}", MoonDistance.formatDistanceKm(moonDistKm));
        String moonBar = buildRelativeBar(moonDistKm, moonRange[0], moonRange[1], BAR_WIDTH);
        logger.info("{}", moonBar);
        logger.info("{}        {}        {}", MoonDistance.formatDistanceKm(moonRange[0]),
                MoonDistance.formatDistanceKm(moonDistKm), MoonDistance.formatDistanceKm(moonRange[1]));

        sendAwtrix("moonDistance", String.format("%,.0fkm", moonDistKm), APIPost.IconType.MOON.toString());
    }

    private void getLightTravelTimes() {
        snapshotLightTimeSunToEarth = LightTravelTime.sunToEarthNow();
        snapshotLightTimeEarthToMars = LightTravelTime.earthToMarsNow();
        snapshotLightTimeEarthToJupiter = LightTravelTime.earthToJupiterNow();
        snapshotLightTimeEarthToSaturn = LightTravelTime.earthToSaturnNow();
        snapshotLightTimeEarthToVoyager1 = LightTravelTime.earthToVoyager1Now();
        snapshotLightTimeEarthToVoyager2 = LightTravelTime.earthToVoyager2Now();

        logger.info("--- Light Travel Times ---");
        logger.info("Sun → Earth:    {}", snapshotLightTimeSunToEarth);
        logger.info("Earth → Mars:   {}", snapshotLightTimeEarthToMars);
        logger.info("Earth → Jupiter: {}", snapshotLightTimeEarthToJupiter);
        logger.info("Earth → Saturn: {}", snapshotLightTimeEarthToSaturn);
        logger.info("Earth → Voyager 1: {}", snapshotLightTimeEarthToVoyager1);
        logger.info("Earth → Voyager 2: {}", snapshotLightTimeEarthToVoyager2);

        sendAwtrix("lightMars", "Lt:" + snapshotLightTimeEarthToMars, APIPost.IconType.LIGHT.toString());
        sendAwtrix("lightJupiter", "Lt:" + snapshotLightTimeEarthToJupiter, APIPost.IconType.LIGHT.toString());
    }

    private static final int AWTRIX_MAX_ATTEMPTS = 3;
    private static final long AWTRIX_RETRY_DELAY_MS = 2000;

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
                    logger.warn("Awtrix send failed ({}) attempt {}/{} – retrying in {}ms: {}",
                            appName, attempt, AWTRIX_MAX_ATTEMPTS, AWTRIX_RETRY_DELAY_MS, e.getMessage());
                    try {
                        Thread.sleep(AWTRIX_RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        awtrixFailureCount.incrementAndGet();
                        return;
                    }
                } else {
                    logger.warn("Awtrix send failed ({}) after {} attempts: {}", appName, AWTRIX_MAX_ATTEMPTS, e.getMessage());
                    awtrixFailureCount.incrementAndGet();
                }
            }
        }
    }

    private String buildRelativeBar(double current, double min, double max, int innerWidth) {
        if (innerWidth < 1) innerWidth = 1;
        StringBuilder sb = new StringBuilder();
        sb.append("Min |");

        // Handle degenerate case
        int pos;
        if (!Double.isFinite(min) || !Double.isFinite(max) || max <= min) {
            logger.warn("buildRelativeBar: invalid range min={} max={} – defaulting to centre", min, max);
            pos = innerWidth / 2;
        } else {
            double frac = (current - min) / (max - min);
            if (Double.isNaN(frac)) frac = 0.5;
            frac = Math.max(0.0, Math.min(1.0, frac));
            pos = (int) Math.round(frac * (innerWidth - 1));
        }

        IntStream.range(0, innerWidth).forEach(i -> sb.append(i == pos ? '0' : '-'));
        sb.append("| Max");
        return sb.toString();
    }
}
