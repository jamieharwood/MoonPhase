package org.iHarwood.integration.awtrix;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.iHarwood.APIPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.iHarwood.config.AwtrixProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
/**
 * Dedicated component responsible for all communication with an Awtrix device.
 *
 * Responsibilities:
 * - Connectivity check at startup
 * - Pushing named apps with retry logic
 * - Batch parallel pushes with fail-fast when the device is unreachable
 * - Tracking success/failure statistics
 * - Graceful shutdown logging
 */
@Component
public class AwtrixPusher {
    private static final Logger logger = LoggerFactory.getLogger(AwtrixPusher.class);
    private static final String DEFAULT_HOSTNAME = "http://moonclock.local";
    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 2000;
    /** Max concurrent HTTP posts to the device. */
    private static final int PUSH_PARALLELISM = 4;
    private final String baseHostname;
    private final String apiPrefix;
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    /** When true, remaining pushes in the current batch are skipped. */
    private final AtomicBoolean circuitOpen = new AtomicBoolean(false);
    private final ExecutorService pushExecutor;
    private final HttpClient httpClient;
    @Autowired
    public AwtrixPusher(AwtrixProperties awtrixProperties, ExecutorService applicationTaskExecutor) {
        String configuredHostname = awtrixProperties.getHostname();
        this.baseHostname = (configuredHostname != null && !configuredHostname.isBlank())
                ? configuredHostname : DEFAULT_HOSTNAME;
        this.apiPrefix = this.baseHostname + "/api/custom?name=";
        this.pushExecutor = applicationTaskExecutor;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .executor(applicationTaskExecutor)
                .build();
    }
    /** Test construction without Spring. */
    AwtrixPusher(String configuredHostname) {
        this.baseHostname = configuredHostname != null ? configuredHostname : DEFAULT_HOSTNAME;
        this.apiPrefix = this.baseHostname + "/api/custom?name=";
        this.pushExecutor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .executor(this.pushExecutor)
                .build();
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
            int status = httpClient.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            if (status >= 200 && status < 300) {
                logger.info("Awtrix reachable at {} (HTTP {})", baseHostname, status);
                circuitOpen.set(false);
            } else {
                logger.warn("Awtrix at {} responded with unexpected status {} - pushes may fail",
                        baseHostname, status);
            }
        } catch (Exception e) {
            logger.warn("Awtrix device not reachable at {} - pushes will fail until it comes online ({})",
                    baseHostname, e.getMessage());
            circuitOpen.set(true);
        }
    }
    public record PushRequest(String appName, String text, String icon) {}
    /**
     * Queues a single push. Prefer {@link #pushAll(List)} when sending many apps.
     */
    public void push(String appName, String text, String icon) {
        pushAll(List.of(new PushRequest(appName, text, icon)));
    }
    /**
     * Sends multiple app updates in parallel (bounded). If the device becomes unreachable,
     * remaining pushes in this batch are skipped (fail-fast circuit).
     */
    public void pushAll(List<PushRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }
        // Fresh cycle: give the device another chance even if a previous cycle failed hard.
        circuitOpen.set(false);
        // Simple chunked parallelism to avoid hammering a tiny ESP32 device.
        List<PushRequest> pending = new ArrayList<>(requests);
        for (int i = 0; i < pending.size(); ) {
            if (circuitOpen.get()) {
                int skipped = pending.size() - i;
                failureCount.addAndGet(skipped);
                logger.warn("Awtrix circuit open — skipping {} remaining push(es)", skipped);
                break;
            }
            int end = Math.min(i + PUSH_PARALLELISM, pending.size());
            List<CompletableFuture<Void>> batch = new ArrayList<>(end - i);
            for (int j = i; j < end; j++) {
                PushRequest req = pending.get(j);
                batch.add(CompletableFuture.runAsync(() -> pushOne(req), pushExecutor));
            }
            CompletableFuture.allOf(batch.toArray(CompletableFuture[]::new)).join();
            i = end;
        }
    }
    private void pushOne(PushRequest req) {
        if (circuitOpen.get()) {
            failureCount.incrementAndGet();
            return;
        }
        String url = apiPrefix + req.appName();
        logger.debug("Awtrix push: {} -> {}", req.appName(), url);
        APIPost apiPost = new APIPost(req.appName(), req.text(), url, "1", "", req.icon());
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            if (circuitOpen.get()) {
                failureCount.incrementAndGet();
                return;
            }
            try {
                int responseCode = apiPost.sendPost();
                logger.debug("Awtrix response ({}): {}", req.appName(), responseCode);
                successCount.incrementAndGet();
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Awtrix send interrupted ({})", req.appName());
                failureCount.incrementAndGet();
                return;
            } catch (IOException e) {
                if (attempt < MAX_ATTEMPTS) {
                    logger.warn("Awtrix send failed ({}) attempt {}/{} - retrying in {}ms: {}",
                            req.appName(), attempt, MAX_ATTEMPTS, RETRY_DELAY_MS, e.getMessage());
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        failureCount.incrementAndGet();
                        return;
                    }
                } else {
                    logger.warn("Awtrix send failed ({}) after {} attempts: {}",
                            req.appName(), MAX_ATTEMPTS, e.getMessage());
                    failureCount.incrementAndGet();
                    // Open circuit so the rest of this batch fails fast.
                    if (circuitOpen.compareAndSet(false, true)) {
                        logger.warn("Awtrix circuit opened after hard failure on {} — remaining pushes will be skipped",
                                req.appName());
                    }
                }
            }
        }
    }
    public int getSuccessCount() {
        return successCount.get();
    }
    public int getFailureCount() {
        return failureCount.get();
    }
    public record AwtrixStats(int success, int failure) {}
    public AwtrixStats getStats() {
        return new AwtrixStats(getSuccessCount(), getFailureCount());
    }
}
