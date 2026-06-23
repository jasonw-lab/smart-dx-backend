package com.smartdx.retail.e2e;

import com.smartdx.retail.ai.model.req.AlertAssistantReq;
import com.smartdx.retail.config.TestSecurityConfig;
import com.smartdx.security.model.UserDetails;
import com.smartdx.security.token.TokenManager;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

/**
 * E2E tests for the AI alert priority assistant endpoint.
 */
@Import(TestSecurityConfig.class)
class AlertAssistantE2ETest extends RetailE2EBase {

    private static final String ENDPOINT = "/api/v1/retail/ai/alerts/priority";
    private static final String VALID_MESSAGE = "今日対応すべき優先アラートは？";
    private static final long E2E_USER_ID = 900001L;
    private static final long E2E_TENANT_ID = 1L;

    @Autowired
    private TokenManager tokenManager;

    @LocalServerPort
    private int port;

    private String bearerToken;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        bearerToken = "Bearer " + createAccessToken();
    }

    @Test
    void priority_withValidMessage_returnsFallbackSummaryAndAlerts() {
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

        // When / Then
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", bearerToken)
                .body(req)
                .when()
                .post(ENDPOINT)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("code", equalTo("00000"))
                .body("data.fallback", equalTo(true))
                .body("data.llmUsed", equalTo(false))
                .body("data.alerts", hasSize(3))
                .body("data.summary", containsString("本日の未解決アラートは計 3 件です。"))
                .body("data.summary", containsString("P1"))
                .body("data.summary", containsString("最優先の対応：P1 アラートから順に対応してください。"));
    }

    @Test
    void priority_withInvalidMessage_returns400() {
        // Given
        AlertAssistantReq req = new AlertAssistantReq();
        req.setMessage("別の質問");

        // When / Then
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", bearerToken)
                .body(req)
                .when()
                .post(ENDPOINT)
                .then()
                .log().ifValidationFails()
                .statusCode(400)
                .body("code", equalTo("A0400"));
    }

    @Test
    void priority_withNoAlerts_returnsEmptyFallbackSummary() {
        // Given
        AlertAssistantReq req = new AlertAssistantReq();
        req.setMessage(VALID_MESSAGE);

        // When / Then
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", bearerToken)
                .body(req)
                .when()
                .post(ENDPOINT)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("code", equalTo("00000"))
                .body("data.fallback", equalTo(true))
                .body("data.llmUsed", equalTo(false))
                .body("data.alerts", hasSize(0))
                .body("data.summary", containsString("本日の未解決アラートは計 0 件です。"));
    }

    @Test
    void priority_withoutAuth_returns401() {
        AlertAssistantReq req = new AlertAssistantReq();
        req.setMessage(VALID_MESSAGE);

        given()
                .contentType(ContentType.JSON)
                .body(req)
                .when()
                .post(ENDPOINT)
                .then()
                .log().ifValidationFails()
                .statusCode(401);
    }

    private String createAccessToken() {
        UserDetails userDetails = new UserDetails();
        userDetails.setUserId(E2E_USER_ID);
        userDetails.setDeptId(1L);
        userDetails.setTenantId(E2E_TENANT_ID);
        userDetails.setUsername("alert-assistant-e2e");
        userDetails.setNickname("Alert Assistant E2E");
        userDetails.setStatus(1);
        userDetails.setCanSwitchTenant(false);
        userDetails.setRoleCodes(Set.of("USER"));

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                "",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        return tokenManager.generateToken(authentication).getAccessToken();
    }
}
