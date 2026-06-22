package com.smartdx.retail.e2e;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for retail domain E2E tests backed by Testcontainers.
 * <p>
 * Containers are managed as singletons (started once per JVM) so that the Spring
 * application context can be safely cached across test classes. If each class
 * started/stopped its own containers, the cached DataSource would point at a
 * port that no longer exists after the first class finishes.
 * </p>
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = com.smartdx.retail.RetailTestApplication.class
)
@ActiveProfiles("e2e")
public abstract class RetailE2EBase {

    static final MySQLContainer<?> mysql;
    static final RedisContainer redis;

    static {
        mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                .withDatabaseName("smart_dx_db")
                .withUsername("root")
                .withPassword("test123");
        mysql.start();

        redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));
        redis.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (redis.isRunning()) {
                redis.stop();
            }
            if (mysql.isRunning()) {
                mysql.stop();
            }
        }));
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");
    }

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void baseSetUp() {
        cleanRetailTables();
    }

    @AfterEach
    void baseTearDown() {
        cleanRetailTables();
    }

    private void cleanRetailTables() {
        jdbcTemplate.update("DELETE FROM retail_inventory_transaction");
        jdbcTemplate.update("DELETE FROM retail_inventory");
        jdbcTemplate.update("DELETE FROM retail_sales_detail");
        jdbcTemplate.update("DELETE FROM retail_sales");
        jdbcTemplate.update("DELETE FROM retail_alert");
        jdbcTemplate.update("DELETE FROM retail_device");
        jdbcTemplate.update("DELETE FROM retail_product");
        jdbcTemplate.update("DELETE FROM retail_store");
        jdbcTemplate.update("DELETE FROM retail_category");
    }

    protected Long seedStore(String storeCode, String storeName, String status) {
        jdbcTemplate.update(
                """
                INSERT INTO retail_store (tenant_id, store_code, store_name, status)
                VALUES (1, ?, ?, ?)
                """,
                storeCode, storeName, status
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM retail_store WHERE store_code = ?",
                Long.class,
                storeCode
        );
    }

    protected Long seedProduct(String productCode, String productName, Long categoryId) {
        jdbcTemplate.update(
                """
                INSERT INTO retail_product (tenant_id, product_code, product_name, category_id, unit_price, status)
                VALUES (1, ?, ?, ?, 100.00, 'active')
                """,
                productCode, productName, categoryId
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM retail_product WHERE product_code = ?",
                Long.class,
                productCode
        );
    }

    protected Long seedCategory(String categoryCode, String categoryName) {
        jdbcTemplate.update(
                """
                INSERT INTO retail_category (tenant_id, category_code, category_name)
                VALUES (1, ?, ?)
                """,
                categoryCode, categoryName
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM retail_category WHERE category_code = ?",
                Long.class,
                categoryCode
        );
    }

    protected void seedInventory(Long storeId, Long productId, String lotNumber, Integer quantity) {
        jdbcTemplate.update(
                """
                INSERT INTO retail_inventory (tenant_id, store_id, product_id, lot_number, quantity)
                VALUES (1, ?, ?, ?, ?)
                """,
                storeId, productId, lotNumber, quantity
        );
    }

    protected void seedSales(Long storeId, String orderNumber, String paymentMethod,
                             java.time.LocalDateTime saleTimestamp, java.math.BigDecimal totalAmount) {
        jdbcTemplate.update(
                """
                INSERT INTO retail_sales (tenant_id, store_id, order_number, payment_method, sale_timestamp, total_amount)
                VALUES (1, ?, ?, ?, ?, ?)
                """,
                storeId, orderNumber, paymentMethod, saleTimestamp, totalAmount
        );
    }

    protected void seedSalesDetail(Long salesId, Long productId, String lotNumber,
                                   Integer quantity, java.math.BigDecimal unitPrice) {
        java.math.BigDecimal subtotal = unitPrice.multiply(java.math.BigDecimal.valueOf(quantity));
        jdbcTemplate.update(
                """
                INSERT INTO retail_sales_detail (tenant_id, sales_id, product_id, lot_number, quantity, unit_price, subtotal)
                VALUES (1, ?, ?, ?, ?, ?, ?)
                """,
                salesId, productId, lotNumber, quantity, unitPrice, subtotal
        );
    }

    protected Long seedDevice(Long storeId, String deviceCode, String deviceType,
                              String deviceName, String status) {
        jdbcTemplate.update(
                """
                INSERT INTO retail_device (tenant_id, store_id, device_code, device_type, device_name, status)
                VALUES (1, ?, ?, ?, ?, ?)
                """,
                storeId, deviceCode, deviceType, deviceName, status
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM retail_device WHERE device_code = ?",
                Long.class,
                deviceCode
        );
    }

    protected void seedAlert(Long storeId, Long productId, String alertType,
                             String status, String priority) {
        jdbcTemplate.update(
                """
                INSERT INTO retail_alert (tenant_id, store_id, product_id, alert_type, status, priority, detected_at)
                VALUES (1, ?, ?, ?, ?, ?, NOW())
                """,
                storeId, productId, alertType, status, priority
        );
    }
}
