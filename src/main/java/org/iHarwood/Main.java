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
import java.util.stream.IntStream;

@Component
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    // Static formatters to avoid recreation on each run
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("EEEE");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter SHORT_DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yy");
    private final String HOSTNAME_ENV_VAR = "AWTRIXHOSTNAME";
    private final String DEFAULT_HOSTNAME = "http://moonclock.local";
    private final String baseHostname = System.getenv().getOrDefault(HOSTNAME_ENV_VAR, DEFAULT_HOSTNAME);
    private final String hostname = baseHostname.concat("/api/custom?name=");

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${app.latitude:${LATITUDE:51.4769}}")
    private double latitude;

    // Awtrix error rate tracking
    private int awtrixSuccessCount = 0;
    private int awtrixFailureCount = 0;

    @PreDestroy
    public void shutdown() {
        logger.info("Application shutting down – Awtrix stats: {} succeeded, {} failed", awtrixSuccessCount, awtrixFailureCount);
    }

    @PostConstruct
    public void init() {
        if (latitude < -90 || latitude > 90) {
            logger.warn("Invalid latitude {} – must be between -90 and 90. Falling back to 51.4769 (Greenwich).", latitude);
            latitude = 51.4769;
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
            logger.info("Awtrix reachable at {} (HTTP {})", baseHostname, status);
        } catch (Exception e) {
            logger.warn("Awtrix device not reachable at {} – pushes will fail until it comes online ({})", baseHostname, e.getMessage());
        }
    }

    @Scheduled(cron = "${app.cron:${CRON_SCHEDULE:0 1 0,12 * * *}}")
    public void update() {
        logger.info("=== Scheduled task starting ===");
        int barWidth = 30;

        LocalDateTime now = LocalDateTime.now();

        logger.info("Date: {} | Day: {} | Time: {}", now.format(DATE_FMT), now.format(DAY_FMT), now.format(TIME_FMT));
        logger.info("1 astronomical units = 92,955,807.273026 miles");

        getSunEarth(barWidth);
        getEarthMars(barWidth);
        getDayLength(barWidth);
        getVoyagerDistance();
        getEquinox();
        getMoonPhase();

        logger.info("Awtrix update summary: {} succeeded, {} failed", awtrixSuccessCount, awtrixFailureCount);
        logger.info("=== Scheduled task completed ===");
    }

    private void getSunEarth(int barWidth) {
        // Compute approximate min/max over the next year (daily sampling)
        double[] range = SunDistance.minMaxDistanceAUNow();
        double min = range[0];
        double max = range[1];

        // Get Sun-Earth distance and print to console
        double sunDistanceAu = SunDistance.distanceAUNow();
        logger.info("Current Earth-Sun distance: {} AU", String.format("%.6f", sunDistanceAu));

        // Print relative ASCII bar
        String bar = buildRelativeBar(sunDistanceAu, min, max, barWidth);
        logger.info("{}", bar);
        logger.info("{}        {}        {}", String.format("%.6f", min), String.format("%.6f", sunDistanceAu), String.format("%.6f", max));
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
            MoonPhase corrected = MoonPhase.fromPhaseName(verification.claudePhase());
            if (corrected != null) {
                mp = corrected;
                logger.info("Moon phase corrected to: {} (from Claude AI)", mp.getPhaseName());
                Arrays.asList(mp.getAscii()).forEach(row -> logger.info("{}", row));
            } else {
                logger.warn("Could not correct moon phase – unrecognised phase name from Claude: {}", verification.claudePhase());
            }
        }

        int daysUntilFull = mp.getDaysUntilFullMoon();
        logger.info("Days until next full moon: {}", daysUntilFull);

        sendAwtrix("moonphase", mp.getPhaseName(), mp.getPhaseIcon());
        sendAwtrix("moonillumination", mp.getIlluminationPercent() + "%", mp.getPhaseIcon());
        sendAwtrix("fullmoon", daysUntilFull + "d", "FullMoon");
    }

    private void getEquinox() {
        // EquinoxCalculator
        String nextSummer = EquinoxCalculator.nextSummerSolstice().format(SHORT_DATE_FMT);
        String nextWinter = EquinoxCalculator.nextWinterSolstice().format(SHORT_DATE_FMT);

        logger.info("Next summer solstice: {}", nextSummer);
        sendAwtrix("summersolstice", EquinoxCalculator.daysUntilSummerSolstice() + "d", APIPost.IconType.SUMMER.toString());

        logger.info("Next winter solstice: {}", nextWinter);
        sendAwtrix("wintersolstice", EquinoxCalculator.daysUntilWinterSolstice() + "d", APIPost.IconType.WINTER.toString());
    }

    private void getEarthMars(int barWidth) {
        double[] marsRange = MarsDistance.minMaxDistanceAUNow();
        double marsMin = marsRange[0];
        double marsMax = marsRange[1];

        // --- Earth-Mars distance and ASCII bar ---
        double marsDistanceAu = MarsDistance.distanceAUNow();
        logger.info("Current Earth-Mars distance: {} AU", String.format("%.6f", marsDistanceAu));

        // Print Mars relative ASCII bar
        String marsBar = buildRelativeBar(marsDistanceAu, marsMin, marsMax, barWidth);
        logger.info("{}", marsBar);
        logger.info("{}        {}        {}", String.format("%.6f", marsMin), String.format("%.6f", marsDistanceAu), String.format("%.6f", marsMax));

        sendAwtrix("marsDistanceAu", String.format("%.1f", marsDistanceAu) + "au", APIPost.IconType.MARS.toString());
    }

    private void getVoyagerDistance() {
        // Voyager distances (simple approximations)
        double v1FromEarthAu = VoyagerDistance.distanceFromEarthV1AUNow();
        double v2FromEarthAu = VoyagerDistance.distanceFromEarthV2AUNow();

        logger.info("Voyager 1 distance from Earth: {} AU", String.format("%.6f", v1FromEarthAu));
        logger.info("Voyager 2 distance from Earth: {} AU", String.format("%.6f", v2FromEarthAu));

        sendAwtrix("voyager1", String.format("V1:%.0f", v1FromEarthAu) + "au", APIPost.IconType.VOYAGER.toString());
        sendAwtrix("voyager2", String.format("V2:%.0f", v2FromEarthAu) + "au", APIPost.IconType.VOYAGER.toString());
    }

    private void getDayLength(int barWidth) {
        // --- Daylight length (hours) ---
        double[] dayRange = DayLight.minMaxDayLengthAUNow(latitude);
        double dayMin = dayRange[0];
        double dayMax = dayRange[1];
        double currentDayHours = DayLight.dayLengthHours(LocalDate.now(), latitude);

        //System.out.println();
        logger.info("Daylight length (hours) at latitude {}", latitude);
        String dayBar = buildRelativeBar(currentDayHours, dayMin, dayMax, barWidth);
        logger.info("{}", dayBar);
        logger.info("{}        {}        {}", String.format("%.2f", dayMin), String.format("%.2f", currentDayHours), String.format("%.2f", dayMax));

        sendAwtrix("CurrentDayLength", String.format("%.1f", currentDayHours) + "hrs", APIPost.IconType.DAYLENGTH.toString());
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
                awtrixSuccessCount++;
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Awtrix send interrupted ({})", appName);
                awtrixFailureCount++;
                return;
            } catch (IOException e) {
                if (attempt < AWTRIX_MAX_ATTEMPTS) {
                    logger.warn("Awtrix send failed ({}) attempt {}/{} – retrying in {}ms: {}",
                            appName, attempt, AWTRIX_MAX_ATTEMPTS, AWTRIX_RETRY_DELAY_MS, e.getMessage());
                    try {
                        Thread.sleep(AWTRIX_RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        awtrixFailureCount++;
                        return;
                    }
                } else {
                    logger.warn("Awtrix send failed ({}) after {} attempts: {}", appName, AWTRIX_MAX_ATTEMPTS, e.getMessage());
                    awtrixFailureCount++;
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
