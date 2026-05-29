package com.smartdx.system.message.service;

import com.smartdx.system.message.dto.DictChangeEvent;
import com.smartdx.system.message.dto.OnlineUserDTO;
import com.smartdx.system.message.registry.SseSessionRegistry;
import com.smartdx.system.message.topic.SseTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * SSE サービス
 */
@Slf4j
@Service("systemSseService")
@RequiredArgsConstructor
public class SseService {

    /** SSE 接続タイムアウト時間：30分 */
    private static final long TIMEOUT = 30 * 60 * 1000L;

    private final SseSessionRegistry sessionRegistry;

    /**
     * SSE 接続を作成
     *
     * @param username ユーザー名
     * @return SseEmitter
     */
    public SseEmitter createConnection(String username) {
        if (username == null || username.isEmpty()) {
            log.warn("SSE接続作成失敗：ユーザー名が空");
            return null;
        }

        SseEmitter emitter = new SseEmitter(TIMEOUT);
        sessionRegistry.userConnected(username, emitter);

        try {
            emitter.send(SseEmitter.event()
                    .name(SseTopics.ONLINE_COUNT)
                    .data(sessionRegistry.getOnlineUserCount()));
        } catch (IOException e) {
            log.warn("初期オンラインユーザー数送信失敗: {}", e.getMessage());
        }

        log.info("ユーザー[{}]SSE接続確立、現在のオンラインユーザー数: {}", username, sessionRegistry.getOnlineUserCount());
        sendOnlineCount();

        return emitter;
    }

    /**
     * 辞書変更イベントを送信
     */
    public void sendDictChange(String dictCode) {
        if (dictCode == null || dictCode.isEmpty()) {
            log.warn("辞書コードが空、送信をスキップ");
            return;
        }

        DictChangeEvent event = new DictChangeEvent(dictCode);
        sessionRegistry.broadcast(SseTopics.DICT, event);
        log.info("辞書変更通知を送信: dictCode={}", dictCode);
    }

    /**
     * オンラインユーザー数を送信
     */
    public void sendOnlineCount() {
        int count = sessionRegistry.getOnlineUserCount();
        sessionRegistry.broadcast(SseTopics.ONLINE_COUNT, count);
        log.debug("オンラインユーザー数を送信: {}", count);
    }

    /**
     * 指定ユーザーにメッセージを送信
     */
    public void sendToUser(String username, String eventName, Object data) {
        if (username == null || username.isEmpty()) {
            log.warn("ユーザー名が空、メッセージ送信不可");
            return;
        }
        sessionRegistry.sendToUser(username, eventName, data);
        log.info("ユーザー[{}]にイベント[{}]を送信", username, eventName);
    }

    /**
     * オンラインユーザーリストを取得
     */
    public List<OnlineUserDTO> getOnlineUsers() {
        return sessionRegistry.getOnlineUsers();
    }

    /**
     * オンラインユーザー数を取得
     */
    public int getOnlineUserCount() {
        return sessionRegistry.getOnlineUserCount();
    }

    /**
     * システムメッセージを送信
     */
    public void sendSystemMessage(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        Map<String, Object> systemMessage = Map.of(
                "sender", "システム通知",
                "content", message,
                "timestamp", System.currentTimeMillis()
        );
        sessionRegistry.broadcast(SseTopics.SYSTEM, systemMessage);
        log.info("システムメッセージを送信: {}", message);
    }
}
