package com.smartdx.retail.ai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdx.retail.ai.config.AlertAssistantConfig;
import com.smartdx.retail.ai.llm.LlmClient;
import com.smartdx.retail.ai.llm.LlmException;
import com.smartdx.retail.ai.llm.LlmRequest;
import com.smartdx.retail.ai.llm.LlmResponse;
import com.smartdx.retail.ai.model.req.AlertAssistantReq;
import com.smartdx.retail.ai.model.vo.AlertAssistantVO;
import com.smartdx.retail.ai.service.AlertAssistantService;
import com.smartdx.retail.model.vo.AlertPageVO;
import com.smartdx.retail.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI alert assistant service implementation.
 *
 * @author jason.w
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertAssistantServiceImpl implements AlertAssistantService {

    private static final String EXPECTED_MESSAGE = "今日対応すべき優先アラートは？";

    private static final String SYSTEM_PROMPT = """
            必ず日本語で回答してください。
            あなたは無人スーパー「Smart Retail DX」の運用アシスタントです。
            本日対応すべきアラートを、優先度順に整理して日本語で簡潔に回答してください。
            各アラートの種類、発生箇所、優先度、ステータスを考慮し、なぜその順序で対応すべきかを含めてください。
            要点を絞った要約を200〜400字程度で作成し、最後に「最優先の対応：...」という形で結論を示してください。
            """;

    private final AlertService alertService;
    private final AlertAssistantConfig alertAssistantConfig;
    private final ObjectProvider<LlmClient> llmClientProvider;
    private final ObjectMapper objectMapper;

    @Override
    public AlertAssistantVO answerPriorityAlerts(AlertAssistantReq request) {
        if (request == null || !EXPECTED_MESSAGE.equals(request.getMessage())) {
            throw new IllegalArgumentException("Unsupported message. Only \"" + EXPECTED_MESSAGE + "\" is accepted.");
        }

        List<AlertPageVO> alerts = alertService.listTodayAlerts();

        LlmClient llmClient = llmClientProvider.getIfAvailable();
        boolean llmEnabled = alertAssistantConfig.getLlm().isEnabled();

        if (llmEnabled && llmClient != null && llmClient.isAvailable()) {
            try {
                LlmResponse response = callLlm(llmClient, alerts);
                AlertAssistantVO vo = new AlertAssistantVO();
                vo.setSummary(response.getContent());
                vo.setAlerts(alerts);
                vo.setLlmUsed(true);
                vo.setLlmModel(resolveModelName(response, llmClient));
                vo.setFallback(false);
                return vo;
            } catch (Exception e) {
                log.warn("LLM call failed, falling back to rule-based summary", e);
            }
        }

        AlertAssistantVO vo = new AlertAssistantVO();
        vo.setSummary(buildFallbackSummary(alerts));
        vo.setAlerts(alerts);
        vo.setLlmUsed(false);
        vo.setLlmModel(null);
        vo.setFallback(true);
        return vo;
    }

    private LlmResponse callLlm(LlmClient llmClient, List<AlertPageVO> alerts) throws LlmException, JsonProcessingException {
        String alertsJson = objectMapper.writeValueAsString(alerts);
        String userPrompt = EXPECTED_MESSAGE + "\n\n" + alertsJson;

        LlmRequest llmRequest = LlmRequest.builder()
                .systemPrompt(SYSTEM_PROMPT)
                .userPrompt(userPrompt)
                .maxOutputTokens(800)
                .temperature(0.2)
                .timeoutMs(5000)
                .build();

        LlmResponse response = llmClient.complete(llmRequest);
        String content = response.getContent();
        if (content == null || content.isBlank()) {
            response.setContent(buildFallbackSummary(alerts));
        }
        return response;
    }

    private String resolveModelName(LlmResponse response, LlmClient llmClient) {
        if (response != null && response.getModel() != null && !response.getModel().isBlank()) {
            return response.getModel();
        }
        String configuredModel = alertAssistantConfig.getLlm().getModel();
        if (configuredModel != null && !configuredModel.isBlank()) {
            return configuredModel;
        }
        return llmClient.getProviderName();
    }

    private String buildFallbackSummary(List<AlertPageVO> alerts) {
        int total = alerts.size();
        if (total == 0) {
            return "Gemini 利用不可のため、ルールベースで表示しています。\n本日の未解決アラートは計 0 件です。\n対応すべきアラートはありません。";
        }

        Map<String, Long> countByPriority = alerts.stream()
                .collect(Collectors.groupingBy(AlertPageVO::getPriority, LinkedHashMap::new, Collectors.counting()));

        Map<String, String> priorityEmoji = Map.of(
                "P1", "🔴",
                "P2", "🟠",
                "P3", "🟡",
                "P4", "🟢"
        );

        Map<String, String> exampleByPriority = new LinkedHashMap<>();
        for (AlertPageVO alert : alerts) {
            exampleByPriority.putIfAbsent(alert.getPriority(), alert.getAlertType());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Gemini 利用不可のため、ルールベースで表示しています。\n");
        sb.append("本日の未解決アラートは計 ").append(total).append(" 件です。\n");

        for (Map.Entry<String, Long> entry : countByPriority.entrySet()) {
            String priority = entry.getKey();
            long count = entry.getValue();
            String emoji = priorityEmoji.getOrDefault(priority, "⚪");
            String example = exampleByPriority.get(priority);
            sb.append(emoji).append(" ").append(priority).append(": ").append(count).append("件");
            if (example != null) {
                sb.append("（例：").append(example).append("）");
            }
            sb.append("\n");
        }

        sb.append("最優先の対応：P1 アラートから順に対応してください。");
        return sb.toString();
    }
}
