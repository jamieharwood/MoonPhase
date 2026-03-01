package org.iHarwood;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void publishSnapshot(AstronomicalSnapshot snapshot) {
        this.latestSnapshot = snapshot;
        broadcastToEmitters(snapshot);
    }

    public AstronomicalSnapshot getLatestSnapshot() {
        return latestSnapshot;
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        Runnable cleanup = () -> emitters.remove(emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        // Send current snapshot immediately on connect so the browser has data
        AstronomicalSnapshot current = latestSnapshot;
        if (current != null) {
            try {
                String json = objectMapper.writeValueAsString(current);
                emitter.send(SseEmitter.event().name("update").data(json));
            } catch (IOException e) {
                logger.warn("Failed to send initial snapshot to new subscriber: {}", e.getMessage());
            }
        }

        return emitter;
    }

    private void broadcastToEmitters(AstronomicalSnapshot snapshot) {
        String json;
        try {
            json = objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialise snapshot for SSE broadcast: {}", e.getMessage());
            return;
        }

        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("update").data(json));
            } catch (IOException e) {
                dead.add(emitter);
                logger.debug("SSE emitter removed (send failed): {}", e.getMessage());
            }
        }
        emitters.removeAll(dead);
        logger.info("SSE broadcast complete: {} active subscriber(s), {} removed", emitters.size(), dead.size());
    }
}
