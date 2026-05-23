package com.smartdx.property.service;

import com.smartdx.property.model.req.SimilarPropertyReq;
import com.smartdx.property.model.vo.SimilarPropertyResponse;

/**
 * LST-SIM-01: 類似物件検索サービス（v2.0.0）
 * 複数検索起点対応: propertyId, imageId, demoRef
 */
public interface SimilarPropertyService {

    /**
     * 複数検索起点から類似物件を検索（v2.0.0）
     *
     * @param req 検索リクエスト（propertyId/imageId/demoRef のいずれか必須）
     * @return 類似物件検索結果
     */
    SimilarPropertyResponse findSimilar(SimilarPropertyReq req);

    /**
     * 基準物件のmain画像ベクトルを使って類似物件を検索（後方互換）
     *
     * @param propertyKey 基準物件キー（UUID v4）
     * @param topK        取得件数（1〜50）
     * @param minScore    最低類似度スコア（0.0〜1.0）
     * @return 類似物件検索結果
     * @deprecated 新しい findSimilar(SimilarPropertyReq) を使用してください
     */
    @Deprecated
    default SimilarPropertyResponse findSimilar(String propertyKey, int topK, double minScore) {
        SimilarPropertyReq req = new SimilarPropertyReq();
        req.setPropertyId(propertyKey);
        req.setLimit(topK);
        req.setMinScore(minScore);
        req.setExcludeSameProperty(true);
        return findSimilar(req);
    }
}
