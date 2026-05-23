package com.smartdx.property;

import com.smartdx.security.model.UserDetails;
import com.smartdx.security.token.TokenManager;
import com.github.xiaoymin.knife4j.spring.configuration.Knife4jProperties;
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

import com.smartdx.core.constant.RedisConstants;
import com.smartdx.core.constant.SystemConstants;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("e2e")
class PropertyBulkImportE2ETest {

    private static final long E2E_TENANT_ID = Long.getLong("e2e.tenant-id", 1L);
    private static final long E2E_USER_ID = Long.getLong("e2e.user-id", 900001L);
    private static final String BASE_PATH = "/api/v1/property-import-jobs";
    private static final String LISTING_1 = "11111111-1111-4111-8111-900000000001";
    private static final String LISTING_2 = "11111111-1111-4111-8111-900000000002";
    private static final String INVALID_LISTING = "11111111-1111-4111-8111-900000000003";

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
        // Set high QPS limit for E2E tests to avoid rate limiting
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
    // Test points: multipart CSV/photos/docs acceptance, async SUCCESS completion, listing persistence, and job detail API response.
    void bulkImportAcceptsCsvAndCompletesSuccessfully() {
        String jobRef = given()
                .header("Authorization", bearerToken)
                .multiPart("listedYear", "2026")
                .multiPart("area", "tokyo-shibuya")
                .multiPart("csv", "e2e-success.csv", successCsv().getBytes(StandardCharsets.UTF_8), "text/csv")
                .multiPart("photos[]", "e2e-main-001.jpg", "main-001".getBytes(StandardCharsets.UTF_8), "image/jpeg")
                .multiPart("photos[]", "e2e-sub-001.jpg", "sub-001".getBytes(StandardCharsets.UTF_8), "image/jpeg")
                .multiPart("photos[]", "e2e-layout-001.png", "layout-001".getBytes(StandardCharsets.UTF_8), "image/png")
                .multiPart("photos[]", "e2e-main-002.jpg", "main-002".getBytes(StandardCharsets.UTF_8), "image/jpeg")
                .multiPart("docs[]", "e2e-doc-001.pdf", "doc-001".getBytes(StandardCharsets.UTF_8), "application/pdf")
                .multiPart("docs[]", "e2e-doc-002.pdf", "doc-002".getBytes(StandardCharsets.UTF_8), "application/pdf")
                .when()
                .post(BASE_PATH)
                .then()
                .log().ifValidationFails()
                .statusCode(202)
                .contentType(ContentType.JSON)
                .body("code", equalTo("00000"))
                .body("data.jobRef", matchesPattern("bulk-[0-9]{8}-[0-9]{4}"))
                .body("data.scope", equalTo("published"))
                .body("data.receivedCount.records", equalTo(2))
                .body("data.receivedCount.photos", equalTo(4))
                .body("data.receivedCount.docs", equalTo(2))
                .extract()
                .path("data.jobRef");

        Map<String, Object> job = waitForTerminalJob(jobRef, "SUCCESS");

        assertThat(job.get("status")).isEqualTo("SUCCESS");
        assertThat(((Number) job.get("succeeded_count")).intValue()).isEqualTo(2);
        assertThat(((Number) job.get("failed_count")).intValue()).isZero();

        Integer listingCount = jdbcTemplate.queryForObject(
                "select count(*) from property_listing where tenant_id = ? and property_key in (?, ?) and scope = 'published' and is_deleted = 0",
                Integer.class,
                E2E_TENANT_ID,
                LISTING_1,
                LISTING_2
        );
        assertThat(listingCount).isEqualTo(2);

        given()
                .header("Authorization", bearerToken)
                .when()
                .get(BASE_PATH + "/{jobId}", jobRef)
                .then()
                .statusCode(200)
                .body("code", equalTo("00000"))
                .body("data.jobRef", equalTo(jobRef))
                .body("data.status", equalTo("SUCCESS"))
                .body("data.succeeded", equalTo(2))
                .body("data.failed", equalTo(0))
                .body("data.total", equalTo(2));
    }

    @Test
    // Test points: one valid row and one missing attachment row produce PARTIAL_SUCCESS and expose the validation error via errors API.
    void bulkImportRecordsValidationErrorsForInvalidRows() {
        String jobRef = given()
                .header("Authorization", bearerToken)
                .multiPart("listedYear", "2026")
                .multiPart("area", "tokyo-shibuya")
                .multiPart("csv", "e2e-partial.csv", partialCsv().getBytes(StandardCharsets.UTF_8), "text/csv")
                .multiPart("photos[]", "e2e-main-001.jpg", "main-001".getBytes(StandardCharsets.UTF_8), "image/jpeg")
                .when()
                .post(BASE_PATH)
                .then()
                .log().ifValidationFails()
                .statusCode(202)
                .body("code", equalTo("00000"))
                .extract()
                .path("data.jobRef");

        Map<String, Object> job = waitForTerminalJob(jobRef, "PARTIAL_SUCCESS");

        assertThat(job.get("status")).isEqualTo("PARTIAL_SUCCESS");
        assertThat(((Number) job.get("succeeded_count")).intValue()).isEqualTo(1);
        assertThat(((Number) job.get("failed_count")).intValue()).isEqualTo(1);

        given()
                .header("Authorization", bearerToken)
                .when()
                .get(BASE_PATH + "/{jobId}/errors", jobRef)
                .then()
                .statusCode(200)
                .body("code", equalTo("00000"))
                .body("data.total", equalTo(1))
                .body("data.errors[0].rowNo", equalTo(3))
                .body("data.errors[0].errorCode", equalTo("ATTACHMENT_NOT_FOUND:e2e-missing.jpg"));
    }

    private Map<String, Object> waitForTerminalJob(String jobRef, String expectedStatus) {
        long deadline = System.nanoTime() + Duration.ofSeconds(90).toNanos();
        while (System.nanoTime() < deadline) {
            Map<String, Object> job = jdbcTemplate.queryForMap(
                    "select status, succeeded_count, failed_count, total_count from property_intake_job where tenant_id = ? and job_ref = ? and is_deleted = 0",
                    E2E_TENANT_ID,
                    jobRef
            );
            String status = String.valueOf(job.get("status"));
            if (List.of("SUCCESS", "PARTIAL_SUCCESS", "FAILED").contains(status)) {
                assertThat(status).isEqualTo(expectedStatus);
                return job;
            }
            sleepOneSecond();
        }
        throw new AssertionError("Timed out waiting for property intake job: " + jobRef);
    }

    private String createAccessToken() {
        UserDetails userDetails = new UserDetails();
        userDetails.setUserId(E2E_USER_ID);
        userDetails.setDeptId(1L);
        userDetails.setTenantId(E2E_TENANT_ID);
        userDetails.setUsername("property-e2e");
        userDetails.setNickname("Property E2E");
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
                "delete from property_intake_job_error where tenant_id = ? and job_ref in (" +
                        "select job_ref from property_intake_job where tenant_id = ? and owner_user_id = ?" +
                        ")",
                E2E_TENANT_ID,
                E2E_TENANT_ID,
                E2E_USER_ID
        );
        jdbcTemplate.update(
                "delete from property_intake_job where tenant_id = ? and owner_user_id = ?",
                E2E_TENANT_ID,
                E2E_USER_ID
        );
        jdbcTemplate.update(
                "delete from property_listing where tenant_id = ? and property_key in (?, ?, ?) and scope = 'published'",
                E2E_TENANT_ID,
                LISTING_1,
                LISTING_2,
                INVALID_LISTING
        );
    }

    private static String successCsv() {
        return csvHeader() +
                LISTING_1 + ",,tokyo-shibuya,Tokyo Shibuya 1,mansion,57500000,2LDK,55.5,,7,2019-04,2026-03-20,2026,A,E2E success 1,Rest Assured import,e2e-main-001.jpg,e2e-sub-001.jpg,e2e-layout-001.png,e2e-doc-001.pdf:summary\n" +
                LISTING_2 + ",,tokyo-shibuya,Tokyo Shibuya 2,house,68000000,3LDK,80.0,,11,2017-09,2026-03-21,2026,B,E2E success 2,Rest Assured import,e2e-main-002.jpg,,,e2e-doc-002.pdf:floor-plan\n";
    }

    private static String partialCsv() {
        return csvHeader() +
                LISTING_1 + ",,tokyo-shibuya,Tokyo Shibuya 1,mansion,57500000,2LDK,55.5,,7,2019-04,2026-03-20,2026,A,E2E valid,Rest Assured import,e2e-main-001.jpg,,,\n" +
                INVALID_LISTING + ",,tokyo-shibuya,Tokyo Shibuya 3,mansion,59000000,2LDK,58.0,,8,2020-01,2026-03-22,2026,C,E2E invalid,Missing attachment,e2e-missing.jpg,,,\n";
    }

    private static String csvHeader() {
        return "listingId,version,area,address,propertyType,priceJpy,layout,areaSqm,landSqm,stationWalkMin,builtYearMonth,listedDate,listedYear,priorityRank,title,description,mainPhoto,subPhotos,layoutPhoto,docs\n";
    }

    private static void sleepOneSecond() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for property intake job", e);
        }
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
