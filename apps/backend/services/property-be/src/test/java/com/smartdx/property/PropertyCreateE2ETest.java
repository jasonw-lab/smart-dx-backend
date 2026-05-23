package com.smartdx.property;

import com.github.xiaoymin.knife4j.spring.configuration.Knife4jProperties;
import com.smartdx.core.constant.RedisConstants;
import com.smartdx.core.constant.SystemConstants;
import com.smartdx.security.model.UserDetails;
import com.smartdx.security.token.TokenManager;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;

/**
 * E2E Tests for LST-INT-01: Property Create API
 * POST /api/v1/properties
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("e2e")
class PropertyCreateE2ETest {

    private static final long E2E_TENANT_ID = Long.getLong("e2e.tenant-id", 1L);
    private static final long E2E_USER_ID = Long.getLong("e2e.user-id", 900002L);
    private static final String BASE_PATH = "/api/v1/properties";

    @Autowired
    TokenManager tokenManager;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @LocalServerPort
    int port;

    String bearerToken;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        bearerToken = "Bearer " + createAccessToken();
        redisTemplate.opsForHash().put(
                RedisConstants.System.CONFIG,
                SystemConstants.SYSTEM_CONFIG_IP_QPS_LIMIT_KEY,
                10000
        );
        cleanupE2eRows();
    }

    @AfterEach
    void tearDown() {
        RestAssured.reset();
        cleanupE2eRows();
    }

    @Test
    void propertyCreate_acceptsCsvAndReturns202() {
        given()
                .header("Authorization", bearerToken)
                .multiPart("listedYear", "2026")
                .multiPart("area", "tokyo-shibuya")
                .multiPart("csv", "property.csv", simpleCsv().getBytes(StandardCharsets.UTF_8), "text/csv")
                .multiPart("photos[]", "main.jpg", "photo-content".getBytes(StandardCharsets.UTF_8), "image/jpeg")
                .multiPart("docs[]", "doc.pdf", "doc-content".getBytes(StandardCharsets.UTF_8), "application/pdf")
        .when()
                .post(BASE_PATH)
        .then()
                .log().ifValidationFails()
                .statusCode(202)
                .contentType(ContentType.JSON)
                .body("code", equalTo("00000"))
                .body("data.jobRef", matchesPattern("single-[0-9]{8}-[0-9]{4}"))
                .body("data.scope", equalTo("draft"))
                .body("data.propertyKey", matchesPattern("[a-f0-9-]{36}"))
                .body("data.receivedCount.records", equalTo(1))
                .body("data.receivedCount.photos", equalTo(1))
                .body("data.receivedCount.docs", equalTo(1));

        Integer jobCount = jdbcTemplate.queryForObject(
                "select count(*) from property_intake_job where tenant_id = ? and owner_user_id = ? and job_type = 'single'",
                Integer.class, E2E_TENANT_ID, E2E_USER_ID
        );
        assertThat(jobCount).isEqualTo(1);

        Integer outboxCount = jdbcTemplate.queryForObject(
                "select count(*) from outbox_event where tenant_id = ? and event_type = 'PROPERTY_INTAKE'",
                Integer.class, E2E_TENANT_ID
        );
        assertThat(outboxCount).isGreaterThanOrEqualTo(1);
    }

    @Test
    void propertyCreate_rejectsWithoutCsvOrExcel() {
        given()
                .header("Authorization", bearerToken)
                .multiPart("listedYear", "2026")
                .multiPart("area", "tokyo-shibuya")
                .multiPart("photos[]", "main.jpg", "photo-content".getBytes(StandardCharsets.UTF_8), "image/jpeg")
        .when()
                .post(BASE_PATH)
        .then()
                .log().ifValidationFails()
                .statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"))
                .body("msg", containsString("CSV または Excel"));
    }

    @Test
    void propertyCreate_rejectsInvalidPhotoFormat() {
        given()
                .header("Authorization", bearerToken)
                .multiPart("listedYear", "2026")
                .multiPart("area", "tokyo-shibuya")
                .multiPart("csv", "property.csv", simpleCsv().getBytes(StandardCharsets.UTF_8), "text/csv")
                .multiPart("photos[]", "main.gif", "photo-content".getBytes(StandardCharsets.UTF_8), "image/gif")
        .when()
                .post(BASE_PATH)
        .then()
                .log().ifValidationFails()
                .statusCode(400)
                .body("code", equalTo("INVALID_MIME"))
                .body("msg", containsString("JPG/JPEG/PNG"));
    }

    @Test
    void propertyCreate_rejectsWithoutListedYear() {
        given()
                .header("Authorization", bearerToken)
                .multiPart("area", "tokyo-shibuya")
                .multiPart("csv", "property.csv", simpleCsv().getBytes(StandardCharsets.UTF_8), "text/csv")
        .when()
                .post(BASE_PATH)
        .then()
                .log().ifValidationFails()
                .statusCode(400)
                .body("msg", notNullValue());
    }

    @Test
    void propertyCreate_rejectsWithoutAuthentication() {
        given()
                .multiPart("listedYear", "2026")
                .multiPart("area", "tokyo-shibuya")
                .multiPart("csv", "property.csv", simpleCsv().getBytes(StandardCharsets.UTF_8), "text/csv")
        .when()
                .post(BASE_PATH)
        .then()
                .log().ifValidationFails()
                .statusCode(401);
    }

    private String createAccessToken() {
        UserDetails userDetails = new UserDetails();
        userDetails.setUserId(E2E_USER_ID);
        userDetails.setDeptId(1L);
        userDetails.setTenantId(E2E_TENANT_ID);
        userDetails.setUsername("property-create-e2e");
        userDetails.setNickname("Property Create E2E");
        userDetails.setStatus(1);
        userDetails.setCanSwitchTenant(false);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                "",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        return tokenManager.generateToken(authentication).getAccessToken();
    }

    private void cleanupE2eRows() {
        jdbcTemplate.update(
                "delete from outbox_event where tenant_id = ? and aggregate_type = 'Property'",
                E2E_TENANT_ID
        );
        jdbcTemplate.update(
                "delete from property_intake_job where tenant_id = ? and owner_user_id = ?",
                E2E_TENANT_ID, E2E_USER_ID
        );
    }

    private static String simpleCsv() {
        return "area,address,propertyType,priceJpy,layout,areaSqm,stationWalkMin,builtYearMonth,listedDate\n" +
               "tokyo-shibuya,Tokyo Shibuya Test,mansion,55000000,2LDK,50.0,5,2020-01,2026-05-04\n";
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
