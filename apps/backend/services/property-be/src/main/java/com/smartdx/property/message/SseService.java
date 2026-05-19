package com.smartdx.property.message;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-Sent Events Service
 */
@Slf4j
@Service
public class SseService {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Create SSE connection
     */
    public SseEmitter connect(String clientId) {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.put(clientId, emitter);

        emitter.onCompletion(() -> emitters.remove(clientId));
        emitter.onTimeout(() -> emitters.remove(clientId));
        emitter.onError(e -> emitters.remove(clientId));

        return emitter;
    }

    /**
     * Send message to specific client
     */
    public void sendMessage(String clientId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(clientId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException e) {
                log.error("Failed to send SSE message to client: {}", clientId, e);
                emitters.remove(clientId);
            }
        }
    }

    /**
     * Broadcast message to all clients
     */
    public void broadcast(String eventName, Object data) {
        emitters.forEach((clientId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException e) {
                log.error("Failed to broadcast SSE message to client: {}", clientId, e);
                emitters.remove(clientId);
            }
        });
    }

    /**
     * Close connection
     */
    public void close(String clientId) {
        SseEmitter emitter = emitters.remove(clientId);
        if (emitter != null) {
            emitter.complete();
        }
    }
}
