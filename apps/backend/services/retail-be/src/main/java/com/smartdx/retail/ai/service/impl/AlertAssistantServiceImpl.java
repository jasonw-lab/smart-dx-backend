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
import com.smartdx.security.model.UserDetails;
import com.smartdx.tenant.TenantContextHolder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
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
    private final List<LlmClient> llmClients;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void logAvailableClients() {
        if (llmClients == null || llmClients.isEmpty()) {
            log.info("[AI_ALERT_AUDIT] No LLM clients available");
        } else {
            String names = llmClients.stream()
                    .map(c -> c.getProviderName() + "(available=" + c.isAvailable() + ")")
                    .collect(Collectors.joining(", "));
            log.info("[AI_ALERT_AUDIT] Available LLM clients: {}", names);
        }
    }

    @Override
    public AlertAssistantVO answerPriorityAlerts(AlertAssistantReq request) {
        if (request == null || !EXPECTED_MESSAGE.equals(request.getMessage())) {
            log.warn("[AI_ALERT_AUDIT] Invalid assistant request: message={}",
                    request == null ? "null" : request.getMessage());
            throw new IllegalArgumentException("Unsupported message. Only \"" + EXPECTED_MESSAGE + "\" is accepted.");
        }

        List<AlertPageVO> alerts = alertService.listTodayAlerts();

        boolean llmEnabled = alertAssistantConfig.getLlm().isEnabled();
        LlmClient llmClient = resolveLlmClient(request.getLlm());
        boolean llmAvailable = llmClient != null && llmClient.isAvailable();
        log.debug("[AI_ALERT_AUDIT] llmEnabled={} requestedLlm={} resolvedClient={} llmAvailable={}",
                llmEnabled, request.getLlm(),
                llmClient != null ? llmClient.getProviderName() : "none",
                llmAvailable);

        if (llmEnabled && llmAvailable) {
            try {
                LlmResponse response = callLlm(llmClient, alerts);
                AlertAssistantVO vo = new AlertAssistantVO();
                vo.setSummary(response.getContent());
                vo.setAlerts(alerts);
                vo.setLlmUsed(true);
                vo.setLlmModel(resolveModelName(response, llmClient));
                vo.setFallback(false);
                auditLog(vo, response.getLatencyMs(), null);
                return vo;
            } catch (Exception e) {
                log.warn("[AI_ALERT_AUDIT] LLM call failed, falling back to rule-based summary", e);
            }
        }

        AlertAssistantVO vo = new AlertAssistantVO();
        vo.setSummary(buildFallbackSummary(alerts));
        vo.setAlerts(alerts);
        vo.setLlmUsed(false);
        vo.setLlmModel(null);
        vo.setFallback(true);
        auditLog(vo, 0L, "LLM unavailable or failed");
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
                .build();

        LlmResponse response = llmClient.complete(llmRequest);
        String content = response.getContent();
        if (content == null || content.isBlank()) {
            throw new LlmException(LlmException.LlmErrorCode.INVALID_RESPONSE, "LLM returned empty content");
        }
        return response;
    }

    private LlmClient resolveLlmClient(String requestedLlm) {
        if (llmClients == null || llmClients.isEmpty()) {
            return null;
        }
        String provider = requestedLlm;
        if (provider == null || provider.isBlank()) {
            provider = alertAssistantConfig.getLlm().getProvider();
        }
        final String target = normalizeProviderName(provider);
        Optional<LlmClient> exact = llmClients.stream()
                .filter(c -> normalizeProviderName(c.getProviderName()).equals(target))
                .findFirst();
        if (exact.isPresent()) {
            return exact.get();
        }
        if (requestedLlm != null && !requestedLlm.isBlank()) {
            log.warn("[AI_ALERT_AUDIT] Unsupported LLM provider requested: {}", requestedLlm);
            throw new IllegalArgumentException("Unsupported LLM provider: " + requestedLlm);
        }
        log.warn("[AI_ALERT_AUDIT] Configured LLM provider is not registered: {}", provider);
        return null;
    }

    private String normalizeProviderName(String provider) {
        String normalized = provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
        if ("smartdx-local".equals(normalized)) {
            return "local";
        }
        return normalized;
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
            return "本日の未解決アラートは計 0 件です。\n対応すべきアラートはありません。";
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

    private void auditLog(AlertAssistantVO vo, long latencyMs, String errorReason) {
        Long tenantId = TenantContextHolder.getTenantId();
        Long userId = null;
        String username = "anonymous";
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                username = authentication.getName();
                Object principal = authentication.getPrincipal();
                if (principal instanceof UserDetails userDetails) {
                    userId = userDetails.getUserId();
                }
            }
        } catch (Exception e) {
            log.debug("Failed to resolve audit user context", e);
        }

        log.info("[AI_ALERT_AUDIT] tenantId={} userId={} username={} question={} alertCount={} llmUsed={} llmModel={} fallback={} latencyMs={} errorReason={}",
                tenantId, userId, username, EXPECTED_MESSAGE, vo.getAlerts().size(),
                vo.isLlmUsed(), vo.getLlmModel(), vo.isFallback(), latencyMs, errorReason);
    }
}
