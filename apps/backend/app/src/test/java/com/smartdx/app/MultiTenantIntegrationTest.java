package com.smartdx.app;

import com.github.xiaoymin.knife4j.spring.configuration.Knife4jProperties;
import com.redis.testcontainers.RedisContainer;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
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
 * Multi-Tenant Integration Tests
 * <p>
 * Verifies that tenant isolation works correctly at the database level.
 * Tests CRUD operations with MyBatis-Plus TenantLineHandler.
 * </p>
 *
 * @author jason.w
 */
@SpringBootTest(
        classes = SmartDxApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=org.redisson.spring.starter.RedissonAutoConfigurationV2",
                "tenant.force-default=true",
                "tenant.default-tenant-id=1"
        }
)
@ActiveProfiles("e2e")
@Testcontainers
@EnabledIfEnvironmentVariable(named = "E2E_TESTS_ENABLED", matches = "true",
        disabledReason = "E2E tests require Docker. Set E2E_TESTS_ENABLED=true to run.")
@DisplayName("Multi-Tenant Integration Tests")
class MultiTenantIntegrationTest {

    private static final String AUTH_BASE_PATH = "/api/v1/auth";
    private static final String STORES_BASE_PATH = "/api/v1/retail/stores";

    private static final long TENANT_1_ID = 1L;
    private static final long TENANT_2_ID = 2L;

    private static final long USER_1_ID = 800001L;
    private static final String USER_1_USERNAME = "e2e-tenant1-user";
    private static final long USER_2_ID = 800002L;
    private static final String USER_2_USERNAME = "e2e-tenant2-user";
    private static final String TEST_PASSWORD = "Test123456";

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
        cleanupTestData();
        seedTestData();
    }

    @AfterEach
    void tearDown() {
        RestAssured.reset();
        cleanupTestData();
    }

    @Nested
    @DisplayName("Tenant Data Setup")
    class TenantDataSetup {

        @Test
        @DisplayName("tenant table has default tenant")
        void tenantTable_hasDefaultTenant() {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys_tenant WHERE id = ?",
                    Integer.class, TENANT_1_ID);

            org.assertj.core.api.Assertions.assertThat(count).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("can create second tenant")
        void canCreateSecondTenant() {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys_tenant WHERE id = ?",
                    Integer.class, TENANT_2_ID);

            org.assertj.core.api.Assertions.assertThat(count).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Store CRUD with Tenant Isolation")
    class StoreCrudWithTenantIsolation {

        @Test
        @DisplayName("creating store sets tenant_id automatically")
        void creatingStore_setsTenantIdAutomatically() {
            String accessToken = loginAndGetToken(USER_1_USERNAME, TENANT_1_ID);

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + accessToken)
                    .body("""
                            {
                              "storeCode": "STORE-MT-001",
                              "storeName": "MT Test Store 1",
                              "address": "Tokyo",
                              "status": "ONLINE"
                            }
                            """)
                    .when()
                    .post(STORES_BASE_PATH)
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("code", equalTo("00000"));

            // Verify tenant_id was set
            Long tenantId = jdbcTemplate.queryForObject(
                    "SELECT tenant_id FROM retail_store WHERE store_code = ?",
                    Long.class, "STORE-MT-001");

            org.assertj.core.api.Assertions.assertThat(tenantId).isEqualTo(TENANT_1_ID);
        }

        @Test
        @DisplayName("listing stores only returns current tenant data")
        void listingStores_onlyReturnsCurrentTenantData() {
            String accessToken = loginAndGetToken(USER_1_USERNAME, TENANT_1_ID);

            // Create stores for both tenants directly in DB
            jdbcTemplate.update(
                    "INSERT INTO retail_store (tenant_id, store_code, store_name, status, is_deleted) VALUES (?, ?, ?, ?, 0)",
                    TENANT_1_ID, "STORE-T1-001", "Tenant 1 Store", "ONLINE");
            jdbcTemplate.update(
                    "INSERT INTO retail_store (tenant_id, store_code, store_name, status, is_deleted) VALUES (?, ?, ?, ?, 0)",
                    TENANT_2_ID, "STORE-T2-001", "Tenant 2 Store", "ONLINE");

            // List stores - should only see tenant 1's store
            given()
                    .header("Authorization", "Bearer " + accessToken)
                    .when()
                    .get(STORES_BASE_PATH)
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("code", equalTo("00000"))
                    .body("data.findAll { it.storeCode == 'STORE-T1-001' }.size()", equalTo(1))
                    .body("data.findAll { it.storeCode == 'STORE-T2-001' }.size()", equalTo(0));
        }

        @Test
        @DisplayName("getting store by ID respects tenant isolation")
        void gettingStoreById_respectsTenantIsolation() {
            String accessToken = loginAndGetToken(USER_1_USERNAME, TENANT_1_ID);

            // Create store for tenant 2
            jdbcTemplate.update(
                    "INSERT INTO retail_store (id, tenant_id, store_code, store_name, status, is_deleted) VALUES (?, ?, ?, ?, ?, 0)",
                    99999L, TENANT_2_ID, "STORE-T2-HIDDEN", "Hidden Store", "ONLINE");

            // Try to get tenant 2's store - should not be accessible
            given()
                    .header("Authorization", "Bearer " + accessToken)
                    .when()
                    .get(STORES_BASE_PATH + "/99999")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("data", nullValue());
        }
    }

    @Nested
    @DisplayName("Query Tenant Filtering")
    class QueryTenantFiltering {

        @Test
        @DisplayName("SELECT queries include tenant_id condition")
        void selectQueries_includeTenantIdCondition() {
            // Insert test data for multiple tenants
            jdbcTemplate.update(
                    "INSERT INTO retail_store (tenant_id, store_code, store_name, status, is_deleted) VALUES (?, ?, ?, ?, 0)",
                    TENANT_1_ID, "QUERY-T1", "Query Test Store T1", "ONLINE");
            jdbcTemplate.update(
                    "INSERT INTO retail_store (tenant_id, store_code, store_name, status, is_deleted) VALUES (?, ?, ?, ?, 0)",
                    TENANT_2_ID, "QUERY-T2", "Query Test Store T2", "ONLINE");

            String accessToken = loginAndGetToken(USER_1_USERNAME, TENANT_1_ID);

            Response response = given()
                    .header("Authorization", "Bearer " + accessToken)
                    .when()
                    .get(STORES_BASE_PATH);

            response.then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("code", equalTo("00000"));

            // Verify no tenant 2 data is returned
            String responseBody = response.getBody().asString();
            org.assertj.core.api.Assertions.assertThat(responseBody).contains("QUERY-T1");
            org.assertj.core.api.Assertions.assertThat(responseBody).doesNotContain("QUERY-T2");
        }

        @Test
        @DisplayName("UPDATE queries are scoped to current tenant")
        void updateQueries_areScopedToCurrentTenant() {
            // Insert store for tenant 2
            jdbcTemplate.update(
                    "INSERT INTO retail_store (id, tenant_id, store_code, store_name, status, is_deleted) VALUES (?, ?, ?, ?, ?, 0)",
                    88888L, TENANT_2_ID, "UPDATE-T2", "Tenant 2 Update Store", "ONLINE");

            String accessToken = loginAndGetToken(USER_1_USERNAME, TENANT_1_ID);

            // Try to update tenant 2's store
            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + accessToken)
                    .body("""
                            {
                              "storeCode": "UPDATE-T2-MODIFIED",
                              "storeName": "Modified by Tenant 1",
                              "status": "OFFLINE"
                            }
                            """)
                    .when()
                    .put(STORES_BASE_PATH + "/88888")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(anyOf(equalTo(200), equalTo(400), equalTo(404)));

            // Verify the store was NOT modified
            String storeName = jdbcTemplate.queryForObject(
                    "SELECT store_name FROM retail_store WHERE id = ?",
                    String.class, 88888L);

            org.assertj.core.api.Assertions.assertThat(storeName).isEqualTo("Tenant 2 Update Store");
        }

        @Test
        @DisplayName("DELETE queries are scoped to current tenant")
        void deleteQueries_areScopedToCurrentTenant() {
            // Insert store for tenant 2
            jdbcTemplate.update(
                    "INSERT INTO retail_store (id, tenant_id, store_code, store_name, status, is_deleted) VALUES (?, ?, ?, ?, ?, 0)",
                    77777L, TENANT_2_ID, "DELETE-T2", "Tenant 2 Delete Store", "ONLINE");

            String accessToken = loginAndGetToken(USER_1_USERNAME, TENANT_1_ID);

            // Try to delete tenant 2's store
            given()
                    .header("Authorization", "Bearer " + accessToken)
                    .when()
                    .delete(STORES_BASE_PATH + "/77777")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(anyOf(equalTo(200), equalTo(400), equalTo(404)));

            // Verify the store was NOT deleted
            Integer isDeleted = jdbcTemplate.queryForObject(
                    "SELECT is_deleted FROM retail_store WHERE id = ?",
                    Integer.class, 77777L);

            org.assertj.core.api.Assertions.assertThat(isDeleted).isEqualTo(0);
        }
    }

    // =============================================
    // Helper Methods
    // =============================================

    private String loginAndGetToken(String username, Long tenantId) {
        Response loginResponse = given()
                .contentType(ContentType.JSON)
                .body(String.format("""
                        {
                          "username": "%s",
                          "password": "%s",
                          "tenantId": %d
                        }
                        """, username, TEST_PASSWORD, tenantId))
                .when()
                .post(AUTH_BASE_PATH + "/login");

        loginResponse.then().log().ifValidationFails().statusCode(200);
        return loginResponse.jsonPath().getString("data.accessToken");
    }

    private void seedTestData() {
        // Ensure default tenant exists (sys_tenant is the single source of truth)
        jdbcTemplate.update("""
                INSERT INTO sys_tenant (id, name, code, status, create_time, update_time)
                VALUES (1, 'Default Tenant', 'DEFAULT', 1, NOW(), NOW())
                ON DUPLICATE KEY UPDATE status = 1
                """);

        // Create second tenant
        jdbcTemplate.update("""
                INSERT INTO sys_tenant (id, name, code, status, create_time, update_time)
                VALUES (2, 'Second Tenant', 'TENANT2', 1, NOW(), NOW())
                ON DUPLICATE KEY UPDATE status = 1
                """);

        // Create test users
        String encodedPassword = passwordEncoder.encode(TEST_PASSWORD);
        jdbcTemplate.update("""
                INSERT INTO sys_user (id, tenant_id, username, password, nickname, status, can_switch_tenant, is_deleted)
                VALUES (?, ?, ?, ?, 'Tenant 1 User', 1, 0, 0)
                ON DUPLICATE KEY UPDATE password = VALUES(password), is_deleted = 0
                """, USER_1_ID, TENANT_1_ID, USER_1_USERNAME, encodedPassword);

        jdbcTemplate.update("""
                INSERT INTO sys_user (id, tenant_id, username, password, nickname, status, can_switch_tenant, is_deleted)
                VALUES (?, ?, ?, ?, 'Tenant 2 User', 1, 0, 0)
                ON DUPLICATE KEY UPDATE password = VALUES(password), is_deleted = 0
                """, USER_2_ID, TENANT_2_ID, USER_2_USERNAME, encodedPassword);
    }

    private void cleanupTestData() {
        // Clean up test stores
        jdbcTemplate.update("DELETE FROM retail_store WHERE store_code LIKE 'STORE-MT-%'");
        jdbcTemplate.update("DELETE FROM retail_store WHERE store_code LIKE 'STORE-T1-%'");
        jdbcTemplate.update("DELETE FROM retail_store WHERE store_code LIKE 'STORE-T2-%'");
        jdbcTemplate.update("DELETE FROM retail_store WHERE store_code LIKE 'QUERY-%'");
        jdbcTemplate.update("DELETE FROM retail_store WHERE store_code LIKE 'UPDATE-%'");
        jdbcTemplate.update("DELETE FROM retail_store WHERE store_code LIKE 'DELETE-%'");
        jdbcTemplate.update("DELETE FROM retail_store WHERE id IN (99999, 88888, 77777)");

        // Clean up test users
        jdbcTemplate.update("DELETE FROM sys_user WHERE id IN (?, ?)", USER_1_ID, USER_2_ID);

        // Clean up test tenant (keep default)
        jdbcTemplate.update("DELETE FROM sys_tenant WHERE id = ?", TENANT_2_ID);
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
