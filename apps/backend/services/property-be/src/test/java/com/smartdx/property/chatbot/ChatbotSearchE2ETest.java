package com.smartdx.property.chatbot;

import com.github.xiaoymin.knife4j.spring.configuration.Knife4jProperties;
import com.smartdx.core.constant.RedisConstants;
import com.smartdx.core.constant.SystemConstants;
import com.smartdx.property.chatbot.model.req.ChatbotSearchReq;
import com.smartdx.security.model.UserDetails;
import com.smartdx.security.token.TokenManager;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Chatbot検索API E2Eテスト
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "property.search.opensearch.enabled=false",
                "chatbot.llm.enabled=false",
                "chatbot.llm.provider=mock"
        }
)
@ActiveProfiles("e2e")
@DisplayName("Chatbot検索API E2Eテスト")
class ChatbotSearchE2ETest {

    private static final long E2E_TENANT_ID = Long.getLong("e2e.tenant-id", 1L);
    private static final long E2E_USER_ID = Long.getLong("e2e.user-id", 900001L);
    private static final String BASE_PATH = "/api/v1/properties/chatbot";
    private static final String LISTING_1 = "33333333-3333-4333-8333-900000000001";
    private static final String LISTING_2 = "33333333-3333-4333-8333-900000000002";
    private static final String LISTING_3 = "33333333-3333-4333-8333-900000000003";

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
    @DisplayName("単純な検索条件で検索できる")
    void searchWithSimpleCondition() {
        ChatbotSearchReq req = new ChatbotSearchReq();
        req.setMessage("渋谷で2LDK、15万以下で探して");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", bearerToken)
                .body(req)
                .when()
                .post(BASE_PATH + "/search")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("code", equalTo("00000"))
                .body("data.extractionMethod", equalTo("rule_based"))
                .body("data.llmUsed", equalTo(false))
                .body("data.extractedCondition.searchItems.area", hasItem("tokyo-shibuya"))
                .body("data.extractedCondition.keyword", equalTo("2LDK"))
                .body("data.extractedCondition.searchItems.priceJpyMax", equalTo(150000))
                .body("data.reply", containsString("渋谷"))
                .body("data.clarificationNeeded", equalTo(false));
    }

    @Test
    @DisplayName("0項目抽出時は確認質問を返す")
    void returnClarificationWhenNoCondition() {
        ChatbotSearchReq req = new ChatbotSearchReq();
        req.setMessage("物件を探しています");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", bearerToken)
                .body(req)
                .when()
                .post(BASE_PATH + "/search")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("code", equalTo("00000"))
                .body("data.clarificationNeeded", equalTo(true))
                .body("data.clarificationQuestion", notNullValue())
                .body("data.searchResults", nullValue());
    }

    @Test
    @DisplayName("複数エリアを指定して検索できる")
    void searchWithMultipleAreas() {
        ChatbotSearchReq req = new ChatbotSearchReq();
        req.setMessage("渋谷か新宿でマンション");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", bearerToken)
                .body(req)
                .when()
                .post(BASE_PATH + "/search")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("code", equalTo("00000"))
                .body("data.extractedCondition.searchItems.area", hasItems("tokyo-shibuya", "tokyo-shinjuku"))
                .body("data.extractedCondition.searchItems.propertyType", hasItem("mansion"));
    }

    @Test
    @DisplayName("駅徒歩条件で検索できる")
    void searchWithStationWalk() {
        ChatbotSearchReq req = new ChatbotSearchReq();
        req.setMessage("横浜で駅徒歩10分以内");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", bearerToken)
                .body(req)
                .when()
                .post(BASE_PATH + "/search")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("code", equalTo("00000"))
                .body("data.extractedCondition.searchItems.area", hasItem("kanagawa-yokohama"))
                .body("data.extractedCondition.searchItems.stationWalkMax", equalTo(10));
    }

    @Test
    @DisplayName("scopeがpublishedに強制される")
    void scopeForcedToPublished() {
        ChatbotSearchReq req = new ChatbotSearchReq();
        req.setMessage("渋谷でマンション");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", bearerToken)
                .body(req)
                .when()
                .post(BASE_PATH + "/search")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("data.extractedCondition.scope", equalTo("published"));
    }

    @Test
    @DisplayName("messageが空の場合は400エラー")
    void emptyMessageReturns400() {
        ChatbotSearchReq req = new ChatbotSearchReq();
        req.setMessage("");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", bearerToken)
                .body(req)
                .when()
                .post(BASE_PATH + "/search")
                .then()
                .log().ifValidationFails()
                .statusCode(400);
    }

    @Test
    @DisplayName("認証なしでは401エラー")
    void unauthorizedReturns401() {
        ChatbotSearchReq req = new ChatbotSearchReq();
        req.setMessage("渋谷でマンション");

        given()
                .contentType(ContentType.JSON)
                .body(req)
                .when()
                .post(BASE_PATH + "/search")
                .then()
                .log().ifValidationFails()
                .statusCode(greaterThanOrEqualTo(401));
    }

    @Test
    @DisplayName("曖昧表現を含む場合でもLLM無効時はrule_basedで検索")
    void ambiguousWithLlmDisabled() {
        ChatbotSearchReq req = new ChatbotSearchReq();
        req.setMessage("渋谷で安いマンション");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", bearerToken)
                .body(req)
                .when()
                .post(BASE_PATH + "/search")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("code", equalTo("00000"))
                .body("data.extractionMethod", equalTo("rule_based"))
                .body("data.llmUsed", equalTo(false))
                .body("data.extractedCondition.searchItems.area", hasItem("tokyo-shibuya"))
                .body("data.extractedCondition.searchItems.propertyType", hasItem("mansion"));
    }

    @Test
    @DisplayName("検索結果にlist/total形式が含まれる")
    void responseContainsListAndTotal() {
        ChatbotSearchReq req = new ChatbotSearchReq();
        req.setMessage("マンション");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", bearerToken)
                .body(req)
                .when()
                .post(BASE_PATH + "/search")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("data.searchResults.list", notNullValue())
                .body("data.searchResults.total", notNullValue());
    }

    @Test
    @DisplayName("駅近キーワードで5分以内に変換")
    void searchWithEkichikaKeyword() {
        ChatbotSearchReq req = new ChatbotSearchReq();
        req.setMessage("渋谷で駅近のマンション");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", bearerToken)
                .body(req)
                .when()
                .post(BASE_PATH + "/search")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("code", equalTo("00000"))
                .body("data.extractedCondition.searchItems.stationWalkMax", equalTo(5));
    }

    @Test
    @DisplayName("価格億単位を正しく解釈")
    void searchWithPriceInOku() {
        ChatbotSearchReq req = new ChatbotSearchReq();
        req.setMessage("渋谷で1億以下のマンション");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", bearerToken)
                .body(req)
                .when()
                .post(BASE_PATH + "/search")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("code", equalTo("00000"))
                .body("data.extractedCondition.searchItems.priceJpyMax", equalTo(100000000));
    }

    private void seedListings() {
        insertListing(LISTING_1, "published", "tokyo-shibuya", "Tokyo Shibuya E2E Chatbot Mansion", "mansion", 57500000L, "A", "APPROVED", 7);
        insertListing(LISTING_2, "published", "tokyo-shinjuku", "Tokyo Shinjuku E2E Chatbot House", "house", 68000000L, "B", "APPROVED", 10);
        insertListing(LISTING_3, "published", "kanagawa-yokohama", "Yokohama E2E Chatbot Mansion", "mansion", 85000000L, "B", "APPROVED", 5);
    }

    private void insertListing(String propertyKey, String scope, String area, String address, String propertyType, Long priceJpy, String priorityRank, String reviewStatus, Integer stationWalkMin) {
        jdbcTemplate.update(
                """
                        insert into property_listing(
                          tenant_id, property_key, scope, version, area, address, property_type, price_jpy,
                          layout, area_sqm, station_walk_min, built_year_month, listed_date, listed_year,
                          priority_rank, review_status, registrant_user_id, registrant_display_name,
                          registered_at, published_at, create_time, update_time, is_deleted
                        ) values (?, ?, ?, 1, ?, ?, ?, ?, '2LDK', 55.50, ?, '2019-04', ?, 2026,
                          ?, ?, ?, 'Chatbot E2E', ?, ?, ?, ?, 0)
                        """,
                E2E_TENANT_ID,
                propertyKey,
                scope,
                area,
                address,
                propertyType,
                priceJpy,
                stationWalkMin,
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
        userDetails.setUsername("chatbot-e2e");
        userDetails.setNickname("Chatbot E2E");
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
