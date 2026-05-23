package com.smartdx.property.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * LST-EMB-01: 短命embeddingRef エンティティ
 * アップロード画像から抽出した特徴量ベクトルを一時保存
 */
@Getter
@Setter
@TableName("property_embedding_ref")
public class PropertyEmbeddingRef {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * emb-{SHA256prefix16} 形式の短命参照
     */
    private String embeddingRef;

    /**
     * 発行ユーザーID
     */
    private Long userId;

    /**
     * テナントID
     */
    private Long tenantId;

    /**
     * 画像種別: main / sub / layout
     */
    private String flag;

    /**
     * 特徴量抽出モデル名
     */
    private String featureModel;

    /**
     * モデルバージョン
     */
    private String featureVersion;

    /**
     * ベクトル次元数
     */
    private Integer dimension;

    /**
     * 特徴量ベクトル（JSON配列）
     */
    private String vectorJson;

    /**
     * 有効期限（発行から15分）
     */
    private LocalDateTime expiresAt;

    /**
     * 作成日時
     */
    private LocalDateTime createdAt;
}
