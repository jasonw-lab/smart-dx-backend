package com.smartdx.retail.ai.llm.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdx.retail.ai.config.AlertAssistantConfig;
import com.smartdx.retail.ai.llm.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Google AI Studio (Gemini) LLM client for the AI alert assistant.
 *
 * @author jason.w
 */
@Component
@ConditionalOnProperty(name = "retail.ai.llm.provider", havingValue = "gemini")
@Slf4j
public class GeminiLlmClient implements LlmClient {

    private static final String GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final AlertAssistantConfig alertAssistantConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiLlmClient(AlertAssistantConfig alertAssistantConfig, ObjectMapper objectMapper) {
        this.alertAssistantConfig = alertAssistantConfig;
        this.objectMapper = objectMapper;

        long timeoutMs = alertAssistantConfig.getLlm().getTimeoutMs();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) timeoutMs);
        requestFactory.setReadTimeout((int) timeoutMs);
        this.restTemplate = new RestTemplate(requestFactory);

        log.info("GeminiLlmClient initialized with model: {}, timeoutMs: {}",
                alertAssistantConfig.getLlm().getModel(), timeoutMs);
    }

    @Override
    public LlmResponse complete(LlmRequest request) throws LlmException {
        long startTime = System.currentTimeMillis();

        try {
            String apiKey = alertAssistantConfig.getLlm().getApiKey();
            String model = alertAssistantConfig.getLlm().getModel();

            if (apiKey == null || apiKey.isBlank()) {
                throw new LlmException(LlmException.LlmErrorCode.UNAVAILABLE, "GOOGLE_AI_API_KEY is not configured");
            }

            // Default model when blank or left at placeholder default.
            if (model == null || model.isBlank() || model.equals("gemini-2.0-flash-001")) {
                model = "gemini-2.0-flash-001";
            }

            String url = GEMINI_API_BASE_URL + model + ":generateContent?key=" + apiKey;

            GeminiRequest geminiRequest = buildGeminiRequest(request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String requestBody = objectMapper.writeValueAsString(geminiRequest);
            log.debug("Gemini API request: {}", truncate(requestBody, 500));

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            long latencyMs = System.currentTimeMillis() - startTime;

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                GeminiResponse geminiResponse = objectMapper.readValue(response.getBody(), GeminiResponse.class);

                String content = extractContent(geminiResponse);
                int inputTokens = extractInputTokens(geminiResponse);
                int outputTokens = extractOutputTokens(geminiResponse);

                log.info("Gemini API call succeeded: model={}, inputTokens={}, outputTokens={}, latencyMs={}",
                        model, inputTokens, outputTokens, latencyMs);

                return LlmResponse.builder()
                        .content(content)
                        .inputTokens(inputTokens)
                        .outputTokens(outputTokens)
                        .latencyMs(latencyMs)
                        .model(model)
                        .finishReason(extractFinishReason(geminiResponse))
                        .build();
            } else {
                throw new LlmException(LlmException.LlmErrorCode.API_ERROR,
                        "Gemini API returned non-success status: " + response.getStatusCode());
            }

        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini API call failed", e);
            throw new LlmException(LlmException.LlmErrorCode.API_ERROR,
                    "Gemini API call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public CompletableFuture<LlmResponse> completeAsync(LlmRequest request) {
        return CompletableFuture.supplyAsync(() -> complete(request));
    }

    @Override
    public String getProviderName() {
        return "gemini";
    }

    @Override
    public boolean isAvailable() {
        String apiKey = alertAssistantConfig.getLlm().getApiKey();
        return apiKey != null && !apiKey.isBlank();
    }

    private GeminiRequest buildGeminiRequest(LlmRequest request) {
        GeminiRequest geminiRequest = new GeminiRequest();

        List<GeminiContent> contents = new ArrayList<>();

        StringBuilder userText = new StringBuilder();
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            userText.append("[System Instructions]\n").append(request.getSystemPrompt()).append("\n\n");
        }

        if (request.getConversationHistory() != null && !request.getConversationHistory().isEmpty()) {
            for (ConversationMessage msg : request.getConversationHistory()) {
                GeminiContent content = new GeminiContent();
                content.setRole(msg.getRole().equals("assistant") ? "model" : "user");
                GeminiPart part = new GeminiPart();
                part.setText(msg.getContent());
                content.setParts(List.of(part));
                contents.add(content);
            }
        }

        userText.append(request.getUserPrompt());
        GeminiContent userContent = new GeminiContent();
        userContent.setRole("user");
        GeminiPart userPart = new GeminiPart();
        userPart.setText(userText.toString());
        userContent.setParts(List.of(userPart));
        contents.add(userContent);

        geminiRequest.setContents(contents);

        GeminiGenerationConfig config = new GeminiGenerationConfig();
        config.setMaxOutputTokens(request.getMaxOutputTokens());
        config.setTemperature(request.getTemperature());
        geminiRequest.setGenerationConfig(config);

        return geminiRequest;
    }

    private String extractContent(GeminiResponse response) {
        if (response.getCandidates() != null && !response.getCandidates().isEmpty()) {
            GeminiCandidate candidate = response.getCandidates().get(0);
            if (candidate.getContent() != null && candidate.getContent().getParts() != null) {
                List<GeminiPart> parts = candidate.getContent().getParts();
                if (!parts.isEmpty()) {
                    return parts.get(0).getText();
                }
            }
        }
        return "";
    }

    private int extractInputTokens(GeminiResponse response) {
        if (response.getUsageMetadata() != null) {
            return response.getUsageMetadata().getPromptTokenCount();
        }
        return 0;
    }

    private int extractOutputTokens(GeminiResponse response) {
        if (response.getUsageMetadata() != null) {
            return response.getUsageMetadata().getCandidatesTokenCount();
        }
        return 0;
    }

    private String extractFinishReason(GeminiResponse response) {
        if (response.getCandidates() != null && !response.getCandidates().isEmpty()) {
            return response.getCandidates().get(0).getFinishReason();
        }
        return "unknown";
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "null";
        return str.length() <= maxLength ? str : str.substring(0, maxLength) + "...";
    }

    // Gemini API DTOs
    @Data
    static class GeminiRequest {
        private List<GeminiContent> contents;
        private GeminiGenerationConfig generationConfig;
    }

    @Data
    static class GeminiContent {
        private String role;
        private List<GeminiPart> parts;
    }

    @Data
    static class GeminiPart {
        private String text;
    }

    @Data
    static class GeminiGenerationConfig {
        private int maxOutputTokens;
        private double temperature;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class GeminiResponse {
        private List<GeminiCandidate> candidates;
        private GeminiUsageMetadata usageMetadata;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class GeminiCandidate {
        private GeminiContent content;
        private String finishReason;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class GeminiUsageMetadata {
        private int promptTokenCount;
        private int candidatesTokenCount;
        private int totalTokenCount;
    }
}
