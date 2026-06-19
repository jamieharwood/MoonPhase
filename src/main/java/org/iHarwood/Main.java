package org.iHarwood;

import jakarta.annotation.PostConstruct;
import org.iHarwood.calculation.CalculationOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Optional;

@Component
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private final CalculationOrchestrator orchestrator;
    private final Optional<AstronomicalDataService> dataService;
    private final Object updateLock = new Object();

    public Main(CalculationOrchestrator orchestrator, Optional<AstronomicalDataService> dataService) {
        this.orchestrator = orchestrator;
        this.dataService = dataService;
    }

    @PostConstruct
    public void init() {
        logger.info("Application started - running initial update");
        update();
    }

    @Scheduled(cron = "${app.cron:${CRON_SCHEDULE:0 1 0,12 * * *}}")
    public void update() {
        synchronized (updateLock) {
            AstronomicalSnapshot snapshot = orchestrator.computeCurrent();
            dataService.ifPresent(svc -> svc.publishSnapshot(snapshot));
        }
    }

    public AstronomicalSnapshot calculateSnapshotForDate(ZonedDateTime target) {
        return orchestrator.computeForDate(target);
    }
}
