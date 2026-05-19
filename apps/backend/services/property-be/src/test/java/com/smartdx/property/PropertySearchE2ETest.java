package com.smartdx.property;

import com.github.xiaoymin.knife4j.spring.configuration.Knife4jProperties;
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

import com.smartdx.core.constant.RedisConstants;
import com.smartdx.core.constant.SystemConstants;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "property.search.opensearch.enabled=false"
)
@ActiveProfiles("e2e")
class PropertySearchE2ETest {

    private static final long E2E_TENANT_ID = Long.getLong("e2e.tenant-id", 1L);
    private static final long E2E_USER_ID = Long.getLong("e2e.user-id", 900001L);
    private static final String BASE_PATH = "/api/v1/properties";
    private static final String LISTING_1 = "22222222-2222-4222-8222-900000000001";
    private static final String LISTING_2 = "22222222-2222-4222-8222-900000000002";
    private static final String LISTING_3 = "22222222-2222-4222-8222-900000000003";

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
        seedListings();
    }

    @AfterEach
    void tearDown() {
        RestAssured.reset();
        cleanupE2eRows();
    }

    @Test
    void lookupReturnsPublishedListingsWithAttributeFilters() {
        given()
                .header("Authorization", bearerToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "scope": "published",
                          "searchItems": {
                            "area": ["tokyo-shibuya"],
                            "priceJpyMax": 60000000,
                            "propertyType": ["mansion"],
                            "priorityRank": ["A"]
                          },
                          "sortBy": "priceJpy",
                          "orderBy": "asc",
                          "page": 0,
                          "size": 20
                        }
                        """)
                .when()
                .post(BASE_PATH + "/search")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("code", equalTo("00000"))
                .body("data.total", equalTo(1))
                .body("data.list[0].propertyKey", equalTo(LISTING_1))
                .body("data.list[0].scope", equalTo("published"))
                .body("data.list[0].area", equalTo("tokyo-shibuya"))
                .body("data.list[0].propertyType", equalTo("mansion"));
    }

    @Test
    void lookupSupportsKeywordAndDraftFiltersForCurrentRegistrant() {
        given()
                .header("Authorization", bearerToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "scope": "draft",
                          "keyword": "Draft",
                          "searchItems": {
                            "reviewStatus": "PENDING",
                            "registrantId": "me"
                          },
                          "sortBy": "registeredAt",
                          "orderBy": "desc",
                          "page": 0,
                          "size": 10
                        }
                        """)
                .when()
                .post(BASE_PATH + "/search")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("code", equalTo("00000"))
                .body("data.total", equalTo(1))
                .body("data.list[0].propertyKey", equalTo(LISTING_3))
                .body("data.list[0].reviewStatus", equalTo("PENDING"));
    }

    @Test
    void lookupReturnsEmptyPageForNoMatches() {
        given()
                .header("Authorization", bearerToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "scope": "published",
                          "searchItems": { "area": ["kanagawa-yokohama"] },
                          "page": 0,
                          "size": 20
                        }
                        """)
                .when()
                .post(BASE_PATH + "/search")
                .then()
                .statusCode(200)
                .body("code", equalTo("00000"))
                .body("data.total", equalTo(0))
                .body("data.list.size()", equalTo(0));
    }

    @Test
    void lookupRejectsInvalidPageSize() {
        given()
                .header("Authorization", bearerToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "scope": "published",
                          "page": 0,
                          "size": 1001
                        }
                        """)
                .when()
                .post(BASE_PATH + "/search")
                .then()
                .statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"));
    }

    @Test
    void lookupRequiresAuthentication() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "scope": "published",
                          "page": 0,
                          "size": 20
                        }
                        """)
                .when()
                .post(BASE_PATH + "/search")
                .then()
                .statusCode(greaterThanOrEqualTo(401));
    }

    // =============================================
    // LST-LST-01 物件詳細取得 API テスト
    // =============================================

    /**
     * QA-D-001: 存在する物件の詳細取得
     * 期待結果: 200 OK + 全フィールド返却
     */
    @Test
    void getDetailReturnsFullDetailWithPublishedScope() {
        given()
                .header("Authorization", bearerToken)
                .queryParam("scope", "published")
                .when()
                .get(BASE_PATH + "/{propertyId}", LISTING_1)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("code", equalTo("00000"))
                .body("data.propertyKey", equalTo(LISTING_1))
                .body("data.scope", equalTo("published"))
                .body("data.version", equalTo(1))
                .body("data.registrant.displayName", equalTo("Property E2E"))
                .body("data.meta.area", equalTo("tokyo-shibuya"))
                .body("data.meta.propertyType", equalTo("mansion"))
                .body("data.meta.priceJpy", org.hamcrest.Matchers.notNullValue())
                .body("data.meta.layout", equalTo("2LDK"))
                .body("data.review.status", equalTo("APPROVED"))
                .body("data.review.priorityRank", equalTo("A"))
                .body("data.permissions.canDownloadRaw", equalTo(true));
    }

    /**
     * QA-D-002: 存在しない propertyKey
     * 期待結果: 404 LISTING_NOT_FOUND
     */
    @Test
    void getDetailReturns404ForNonExistentListing() {
        given()
                .header("Authorization", bearerToken)
                .queryParam("scope", "published")
                .when()
                .get(BASE_PATH + "/{propertyId}", "00000000-0000-0000-0000-000000000000")
                .then()
                .log().ifValidationFails()
                .statusCode(404)
                .body("code", equalTo("LISTING_NOT_FOUND"));
    }

    /**
     * QA-D-003: scope=draft（reviewer/admin）
     * 期待結果: 200 OK + review 詳細含む
     */
    @Test
    void getDetailReturnsDraftForReviewer() {
        given()
                .header("Authorization", bearerToken)
                .queryParam("scope", "draft")
                .when()
                .get(BASE_PATH + "/{propertyId}", LISTING_3)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("code", equalTo("00000"))
                .body("data.propertyKey", equalTo(LISTING_3))
                .body("data.scope", equalTo("draft"))
                .body("data.review.status", equalTo("PENDING"))
                .body("data.permissions.canViewReviewMemo", equalTo(true))
                .body("data.permissions.canEditReviewMemo", equalTo(true));
    }

    /**
     * QA-D-005: scope パラメータ未指定
     * 期待結果: エラーコード A0410（必須パラメータ不足）
     */
    @Test
    void getDetailReturnsErrorWhenScopeMissing() {
        given()
                .header("Authorization", bearerToken)
                .when()
                .get(BASE_PATH + "/{propertyId}", LISTING_1)
                .then()
                .log().ifValidationFails()
                .statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"));
    }

    /**
     * QA-D-006: 画像なし物件
     * 期待結果: assets: []
     */
    @Test
    void getDetailReturnsEmptyAssetsWhenNoImages() {
        given()
                .header("Authorization", bearerToken)
                .queryParam("scope", "published")
                .when()
                .get(BASE_PATH + "/{propertyId}", LISTING_1)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("code", equalTo("00000"))
                .body("data.assets.size()", equalTo(0));
    }

    /**
     * QA-D-007: 文書なし物件
     * 期待結果: documents: []
     */
    @Test
    void getDetailReturnsEmptyDocumentsWhenNoDocs() {
        given()
                .header("Authorization", bearerToken)
                .queryParam("scope", "published")
                .when()
                .get(BASE_PATH + "/{propertyId}", LISTING_1)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("code", equalTo("00000"))
                .body("data.documents.size()", equalTo(0));
    }

    /**
     * QA-D-008: reviewer で機微文書確認
     * 期待結果: canViewSensitiveDocs: true
     */
    @Test
    void getDetailReturnsCanViewSensitiveDocsForReviewer() {
        given()
                .header("Authorization", bearerToken)
                .queryParam("scope", "published")
                .when()
                .get(BASE_PATH + "/{propertyId}", LISTING_1)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("code", equalTo("00000"))
                .body("data.permissions.canViewSensitiveDocs", equalTo(true));
    }

    /**
     * QA-D-013: 未ログインで取得
     * 期待結果: 401 UNAUTHORIZED
     */
    @Test
    void getDetailReturns401WhenUnauthenticated() {
        given()
                .queryParam("scope", "published")
                .when()
                .get(BASE_PATH + "/{propertyId}", LISTING_1)
                .then()
                .log().ifValidationFails()
                .statusCode(greaterThanOrEqualTo(401));
    }

    /**
     * QA-D-014: 審査未完了物件（scope=draft）
     * 期待結果: review.status: "PENDING", priorityRank: null or "S"
     */
    @Test
    void getDetailReturnsPendingReviewForDraft() {
        given()
                .header("Authorization", bearerToken)
                .queryParam("scope", "draft")
                .when()
                .get(BASE_PATH + "/{propertyId}", LISTING_3)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("code", equalTo("00000"))
                .body("data.review.status", equalTo("PENDING"));
    }

    /**
     * 不正な propertyKey 形式
     * 期待結果: エラーコード A0402（入力エラー）または A0404（物件不在）
     */
    @Test
    void getDetailReturnsErrorForInvalidListingKeyFormat() {
        given()
                .header("Authorization", bearerToken)
                .queryParam("scope", "published")
                .when()
                .get(BASE_PATH + "/{propertyId}", "invalid-key-format")
                .then()
                .log().ifValidationFails()
                // バリデーションまたは物件不在エラー
                .statusCode(org.hamcrest.Matchers.anyOf(
                        org.hamcrest.Matchers.equalTo(400),
                        org.hamcrest.Matchers.equalTo(404)
                ));
    }

    /**
     * 不正な scope 値
     * 期待結果: エラーコード A0402（入力エラー）
     */
    @Test
    void getDetailReturnsErrorForInvalidScope() {
        given()
                .header("Authorization", bearerToken)
                .queryParam("scope", "invalid")
                .when()
                .get(BASE_PATH + "/{propertyId}", LISTING_1)
                .then()
                .log().ifValidationFails()
                .statusCode(400)
                .body("code", equalTo("VALIDATION_ERROR"));
    }

    private void seedListings() {
        insertListing(LISTING_1, "published", "tokyo-shibuya", "Tokyo Shibuya E2E Search Mansion", "mansion", 57500000L, "A", "APPROVED");
        insertListing(LISTING_2, "published", "tokyo-meguro", "Tokyo Meguro E2E Search House", "house", 68000000L, "B", "APPROVED");
        insertListing(LISTING_3, "draft", "tokyo-shibuya", "Tokyo Shibuya E2E Search Draft", "mansion", 53000000L, "S", "PENDING");
    }

    private void insertListing(String propertyKey, String scope, String area, String address, String propertyType, Long priceJpy, String priorityRank, String reviewStatus) {
        jdbcTemplate.update(
                """
                        insert into property_listing(
                          tenant_id, property_key, scope, version, area, address, property_type, price_jpy,
                          layout, area_sqm, station_walk_min, built_year_month, listed_date, listed_year,
                          priority_rank, review_status, registrant_user_id, registrant_display_name,
                          registered_at, published_at, create_time, update_time, is_deleted
                        ) values (?, ?, ?, 1, ?, ?, ?, ?, '2LDK', 55.50, 7, '2019-04', ?, 2026,
                          ?, ?, ?, 'Property E2E', ?, ?, ?, ?, 0)
                        """,
                E2E_TENANT_ID,
                propertyKey,
                scope,
                area,
                address,
                propertyType,
                priceJpy,
                LocalDate.of(2026, 3, 20),
                priorityRank,
                reviewStatus,
                E2E_USER_ID,
                LocalDateTime.of(2026, 3, 20, 9, 0),
                "published".equals(scope) ? LocalDateTime.of(2026, 3, 21, 9, 0) : null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
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
        userDetails.setRoleCodes(Set.of("ADMIN", "REVIEWER"));

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                "",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        return tokenManager.generateToken(authentication).getAccessToken();
    }

    private void cleanupE2eRows() {
        jdbcTemplate.update(
                "delete from property_listing where tenant_id = ? and property_key in (?, ?, ?)",
                E2E_TENANT_ID,
                LISTING_1,
                LISTING_2,
                LISTING_3
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
