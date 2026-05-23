package com.smartdx.system;

import com.github.xiaoymin.knife4j.spring.configuration.Knife4jProperties;
import com.redis.testcontainers.RedisContainer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Auth API E2E Tests with TestContainers
 *
 * Tests authentication endpoints integrated into system-be:
 * - GET /api/v1/auth/captcha
 * - POST /api/v1/auth/login
 * - POST /api/v1/auth/logout
 * - POST /api/v1/auth/refresh-token
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.autoconfigure.exclude=org.redisson.spring.starter.RedissonAutoConfigurationV2"
)
@ActiveProfiles("e2e")
@Testcontainers
class AuthE2ETest {

    private static final String AUTH_BASE_PATH = "/api/v1/auth";
    private static final String USERS_BASE_PATH = "/api/v1/users";

    private static final long E2E_TENANT_ID = 1L;
    private static final long E2E_USER_ID = 900001L;
    private static final String E2E_USERNAME = "e2e-auth-test";
    private static final String E2E_PASSWORD = "Test123456";

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("smart_dx_db")
            .withUsername("root")
            .withPassword("test123");

    @Container
    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");
    }

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        cleanupE2eUser();
        seedE2eUser();
    }

    @AfterEach
    void tearDown() {
        RestAssured.reset();
        cleanupE2eUser();
    }

    // =============================================
    // Captcha API Tests
    // =============================================

    @Test
    void getCaptcha_returnsOk_withoutAuthentication() {
        given()
                .when()
                .get(AUTH_BASE_PATH + "/captcha")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("code", equalTo("00000"))
                .body("data.captchaId", notNullValue())
                .body("data.captchaBase64", notNullValue());
    }

    // =============================================
    // Login API Tests
    // =============================================

    @Test
    void login_returnsToken_withValidCredentials() {
        given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                        {
                          "username": "%s",
                          "password": "%s",
                          "tenantId": %d
                        }
                        """, E2E_USERNAME, E2E_PASSWORD, E2E_TENANT_ID))
                .when()
                .post(AUTH_BASE_PATH + "/login")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("code", equalTo("00000"))
                .body("data.accessToken", notNullValue())
                .body("data.refreshToken", notNullValue())
                .body("data.tokenType", equalTo("Bearer"));
    }

    @Test
    void login_returnsError_withInvalidPassword() {
        given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                        {
                          "username": "%s",
                          "password": "wrongpassword",
                          "tenantId": %d
                        }
                        """, E2E_USERNAME, E2E_TENANT_ID))
                .when()
                .post(AUTH_BASE_PATH + "/login")
                .then()
                .log().ifValidationFails()
                .statusCode(anyOf(equalTo(400), equalTo(401), equalTo(500)))
                .body("code", not(equalTo("00000")));
    }

    @Test
    void login_returnsError_withNonExistentUser() {
        given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                        {
                          "username": "nonexistent-user",
                          "password": "password",
                          "tenantId": %d
                        }
                        """, E2E_TENANT_ID))
                .when()
                .post(AUTH_BASE_PATH + "/login")
                .then()
                .log().ifValidationFails()
                .statusCode(anyOf(equalTo(400), equalTo(401), equalTo(500)))
                .body("code", not(equalTo("00000")));
    }

    @Test
    void login_returnsError_withMissingUsername() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "password": "password"
                        }
                        """)
                .when()
                .post(AUTH_BASE_PATH + "/login")
                .then()
                .log().ifValidationFails()
                .statusCode(anyOf(equalTo(400), equalTo(401)));
    }

    // =============================================
    // Refresh Token API Tests
    // =============================================

    @Test
    void refreshToken_returnsNewToken_withValidRefreshToken() {
        // First login to get refresh token
        Response loginResponse = given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                        {
                          "username": "%s",
                          "password": "%s",
                          "tenantId": %d
                        }
                        """, E2E_USERNAME, E2E_PASSWORD, E2E_TENANT_ID))
                .when()
                .post(AUTH_BASE_PATH + "/login");

        String refreshToken = loginResponse.jsonPath().getString("data.refreshToken");

        // Use refresh token to get new access token
        given()
                .queryParam("refreshToken", refreshToken)
                .when()
                .post(AUTH_BASE_PATH + "/refresh-token")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("code", equalTo("00000"))
                .body("data.accessToken", notNullValue())
                .body("data.refreshToken", notNullValue());
    }

    // =============================================
    // Logout API Tests
    // =============================================

    @Test
    void logout_returnsOk_withValidToken() {
        // First login to get access token
        Response loginResponse = given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                        {
                          "username": "%s",
                          "password": "%s",
                          "tenantId": %d
                        }
                        """, E2E_USERNAME, E2E_PASSWORD, E2E_TENANT_ID))
                .when()
                .post(AUTH_BASE_PATH + "/login");

        String accessToken = loginResponse.jsonPath().getString("data.accessToken");

        // Logout
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .post(AUTH_BASE_PATH + "/logout")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("code", equalTo("00000"));
    }

    // =============================================
    // Security Integration Tests
    // =============================================

    @Test
    void protectedEndpoint_returns401_withoutAuthentication() {
        given()
                .when()
                .get(USERS_BASE_PATH)
                .then()
                .log().ifValidationFails()
                .statusCode(greaterThanOrEqualTo(401));
    }

    @Test
    void tokenIsValidAfterLogin() {
        // First login to get access token
        Response loginResponse = given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                        {
                          "username": "%s",
                          "password": "%s",
                          "tenantId": %d
                        }
                        """, E2E_USERNAME, E2E_PASSWORD, E2E_TENANT_ID))
                .when()
                .post(AUTH_BASE_PATH + "/login");

        loginResponse.then().log().ifValidationFails().statusCode(200);

        String accessToken = loginResponse.jsonPath().getString("data.accessToken");

        // Logout succeeds with valid token (proves token is valid)
        given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .post(AUTH_BASE_PATH + "/logout")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("code", equalTo("00000"));
    }

    // =============================================
    // Helper Methods
    // =============================================

    private void seedE2eUser() {
        String encodedPassword = passwordEncoder.encode(E2E_PASSWORD);
        jdbcTemplate.update(
                """
                INSERT INTO sys_user (id, tenant_id, username, password, nickname, status, can_switch_tenant, deleted)
                VALUES (?, ?, ?, ?, 'E2E Auth Test User', 1, 0, 0)
                ON DUPLICATE KEY UPDATE password = VALUES(password), deleted = 0
                """,
                E2E_USER_ID,
                E2E_TENANT_ID,
                E2E_USERNAME,
                encodedPassword
        );
    }

    private void cleanupE2eUser() {
        jdbcTemplate.update(
                "DELETE FROM sys_user WHERE id = ?",
                E2E_USER_ID
        );
    }

    @TestConfiguration
    static class E2eApiDocConfig {
        @Bean
        Knife4jProperties knife4jProperties() {
            Knife4jProperties properties = new Knife4jProperties();
            properties.setEnable(false);
            return properties;
        }
    }
}
