package com.smartdx.property.model.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * LST-EMB-01: 画像特徴量抽出レスポンス
 */
@Getter
@Setter
@Builder
public class PropertyEmbeddingVO {

    /**
     * 短命参照キー（emb-{SHA256prefix16} 形式）
     */
    private String embeddingRef;

    /**
     * 有効期限（ISO8601形式）
     */
    private String expiresAt;

    /**
     * ベクトル次元数
     */
    private Integer dimension;
}
