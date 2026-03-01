package org.iHarwood;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE;

/**
 * HTTP endpoints for the live astronomical dashboard.
 * Only active when running as a servlet web application.
 */
@Controller
@ConditionalOnWebApplication
public class DashboardController {

    private final AstronomicalDataService dataService;

    public DashboardController(AstronomicalDataService dataService) {
        this.dataService = dataService;
    }

    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }

    @GetMapping(value = "/api/data", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<AstronomicalSnapshot> currentData() {
        AstronomicalSnapshot snapshot = dataService.getLatestSnapshot();
        if (snapshot == null) {
            return ResponseEntity.status(503).build();
        }
        return ResponseEntity.ok(snapshot);
    }

    @GetMapping(value = "/api/events", produces = TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public SseEmitter subscribe() {
        return dataService.subscribe();
    }
}
