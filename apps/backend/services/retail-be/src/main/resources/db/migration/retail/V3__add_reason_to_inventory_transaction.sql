-- ============================================================
-- Add reason column to retail_inventory_transaction
-- ============================================================
-- ADR-011: 廃棄理由などの監査用自由記述（1〜100文字）。
-- Vue 版の既存呼出 API (POST /retail/inventories/{id}/discard) 修復に伴う追加。
-- ============================================================
ALTER TABLE `retail_inventory_transaction`
  ADD COLUMN `reason` varchar(100) DEFAULT NULL COMMENT 'Reason (e.g. disposal reason)' AFTER `source_type`;
