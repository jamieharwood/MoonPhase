package org.iHarwood.integration.awtrix;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.iHarwood.APIPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dedicated component responsible for all communication with an Awtrix device.
 *
 * Responsibilities:
 * - Connectivity check at startup
 * - Pushing named apps with retry logic
 * - Tracking success/failure statistics
 * - Graceful shutdown logging
 *
 * This extraction removes a large amount of Awtrix-specific code from Main.java,
 * making the scheduler/orchestrator much easier to understand and test.
 */
@Component
public class AwtrixPusher {

    private static final Logger logger = LoggerFactory.getLogger(AwtrixPusher.class);

    private static final String HOSTNAME_ENV_VAR = "AWTRIXHOSTNAME";
    private static final String DEFAULT_HOSTNAME = "http://moonclock.local";
    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 2000;

    private final String baseHostname;
    private final String apiPrefix;

    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public AwtrixPusher(
            @Value("${awtrix.hostname:${AWTRIXHOSTNAME:" + DEFAULT_HOSTNAME + "}}") String configuredHostname) {

        this.baseHostname = configuredHostname != null ? configuredHostname : DEFAULT_HOSTNAME;
        this.apiPrefix = this.baseHostname + "/api/custom?name=";
    }

    @PostConstruct
    public void init() {
        checkConnectivity();
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Application shutting down - Awtrix stats: {} succeeded, {} failed",
                successCount.get(), failureCount.get());
    }

    /**
     * Checks basic connectivity to the Awtrix device at startup.
     */
    public void checkConnectivity() {
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
     * Sends a single app update to the Awtrix device with retry logic.
     *
     * @param appName internal Awtrix app name (e.g. "moonphase", "marsDistanceAu")
     * @param text    text to display
     * @param icon    icon name (see APIPost.IconType or custom)
     */
    public void push(String appName, String text, String icon) {
        String url = apiPrefix + appName;
        logger.info("Awtrix host API call:{}", url);

        APIPost apiPost = new APIPost(appName, text, url, "1", "", icon);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                int responseCode = apiPost.sendPost();
                logger.info("Awtrix response ({}): {}", appName, responseCode);
                successCount.incrementAndGet();
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Awtrix send interrupted ({})", appName);
                failureCount.incrementAndGet();
                return;
            } catch (IOException e) {
                if (attempt < MAX_ATTEMPTS) {
                    logger.warn("Awtrix send failed ({}) attempt {}/{} - retrying in {}ms: {}",
                            appName, attempt, MAX_ATTEMPTS, RETRY_DELAY_MS, e.getMessage());
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        failureCount.incrementAndGet();
                        return;
                    }
                } else {
                    logger.warn("Awtrix send failed ({}) after {} attempts: {}",
                            appName, MAX_ATTEMPTS, e.getMessage());
                    failureCount.incrementAndGet();
                }
            }
        }
    }

    // --- Statistics accessors (useful for testing and future metrics) ---

    public int getSuccessCount() {
        return successCount.get();
    }

    public int getFailureCount() {
        return failureCount.get();
    }

    /**
     * Lightweight stats record for potential future exposure via Actuator or logging.
     */
    public record AwtrixStats(int success, int failure) {}

    public AwtrixStats getStats() {
        return new AwtrixStats(getSuccessCount(), getFailureCount());
    }
}
