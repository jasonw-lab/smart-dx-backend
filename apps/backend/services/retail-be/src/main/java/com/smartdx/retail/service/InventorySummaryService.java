package com.smartdx.retail.service;

import com.smartdx.retail.model.vo.InventorySummaryVO;

import java.util.List;

/**
 * 在庫集約サービスインターフェース（ADR-011）
 */
public interface InventorySummaryService {

    /**
     * 在庫集約一覧（店舗×商品単位）を取得する
     *
     * @param storeId 店舗ID（任意）
     * @param productId 商品ID（任意）
     * @param productName 商品名（任意、部分一致）
     * @param status 集約ステータス（任意、集約後ステータスで絞り込み）
     * @return 集約一覧
     */
    List<InventorySummaryVO> listSummaries(Long storeId, Long productId, String productName, String status);
}
