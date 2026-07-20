package com.smartdx.retail.e2e;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * ADR-011 の在庫 API 契約を固定する E2E テスト。
 * <p>
 * - GET /retail/inventories: ロット単位配列（現行 Vue 契約）
 * - GET /retail/inventory-summaries: 店舗×商品集約（新規）
 * - POST /retail/inventories/{id}/discard: 廃棄（新規）
 * </p>
 */
@Import(TestSecurityConfig.class)
class InventoryApiContractE2ETest extends RetailE2EBase {

    private static final String INVENTORIES_ENDPOINT = "/api/v1/retail/inventories";
    private static final String SUMMARIES_ENDPOINT = "/api/v1/retail/inventory-summaries";
    private static final String TRANSACTIONS_ENDPOINT = "/api/v1/retail/inventory-transactions";
    private static final long E2E_USER_ID = 900002L;
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
    void listInventories_returnsLotBasedArray() {
        // Given: 同一店舗・商品に 2 ロット
        Long storeId = seedStore("S-CON-001", "契約テスト店", "ONLINE");
        Long categoryId = seedCategory("CAT-CON-001", "契約カテゴリ");
        Long productId = seedProduct("P-CON-001", "契約商品", categoryId);
        seedInventory(storeId, productId, "LOT-CON-001", 10);
        seedInventory(storeId, productId, "LOT-CON-002", 20);

        // When / Then: ロット単位の配列が返る（Vue 契約の固定）
        given()
                .header("Authorization", bearerToken)
                .when()
                .get(INVENTORIES_ENDPOINT)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("code", equalTo("00000"))
                .body("data", hasSize(2))
                .body("data.id", notNullValue())
                .body("data.lotNumber", containsInAnyOrder("LOT-CON-001", "LOT-CON-002"))
                .body("data.quantity", containsInAnyOrder(10, 20));
    }

    @Test
    void listSummaries_aggregatesLotsByStoreAndProduct() {
        // Given: 同一店舗に商品A 2 ロット、商品B 1 ロット
        Long storeId = seedStore("S-CON-002", "集約テスト店", "ONLINE");
        Long categoryId = seedCategory("CAT-CON-002", "集約カテゴリ");
        Long productAId = seedProduct("P-CON-002A", "集約商品A", categoryId);
        Long productBId = seedProduct("P-CON-002B", "集約商品B", categoryId);
        seedInventory(storeId, productAId, "LOT-CON-101", 10);
        seedInventory(storeId, productAId, "LOT-CON-102", 20);
        seedInventory(storeId, productBId, "LOT-CON-201", 5);

        Long lotId1 = getInventoryId("LOT-CON-101");
        Long lotId2 = getInventoryId("LOT-CON-102");
        String summaryKey = storeId + "-" + productAId;

        // When / Then: 店舗×商品に集約される
        given()
                .header("Authorization", bearerToken)
                .when()
                .get(SUMMARIES_ENDPOINT)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("code", equalTo("00000"))
                .body("data", hasSize(2))
                .body("data.summaryKey", containsInAnyOrder(summaryKey, storeId + "-" + productBId))
                .body("data.find { it.summaryKey == '" + summaryKey + "' }.storeName", equalTo("集約テスト店"))
                .body("data.find { it.summaryKey == '" + summaryKey + "' }.productCode", equalTo("P-CON-002A"))
                .body("data.find { it.summaryKey == '" + summaryKey + "' }.productName", equalTo("集約商品A"))
                .body("data.find { it.summaryKey == '" + summaryKey + "' }.totalQuantity", equalTo(30))
                .body("data.find { it.summaryKey == '" + summaryKey + "' }.status", equalTo("NORMAL"))
                // lots[].id はロット（在庫レコード）ID と一致する
                .body("data.find { it.summaryKey == '" + summaryKey + "' }.lots", hasSize(2))
                .body("data.find { it.summaryKey == '" + summaryKey + "' }.lots.id",
                        containsInAnyOrder(lotId1.intValue(), lotId2.intValue()))
                .body("data.find { it.summaryKey == '" + summaryKey + "' }.lots.lotNumber",
                        containsInAnyOrder("LOT-CON-101", "LOT-CON-102"))
                // turnoverRate は要件未確定のため null
                .body("data.find { it.summaryKey == '" + summaryKey + "' }.turnoverRate", nullValue())
                // 集約行に数値 id は含めない
                .body("data.find { it.summaryKey == '" + summaryKey + "' }.id", nullValue());
    }

    @Test
    void listSummaries_filtersByStatus() {
        // Given: reorder_point=10 の商品に qty=5 のロット → LOW_STOCK
        Long storeId = seedStore("S-CON-003", "絞込テスト店", "ONLINE");
        Long categoryId = seedCategory("CAT-CON-003", "絞込カテゴリ");
        Long productId = seedProduct("P-CON-003", "絞込商品", categoryId);
        jdbcTemplate.update("UPDATE retail_product SET reorder_point = 10 WHERE id = ?", productId);
        seedInventory(storeId, productId, "LOT-CON-301", 5);

        given()
                .header("Authorization", bearerToken)
                .queryParam("status", "LOW_STOCK")
                .when()
                .get(SUMMARIES_ENDPOINT)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("data", hasSize(1))
                .body("data[0].summaryKey", equalTo(storeId + "-" + productId))
                .body("data[0].reorderPoint", equalTo(10))
                .body("data[0].status", equalTo("LOW_STOCK"));

        given()
                .header("Authorization", bearerToken)
                .queryParam("status", "NORMAL")
                .when()
                .get(SUMMARIES_ENDPOINT)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("data", hasSize(0));
    }

    @Test
    void discard_decreasesQuantityAndRecordsDisposalWithReason() {
        // Given: qty=10 のロット
        Long storeId = seedStore("S-CON-004", "廃棄テスト店", "ONLINE");
        Long categoryId = seedCategory("CAT-CON-004", "廃棄カテゴリ");
        Long productId = seedProduct("P-CON-004", "廃棄商品", categoryId);
        seedInventory(storeId, productId, "LOT-CON-401", 10);
        Long inventoryId = getInventoryId("LOT-CON-401");

        Map<String, Object> body = new HashMap<>();
        body.put("quantity", 3);
        body.put("reason", "期限切れ");
        body.put("remarks", "棚卸時に確認");

        // When
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", bearerToken)
                .body(body)
                .when()
                .post(INVENTORIES_ENDPOINT + "/" + inventoryId + "/discard")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("code", equalTo("00000"));

        // Then: ロット数量が減算されている
        Integer remaining = jdbcTemplate.queryForObject(
                "SELECT quantity FROM retail_inventory WHERE id = ?", Integer.class, inventoryId);
        assertThat(remaining).isEqualTo(7);

        // Then: reason 付きの DISPOSAL 履歴が登録されている
        given()
                .header("Authorization", bearerToken)
                .when()
                .get(TRANSACTIONS_ENDPOINT)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .body("data", hasSize(1))
                .body("data[0].txnType", equalTo("DISPOSAL"))
                .body("data[0].quantityDelta", equalTo(-3))
                .body("data[0].sourceType", equalTo("MANUAL"))
                .body("data[0].reason", equalTo("期限切れ"))
                .body("data[0].note", equalTo("棚卸時に確認"))
                .body("data[0].inventoryId", equalTo(inventoryId.intValue()))
                .body("data[0].lotNumber", equalTo("LOT-CON-401"));
    }

    @Test
    void discard_withoutQuantity_returns400() {
        Long storeId = seedStore("S-CON-005", "廃棄400店", "ONLINE");
        Long categoryId = seedCategory("CAT-CON-005", "廃棄400カテゴリ");
        Long productId = seedProduct("P-CON-005", "廃棄400商品", categoryId);
        seedInventory(storeId, productId, "LOT-CON-501", 10);
        Long inventoryId = getInventoryId("LOT-CON-501");

        Map<String, Object> body = new HashMap<>();
        body.put("reason", "期限切れ");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", bearerToken)
                .body(body)
                .when()
                .post(INVENTORIES_ENDPOINT + "/" + inventoryId + "/discard")
                .then()
                .log().ifValidationFails()
                .statusCode(400)
                .body("code", equalTo("A0400"));
    }

    @Test
    void discard_withZeroQuantity_returns400() {
        Long storeId = seedStore("S-CON-006", "廃棄0店", "ONLINE");
        Long categoryId = seedCategory("CAT-CON-006", "廃棄0カテゴリ");
        Long productId = seedProduct("P-CON-006", "廃棄0商品", categoryId);
        seedInventory(storeId, productId, "LOT-CON-601", 10);
        Long inventoryId = getInventoryId("LOT-CON-601");

        Map<String, Object> body = new HashMap<>();
        body.put("quantity", 0);
        body.put("reason", "期限切れ");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", bearerToken)
                .body(body)
                .when()
                .post(INVENTORIES_ENDPOINT + "/" + inventoryId + "/discard")
                .then()
                .log().ifValidationFails()
                .statusCode(400)
                .body("code", equalTo("A0400"));
    }

    @Test
    void discard_withoutReason_returns400() {
        Long storeId = seedStore("S-CON-007", "理由なし店", "ONLINE");
        Long categoryId = seedCategory("CAT-CON-007", "理由なしカテゴリ");
        Long productId = seedProduct("P-CON-007", "理由なし商品", categoryId);
        seedInventory(storeId, productId, "LOT-CON-701", 10);
        Long inventoryId = getInventoryId("LOT-CON-701");

        Map<String, Object> body = new HashMap<>();
        body.put("quantity", 1);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", bearerToken)
                .body(body)
                .when()
                .post(INVENTORIES_ENDPOINT + "/" + inventoryId + "/discard")
                .then()
                .log().ifValidationFails()
                .statusCode(400)
                .body("code", equalTo("A0400"));
    }

    @Test
    void discard_exceedingCurrentQuantity_returns409() {
        // Given: qty=10 のロットに 11 の廃棄要求
        Long storeId = seedStore("S-CON-008", "残量不足店", "ONLINE");
        Long categoryId = seedCategory("CAT-CON-008", "残量不足カテゴリ");
        Long productId = seedProduct("P-CON-008", "残量不足商品", categoryId);
        seedInventory(storeId, productId, "LOT-CON-801", 10);
        Long inventoryId = getInventoryId("LOT-CON-801");

        Map<String, Object> body = new HashMap<>();
        body.put("quantity", 11);
        body.put("reason", "期限切れ");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", bearerToken)
                .body(body)
                .when()
                .post(INVENTORIES_ENDPOINT + "/" + inventoryId + "/discard")
                .then()
                .log().ifValidationFails()
                .statusCode(409);

        // Then: 在庫は減算されず、履歴も登録されない
        Integer remaining = jdbcTemplate.queryForObject(
                "SELECT quantity FROM retail_inventory WHERE id = ?", Integer.class, inventoryId);
        assertThat(remaining).isEqualTo(10);
    }

    @Test
    void discard_unknownInventory_returns400() {
        Map<String, Object> body = new HashMap<>();
        body.put("quantity", 1);
        body.put("reason", "期限切れ");

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", bearerToken)
                .body(body)
                .when()
                .post(INVENTORIES_ENDPOINT + "/999999/discard")
                .then()
                .log().ifValidationFails()
                .statusCode(400);
    }

    private Long getInventoryId(String lotNumber) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM retail_inventory WHERE lot_number = ?",
                Long.class,
                lotNumber
        );
    }

    private String createAccessToken() {
        UserDetails userDetails = new UserDetails();
        userDetails.setUserId(E2E_USER_ID);
        userDetails.setDeptId(1L);
        userDetails.setTenantId(E2E_TENANT_ID);
        userDetails.setUsername("inventory-contract-e2e");
        userDetails.setNickname("Inventory Contract E2E");
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
