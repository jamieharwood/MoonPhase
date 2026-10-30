package org.iHarwood;
import org.iHarwood.calculation.CalculationOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
/**
 * Owns the scheduled / manual update loop. Implements {@link ApplicationRunner}
 * so the first compute runs after the full application context is ready
 * (SSE subscribers, Mongo, Actuator, etc.).
 */
@Component
@Order(100)
public class Main implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private final CalculationOrchestrator orchestrator;
    private final Optional<AstronomicalDataService> dataService;
    private final ReentrantLock updateLock = new ReentrantLock();
    public Main(CalculationOrchestrator orchestrator, Optional<AstronomicalDataService> dataService) {
        this.orchestrator = orchestrator;
        this.dataService = dataService;
    }
    @Override
    public void run(ApplicationArguments args) {
        logger.info("Application ready — running initial update");
        update();
    }
    @Scheduled(cron = "${app.cron}")
    public void update() {
        if (!updateLock.tryLock()) {
            logger.info("Update already in progress — skipping overlapping trigger");
            return;
        }
        try {
            // Build snapshot first, publish to dashboard/SSE, then deliver Awtrix/Claude.
            // This keeps the web UI responsive even when the LED device is slow or offline.
            CalculationOrchestrator.LiveComputeResult result = orchestrator.computeCurrentResult();
            dataService.ifPresent(svc -> svc.publishSnapshot(result.snapshot()));
            orchestrator.deliverSideEffects(result);
        } finally {
            updateLock.unlock();
        }
    }
    public AstronomicalSnapshot calculateSnapshotForDate(ZonedDateTime target) {
        return orchestrator.computeForDate(target);
    }
}
