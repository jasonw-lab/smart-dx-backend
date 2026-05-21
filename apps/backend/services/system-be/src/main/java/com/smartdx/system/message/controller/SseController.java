package com.smartdx.system.message.controller;

import com.smartdx.core.result.Result;
import com.smartdx.security.model.UserDetails;
import com.smartdx.security.util.SecurityUtils;
import com.smartdx.system.message.service.SseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE コントローラー
 */
@Tag(name = "14. SSE接続")
@Slf4j
@RestController
@RequestMapping("/api/v1/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseService sseService;

    @Operation(summary = "SSE接続を確立")
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect() {
        UserDetails user = SecurityUtils.getUser().orElse(null);
        if (user == null) {
            log.warn("SSE接続失敗：現在のユーザーを取得できません");
            return null;
        }
        return sseService.createConnection(user.getUsername());
    }

    @Operation(summary = "オンラインユーザー数を取得")
    @GetMapping("/online-count")
    public Result<Integer> getOnlineCount() {
        return Result.success(sseService.getOnlineUserCount());
    }
}
