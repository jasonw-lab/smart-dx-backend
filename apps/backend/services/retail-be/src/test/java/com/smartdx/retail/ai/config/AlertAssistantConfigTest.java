package com.smartdx.retail.ai.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link AlertAssistantConfig} property binding.
 *
 * @author jason.w
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AlertAssistantConfig.class)
@EnableConfigurationProperties(AlertAssistantConfig.class)
@TestPropertySource(properties = {
        "retail.ai.llm.enabled=false",
        "retail.ai.llm.provider=gemini",
        "retail.ai.llm.api-key=test-api-key",
        "retail.ai.llm.model=gemini-test-model",
        "retail.ai.llm.timeout-ms=3000"
})
class AlertAssistantConfigTest {

    @Autowired
    private AlertAssistantConfig alertAssistantConfig;

    @Test
    void bindsLlmProperties() {
        assertThat(alertAssistantConfig.getLlm().isEnabled()).isFalse();
        assertThat(alertAssistantConfig.getLlm().getProvider()).isEqualTo("gemini");
        assertThat(alertAssistantConfig.getLlm().getApiKey()).isEqualTo("test-api-key");
        assertThat(alertAssistantConfig.getLlm().getModel()).isEqualTo("gemini-test-model");
        assertThat(alertAssistantConfig.getLlm().getTimeoutMs()).isEqualTo(3000L);
    }
}
