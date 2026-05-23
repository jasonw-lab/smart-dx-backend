package com.smartdx.property.service;

import com.smartdx.property.model.vo.PropertyAssetSignedUrlVO;

/**
 * LST-AST-01/02/03: アセット署名付きURL取得サービス
 */
public interface PropertyAssetService {

    /**
     * 原寸画像の署名付きURLを取得（LST-AST-01）
     *
     * @param assetKey アセットキー
     * @return 署名付きURL（60分有効）
     */
    PropertyAssetSignedUrlVO getOriginalUrl(String assetKey);

    /**
     * サムネイル画像の署名付きURLを取得（LST-AST-02）
     *
     * @param assetKey アセットキー
     * @param size     サイズ（sm/md/lg）
     * @return 署名付きURL（60分有効）
     */
    PropertyAssetSignedUrlVO getThumbnailUrl(String assetKey, String size);

    /**
     * ドキュメントの署名付きURLを取得（LST-AST-03）
     *
     * @param docKey ドキュメントキー
     * @return 署名付きURL（60分有効）
     */
    PropertyAssetSignedUrlVO getDocumentUrl(String docKey);
}
