package com.smartdx.retail.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdx.core.result.Result;
import com.smartdx.retail.ai.model.req.AlertAssistantReq;
import com.smartdx.retail.ai.model.vo.AlertAssistantVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E tests for the AI alert priority assistant endpoint.
 */
class AlertAssistantE2ETest extends RetailE2EBase {

    private static final String ENDPOINT = "/api/v1/retail/ai/alerts/priority";
    private static final String VALID_MESSAGE = "今日対応すべき優先アラートは？";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void priority_withValidMessage_returnsFallbackSummaryAndAlerts() throws Exception {
        // Given
        Long storeId = seedStore("S-AI-001", "AIアシスタント店", "ONLINE");
        Long categoryId = seedCategory("CAT-AI-001", "AIカテゴリ");
        Long productId = seedProduct("P-AI-001", "AI商品", categoryId);

        seedAlert(storeId, productId, "LOW_STOCK", "NEW", "P1");
        seedAlert(storeId, productId, "HIGH_STOCK", "ACK", "P2");
        seedAlert(storeId, productId, "EXPIRY_SOON", "IN_PROGRESS", "P3");
        seedAlert(storeId, productId, "LOW_STOCK", "RESOLVED", "P1");

        AlertAssistantReq req = new AlertAssistantReq();
        req.setMessage(VALID_MESSAGE);

        // When
        ResponseEntity<Result> response = restTemplate.postForEntity(ENDPOINT, req, Result.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Result result = response.getBody();
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("00000");

        AlertAssistantVO vo = objectMapper.convertValue(result.getData(), AlertAssistantVO.class);
        assertThat(vo).isNotNull();
        assertThat(vo.isFallback()).isTrue();
        assertThat(vo.isLlmUsed()).isFalse();
        assertThat(vo.getAlerts()).hasSize(3);
        assertThat(vo.getSummary()).contains("本日の未解決アラートは計 3 件です。");
        assertThat(vo.getSummary()).contains("P1");
        assertThat(vo.getSummary()).contains("最優先の対応：P1 アラートから順に対応してください。");
    }

    @Test
    void priority_withInvalidMessage_returns400() {
        // Given
        AlertAssistantReq req = new AlertAssistantReq();
        req.setMessage("別の質問");

        // When
        ResponseEntity<Result> response = restTemplate.postForEntity(ENDPOINT, req, Result.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Result result = response.getBody();
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("A0400");
    }

    @Test
    void priority_withNoAlerts_returnsEmptyFallbackSummary() throws Exception {
        // Given
        AlertAssistantReq req = new AlertAssistantReq();
        req.setMessage(VALID_MESSAGE);

        // When
        ResponseEntity<Result> response = restTemplate.postForEntity(ENDPOINT, req, Result.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Result result = response.getBody();
        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("00000");

        AlertAssistantVO vo = objectMapper.convertValue(result.getData(), AlertAssistantVO.class);
        assertThat(vo).isNotNull();
        assertThat(vo.isFallback()).isTrue();
        assertThat(vo.getAlerts()).isEmpty();
        assertThat(vo.getSummary()).contains("本日の未解決アラートは計 0 件です。");
    }
}
