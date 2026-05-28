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

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Tenant Isolation Security Tests
 * <p>
 * Security-focused tests to verify that tenant data cannot be leaked
 * through various attack vectors.
 * </p>
 *
 * @author jason.w
 */
@SpringBootTest(
        classes = SmartDxApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.autoconfigure.exclude=org.redisson.spring.starter.RedissonAutoConfigurationV2",
                "tenant.force-default=false",
                "tenant.default-tenant-id=1"
        }
)
@ActiveProfiles("e2e")
@Testcontainers
@EnabledIfEnvironmentVariable(named = "E2E_TESTS_ENABLED", matches = "true",
        disabledReason = "E2E tests require Docker. Set E2E_TESTS_ENABLED=true to run.")
@DisplayName("Tenant Isolation Security Tests")
class TenantIsolationSecurityTest {

    private static final String AUTH_BASE_PATH = "/api/v1/auth";
    private static final String STORES_BASE_PATH = "/api/v1/retail/stores";
    private static final String PRODUCTS_BASE_PATH = "/api/v1/retail/products";
    private static final String CATEGORIES_BASE_PATH = "/api/v1/retail/categories";

    private static final long TENANT_1_ID = 1L;
    private static final long TENANT_2_ID = 2L;
    private static final long ATTACKER_TENANT_ID = 999L;

    private static final long VICTIM_USER_ID = 700001L;
    private static final String VICTIM_USERNAME = "e2e-victim-user";
    private static final long ATTACKER_USER_ID = 700002L;
    private static final String ATTACKER_USERNAME = "e2e-attacker-user";
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
    @DisplayName("Direct ID Access Attack Prevention")
    class DirectIdAccessAttackPrevention {

        @Test
        @DisplayName("cannot access other tenant's store by ID")
        void cannotAccessOtherTenantStore_byId() {
            // Create victim's store
            Long victimStoreId = createStoreForTenant(TENANT_1_ID, "VICTIM-STORE-001", "Victim Store");

            String attackerToken = loginAndGetToken(ATTACKER_USERNAME, TENANT_2_ID);

            // Attacker tries to access victim's store
            given()
                    .header("Authorization", "Bearer " + attackerToken)
                    .when()
                    .get(STORES_BASE_PATH + "/" + victimStoreId)
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("data", nullValue());
        }

        @Test
        @DisplayName("cannot update other tenant's store by ID")
        void cannotUpdateOtherTenantStore_byId() {
            Long victimStoreId = createStoreForTenant(TENANT_1_ID, "VICTIM-STORE-002", "Victim Store 2");
            String originalName = "Victim Store 2";

            String attackerToken = loginAndGetToken(ATTACKER_USERNAME, TENANT_2_ID);

            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + attackerToken)
                    .body("""
                            {
                              "storeCode": "HACKED",
                              "storeName": "Hacked by Attacker",
                              "status": "OFFLINE"
                            }
                            """)
                    .when()
                    .put(STORES_BASE_PATH + "/" + victimStoreId)
                    .then()
                    .log().ifValidationFails();

            // Verify store was NOT modified
            String storeName = jdbcTemplate.queryForObject(
                    "SELECT store_name FROM retail_store WHERE id = ?",
                    String.class, victimStoreId);
            assertThat(storeName).isEqualTo(originalName);
        }

        @Test
        @DisplayName("cannot delete other tenant's store by ID")
        void cannotDeleteOtherTenantStore_byId() {
            Long victimStoreId = createStoreForTenant(TENANT_1_ID, "VICTIM-STORE-003", "Victim Store 3");

            String attackerToken = loginAndGetToken(ATTACKER_USERNAME, TENANT_2_ID);

            given()
                    .header("Authorization", "Bearer " + attackerToken)
                    .when()
                    .delete(STORES_BASE_PATH + "/" + victimStoreId)
                    .then()
                    .log().ifValidationFails();

            // Verify store was NOT deleted
            Integer isDeleted = jdbcTemplate.queryForObject(
                    "SELECT is_deleted FROM retail_store WHERE id = ?",
                    Integer.class, victimStoreId);
            assertThat(isDeleted).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Data Enumeration Attack Prevention")
    class DataEnumerationAttackPrevention {

        @Test
        @DisplayName("listing stores does not reveal other tenant's data")
        void listingStores_doesNotRevealOtherTenantData() {
            // Create stores for multiple tenants
            createStoreForTenant(TENANT_1_ID, "ENUM-T1-001", "Tenant 1 Confidential Store");
            createStoreForTenant(TENANT_1_ID, "ENUM-T1-002", "Tenant 1 Secret Store");
            createStoreForTenant(TENANT_2_ID, "ENUM-T2-001", "Tenant 2 Store");

            String attackerToken = loginAndGetToken(ATTACKER_USERNAME, TENANT_2_ID);

            Response response = given()
                    .header("Authorization", "Bearer " + attackerToken)
                    .when()
                    .get(STORES_BASE_PATH);

            response.then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("code", equalTo("00000"));

            String responseBody = response.getBody().asString();
            assertThat(responseBody).doesNotContain("ENUM-T1-001");
            assertThat(responseBody).doesNotContain("ENUM-T1-002");
            assertThat(responseBody).doesNotContain("Confidential");
            assertThat(responseBody).doesNotContain("Secret");
        }

        @Test
        @DisplayName("sequential ID probing does not reveal tenant data")
        void sequentialIdProbing_doesNotRevealTenantData() {
            // Create stores with sequential IDs for tenant 1
            for (int i = 0; i < 5; i++) {
                createStoreForTenant(TENANT_1_ID, "PROBE-T1-" + i, "Hidden Store " + i);
            }

            String attackerToken = loginAndGetToken(ATTACKER_USERNAME, TENANT_2_ID);

            // Probe sequential IDs
            List<Map<String, Object>> stores = jdbcTemplate.queryForList(
                    "SELECT id FROM retail_store WHERE store_code LIKE 'PROBE-T1-%'");

            for (Map<String, Object> store : stores) {
                Long storeId = (Long) store.get("id");

                Response response = given()
                        .header("Authorization", "Bearer " + attackerToken)
                        .when()
                        .get(STORES_BASE_PATH + "/" + storeId);

                response.then()
                        .body("data", nullValue());

                String responseBody = response.getBody().asString();
                assertThat(responseBody).doesNotContain("PROBE-T1-");
                assertThat(responseBody).doesNotContain("Hidden Store");
            }
        }
    }

    @Nested
    @DisplayName("Cross-Tenant Reference Attack Prevention")
    class CrossTenantReferenceAttackPrevention {

        @Test
        @DisplayName("cannot create store with wrong tenant_id in request")
        void cannotCreateStore_withWrongTenantIdInRequest() {
            String attackerToken = loginAndGetToken(ATTACKER_USERNAME, TENANT_2_ID);

            // Note: Even if request contains tenant_id, MyBatis-Plus handler should override it
            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + attackerToken)
                    .body("""
                            {
                              "storeCode": "CROSS-REF-001",
                              "storeName": "Cross Ref Attack Store",
                              "tenantId": 1,
                              "status": "ONLINE"
                            }
                            """)
                    .when()
                    .post(STORES_BASE_PATH)
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200);

            // Verify the store was created with attacker's tenant ID, not victim's
            Long tenantId = jdbcTemplate.queryForObject(
                    "SELECT tenant_id FROM retail_store WHERE store_code = ?",
                    Long.class, "CROSS-REF-001");

            // Should be TENANT_2_ID (attacker's tenant) or force-default tenant
            assertThat(tenantId).isNotEqualTo(TENANT_1_ID);
        }
    }

    @Nested
    @DisplayName("SQL Injection Prevention")
    class SqlInjectionPrevention {

        @Test
        @DisplayName("tenant filter cannot be bypassed with SQL injection in store code")
        void tenantFilter_cannotBeBypassed_withSqlInjectionInStoreCode() {
            createStoreForTenant(TENANT_1_ID, "SQLI-TARGET", "SQL Injection Target");

            String attackerToken = loginAndGetToken(ATTACKER_USERNAME, TENANT_2_ID);

            // Attempt SQL injection in path parameter
            given()
                    .header("Authorization", "Bearer " + attackerToken)
                    .when()
                    .get(STORES_BASE_PATH + "/1 OR tenant_id=1")
                    .then()
                    .log().ifValidationFails()
                    // Should not return victim's data or cause error
                    .statusCode(anyOf(equalTo(200), equalTo(400), equalTo(404)));

            // Verify no data was leaked via response
            // (This test mainly ensures the application handles the input safely)
        }

        @Test
        @DisplayName("malicious store name does not affect tenant filtering")
        void maliciousStoreName_doesNotAffectTenantFiltering() {
            String attackerToken = loginAndGetToken(ATTACKER_USERNAME, TENANT_2_ID);

            // Create store with SQL-like content in name
            given()
                    .contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + attackerToken)
                    .body("""
                            {
                              "storeCode": "SQLI-NAME-001",
                              "storeName": "'; DELETE FROM retail_store WHERE '1'='1",
                              "status": "ONLINE"
                            }
                            """)
                    .when()
                    .post(STORES_BASE_PATH)
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200);

            // Verify other stores still exist (SQL injection didn't work)
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM retail_store",
                    Integer.class);
            assertThat(count).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Concurrent Access Isolation")
    class ConcurrentAccessIsolation {

        @Test
        @DisplayName("concurrent requests from different tenants are isolated")
        void concurrentRequests_fromDifferentTenants_areIsolated() throws InterruptedException {
            // Create stores for both tenants
            createStoreForTenant(TENANT_1_ID, "CONCURRENT-T1", "Tenant 1 Concurrent Store");
            createStoreForTenant(TENANT_2_ID, "CONCURRENT-T2", "Tenant 2 Concurrent Store");

            String victimToken = loginAndGetToken(VICTIM_USERNAME, TENANT_1_ID);
            String attackerToken = loginAndGetToken(ATTACKER_USERNAME, TENANT_2_ID);

            // Execute concurrent requests
            Thread victimThread = new Thread(() -> {
                for (int i = 0; i < 10; i++) {
                    Response response = given()
                            .header("Authorization", "Bearer " + victimToken)
                            .when()
                            .get(STORES_BASE_PATH);

                    String body = response.getBody().asString();
                    assertThat(body).contains("CONCURRENT-T1");
                    assertThat(body).doesNotContain("CONCURRENT-T2");
                }
            });

            Thread attackerThread = new Thread(() -> {
                for (int i = 0; i < 10; i++) {
                    Response response = given()
                            .header("Authorization", "Bearer " + attackerToken)
                            .when()
                            .get(STORES_BASE_PATH);

                    String body = response.getBody().asString();
                    assertThat(body).contains("CONCURRENT-T2");
                    assertThat(body).doesNotContain("CONCURRENT-T1");
                }
            });

            victimThread.start();
            attackerThread.start();
            victimThread.join();
            attackerThread.join();
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

    private Long createStoreForTenant(Long tenantId, String storeCode, String storeName) {
        jdbcTemplate.update(
                "INSERT INTO retail_store (tenant_id, store_code, store_name, status, is_deleted) VALUES (?, ?, ?, 'ONLINE', 0)",
                tenantId, storeCode, storeName);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM retail_store WHERE store_code = ?",
                Long.class, storeCode);
    }

    private void seedTestData() {
        // Ensure tenants exist (sys_tenant is the single source of truth)
        jdbcTemplate.update("""
                INSERT INTO sys_tenant (id, name, code, status, create_time, update_time)
                VALUES (1, 'Default Tenant', 'DEFAULT', 1, NOW(), NOW())
                ON DUPLICATE KEY UPDATE status = 1
                """);
        jdbcTemplate.update("""
                INSERT INTO sys_tenant (id, name, code, status, create_time, update_time)
                VALUES (2, 'Second Tenant', 'TENANT2', 1, NOW(), NOW())
                ON DUPLICATE KEY UPDATE status = 1
                """);

        // Create test users
        String encodedPassword = passwordEncoder.encode(TEST_PASSWORD);
        jdbcTemplate.update("""
                INSERT INTO sys_user (id, tenant_id, username, password, nickname, status, can_switch_tenant, is_deleted)
                VALUES (?, ?, ?, ?, 'Victim User', 1, 0, 0)
                ON DUPLICATE KEY UPDATE password = VALUES(password), is_deleted = 0
                """, VICTIM_USER_ID, TENANT_1_ID, VICTIM_USERNAME, encodedPassword);

        jdbcTemplate.update("""
                INSERT INTO sys_user (id, tenant_id, username, password, nickname, status, can_switch_tenant, is_deleted)
                VALUES (?, ?, ?, ?, 'Attacker User', 1, 0, 0)
                ON DUPLICATE KEY UPDATE password = VALUES(password), is_deleted = 0
                """, ATTACKER_USER_ID, TENANT_2_ID, ATTACKER_USERNAME, encodedPassword);
    }

    private void cleanupTestData() {
        // Clean up test stores
        jdbcTemplate.update("DELETE FROM retail_store WHERE store_code LIKE 'VICTIM-%'");
        jdbcTemplate.update("DELETE FROM retail_store WHERE store_code LIKE 'ENUM-%'");
        jdbcTemplate.update("DELETE FROM retail_store WHERE store_code LIKE 'PROBE-%'");
        jdbcTemplate.update("DELETE FROM retail_store WHERE store_code LIKE 'CROSS-REF-%'");
        jdbcTemplate.update("DELETE FROM retail_store WHERE store_code LIKE 'SQLI-%'");
        jdbcTemplate.update("DELETE FROM retail_store WHERE store_code LIKE 'CONCURRENT-%'");

        // Clean up test users
        jdbcTemplate.update("DELETE FROM sys_user WHERE id IN (?, ?)", VICTIM_USER_ID, ATTACKER_USER_ID);

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
