package com.smartdx.system.message.registry;

import com.smartdx.system.message.dto.OnlineUserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * SSE セッションレジストリ
 * <p>
 * SSE接続のユーザーセッション情報を管理。マルチデバイス同時ログインをサポート。
 */
@Slf4j
@Component
public class SseSessionRegistry {

    /** ユーザー名 -> SseEmitter 集合（マルチデバイス対応） */
    private final Map<String, Set<SseEmitter>> userEmittersMap = new ConcurrentHashMap<>();

    /** SseEmitter -> ユーザー名（高速ルックアップ用） */
    private final Map<SseEmitter, String> emitterUserMap = new ConcurrentHashMap<>();

    /** SseEmitter -> 接続時刻 */
    private final Map<SseEmitter, Long> emitterTimeMap = new ConcurrentHashMap<>();

    /**
     * ユーザー接続（SSE接続確立）
     */
    public void userConnected(String username, SseEmitter emitter) {
        userEmittersMap.computeIfAbsent(username, k -> ConcurrentHashMap.newKeySet()).add(emitter);
        emitterUserMap.put(emitter, username);
        emitterTimeMap.put(emitter, System.currentTimeMillis());
        log.debug("ユーザー[{}]SSE接続確立", username);

        emitter.onCompletion(() -> {
            removeEmitter(emitter);
            log.debug("ユーザー[{}]SSE接続完了", username);
        });
        emitter.onTimeout(() -> {
            removeEmitter(emitter);
            log.debug("ユーザー[{}]SSE接続タイムアウト", username);
        });
        emitter.onError(e -> {
            removeEmitter(emitter);
            log.debug("ユーザー[{}]SSE接続エラー: {}", username, e.getMessage());
        });
    }

    private void removeEmitter(SseEmitter emitter) {
        String username = emitterUserMap.remove(emitter);
        if (username == null) {
            return;
        }
        emitterTimeMap.remove(emitter);

        Set<SseEmitter> emitters = userEmittersMap.get(username);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userEmittersMap.remove(username);
                log.debug("ユーザー[{}]の全SSE接続が切断", username);
            }
        }
    }

    /**
     * ユーザー切断（全SSE接続を切断）
     */
    public void userDisconnected(String username) {
        Set<SseEmitter> emitters = userEmittersMap.remove(username);
        if (emitters == null) {
            return;
        }
        emitters.forEach(emitter -> {
            emitterUserMap.remove(emitter);
            emitterTimeMap.remove(emitter);
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        });
        log.debug("ユーザー[{}]がオフライン、{}個のSSE接続を削除", username, emitters.size());
    }

    public int getOnlineUserCount() {
        return userEmittersMap.size();
    }

    public int getTotalConnectionCount() {
        return emitterUserMap.size();
    }

    public int getUserConnectionCount(String username) {
        Set<SseEmitter> emitters = userEmittersMap.get(username);
        return emitters != null ? emitters.size() : 0;
    }

    public boolean isUserOnline(String username) {
        Set<SseEmitter> emitters = userEmittersMap.get(username);
        return emitters != null && !emitters.isEmpty();
    }

    public List<OnlineUserDTO> getOnlineUsers() {
        return userEmittersMap.entrySet().stream()
                .map(entry -> {
                    String username = entry.getKey();
                    Set<SseEmitter> emitters = entry.getValue();
                    long earliestTime = emitters.stream()
                            .map(emitterTimeMap::get)
                            .filter(t -> t != null)
                            .mapToLong(Long::longValue)
                            .min()
                            .orElse(System.currentTimeMillis());
                    return new OnlineUserDTO(username, emitters.size(), earliestTime);
                })
                .collect(Collectors.toList());
    }

    public Set<SseEmitter> getAllEmitters() {
        return emitterUserMap.keySet();
    }

    public Set<SseEmitter> getUserEmitters(String username) {
        return userEmittersMap.get(username);
    }

    public boolean sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
            return true;
        } catch (IOException e) {
            log.warn("SSEイベント送信失敗: {}", e.getMessage());
            removeEmitter(emitter);
            return false;
        }
    }

    public void broadcast(String eventName, Object data) {
        getAllEmitters().forEach(emitter -> sendEvent(emitter, eventName, data));
    }

    public void sendToUser(String username, String eventName, Object data) {
        Set<SseEmitter> emitters = userEmittersMap.get(username);
        if (emitters != null) {
            emitters.forEach(emitter -> sendEvent(emitter, eventName, data));
        }
    }

    /**
     * ハートビート検出：30秒ごとに全接続にpingイベントを送信し、切断済みのゾンビ接続をクリーンアップ
     */
    @Scheduled(fixedRate = 30000)
    public void heartbeat() {
        if (emitterUserMap.isEmpty()) {
            return;
        }
        List<SseEmitter> failedEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitterUserMap.keySet()) {
            try {
                emitter.send(SseEmitter.event().name("ping").data("heartbeat"));
            } catch (Exception e) {
                failedEmitters.add(emitter);
            }
        }
        if (!failedEmitters.isEmpty()) {
            log.debug("ハートビート検出で{}個の無効なSSE接続をクリーンアップ", failedEmitters.size());
            failedEmitters.forEach(this::removeEmitter);
        }
    }

    /**
     * コンテナ終了時に全SSE接続を積極的に切断し、アプリケーション停止のブロックを回避
     */
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EventListener(ContextClosedEvent.class)
    public void destroy() {
        int count = emitterUserMap.size();
        if (count == 0) {
            return;
        }
        log.info("アプリケーション終了、{}個のSSE接続を切断中...", count);
        emitterUserMap.keySet().forEach(emitter -> {
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        });
        userEmittersMap.clear();
        emitterUserMap.clear();
        emitterTimeMap.clear();
        log.info("全SSE接続を切断完了");
    }
}
