package com.smartdx.property.service;

import com.smartdx.property.model.entity.PropertyEmbeddingRef;
import com.smartdx.property.model.vo.PropertyEmbeddingVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * LST-EMB-01: 画像特徴量抽出サービス
 */
public interface PropertyEmbeddingService {

    /**
     * 画像から特徴量を抽出し、短命embeddingRefを発行する
     *
     * @param image 画像ファイル（JPG/PNG、10MB以下）
     * @param flag  画像種別（main/sub/layout）
     * @return embeddingRef と有効期限
     */
    PropertyEmbeddingVO extractAndSave(MultipartFile image, String flag);

    /**
     * embeddingRefから特徴量エンティティを取得（有効期限チェック付き）
     *
     * @param embeddingRef 短命参照キー
     * @return エンティティ（見つからない場合はnull）
     */
    PropertyEmbeddingRef findByRef(String embeddingRef);

    /**
     * 期限切れレコードを削除
     *
     * @return 削除件数
     */
    int cleanupExpired();
}
