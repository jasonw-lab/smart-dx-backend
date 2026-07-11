package com.smartdx.retail.ai.llm.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdx.retail.ai.config.AlertAssistantConfig;
import com.smartdx.retail.ai.llm.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

/**
 * Kimi / Moonshot LLM client for the AI alert assistant.
 * <p>
 * Supports two key formats:
 * <ul>
 *     <li>{@code sk-kimi-...} keys from Kimi Code use the Anthropic Messages-compatible
 *         endpoint at {@code https://api.kimi.com/coding}.</li>
 *     <li>Other keys use the Moonshot Open Platform OpenAI-compatible endpoint at
 *         {@code https://api.moonshot.cn/v1}.</li>
 * </ul>
 *
 * @author jason.w
 */
@Component("retailKimiLlmClient")
@Slf4j
public class KimiLlmClient implements LlmClient {

    private static final String KIMI_CODE_DEFAULT_BASE_URL = "https://api.kimi.com/coding";
    private static final String KIMI_CODE_DEFAULT_MODEL = "kimi-for-coding";
    private static final String KIMI_CODE_USER_AGENT = "claude-code/0.1.0";
    private static final int KIMI_CODE_MAX_TOKENS = 500;

    private static final String MOONSHOT_DEFAULT_BASE_URL = "https://api.moonshot.cn/v1";
    private static final String MOONSHOT_DEFAULT_MODEL = "moonshot-v1-8k";

    private final AlertAssistantConfig alertAssistantConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public KimiLlmClient(AlertAssistantConfig alertAssistantConfig, ObjectMapper objectMapper) {
        this.alertAssistantConfig = alertAssistantConfig;
        this.objectMapper = objectMapper;

        long timeoutMs = alertAssistantConfig.getLlm().getTimeoutMs();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) timeoutMs);
        requestFactory.setReadTimeout((int) timeoutMs);
        this.restTemplate = new RestTemplate(requestFactory);

        AlertAssistantConfig.ProviderConfig providerConfig = alertAssistantConfig.getLlm().getProvider("kimi");
        log.info("KimiLlmClient initialized: modelHint={}, timeoutMs={}",
                providerConfig.getModel(), timeoutMs);
    }

    @Override
    public LlmResponse complete(LlmRequest request) throws LlmException {
        long startTime = System.currentTimeMillis();

        try {
            AlertAssistantConfig.ProviderConfig providerConfig = alertAssistantConfig.getLlm().getProvider("kimi");
            String apiKey = providerConfig.getApiKey();

            if (apiKey == null || apiKey.isBlank()) {
                throw new LlmException(LlmException.LlmErrorCode.UNAVAILABLE, "KIMI_API_KEY is not configured");
            }

            boolean isKimiCodeKey = apiKey.startsWith("sk-kimi-");
            String baseUrl = providerConfig.getBaseUrl();
            String model = providerConfig.getModel();

            if (isKimiCodeKey) {
                if (baseUrl == null || baseUrl.isBlank()) {
                    baseUrl = KIMI_CODE_DEFAULT_BASE_URL;
                }
                // Kimi Code keys only work with coding models on the coding endpoint.
                if (model == null || model.isBlank() || !isKimiCodeModel(model)) {
                    model = KIMI_CODE_DEFAULT_MODEL;
                }
                return callAnthropicMessages(request, apiKey, model, baseUrl, startTime);
            } else {
                if (baseUrl == null || baseUrl.isBlank()) {
                    baseUrl = MOONSHOT_DEFAULT_BASE_URL;
                }
                if (model == null || model.isBlank()) {
                    model = MOONSHOT_DEFAULT_MODEL;
                }
                return callOpenAiChatCompletions(request, apiKey, model, baseUrl, startTime);
            }

        } catch (LlmException e) {
            throw e;
        } catch (HttpStatusCodeException e) {
            LlmException.LlmErrorCode errorCode = e.getStatusCode().value() == 429
                    ? LlmException.LlmErrorCode.RATE_LIMITED
                    : LlmException.LlmErrorCode.API_ERROR;
            String message = "Kimi API returned status: " + e.getStatusCode().value();
            log.warn("Kimi API call failed: {}", message);
            throw new LlmException(errorCode, message, e);
        } catch (ResourceAccessException e) {
            LlmException.LlmErrorCode errorCode = containsTimeout(e)
                    ? LlmException.LlmErrorCode.TIMEOUT
                    : LlmException.LlmErrorCode.API_ERROR;
            String message = sanitizeExceptionMessage(e);
            log.warn("Kimi API call failed: {}", message, e);
            throw new LlmException(errorCode, "Kimi API call failed: " + message, e);
        } catch (Exception e) {
            String message = sanitizeExceptionMessage(e);
            log.error("Kimi API call failed: {}", message, e);
            throw new LlmException(LlmException.LlmErrorCode.API_ERROR,
                    "Kimi API call failed: " + message, e);
        }
    }

    @Override
    public String getProviderName() {
        return "kimi";
    }

    @Override
    public boolean isAvailable() {
        AlertAssistantConfig.ProviderConfig providerConfig = alertAssistantConfig.getLlm().getProvider("kimi");
        String apiKey = providerConfig.getApiKey();
        return apiKey != null && !apiKey.isBlank();
    }

    // ---------- Moonshot Open Platform (OpenAI-compatible) ----------

    private LlmResponse callOpenAiChatCompletions(LlmRequest request, String apiKey, String model,
                                                   String baseUrl, long startTime) throws Exception {
        String url = baseUrl.replaceAll("/$", "") + "/chat/completions";

        OpenAiChatRequest chatRequest = buildOpenAiChatRequest(request, model);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"));
        headers.setBearerAuth(apiKey);

        String requestBody = objectMapper.writeValueAsString(chatRequest);
        log.debug("Kimi (Moonshot) API request: {}", truncate(requestBody, 500));

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        long latencyMs = System.currentTimeMillis() - startTime;

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            OpenAiChatResponse chatResponse = objectMapper.readValue(response.getBody(), OpenAiChatResponse.class);
            String content = extractOpenAiContent(chatResponse);
            int inputTokens = extractOpenAiInputTokens(chatResponse);
            int outputTokens = extractOpenAiOutputTokens(chatResponse);
            log.info("Kimi (Moonshot) API call succeeded: model={}, inputTokens={}, outputTokens={}, latencyMs={}",
                    model, inputTokens, outputTokens, latencyMs);
            return LlmResponse.builder()
                    .content(content)
                    .inputTokens(inputTokens)
                    .outputTokens(outputTokens)
                    .latencyMs(latencyMs)
                    .model(model)
                    .finishReason(extractOpenAiFinishReason(chatResponse))
                    .build();
        } else {
            throw new LlmException(LlmException.LlmErrorCode.API_ERROR,
                    "Kimi (Moonshot) API returned non-success status: " + response.getStatusCode());
        }
    }

    private OpenAiChatRequest buildOpenAiChatRequest(LlmRequest request, String model) {
        OpenAiChatRequest chatRequest = new OpenAiChatRequest();
        chatRequest.setModel(model);
        chatRequest.setMaxTokens(request.getMaxOutputTokens());
        chatRequest.setTemperature(request.getTemperature());

        List<OpenAiMessage> messages = new ArrayList<>();
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            OpenAiMessage systemMessage = new OpenAiMessage();
            systemMessage.setRole("system");
            systemMessage.setContent(request.getSystemPrompt());
            messages.add(systemMessage);
        }
        if (request.getConversationHistory() != null && !request.getConversationHistory().isEmpty()) {
            for (ConversationMessage msg : request.getConversationHistory()) {
                OpenAiMessage message = new OpenAiMessage();
                message.setRole(msg.getRole().equals("assistant") ? "assistant" : "user");
                message.setContent(msg.getContent());
                messages.add(message);
            }
        }
        OpenAiMessage userMessage = new OpenAiMessage();
        userMessage.setRole("user");
        userMessage.setContent(request.getUserPrompt());
        messages.add(userMessage);

        chatRequest.setMessages(messages);
        return chatRequest;
    }

    private String extractOpenAiContent(OpenAiChatResponse response) {
        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
            OpenAiChoice choice = response.getChoices().get(0);
            if (choice.getMessage() != null && choice.getMessage().getContent() != null) {
                return choice.getMessage().getContent();
            }
        }
        return "";
    }

    private int extractOpenAiInputTokens(OpenAiChatResponse response) {
        return response.getUsage() != null ? response.getUsage().getPromptTokens() : 0;
    }

    private int extractOpenAiOutputTokens(OpenAiChatResponse response) {
        return response.getUsage() != null ? response.getUsage().getCompletionTokens() : 0;
    }

    private String extractOpenAiFinishReason(OpenAiChatResponse response) {
        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
            return response.getChoices().get(0).getFinishReason();
        }
        return "unknown";
    }

    // ---------- Kimi Code (Anthropic Messages-compatible) ----------

    private LlmResponse callAnthropicMessages(LlmRequest request, String apiKey, String model,
                                               String baseUrl, long startTime) throws Exception {
        String url = baseUrl.replaceAll("/$", "") + "/v1/messages";

        AnthropicMessagesRequest messagesRequest = buildAnthropicMessagesRequest(request, model);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"));
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");
        headers.set("User-Agent", KIMI_CODE_USER_AGENT);

        String requestBody = objectMapper.writeValueAsString(messagesRequest);
        log.debug("Kimi Code API request: {}", truncate(requestBody, 500));

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        long latencyMs = System.currentTimeMillis() - startTime;

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            AnthropicMessagesResponse messagesResponse = objectMapper.readValue(response.getBody(), AnthropicMessagesResponse.class);
            String content = extractAnthropicContent(messagesResponse);
            int inputTokens = extractAnthropicInputTokens(messagesResponse);
            int outputTokens = extractAnthropicOutputTokens(messagesResponse);
            log.info("Kimi Code API call succeeded: model={}, inputTokens={}, outputTokens={}, latencyMs={}",
                    model, inputTokens, outputTokens, latencyMs);
            return LlmResponse.builder()
                    .content(content)
                    .inputTokens(inputTokens)
                    .outputTokens(outputTokens)
                    .latencyMs(latencyMs)
                    .model(model)
                    .finishReason(messagesResponse.getStopReason())
                    .build();
        } else {
            throw new LlmException(LlmException.LlmErrorCode.API_ERROR,
                    "Kimi Code API returned non-success status: " + response.getStatusCode());
        }
    }

    private AnthropicMessagesRequest buildAnthropicMessagesRequest(LlmRequest request, String model) {
        AnthropicMessagesRequest messagesRequest = new AnthropicMessagesRequest();
        messagesRequest.setModel(model);
        messagesRequest.setMaxTokens(Math.min(request.getMaxOutputTokens(), KIMI_CODE_MAX_TOKENS));
        messagesRequest.setTemperature(request.getTemperature());
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            messagesRequest.setSystem(request.getSystemPrompt());
        }

        List<AnthropicMessage> messages = new ArrayList<>();
        if (request.getConversationHistory() != null && !request.getConversationHistory().isEmpty()) {
            for (ConversationMessage msg : request.getConversationHistory()) {
                AnthropicMessage message = new AnthropicMessage();
                message.setRole(msg.getRole().equals("assistant") ? "assistant" : "user");
                message.setContent(msg.getContent());
                messages.add(message);
            }
        }
        AnthropicMessage userMessage = new AnthropicMessage();
        userMessage.setRole("user");
        userMessage.setContent(request.getUserPrompt());
        messages.add(userMessage);

        messagesRequest.setMessages(messages);
        return messagesRequest;
    }

    private String extractAnthropicContent(AnthropicMessagesResponse response) {
        if (response.getContent() != null) {
            StringBuilder sb = new StringBuilder();
            for (AnthropicContentBlock block : response.getContent()) {
                if ("text".equals(block.getType()) && block.getText() != null) {
                    sb.append(block.getText());
                }
            }
            return sb.toString();
        }
        return "";
    }

    private int extractAnthropicInputTokens(AnthropicMessagesResponse response) {
        return response.getUsage() != null ? response.getUsage().getInputTokens() : 0;
    }

    private int extractAnthropicOutputTokens(AnthropicMessagesResponse response) {
        return response.getUsage() != null ? response.getUsage().getOutputTokens() : 0;
    }

    private boolean isKimiCodeModel(String model) {
        return model.startsWith("kimi-") || model.startsWith("kimi_for_") || model.startsWith("kimi_for_coding");
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "null";
        return str.length() <= maxLength ? str : str.substring(0, maxLength) + "...";
    }

    private boolean containsTimeout(Throwable e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String sanitizeExceptionMessage(Throwable e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return e.getClass().getSimpleName();
        }
        return message
                .replaceAll("(Authorization:\\s*Bearer\\s+)[^,\\]\\s]+", "$1****")
                .replaceAll("(x-api-key[=:]\\s*)[^,\\]\\s]+", "$1****");
    }

    // OpenAI-compatible DTOs
    @Data
    static class OpenAiChatRequest {
        private String model;
        private List<OpenAiMessage> messages;
        @JsonProperty("max_tokens")
        private int maxTokens;
        private double temperature;
    }

    @Data
    static class OpenAiMessage {
        private String role;
        private String content;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OpenAiChatResponse {
        private List<OpenAiChoice> choices;
        private OpenAiUsage usage;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OpenAiChoice {
        private OpenAiMessage message;
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OpenAiUsage {
        @JsonProperty("prompt_tokens")
        private int promptTokens;
        @JsonProperty("completion_tokens")
        private int completionTokens;
        @JsonProperty("total_tokens")
        private int totalTokens;
    }

    // Anthropic Messages-compatible DTOs
    @Data
    static class AnthropicMessagesRequest {
        private String model;
        @JsonProperty("max_tokens")
        private int maxTokens;
        private double temperature;
        private String system;
        private List<AnthropicMessage> messages;
    }

    @Data
    static class AnthropicMessage {
        private String role;
        private String content;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AnthropicMessagesResponse {
        private String id;
        private String type;
        private String role;
        private String model;
        @JsonProperty("stop_reason")
        private String stopReason;
        private List<AnthropicContentBlock> content;
        private AnthropicUsage usage;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AnthropicContentBlock {
        private String type;
        private String text;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AnthropicUsage {
        @JsonProperty("input_tokens")
        private int inputTokens;
        @JsonProperty("output_tokens")
        private int outputTokens;
        @JsonProperty("total_tokens")
        private int totalTokens;
    }
}
