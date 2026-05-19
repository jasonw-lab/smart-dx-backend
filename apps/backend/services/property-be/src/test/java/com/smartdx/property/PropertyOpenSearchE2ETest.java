package com.smartdx.property;

import com.github.xiaoymin.knife4j.spring.configuration.Knife4jProperties;
import com.smartdx.security.model.UserDetails;
import com.smartdx.security.token.TokenManager;
import com.smartdx.property.config.PropertyOpenSearchProperties;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.DeleteByQueryRequest;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.indices.RefreshRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;

import com.smartdx.core.constant.RedisConstants;
import com.smartdx.core.constant.SystemConstants;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * E2E tests for property search API using OpenSearch.
 * Requires OpenSearch to be running at the configured endpoint.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("e2e")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PropertyOpenSearchE2ETest {

    private static final long E2E_TENANT_ID = Long.getLong("e2e.tenant-id", 1L);
    private static final long E2E_USER_ID = Long.getLong("e2e.user-id", 900001L);
    private static final String BASE_PATH = "/api/v1/properties";

    private static final String ES_LISTING_1 = "es-test-22222222-2222-4222-8222-900000000001";
    private static final String ES_LISTING_2 = "es-test-22222222-2222-4222-8222-900000000002";
    private static final String ES_LISTING_3 = "es-test-22222222-2222-4222-8222-900000000003";
    private static final String ES_LISTING_4 = "es-test-22222222-2222-4222-8222-900000000004";

    @Autowired
    TokenManager tokenManager;

    @Autowired
    PropertyOpenSearchProperties openSearchProperties;

    @Autowired(required = false)
    OpenSearchClient openSearchClient;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @LocalServerPort
    int port;

    String bearerToken;
    boolean openSearchAvailable = false;

    @BeforeAll
    void checkOpenSearchAvailability() {
        if (openSearchClient == null || !openSearchProperties.isEnabled()) {
            return;
        }
        try {
            var info = openSearchClient.info();
            openSearchAvailable = info.version() != null;
        } catch (Exception e) {
            openSearchAvailable = false;
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        RestAssured.port = port;
        bearerToken = "Bearer " + createAccessToken();
        // Set high QPS limit for E2E tests to avoid rate limiting
        redisTemplate.opsForHash().put(
                RedisConstants.System.CONFIG,
                SystemConstants.SYSTEM_CONFIG_IP_QPS_LIMIT_KEY,
                10000
        );

        if (openSearchAvailable) {
            cleanupOpenSearchTestData();
            seedOpenSearchData();
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        RestAssured.reset();
        if (openSearchAvailable) {
            cleanupOpenSearchTestData();
        }
    }

    @Test
    void lookupReturnsPublishedListingsFromOpenSearch() {
        assumeTrue(openSearchAvailable, "OpenSearch is not available");

        given()
                .header("Authorization", bearerToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "scope": "published",
                          "searchItems": {
                            "area": ["tokyo-shibuya"],
                            "priceJpyMax": 60000000,
                            "propertyType": ["mansion"]
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
                .body("data.total", greaterThanOrEqualTo(1))
                .body("data.list[0].propertyKey", equalTo(ES_LISTING_1))
                .body("data.list[0].scope", equalTo("published"))
                .body("data.list[0].area", equalTo("tokyo-shibuya"))
                .body("data.list[0].propertyType", equalTo("mansion"));
    }

    @Test
    void lookupSupportsKeywordSearchFromOpenSearch() {
        assumeTrue(openSearchAvailable, "OpenSearch is not available");

        given()
                .header("Authorization", bearerToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "scope": "published",
                          "keyword": "luxury penthouse",
                          "sortBy": "listedDate",
                          "orderBy": "desc",
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
                .body("data.total", greaterThanOrEqualTo(1))
                .body("data.list[0].propertyKey", equalTo(ES_LISTING_2));
    }

    @Test
    void lookupSupportsDraftScopeWithReviewStatusFilter() {
        assumeTrue(openSearchAvailable, "OpenSearch is not available");

        given()
                .header("Authorization", bearerToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "scope": "draft",
                          "searchItems": {
                            "reviewStatus": "PENDING"
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
                .body("data.total", greaterThanOrEqualTo(1))
                .body("data.list[0].reviewStatus", equalTo("PENDING"));
    }

    @Test
    void lookupSupportsPriceRangeFilter() {
        assumeTrue(openSearchAvailable, "OpenSearch is not available");

        given()
                .header("Authorization", bearerToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "scope": "published",
                          "searchItems": {
                            "priceJpyMin": 50000000,
                            "priceJpyMax": 70000000
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
                .body("data.total", greaterThanOrEqualTo(1));
    }

    @Test
    void lookupSupportsPriorityRankFilter() {
        assumeTrue(openSearchAvailable, "OpenSearch is not available");

        given()
                .header("Authorization", bearerToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "scope": "published",
                          "searchItems": {
                            "priorityRank": ["S", "A"]
                          },
                          "sortBy": "priorityRank",
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
                .body("data.total", greaterThanOrEqualTo(1));
    }

    @Test
    void lookupSupportsAllScopeCrossSearch() {
        assumeTrue(openSearchAvailable, "OpenSearch is not available");

        given()
                .header("Authorization", bearerToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "scope": "all",
                          "sortBy": "listedDate",
                          "orderBy": "desc",
                          "page": 0,
                          "size": 50
                        }
                        """)
                .when()
                .post(BASE_PATH + "/search")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("code", equalTo("00000"))
                .body("data.total", greaterThanOrEqualTo(3));
    }

    @Test
    void lookupReturnsEmptyForNoMatchingResults() {
        assumeTrue(openSearchAvailable, "OpenSearch is not available");

        given()
                .header("Authorization", bearerToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "scope": "published",
                          "searchItems": {
                            "area": ["nonexistent-area-xyz"]
                          },
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
                .body("data.total", equalTo(0))
                .body("data.list.size()", equalTo(0));
    }

    @Test
    void lookupSupportsPagination() {
        assumeTrue(openSearchAvailable, "OpenSearch is not available");

        given()
                .header("Authorization", bearerToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "scope": "published",
                          "sortBy": "listedDate",
                          "orderBy": "desc",
                          "page": 0,
                          "size": 1
                        }
                        """)
                .when()
                .post(BASE_PATH + "/search")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("code", equalTo("00000"))
                .body("data.list.size()", equalTo(1));
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

    private void seedOpenSearchData() throws IOException {
        String publishedIndex = openSearchProperties.getPublishedIndex();
        String draftIndex = openSearchProperties.getDraftIndex();

        indexDocument(publishedIndex, ES_LISTING_1, createPublishedListing(
                ES_LISTING_1, "tokyo-shibuya", "Tokyo Shibuya Mansion", "mansion", 55000000L,
                "A", "2LDK", 8, LocalDate.of(2026, 3, 15)
        ));

        indexDocument(publishedIndex, ES_LISTING_2, createPublishedListing(
                ES_LISTING_2, "tokyo-meguro", "Luxury Penthouse in Meguro", "mansion", 120000000L,
                "S", "4LDK", 5, LocalDate.of(2026, 3, 20)
        ));

        indexDocument(draftIndex, ES_LISTING_3, createDraftListing(
                ES_LISTING_3, "tokyo-shibuya", "Draft Property Shibuya", "house", 45000000L,
                "B", "PENDING", LocalDateTime.of(2026, 3, 18, 10, 0)
        ));

        indexDocument(draftIndex, ES_LISTING_4, createDraftListing(
                ES_LISTING_4, "tokyo-minato", "Draft Property Minato", "mansion", 78000000L,
                "A", "APPROVED", LocalDateTime.of(2026, 3, 19, 14, 30)
        ));

        refreshIndices(publishedIndex, draftIndex);
    }

    private Map<String, Object> createPublishedListing(String propertyKey, String area, String title,
            String propertyType, long priceJpy, String priorityRank, String layout,
            int stationWalkMin, LocalDate listedDate) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("propertyKey", propertyKey);
        doc.put("scope", "published");
        doc.put("version", 1);
        doc.put("tenantId", E2E_TENANT_ID);
        doc.put("area", area);
        doc.put("title", title);
        doc.put("address", "東京都" + area + "区テスト町1-2-3");
        doc.put("propertyType", propertyType);
        doc.put("priceJpy", priceJpy);
        doc.put("priorityRank", priorityRank);
        doc.put("layout", layout);
        doc.put("stationWalkMin", stationWalkMin);
        doc.put("listedDate", listedDate.toString());
        doc.put("registeredAt", LocalDateTime.now().minusDays(5).atOffset(ZoneOffset.ofHours(9)).toString());
        doc.put("reviewStatus", "APPROVED");

        Map<String, Object> registrant = new HashMap<>();
        registrant.put("userId", String.valueOf(E2E_USER_ID));
        registrant.put("displayName", "E2E Test User");
        doc.put("registrant", registrant);

        return doc;
    }

    private Map<String, Object> createDraftListing(String propertyKey, String area, String title,
            String propertyType, long priceJpy, String priorityRank, String reviewStatus,
            LocalDateTime registeredAt) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("propertyKey", propertyKey);
        doc.put("scope", "draft");
        doc.put("version", 1);
        doc.put("tenantId", E2E_TENANT_ID);
        doc.put("area", area);
        doc.put("title", title);
        doc.put("address", "東京都" + area + "区テスト町1-2-3");
        doc.put("propertyType", propertyType);
        doc.put("priceJpy", priceJpy);
        doc.put("priorityRank", priorityRank);
        doc.put("layout", "3LDK");
        doc.put("stationWalkMin", 10);
        doc.put("listedDate", LocalDate.now().minusDays(3).toString());
        doc.put("registeredAt", registeredAt.atOffset(ZoneOffset.ofHours(9)).toString());
        doc.put("reviewStatus", reviewStatus);

        Map<String, Object> registrant = new HashMap<>();
        registrant.put("userId", String.valueOf(E2E_USER_ID));
        registrant.put("displayName", "E2E Test User");
        doc.put("registrant", registrant);

        return doc;
    }

    private void indexDocument(String index, String id, Map<String, Object> document) throws IOException {
        IndexRequest<Map<String, Object>> request = IndexRequest.of(i -> i
                .index(index)
                .id(id)
                .document(document)
        );
        openSearchClient.index(request);
    }

    private void refreshIndices(String... indices) throws IOException {
        openSearchClient.indices().refresh(RefreshRequest.of(r -> r.index(List.of(indices))));
    }

    private void cleanupOpenSearchTestData() throws IOException {
        String publishedIndex = openSearchProperties.getPublishedIndex();
        String draftIndex = openSearchProperties.getDraftIndex();

        deleteByPrefix(publishedIndex, "es-test-");
        deleteByPrefix(draftIndex, "es-test-");

        refreshIndices(publishedIndex, draftIndex);
    }

    private void deleteByPrefix(String index, String prefix) {
        try {
            openSearchClient.deleteByQuery(DeleteByQueryRequest.of(d -> d
                    .index(index)
                    .query(q -> q.prefix(p -> p.field("propertyKey").value(prefix)))
            ));
        } catch (Exception e) {
            // Index may not exist, ignore
        }
    }

    private String createAccessToken() {
        UserDetails userDetails = new UserDetails();
        userDetails.setUserId(E2E_USER_ID);
        userDetails.setDeptId(1L);
        userDetails.setTenantId(E2E_TENANT_ID);
        userDetails.setUsername("property-opensearch-e2e");
        userDetails.setNickname("Property OpenSearch E2E");
        userDetails.setStatus(1);
        userDetails.setCanSwitchTenant(false);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                "",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        return tokenManager.generateToken(authentication).getAccessToken();
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
