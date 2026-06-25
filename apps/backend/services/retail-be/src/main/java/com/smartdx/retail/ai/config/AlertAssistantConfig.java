package com.smartdx.retail.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * AI Alert Assistant configuration.
 *
 * @author jason.w
 */
@Configuration
@ConfigurationProperties(prefix = "retail.ai")
@Getter
@Setter
public class AlertAssistantConfig {

    /**
     * LLM settings.
     */
    private LlmConfig llm = new LlmConfig();

    @Getter
    @Setter
    public static class LlmConfig {

        /**
         * Whether the LLM integration is enabled.
         */
        private boolean enabled = true;

        /**
         * Default provider when the frontend does not specify one.
         * <p>Supported: gemini, kimi, local.</p>
         */
        private String provider = "gemini";

        /**
         * API key (kept for backward compatibility; prefer providers map).
         */
        private String apiKey;

        /**
         * Model name (kept for backward compatibility; prefer providers map).
         */
        private String model = "gemini-2.0-flash-001";

        /**
         * Timeout in milliseconds.
         */
        private long timeoutMs = 5000;

        /**
         * Per-provider configuration.
         */
        private Map<String, ProviderConfig> providers = new HashMap<>();

        /**
         * Resolve provider configuration, falling back to the top-level legacy settings.
         *
         * @param name provider name, e.g. gemini, kimi
         * @return provider config, never null
         */
        public ProviderConfig getProvider(String name) {
            ProviderConfig providerConfig = providers.get(name);
            if (providerConfig == null) {
                providerConfig = new ProviderConfig();
            }
            if (providerConfig.getApiKey() == null || providerConfig.getApiKey().isBlank()) {
                if (name.equalsIgnoreCase(provider)) {
                    providerConfig.setApiKey(apiKey);
                }
            }
            if (providerConfig.getModel() == null || providerConfig.getModel().isBlank()) {
                if (name.equalsIgnoreCase(provider)) {
                    providerConfig.setModel(model);
                }
            }
            if (providerConfig.getApiKey() == null) {
                providerConfig.setApiKey("");
            }
            if (providerConfig.getModel() == null) {
                providerConfig.setModel("");
            }
            return providerConfig;
        }
    }

    @Getter
    @Setter
    public static class ProviderConfig {

        /**
         * API key for this provider.
         */
        private String apiKey;

        /**
         * Model name for this provider.
         */
        private String model;

        /**
         * Base URL override (optional).
         */
        private String baseUrl;
    }
}
