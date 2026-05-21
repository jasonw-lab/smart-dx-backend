package com.smartdx.system.message.job;

import com.smartdx.system.message.registry.SseSessionRegistry;
import com.smartdx.system.message.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * オンラインユーザー数統計定期タスク
 * <p>
 * 定期的にオンラインユーザー数を統計し、全SSEクライアントにブロードキャスト
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OnlineUserCountJob {

    private final SseSessionRegistry sessionRegistry;
    private final SseService sseService;

    /**
     * オンラインユーザー数を定期統計してブロードキャスト
     * <p>
     * 3分ごとに実行し、現在のオンラインユーザー数をプッシュ
     */
    @Scheduled(cron = "0 */3 * * * ?")
    public void execute() {
        int onlineCount = sessionRegistry.getOnlineUserCount();
        int connectionCount = sessionRegistry.getTotalConnectionCount();

        log.debug("定期統計：オンラインユーザー数={}, 総接続数={}", onlineCount, connectionCount);

        sseService.sendOnlineCount();
    }
}
