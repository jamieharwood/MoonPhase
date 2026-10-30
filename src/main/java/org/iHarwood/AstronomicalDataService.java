package org.iHarwood;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
/**
 * Holds the latest astronomical snapshot and manages SSE subscribers.
 * Only instantiated when running as a servlet web application.
 */
@Service
@ConditionalOnWebApplication
public class AstronomicalDataService {
    private static final Logger logger = LoggerFactory.getLogger(AstronomicalDataService.class);
    private volatile AstronomicalSnapshot latestSnapshot = null;
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final JsonMapper jsonMapper;
    private final Optional<HistoryService> historyService;
    public AstronomicalDataService(JsonMapper jsonMapper, Optional<HistoryService> historyService) {
        this.jsonMapper = jsonMapper;
        this.historyService = historyService;
    }
    public void publishSnapshot(AstronomicalSnapshot snapshot) {
        this.latestSnapshot = snapshot;
        historyService.ifPresent(svc -> svc.save(snapshot));
        broadcastToEmitters(snapshot);
    }
    public AstronomicalSnapshot getLatestSnapshot() {
        return latestSnapshot;
    }
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L); // no timeout; client/proxy manage lifecycle
        emitters.add(emitter);
        Runnable cleanup = () -> emitters.remove(emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());
        // Send current snapshot immediately on connect so the browser has data
        AstronomicalSnapshot current = latestSnapshot;
        if (current != null) {
            try {
                sendUpdate(emitter, current);
            } catch (IOException e) {
                logger.warn("Failed to send initial snapshot to new subscriber: {}", e.getMessage());
                emitters.remove(emitter);
            }
        }
        return emitter;
    }
    private void broadcastToEmitters(AstronomicalSnapshot snapshot) {
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                sendUpdate(emitter, snapshot);
            } catch (IOException e) {
                dead.add(emitter);
                logger.debug("SSE emitter removed (send failed): {}", e.getMessage());
            } catch (JacksonException e) {
                logger.error("Failed to serialise snapshot for SSE broadcast: {}", e.getMessage());
                return;
            }
        }
        emitters.removeAll(dead);
        logger.info("SSE broadcast complete: {} active subscriber(s), {} removed", emitters.size(), dead.size());
    }
    private void sendUpdate(SseEmitter emitter, AstronomicalSnapshot snapshot) throws IOException {
        // Prefer Spring's HttpMessageConverter path (uses Boot-configured JsonMapper).
        // Fall back to pre-serialised JSON only if needed for clients that expect a string body.
        try {
            emitter.send(SseEmitter.event()
                    .name("update")
                    .data(snapshot, MediaType.APPLICATION_JSON));
        } catch (IllegalStateException ex) {
            // Converter path unavailable — write JSON string explicitly
            String json = jsonMapper.writeValueAsString(snapshot);
            emitter.send(SseEmitter.event().name("update").data(json));
        }
    }
}
