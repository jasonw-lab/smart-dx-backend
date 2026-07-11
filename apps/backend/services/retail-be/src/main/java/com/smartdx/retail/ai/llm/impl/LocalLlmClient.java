package com.smartdx.retail.ai.llm.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdx.retail.ai.llm.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ローカル LLM クライアント。
 * <p>
 * 外部 API キー不要で、アラート情報からルールベースの日本語要約を生成します。
 * デフォルトの AI アシスタントエンジンとして使用します。
 * </p>
 *
 * @author jason.w
 */
@Component
@Slf4j
public class LocalLlmClient implements LlmClient {

    private static final String MODEL_NAME = "smartdx-local";
    private static final String PROVIDER_NAME = "local";

    private final ObjectMapper objectMapper;

    public LocalLlmClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        log.info("LocalLlmClient initialized as default alert assistant engine");
    }

    @Override
    public LlmResponse complete(LlmRequest request) throws LlmException {
        long startTime = System.currentTimeMillis();

        try {
            List<Map<String, Object>> alerts = extractAlerts(request.getUserPrompt());
            String summary = buildSummary(alerts);
            long latencyMs = System.currentTimeMillis() - startTime;

            return LlmResponse.builder()
                    .content(summary)
                    .inputTokens(estimateTokens(request.getUserPrompt()))
                    .outputTokens(estimateTokens(summary))
                    .latencyMs(latencyMs)
                    .model(MODEL_NAME)
                    .finishReason("stop")
                    .build();
        } catch (Exception e) {
            log.error("Local LLM summary generation failed", e);
            throw new LlmException(LlmException.LlmErrorCode.UNAVAILABLE,
                    "Local summary generation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractAlerts(String userPrompt) throws LlmException {
        if (userPrompt == null || userPrompt.isBlank()) {
            return Collections.emptyList();
        }

        String json = userPrompt;
        int separatorIndex = userPrompt.indexOf("\n\n");
        if (separatorIndex >= 0 && separatorIndex + 2 < userPrompt.length()) {
            json = userPrompt.substring(separatorIndex + 2).trim();
        }

        if (json.isBlank() || !json.startsWith("[")) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            throw new LlmException(LlmException.LlmErrorCode.INVALID_RESPONSE,
                    "Failed to parse alerts JSON from prompt", e);
        }
    }

    private String buildSummary(List<Map<String, Object>> alerts) {
        int total = alerts.size();
        if (total == 0) {
            return "本日対応すべき未解決アラートはありません。\n店舗運用は正常に継続されています。";
        }

        Map<String, Long> countByPriority = alerts.stream()
                .collect(Collectors.groupingBy(
                        alert -> String.valueOf(alert.getOrDefault("priority", "UNKNOWN")),
                        LinkedHashMap::new,
                        Collectors.counting()));

        Map<String, String> priorityEmoji = Map.of(
                "P1", "🔴",
                "P2", "🟠",
                "P3", "🟡",
                "P4", "🟢"
        );

        Map<String, String> exampleByPriority = new LinkedHashMap<>();
        for (Map<String, Object> alert : alerts) {
            String priority = String.valueOf(alert.getOrDefault("priority", "UNKNOWN"));
            exampleByPriority.putIfAbsent(priority, String.valueOf(alert.getOrDefault("alertType", "")));
        }

        StringBuilder sb = new StringBuilder();
        sb.append("本日対応すべき優先アラートは計 ").append(total).append(" 件です。\n");

        List<String> priorityOrder = List.of("P1", "P2", "P3", "P4");
        for (String priority : priorityOrder) {
            Long count = countByPriority.get(priority);
            if (count == null || count == 0) {
                continue;
            }
            String emoji = priorityEmoji.getOrDefault(priority, "⚪");
            String example = exampleByPriority.get(priority);
            sb.append(emoji).append(" ").append(priority).append(": ").append(count).append("件");
            if (example != null && !example.isBlank()) {
                sb.append("（例：").append(example).append("）");
            }
            sb.append("\n");
        }

        sb.append("最優先の対応：P1 アラートから順に対応してください。");
        return sb.toString();
    }

    private int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        // 日本語・英語混在を近似：文字数の 1/2 をトークン数とする
        return Math.max(1, text.length() / 2);
    }
}
