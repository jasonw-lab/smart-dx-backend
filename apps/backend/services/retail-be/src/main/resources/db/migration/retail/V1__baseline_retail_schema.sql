-- ============================================================
-- Smart Retail Database Schema (Multi-tenant)
-- Baseline migration for retail-be
-- ============================================================
-- All tables include tenant_id column for multi-tenant support
-- retail-be runs in force-default mode (tenant_id = 1)
-- ============================================================

-- ============================================================
-- 1. Master Tables
-- ============================================================

-- ----------------------------
-- 1.1 Store Master (retail_store)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `retail_store` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Store ID (PK)',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT 'Tenant ID',
  `store_code` varchar(20) NOT NULL COMMENT 'Store code (unique within tenant)',
  `store_name` varchar(100) NOT NULL COMMENT 'Store name',
  `address` varchar(255) DEFAULT NULL COMMENT 'Address',
  `phone` varchar(20) DEFAULT NULL COMMENT 'Phone number',
  `manager` varchar(50) DEFAULT NULL COMMENT 'Manager name',
  `status` enum('ONLINE','MAINTENANCE','OFFLINE') DEFAULT 'ONLINE' COMMENT 'Status',
  `opening_hours` varchar(100) DEFAULT NULL COMMENT 'Opening hours',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `create_by` bigint DEFAULT NULL COMMENT 'Created by',
  `update_by` bigint DEFAULT NULL COMMENT 'Updated by',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT 'Deleted flag',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_store_code` (`tenant_id`, `store_code`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Store Master';

-- ----------------------------
-- 1.2 Category Master (retail_category)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `retail_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Category ID (PK)',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT 'Tenant ID',
  `category_code` varchar(20) NOT NULL COMMENT 'Category code',
  `category_name` varchar(50) NOT NULL COMMENT 'Category name',
  `parent_id` bigint DEFAULT NULL COMMENT 'Parent category ID',
  `sort_order` int DEFAULT '0' COMMENT 'Sort order',
  `description` varchar(255) DEFAULT NULL COMMENT 'Description',
  `status` varchar(20) DEFAULT 'active' COMMENT 'Status',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `create_by` bigint DEFAULT NULL COMMENT 'Created by',
  `update_by` bigint DEFAULT NULL COMMENT 'Updated by',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT 'Deleted flag',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_category_code` (`tenant_id`, `category_code`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Category Master';

-- ----------------------------
-- 1.3 Product Master (retail_product)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `retail_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Product ID (PK)',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT 'Tenant ID',
  `product_code` varchar(30) NOT NULL COMMENT 'Product code',
  `product_name` varchar(100) NOT NULL COMMENT 'Product name',
  `barcode` varchar(50) DEFAULT NULL COMMENT 'Barcode (JAN code)',
  `category_id` bigint DEFAULT NULL COMMENT 'Category ID',
  `category_name` varchar(50) DEFAULT NULL COMMENT 'Category name (denormalized)',
  `unit_price` decimal(10,2) NOT NULL COMMENT 'Unit price (incl. tax)',
  `cost_price` decimal(10,2) DEFAULT NULL COMMENT 'Cost price',
  `unit` varchar(20) DEFAULT NULL COMMENT 'Unit (pcs, kg, etc.)',
  `reorder_point` int NOT NULL DEFAULT 0 COMMENT 'Reorder point',
  `max_stock` int NOT NULL DEFAULT 0 COMMENT 'Max stock threshold',
  `shelf_life_days` int DEFAULT NULL COMMENT 'Shelf life in days',
  `supplier_id` bigint DEFAULT NULL COMMENT 'Supplier ID',
  `supplier_name` varchar(100) DEFAULT NULL COMMENT 'Supplier name (denormalized)',
  `description` varchar(500) DEFAULT NULL COMMENT 'Product description',
  `image_url` varchar(255) DEFAULT NULL COMMENT 'Product image URL',
  `status` varchar(20) DEFAULT 'active' COMMENT 'Status (active/inactive)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `create_by` bigint DEFAULT NULL COMMENT 'Created by',
  `update_by` bigint DEFAULT NULL COMMENT 'Updated by',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT 'Deleted flag',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_product_code` (`tenant_id`, `product_code`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_supplier_id` (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Product Master';

-- ============================================================
-- 2. Inventory Management Tables
-- ============================================================

-- ----------------------------
-- 2.1 Inventory Table (retail_inventory)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `retail_inventory` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Inventory ID (PK)',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT 'Tenant ID',
  `store_id` bigint NOT NULL COMMENT 'Store ID',
  `product_id` bigint NOT NULL COMMENT 'Product ID',
  `lot_number` varchar(50) NOT NULL COMMENT 'Lot number',
  `quantity` int NOT NULL DEFAULT '0' COMMENT 'Current quantity',
  `expiry_date` date DEFAULT NULL COMMENT 'Expiry date',
  `received_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Received at',
  `location` varchar(50) DEFAULT NULL COMMENT 'Storage location',
  `status` enum('normal','low','high','expired','out_of_stock') DEFAULT 'normal' COMMENT 'Inventory status',
  `last_count_date` datetime DEFAULT NULL COMMENT 'Last inventory count date',
  `remarks` varchar(500) DEFAULT NULL COMMENT 'Remarks',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `create_by` bigint DEFAULT NULL COMMENT 'Created by',
  `update_by` bigint DEFAULT NULL COMMENT 'Updated by',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT 'Deleted flag',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_store_product_lot` (`tenant_id`, `store_id`, `product_id`, `lot_number`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_status` (`status`),
  KEY `idx_expiry_date` (`expiry_date`),
  KEY `idx_fefo` (`store_id`, `product_id`, `expiry_date`, `received_at`),
  CONSTRAINT `chk_inventory_quantity` CHECK (`quantity` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Inventory Table (lot-based)';

-- ----------------------------
-- 2.2 Inventory Transaction Table (retail_inventory_transaction)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `retail_inventory_transaction` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Transaction ID (PK)',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT 'Tenant ID',
  `inventory_id` bigint NOT NULL COMMENT 'Inventory ID',
  `store_id` bigint NOT NULL COMMENT 'Store ID (denormalized)',
  `product_id` bigint NOT NULL COMMENT 'Product ID (denormalized)',
  `lot_number` varchar(50) NOT NULL COMMENT 'Lot number (denormalized)',
  `txn_type` enum('INBOUND','SALE','ADJUSTMENT','DISPOSAL','TRANSFER_IN','TRANSFER_OUT') NOT NULL COMMENT 'Transaction type',
  `quantity_delta` int NOT NULL COMMENT 'Quantity change (+ increase, - decrease)',
  `source_type` enum('MANUAL','POS','BATCH') NOT NULL DEFAULT 'MANUAL' COMMENT 'Source type',
  `reference_no` varchar(100) DEFAULT NULL COMMENT 'Reference number',
  `occurred_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Occurred at',
  `note` varchar(500) DEFAULT NULL COMMENT 'Note',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `create_by` bigint DEFAULT NULL COMMENT 'Created by',
  `update_by` bigint DEFAULT NULL COMMENT 'Updated by',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT 'Deleted flag',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_inventory_id` (`inventory_id`, `occurred_at`),
  KEY `idx_store_product_time` (`store_id`, `product_id`, `occurred_at`),
  KEY `idx_txn_type` (`txn_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Inventory Transaction Table';

-- ============================================================
-- 3. Device Management Table
-- ============================================================

-- ----------------------------
-- 3.1 Device Master (retail_device)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `retail_device` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Device ID (PK)',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT 'Tenant ID',
  `store_id` bigint NOT NULL COMMENT 'Store ID',
  `device_code` varchar(50) NOT NULL COMMENT 'Device code',
  `device_type` enum('PAYMENT_TERMINAL','CAMERA','GATE','REFRIGERATOR_SENSOR','PRINTER','NETWORK_ROUTER') NOT NULL COMMENT 'Device type',
  `device_name` varchar(100) NOT NULL COMMENT 'Device name',
  `status` enum('ONLINE','OFFLINE','ERROR','MAINTENANCE') DEFAULT 'ONLINE' COMMENT 'Status',
  `last_heartbeat` datetime DEFAULT NULL COMMENT 'Last heartbeat',
  `error_code` varchar(50) DEFAULT NULL COMMENT 'Error code',
  `metadata` json DEFAULT NULL COMMENT 'Device metadata (JSON)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `create_by` bigint DEFAULT NULL COMMENT 'Created by',
  `update_by` bigint DEFAULT NULL COMMENT 'Updated by',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT 'Deleted flag',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_device_code` (`tenant_id`, `device_code`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_device_store` (`store_id`),
  KEY `idx_device_status` (`status`),
  KEY `idx_device_heartbeat` (`last_heartbeat`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Device Master';

-- ============================================================
-- 4. Alert Management Table
-- ============================================================

-- ----------------------------
-- 4.1 Alert Table (retail_alert)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `retail_alert` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Alert ID (PK)',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT 'Tenant ID',
  `store_id` bigint NOT NULL COMMENT 'Store ID',
  `product_id` bigint DEFAULT NULL COMMENT 'Product ID (for inventory alerts)',
  `device_id` bigint DEFAULT NULL COMMENT 'Device ID (for device alerts)',
  `lot_number` varchar(50) DEFAULT NULL COMMENT 'Lot number',
  `alert_type` enum('LOW_STOCK','EXPIRY_SOON','HIGH_STOCK','COMMUNICATION_DOWN','PAYMENT_TERMINAL_DOWN','CARD_READER_ERROR','PRINTER_PAPER_EMPTY') NOT NULL COMMENT 'Alert type',
  `priority` enum('P1','P2','P3','P4') NOT NULL COMMENT 'Priority',
  `status` enum('NEW','ACK','IN_PROGRESS','RESOLVED','CLOSED') DEFAULT 'NEW' COMMENT 'Status',
  `message` varchar(500) DEFAULT NULL COMMENT 'Alert message',
  `threshold_value` varchar(50) DEFAULT NULL COMMENT 'Threshold value',
  `current_value` varchar(50) DEFAULT NULL COMMENT 'Current value at detection',
  `detected_at` datetime NOT NULL COMMENT 'Detected at',
  `acknowledged_at` datetime DEFAULT NULL COMMENT 'Acknowledged at',
  `resolved_at` datetime DEFAULT NULL COMMENT 'Resolved at',
  `closed_at` datetime DEFAULT NULL COMMENT 'Closed at',
  `resolution_note` text DEFAULT NULL COMMENT 'Resolution note',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `create_by` bigint DEFAULT NULL COMMENT 'Created by',
  `update_by` bigint DEFAULT NULL COMMENT 'Updated by',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT 'Deleted flag',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_alert_store_status` (`store_id`, `status`, `detected_at`),
  KEY `idx_alert_type_priority` (`alert_type`, `priority`),
  KEY `idx_alert_detected_at` (`detected_at`),
  KEY `idx_alert_product` (`product_id`),
  KEY `idx_alert_device` (`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Alert Table';

-- ============================================================
-- 5. Sales Management Tables
-- ============================================================

-- ----------------------------
-- 5.1 Sales Header Table (retail_sales)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `retail_sales` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Sales ID (PK)',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT 'Tenant ID',
  `store_id` bigint NOT NULL COMMENT 'Store ID',
  `order_number` varchar(50) NOT NULL COMMENT 'Order number',
  `total_amount` decimal(10,2) NOT NULL COMMENT 'Total amount (incl. tax)',
  `payment_method` enum('CASH','CARD','QR','OTHER') NOT NULL COMMENT 'Payment method',
  `payment_provider` varchar(50) DEFAULT NULL COMMENT 'Payment provider',
  `payment_reference_id` varchar(100) DEFAULT NULL COMMENT 'Payment reference ID',
  `sale_timestamp` datetime NOT NULL COMMENT 'Sale timestamp',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `create_by` bigint DEFAULT NULL COMMENT 'Created by',
  `update_by` bigint DEFAULT NULL COMMENT 'Updated by',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT 'Deleted flag',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_order_number` (`tenant_id`, `order_number`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_sale_timestamp` (`sale_timestamp`),
  KEY `idx_store_sale_time` (`store_id`, `sale_timestamp`),
  KEY `idx_payment_method` (`payment_method`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Sales Header Table';

-- ----------------------------
-- 5.2 Sales Detail Table (retail_sales_detail)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `retail_sales_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Sales Detail ID (PK)',
  `tenant_id` bigint NOT NULL DEFAULT 1 COMMENT 'Tenant ID',
  `sales_id` bigint NOT NULL COMMENT 'Sales ID',
  `product_id` bigint NOT NULL COMMENT 'Product ID',
  `lot_number` varchar(50) NOT NULL COMMENT 'Lot number',
  `quantity` int NOT NULL COMMENT 'Quantity',
  `unit_price` decimal(10,2) NOT NULL COMMENT 'Unit price at sale',
  `subtotal` decimal(10,2) NOT NULL COMMENT 'Subtotal',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'Created at',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Updated at',
  `create_by` bigint DEFAULT NULL COMMENT 'Created by',
  `update_by` bigint DEFAULT NULL COMMENT 'Updated by',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT 'Deleted flag',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_sales_id` (`sales_id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Sales Detail Table';

-- ============================================================
-- END OF BASELINE MIGRATION
-- ============================================================
